package kr.voicemate.malitda.ui.v2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kr.voicemate.malitda.data.ai.R4Client as R4
import kr.voicemate.malitda.data.db.MeaningEntity
import kr.voicemate.malitda.tts.TtsState
import kr.voicemate.malitda.ui.vm.SessionEvent
import kr.voicemate.malitda.ui.vm.SessionViewModel
import kr.voicemate.malitda.ui.vm.SessionViewModel.V2Origin

/**
 * v2(42화면) 학생 핵심 흐름 호스트: U00 안내 → U01 홈 → U02 녹음/처리 → U03 뜻 확인 → U04 전달.
 * U06 다른 뜻 선택과 예외 변형(U02-D, U03-E/M/V, U04-F/S)을 포함한다.
 * 등록(U05·U07)·내 표현(U08~U11)·설정(U12)·소리 확인(U13)은 다음 단계.
 */
/** 그림 만들기 화면에 보여 줄 서버 상태 안내. null이면 생성할 수 있다. */
private fun r4Note(s: R4.Status?): String? = when {
    s == null -> "PC 서버 연결을 확인하고 있어요…"
    !s.reachable -> "PC 서버에 연결되지 않았어요.\nPC에서 R4 서버를 켜고 adb reverse로 연결해 주세요."
    s.reviewMode -> "PC 서버가 점검 모드예요.\n새 그림은 만들 수 없고, 사진이나 기본 그림을 쓸 수 있어요."
    !s.ready -> "PC 서버에 그림 생성 키가 설정되지 않았어요."
    else -> null
}

@Composable
fun MalitdaV2App(vm: SessionViewModel) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var route by rememberSaveable { mutableStateOf("U00") }
    var soundless by rememberSaveable { mutableStateOf(false) }
    // 지원자 등록 폼(U05) 작성 상태
    var regExpression by rememberSaveable { mutableStateOf("") }
    var regMeaning by rememberSaveable { mutableStateOf("") }
    var regImageKey by rememberSaveable { mutableStateOf<String?>(null) }
    var regSituation by rememberSaveable { mutableStateOf("") }
    var regShowError by rememberSaveable { mutableStateOf(false) }
    var regSavedId by rememberSaveable { mutableStateOf(-1L) }
    var regChecked by rememberSaveable { mutableStateOf(false) }
    // 수정 모드(U09-E)면 원본 id, 아니면 -1
    var editId by rememberSaveable { mutableStateOf(-1L) }
    // U10·U07-S·U13을 어디서 열었는지(돌아갈 곳)
    var u10Return by rememberSaveable { mutableStateOf("U07") }
    var u07sReturn by rememberSaveable { mutableStateOf("U07") }
    var u13Return by rememberSaveable { mutableStateOf("U04") }
    var u13Text by rememberSaveable { mutableStateOf("") }
    var u13ImageKey by rememberSaveable { mutableStateOf<String?>(null) }

    /** 소리 확인(U13) 열기: 화면 진입 후 재생한다. */
    fun openSound(text: String, imageKey: String?, from: String) {
        u13Text = text; u13ImageKey = imageKey; u13Return = from
        route = "U13"
    }
    // 내 표현(U08)에서 열어 본 뜻
    var libId by rememberSaveable { mutableStateOf(-1L) }
    var libExpression by rememberSaveable { mutableStateOf("") }

    var regConflict by rememberSaveable { mutableStateOf<String?>(null) }
    // 저장 대상이 아니다: 앱이 재시작되면 진행 중이던 요청도 사라지므로 '만드는 중'에 갇히지 않게.
    var generating by remember { mutableStateOf(false) }
    // 그림 생성 회차. 그만두거나 등록을 끝내면 올려서, 늦게 도착한 그림이 다른 폼에 붙지 않게 한다.
    var genRound by remember { mutableStateOf(0) }
    // R4 서버(팀원 그림 생성 서버) 상태·동의·그림체 미리보기
    var r4Status by remember { mutableStateOf<R4.Status?>(null) }
    var r4CheckRound by remember { mutableStateOf(0) }
    var r4Consent by remember { mutableStateOf(false) }
    var stylePreviews by remember { mutableStateOf<Map<String, ByteArray>>(emptyMap()) }
    var u15Return by rememberSaveable { mutableStateOf("U12") }

    fun resetRegForm() {
        regExpression = ""; regMeaning = ""; regImageKey = null; regSituation = ""
        regShowError = false; regSavedId = -1L; regChecked = false; editId = -1L; regConflict = null
        genRound++; generating = false
    }

    /** 표현 칸은 쉼표·줄바꿈으로 여러 개를 받는다. */
    fun parseExpressions(s: String): List<String> =
        s.split(",", "\n").map { it.trim() }.filter { it.isNotBlank() }.distinct()

    val ui by vm.ui.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val v2 by vm.v2.collectAsStateWithLifecycle()
    val meanings by vm.meanings.collectAsStateWithLifecycle()
    val name by vm.profileName.collectAsStateWithLifecycle()
    val speaking by vm.ttsSpeaking.collectAsStateWithLifecycle()
    val artStyle by vm.artStyle.collectAsStateWithLifecycle()
    val ttsState by vm.ttsState.collectAsStateWithLifecycle()

    fun toast(m: String) = Toast.makeText(ctx, m, Toast.LENGTH_SHORT).show()
    var helpOpen by remember { mutableStateOf(false) }
    fun help() { helpOpen = true }

    // 선택된 뜻을 계속 관찰한다(수정·삭제 감지 → U03-V).
    var meaning by remember { mutableStateOf<MeaningEntity?>(null) }
    LaunchedEffect(v2.meaningId) {
        meaning = null
        v2.meaningId?.let { id -> vm.v2ObserveMeaning(id).collect { meaning = it } }
    }
    // 내 표현 상세(U09)에서 보는 뜻
    var libMeaning by remember { mutableStateOf<MeaningEntity?>(null) }
    LaunchedEffect(libId) {
        libMeaning = null; libExpression = ""
        if (libId > 0) {
            // 뜻이 바뀔 때마다 표현 목록도 다시 읽는다(수정 뒤 옛 표현이 남지 않게).
            vm.v2ObserveMeaning(libId).collect {
                libExpression = vm.v2ExpressionsText(libId)
                libMeaning = it
            }
        }
    }
    val archived by vm.meaningsArchived.collectAsStateWithLifecycle()
    val profilesList by vm.profiles.collectAsStateWithLifecycle()
    val profileId by vm.profileId.collectAsStateWithLifecycle()

    /** 보관함·사용 중지 뜻을 U10 재확인으로 보낼 준비. */
    fun startReconfirm(m: MeaningEntity, from: String) {
        scope.launch {
            regExpression = vm.v2ExpressionsText(m.id)
            regMeaning = m.text; regImageKey = m.imageKey; regSituation = m.situation.orEmpty()
            regSavedId = m.id; regChecked = false; u10Return = from
            route = "U10"
        }
    }

    fun goHome() {
        vm.resetSession()
        soundless = false
        route = "U01"
    }

    // 첫 실행에만 안내(U00)로 시작한다.
    LaunchedEffect(Unit) { if (vm.c.settings.current().onboarded) route = "U01" }
    // 화면을 떠나면 읽어주던 소리를 멈춘다.
    LaunchedEffect(route) { vm.stopSpeaking() }

    val micPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { route = "U02"; vm.startListening() } else route = "U02D"
    }
    // 의미 그림으로 쓸 사진 선택(앱 전용 저장소로 복사해 참조만 보관).
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            runCatching { kr.voicemate.malitda.data.repo.MeaningImageStore.import(ctx, uri) }
                .onSuccess { key -> regImageKey = key }
                .onFailure { e -> toast(e.message ?: "사진을 불러오지 못했어요") }
        }
    }
    fun hasMic() = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    fun startMic() {
        vm.prepareRespeak()
        vm.v2ClearHeard()
        vm.trialStartVoice()
        val files = vm.testAudioFiles()
        // 에뮬레이터 등 평가음원이 있으면 마이크 없이 같은 인식 흐름을 탄다.
        if (files.isNotEmpty()) { route = "U02"; vm.recognizeTestFile(files.first()); return }
        if (hasMic()) { route = "U02"; vm.startListening() } else micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    fun openAppSettings() {
        runCatching {
            ctx.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
    fun openTtsSettings() {
        val ok = runCatching {
            ctx.startActivity(Intent("com.android.settings.TTS_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
        if (!ok) openAppSettings()
    }
    fun shareMeaning(m: MeaningEntity) {
        val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, m.text) }
        runCatching { ctx.startActivity(Intent.createChooser(send, "공유할 앱 선택").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { toast("공유할 수 있는 앱이 없어요") }
    }

    // 인식 이벤트 → v2 라우팅. 인식 결과는 말하기 화면(U02)에 있을 때만 받는다 —
    // 학생이 그사이 카드로 고른 뜻을 늦게 도착한 결과가 덮어쓰면 안 된다.
    LaunchedEffect(Unit) {
        vm.events.collect { e ->
            when (e) {
                SessionEvent.GoResult -> if (route == "U02") {
                    val matches = vm.v2ResolveFromStt()
                    if (route == "U02") route = if (matches.isNotEmpty()) "U03" else "U03M"
                }
                SessionEvent.GoNoResult -> if (route == "U02") route = "U03E"
                is SessionEvent.GoSttError -> if (route == "U02") { toast(e.message); route = "U03E" }
                SessionEvent.GoTtsError ->
                    if (route == "U04" || (route == "U13" && u13Return == "U04")) route = "U04F"
                    else toast("소리를 사용할 수 없어요")
                is SessionEvent.Toast -> toast(e.message)
                else -> Unit // v1 전용 이벤트(GoConfirm 등)는 v2 흐름에서 쓰지 않는다.
            }
        }
    }

    // 홈과 첫 실행 안내에서는 시스템 기본(앱 종료)을 따른다.
    BackHandler(enabled = route != "U01" && !(route == "U00" && !settings.onboarded)) {
        when (route) {
            "U00" -> route = "U01"
            "U02" -> { vm.cancelListening(); route = "U01" }
            "U04" -> route = "U03"
            "U04F", "U04S" -> route = "U04"
            "U06" -> route = if (v2.meaningId != null) "U03" else "U01"
            "U05" -> route = if (editId > 0) "U09" else "U07"
            "U05M" -> route = "U05"
            "U07S" -> route = u07sReturn
            "U10" -> { toast("미확인으로 두었어요"); val r = u10Return; resetRegForm(); route = r }
            "U08" -> route = "U01"
            "U09" -> route = "U08"
            "U09D" -> route = "U09"
            "U11" -> route = "U08"
            "U12" -> route = "U01"
            "U12P", "U12T", "U14" -> route = "U12"
            "U15" -> route = u15Return
            "U13" -> route = u13Return
            else -> goHome()
        }
    }

    // U03·U04에서 보던 뜻이 수정되면(U03-V) 알린다. 선택 당시 updatedAt과 비교.
    val m = meaning
    val changed = m != null && v2.snapshotUpdatedAt != null &&
        (m.updatedAt != v2.snapshotUpdatedAt || !m.confirmed || !m.active)
    val trialDraft by vm.trialDraft.collectAsStateWithLifecycle()
    val trialLogs by vm.trialLogs.collectAsStateWithLifecycle()

    if (helpOpen) V2HelpDialog(helpTextFor(route)) { helpOpen = false }

    when (route) {
        "U00" -> U00Guide(
            onStart = {
                scope.launch { vm.c.settings.setOnboarded(true) }
                route = "U01"
            },
            onPrepare = {
                scope.launch { vm.c.settings.setOnboarded(true) }
                route = "U07"
            },
            onBack = null,
            onHelp = ::help,
        )

        "U01" -> U01Home(
            name = name,
            meanings = meanings,
            onSpeak = { startMic() },
            onCard = { picked -> vm.trialStartCard(); vm.v2SelectMeaning(picked, V2Origin.CARD); route = "U03" },
            onRegister = { route = "U07" },
            onGuide = { route = "U00" },
            onHelp = ::help,
            onLibrary = { route = "U08" },
            onSettings = { route = "U12" },
        )

        "U02" -> U02Listen(
            listen = ui.listen,
            onStop = { vm.stopListening() },
            onPickCards = { vm.cancelListening(); route = "U06" },
            onCancel = { vm.cancelListening(); goHome() },
            onHelp = ::help,
        )

        "U02D" -> U02MicDenied(
            onPickCards = { route = "U06" },
            onHowToEnable = { route = "U12P" },
            onCancel = { goHome() },
            onHelp = ::help,
        )

        "U03" -> when {
            m == null -> V2Screen {} // 불러오는 중(순간)
            changed -> U03Changed(
                onRecheck = { scope.launch { route = if (vm.v2RefreshSnapshot()) "U03" else "U06" } },
                onHome = { goHome() },
                onHelp = ::help,
            )
            else -> U03Confirm(
                meaning = m,
                origin = v2.origin,
                studentName = name,
                onListen = { openSound(m.text, m.imageKey, from = "U03") },
                onYes = { vm.v2ConfirmUse(m.text); soundless = false; route = "U04" },
                onOther = { vm.trialOtherMeaning(); route = "U06" },
                onBack = { goHome() },
                onHelp = ::help,
            )
        }

        "U03E" -> U03NoHear(
            onRespeak = { startMic() },
            onPickCards = { route = "U06" },
            onCancel = { goHome() },
            onHelp = ::help,
        )

        "U03M" -> U03NoMeaning(
            origin = v2.origin,
            heard = if (v2.origin == V2Origin.VOICE) ui.raw else null,
            onPickCards = { route = "U06" },
            onRegister = {
                // 앱이 들은 말을 표현 칸에 미리 채운다 — 이 학생 목소리에서 나온 그대로(오인식 포함) 등록할 수 있게.
                val heard = if (v2.origin == V2Origin.VOICE) ui.raw?.trim()?.trimEnd('.', '!', '?') else null
                resetRegForm()
                if (!heard.isNullOrBlank()) { regExpression = heard; route = "U05" } else route = "U07"
            },
            onRespeak = { startMic() },
            onBack = { goHome() },
            onHelp = ::help,
        )

        "U04" -> when {
            m == null -> V2Screen {}
            changed -> U03Changed(
                onRecheck = { scope.launch { route = if (vm.v2RefreshSnapshot()) "U03" else "U06" } },
                onHome = { goHome() },
                onHelp = ::help,
            )
            else -> U04Deliver(
                meaning = m,
                speaking = speaking,
                soundless = soundless || ttsState is TtsState.Unavailable,
                onSound = { if (ttsState is TtsState.Unavailable) route = "U04F" else openSound(m.text, m.imageKey, from = "U04") },
                onRepick = { route = "U06" },
                onShare = { shareMeaning(m); route = "U04S" },
                onBack = { route = "U03" },
                onHelp = ::help,
                // 말로 시도했는데 학생이 카드로 뜻을 고쳐 확정한 경우에만 제안한다.
                learnHeard = if (v2.origin == V2Origin.CARD) v2.heard else null,
                onLearn = {
                    scope.launch {
                        val heard = v2.heard
                        when (val r = vm.v2LearnHeard()) {
                            is kr.voicemate.malitda.data.repo.MeaningRepository.AddExpressionResult.Added ->
                                toast("다음부터 “$heard”도 '${m.text}'(으)로 알아들어요")
                            is kr.voicemate.malitda.data.repo.MeaningRepository.AddExpressionResult.AlreadyThere ->
                                toast("이미 이 뜻에 등록된 표현이에요")
                            is kr.voicemate.malitda.data.repo.MeaningRepository.AddExpressionResult.TakenBy ->
                                toast("“$heard”은(는) 이미 '${r.meaningText}'에 등록돼 있어요")
                            null -> Unit
                        }
                    }
                },
                trialUnderstood = if (settings.trialLogging) trialDraft?.understood else null,
                trialHelped = trialDraft?.supporterHelped ?: false,
                onTrialUnderstood = { vm.trialSetUnderstood(it) },
                onTrialHelped = { vm.trialSetHelped(it) },
            )
        }

        "U04F" -> m?.let {
            U04NoSound(
                meaning = it,
                onKeepVisual = { soundless = true; route = "U04" },
                onSoundSettings = { route = "U12T" },
                onBack = { route = "U04" },
                onHelp = ::help,
            )
        } ?: V2Screen {}

        "U04S" -> m?.let {
            U04Share(
                meaning = it,
                onReturn = { route = "U04" },
                // 안내문("선택을 취소해도 뜻은 그대로예요")대로 전달 화면으로 돌아간다.
                onCancel = { route = "U04" },
                onHelp = ::help,
            )
        } ?: V2Screen {}

        // ---------- 지원자 등록 흐름 (U07 → U05 → U10) ----------
        "U07" -> U07Entry(
            studentName = name,
            onRegister = { resetRegForm(); route = "U05" },
            onMyExpressions = { route = "U08" },
            onStudentChip = { u07sReturn = "U07"; route = "U07S" },
            onBack = { route = "U01" },
            onHelp = ::help,
        )

        "U07S" -> U07NewSession(
            studentName = name,
            others = profilesList.filter { it.id != profileId }.map { it.id to it.name },
            onRestart = { vm.resetSession(); resetRegForm(); toast("새로 시작해요"); route = "U01" },
            onSwitch = { id -> vm.v2SwitchStudent(id); resetRegForm(); toast("학생을 바꿨어요"); route = "U01" },
            onAdd = { newName -> vm.v2AddStudent(newName); resetRegForm(); toast("${newName} 학생으로 시작해요"); route = "U01" },
            onCancel = { route = u07sReturn },
            onHelp = ::help,
            onRename = { newName -> vm.v2RenameStudent(newName); toast("이름을 '$newName'(으)로 바꿨어요") },
        )

        "U05" -> U05Register(
            expression = regExpression,
            meaning = regMeaning,
            imageKey = regImageKey,
            situation = regSituation,
            showError = regShowError,
            conflictMessage = regConflict,
            imageGenerating = generating,
            onExpression = { regExpression = it; regConflict = null },
            onMeaning = { regMeaning = it },
            onSituation = { regSituation = it },
            onPickImage = { route = "U05M" },
            onConfirmWithStudent = {
                if (generating) { toast("그림이 다 만들어지면 저장할 수 있어요"); return@U05Register }
                if (regExpression.isBlank() || regMeaning.isBlank()) { regShowError = true; return@U05Register }
                scope.launch {
                    val exprs = parseExpressions(regExpression)
                    val conflicts = vm.v2FindConflicts(exprs, excludeMeaningId = if (editId > 0) editId else 0L)
                    if (conflicts.isNotEmpty()) {
                        regConflict = conflicts.joinToString("\n") { "'${it.expression}'은(는) 이미 '${it.meaningText}'에 등록돼 있어요." }
                        return@launch
                    }
                    if (editId > 0) {
                        vm.v2UpdateMeaning(editId, exprs, regMeaning, regImageKey, regSituation)
                        regSavedId = editId; u10Return = "U09"
                    } else {
                        regSavedId = vm.v2RegisterMeaning(exprs, regMeaning, regImageKey, regSituation)
                        u10Return = "U07"
                    }
                    regChecked = false
                    route = "U10"
                }
            },
            onSaveUnconfirmed = {
                if (generating) { toast("그림이 다 만들어지면 저장할 수 있어요"); return@U05Register }
                if (regExpression.isBlank() || regMeaning.isBlank()) { regShowError = true; return@U05Register }
                scope.launch {
                    val exprs = parseExpressions(regExpression)
                    val conflicts = vm.v2FindConflicts(exprs, excludeMeaningId = if (editId > 0) editId else 0L)
                    if (conflicts.isNotEmpty()) {
                        regConflict = conflicts.joinToString("\n") { "'${it.expression}'은(는) 이미 '${it.meaningText}'에 등록돼 있어요." }
                        return@launch
                    }
                    if (editId > 0) {
                        vm.v2UpdateMeaning(editId, exprs, regMeaning, regImageKey, regSituation)
                        toast("수정했어요. 학생과 확인하면 다시 쓸 수 있어요.")
                        resetRegForm(); route = "U09"
                    } else {
                        vm.v2RegisterMeaning(exprs, regMeaning, regImageKey, regSituation)
                        toast("미확인으로 저장했어요. 학생과 확인하면 카드로 나와요.")
                        resetRegForm(); route = "U07"
                    }
                }
            },
            onRetry = { regShowError = false },
            onCancel = { val back = if (editId > 0) "U09" else "U07"; resetRegForm(); route = back },
            onHelp = ::help,
        )

        "U05M" -> {
          // 화면에 들어올 때 PC 서버 상태를 확인한다(꺼짐·점검 모드면 생성 대신 안내).
          LaunchedEffect(r4CheckRound) { if (R4.isEnabled) r4Status = R4.status() }
          U05PickImage(
            meaningText = regMeaning,
            selectedKey = regImageKey,
            onSelect = { regImageKey = it },
            onPickPhoto = { photoLauncher.launch("image/*") },
            onUse = { route = "U05" },
            onCancel = { route = "U05" },
            onHelp = ::help,
            serverNote = r4Note(r4Status),
            styleLabel = kr.voicemate.malitda.domain.ArtStyles.label(artStyle),
            onChangeStyle = { u15Return = "U05M"; route = "U15" },
            consent = r4Consent,
            onConsent = { r4Consent = it },
            onRecheckServer = { r4CheckRound++ },
            onGenerate = if (!R4.isEnabled) null else ({
                if (regMeaning.isBlank()) {
                    toast("먼저 '전할 뜻'을 입력해 주세요")
                } else if (!generating) {
                    generating = true
                    val round = ++genRound
                    scope.launch {
                        val result = runCatching { vm.r4GenerateMeaningImage(ctx, regMeaning) }
                        // 그사이 그만뒀거나 등록을 끝냈다면 결과를 버린다(파일은 다음 정리 때 지워진다).
                        if (round != genRound) return@launch
                        generating = false
                        // 한 번 생성할 때마다 동의를 다시 받는다(비용이 드는 동작).
                        r4Consent = false
                        result.onSuccess { key -> regImageKey = key; toast("그림을 만들었어요. 학생과 함께 확인해 주세요.") }
                            .onFailure { e ->
                                toast(e.message ?: "그림을 만들지 못했어요")
                                r4CheckRound++
                            }
                    }
                }
            }),
            generating = generating,
            onCancelGenerate = { genRound++; generating = false; toast("그림 만들기를 그만뒀어요") },
          )
        }

        "U15" -> {
            LaunchedEffect(Unit) {
                if (R4.isEnabled) {
                    r4Status = R4.status()
                    if (r4Status?.reachable == true && stylePreviews.isEmpty()) stylePreviews = R4.stylePreviews()
                }
            }
            U15ArtStyle(
                studentName = name,
                selected = artStyle,
                previews = stylePreviews,
                serverNote = if (!R4.isEnabled) "이 배포본에는 그림 만들기가 들어 있지 않아요." else if (r4Status?.reachable == false) "PC 서버에 연결되지 않아 미리보기 그림이 보이지 않아요. 그래도 고를 수 있어요." else null,
                onSelect = { vm.setArtStyle(it) },
                onBack = { route = u15Return },
                onHelp = ::help,
            )
        }

        "U10" -> U10Activate(
            studentName = name,
            expression = regExpression,
            meaningText = regMeaning,
            imageKey = regImageKey,
            checked = regChecked,
            onCheckedChange = { regChecked = it },
            onListen = { openSound(regMeaning, regImageKey, from = "U10") },
            onUse = {
                scope.launch {
                    val id = regSavedId
                    if (id > 0) {
                        vm.v2ActivateMeaning(id)
                        vm.v2MeaningById(id)?.let { vm.v2SelectMeaning(it, V2Origin.CARD) }
                        resetRegForm()
                        route = "U03"
                    } else {
                        resetRegForm(); route = "U01"
                    }
                }
            },
            onLater = { toast("미확인 보관함에 두었어요"); val r = u10Return; resetRegForm(); route = r },
            onBack = { toast("미확인으로 두었어요"); val r = u10Return; resetRegForm(); route = r },
            onHelp = ::help,
        )

        // ---------- 내 표현(U08·U09)·보관함(U11)·설정(U12) ----------
        "U08" -> U08Library(
            studentName = name,
            meanings = meanings,
            onOpen = { picked -> libId = picked.id; route = "U09" },
            onRegister = { route = "U07" },
            onArchive = { route = "U11" },
            onHome = { goHome() },
            onSettings = { route = "U12" },
            onHelp = ::help,
        )

        "U09" -> libMeaning?.let { lm ->
            U09Detail(
                meaning = lm,
                expression = libExpression,
                onEdit = {
                    // U09-E: 폼을 원본으로 채우고 수정 모드로 연다.
                    regExpression = libExpression; regMeaning = lm.text
                    regImageKey = lm.imageKey; regSituation = lm.situation.orEmpty()
                    regShowError = false; regConflict = null; editId = lm.id
                    route = "U05"
                },
                onDeactivate = { vm.v2SetActive(lm.id, false); toast("잠깐 사용하지 않아요") },
                onReconfirm = { startReconfirm(lm, from = "U09") },
                onDelete = { route = "U09D" },
                onToLibrary = { route = "U08" },
                onBack = { route = "U08" },
                onHelp = ::help,
            )
        } ?: V2Screen {}

        "U09D" -> libMeaning?.let { lm ->
            U09Delete(
                meaning = lm,
                onConfirmDelete = {
                    scope.launch {
                        vm.v2DeleteMeaning(lm.id)
                        toast("뜻을 삭제했어요")
                        libId = -1L
                        route = "U08"
                    }
                },
                onKeep = { route = "U09" },
                onHelp = ::help,
            )
        } ?: V2Screen {}

        "U11" -> U11Archive(
            meanings = archived,
            onConfirm = { picked -> libId = picked.id; startReconfirm(picked, from = "U11") },
            onToLibrary = { route = "U08" },
            onBack = { route = "U08" },
            onHelp = ::help,
        )

        "U12" -> U12Settings(
            studentName = name,
            onStudent = { u07sReturn = "U12"; route = "U07S" },
            onMicGuide = { route = "U12P" },
            onSoundGuide = { route = "U12T" },
            onRegister = { route = "U07" },
            onFirstGuide = { route = "U00" },
            onHome = { goHome() },
            onLibrary = { route = "U08" },
            onHelp = ::help,
            largeText = settings.fontScale > 1.05f,
            onLargeText = { large -> vm.setFontScale(if (large) 1.2f else 1.0f) },
            trialLogging = settings.trialLogging,
            onTrialLog = { route = "U14" },
            artStyleLabel = kr.voicemate.malitda.domain.ArtStyles.label(artStyle),
            onArtStyle = { u15Return = "U12"; route = "U15" },
            onRemoveSamples = {
                scope.launch {
                    val n = vm.v2RemoveSamples()
                    toast(if (n > 0) "예시 뜻 ${n}개를 지웠어요" else "지울 예시 뜻이 없어요")
                }
            },
        )

        "U14" -> U14TrialLog(
            studentName = name,
            enabled = settings.trialLogging,
            summary = kr.voicemate.malitda.domain.TrialSummary.of(trialLogs),
            onToggle = { vm.setTrialLogging(it) },
            onExport = {
                scope.launch {
                    val csv = vm.trialCsv()
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "말잇다 실증 기록")
                        putExtra(Intent.EXTRA_TEXT, csv)
                    }
                    runCatching { ctx.startActivity(Intent.createChooser(send, "실증 기록 내보내기").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        .onFailure { toast("내보낼 수 있는 앱이 없어요") }
                }
            },
            onClear = { vm.clearTrialLogs(); toast("이 학생 기록을 지웠어요") },
            onBack = { route = "U12" },
            onHelp = ::help,
        )

        "U12P" -> U12MicGuide(
            onPickCards = { route = "U06" },
            onOpenSettings = { openAppSettings() },
            onBack = { route = "U12" },
            onHelp = ::help,
        )

        "U12T" -> U12SoundGuide(
            onPickCards = { route = "U06" },
            onOpenSettings = { openTtsSettings() },
            onBack = { route = "U12" },
            onHelp = ::help,
        )

        "U13" -> {
            // 화면에 들어오면 바로 들려준다(화면 전환이 소리를 멈춘 뒤 재생되도록 route 효과 이후 실행).
            LaunchedEffect("U13") { vm.speak(u13Text) }
            U13Sound(
                meaningText = u13Text,
                imageKey = u13ImageKey,
                speaking = speaking,
                onReplay = { vm.speak(u13Text) },
                onClose = { route = u13Return },
                onHelp = ::help,
            )
        }

        "U06" -> {
            // 음성 인식 후보 순위를 앞세우고, 지금 보던 뜻은 뺀다.
            val rank = v2.matchIds.withIndex().associate { (i, id) -> id to i }
            val list = meanings
                .filter { it.id != v2.meaningId }
                .sortedBy { rank[it.id] ?: (rank.size + meanings.indexOf(it)) }
            U06Pick(
                meanings = list,
                onPick = { picked -> vm.trialCardPicked(); vm.v2SelectMeaning(picked, V2Origin.CARD); route = "U03" },
                onNotHere = { route = "U03M" },
                onRespeak = { startMic() },
                onCancel = { route = if (v2.meaningId != null) "U03" else "U01" },
                onHelp = ::help,
            )
        }
    }
}
