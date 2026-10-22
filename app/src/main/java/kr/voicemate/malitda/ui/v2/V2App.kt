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
    // U10·U07-S를 어디서 열었는지(돌아갈 곳)
    var u10Return by rememberSaveable { mutableStateOf("U07") }
    var u07sReturn by rememberSaveable { mutableStateOf("U07") }
    // 내 표현(U08)에서 열어 본 뜻
    var libId by rememberSaveable { mutableStateOf(-1L) }
    var libExpression by rememberSaveable { mutableStateOf("") }

    fun resetRegForm() {
        regExpression = ""; regMeaning = ""; regImageKey = null; regSituation = ""
        regShowError = false; regSavedId = -1L; regChecked = false; editId = -1L
    }

    val ui by vm.ui.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val v2 by vm.v2.collectAsStateWithLifecycle()
    val meanings by vm.meanings.collectAsStateWithLifecycle()
    val name by vm.profileName.collectAsStateWithLifecycle()
    val speaking by vm.ttsSpeaking.collectAsStateWithLifecycle()
    val ttsState by vm.ttsState.collectAsStateWithLifecycle()

    fun toast(m: String) = Toast.makeText(ctx, m, Toast.LENGTH_SHORT).show()
    fun stub(what: String) = toast("$what 은(는) 다음 단계에서 열려요")
    fun help() = toast("도움말은 준비 중이에요")

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
            libExpression = vm.v2FirstExpression(libId)
            vm.v2ObserveMeaning(libId).collect { libMeaning = it }
        }
    }
    val archived by vm.meaningsArchived.collectAsStateWithLifecycle()

    /** 보관함·사용 중지 뜻을 U10 재확인으로 보낼 준비. */
    fun startReconfirm(m: MeaningEntity, from: String) {
        scope.launch {
            regExpression = vm.v2FirstExpression(m.id)
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
    fun hasMic() = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    fun startMic() {
        vm.prepareRespeak()
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

    // 인식 이벤트 → v2 라우팅
    LaunchedEffect(Unit) {
        vm.events.collect { e ->
            when (e) {
                SessionEvent.GoResult -> {
                    val matches = vm.v2ResolveFromStt()
                    route = if (matches.isNotEmpty()) "U03" else "U03M"
                }
                SessionEvent.GoNoResult -> route = "U03E"
                is SessionEvent.GoSttError -> { toast(e.message); route = "U03E" }
                SessionEvent.GoTtsError -> if (route == "U04") route = "U04F" else toast("소리를 사용할 수 없어요")
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
            "U12P", "U12T" -> route = "U12"
            else -> goHome()
        }
    }

    // U03·U04에서 보던 뜻이 수정되면(U03-V) 알린다. 선택 당시 updatedAt과 비교.
    val m = meaning
    val changed = m != null && v2.snapshotUpdatedAt != null &&
        (m.updatedAt != v2.snapshotUpdatedAt || !m.confirmed || !m.active)

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
            onCard = { picked -> vm.v2SelectMeaning(picked, V2Origin.CARD); route = "U03" },
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
                onListen = { vm.speak(m.text) },
                onYes = { vm.v2ConfirmUse(); soundless = false; route = "U04" },
                onOther = { route = "U06" },
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
            onPickCards = { route = "U06" },
            onRegister = { route = "U07" },
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
                onSound = { if (ttsState is TtsState.Unavailable) route = "U04F" else vm.speak(m.text) },
                onRepick = { route = "U06" },
                onShare = { shareMeaning(m); route = "U04S" },
                onBack = { route = "U03" },
                onHelp = ::help,
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
                onCancel = { goHome() },
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
            onRestart = { vm.resetSession(); resetRegForm(); toast("새로 시작해요"); route = "U01" },
            onCancel = { route = u07sReturn },
            onHelp = ::help,
        )

        "U05" -> U05Register(
            expression = regExpression,
            meaning = regMeaning,
            imageKey = regImageKey,
            situation = regSituation,
            showError = regShowError,
            onExpression = { regExpression = it },
            onMeaning = { regMeaning = it },
            onSituation = { regSituation = it },
            onPickImage = { route = "U05M" },
            onConfirmWithStudent = {
                if (regExpression.isBlank() || regMeaning.isBlank()) { regShowError = true; return@U05Register }
                scope.launch {
                    if (editId > 0) {
                        vm.v2UpdateMeaning(editId, regExpression, regMeaning, regImageKey, regSituation)
                        regSavedId = editId; u10Return = "U09"
                    } else {
                        regSavedId = vm.v2RegisterMeaning(regExpression, regMeaning, regImageKey, regSituation)
                        u10Return = "U07"
                    }
                    regChecked = false
                    route = "U10"
                }
            },
            onSaveUnconfirmed = {
                if (regExpression.isBlank() || regMeaning.isBlank()) { regShowError = true; return@U05Register }
                scope.launch {
                    if (editId > 0) {
                        vm.v2UpdateMeaning(editId, regExpression, regMeaning, regImageKey, regSituation)
                        toast("수정했어요. 학생과 확인하면 다시 쓸 수 있어요.")
                        val back = "U09"
                        resetRegForm(); route = back
                    } else {
                        vm.v2RegisterMeaning(regExpression, regMeaning, regImageKey, regSituation)
                        toast("미확인으로 저장했어요. 학생과 확인하면 카드로 나와요.")
                        resetRegForm(); route = "U07"
                    }
                }
            },
            onRetry = { regShowError = false },
            onCancel = { val back = if (editId > 0) "U09" else "U07"; resetRegForm(); route = back },
            onHelp = ::help,
        )

        "U05M" -> U05PickImage(
            meaningText = regMeaning,
            selectedKey = regImageKey,
            onSelect = { regImageKey = it },
            onUse = { route = "U05" },
            onCancel = { route = "U05" },
            onHelp = ::help,
        )

        "U10" -> U10Activate(
            studentName = name,
            expression = regExpression,
            meaningText = regMeaning,
            imageKey = regImageKey,
            checked = regChecked,
            onCheckedChange = { regChecked = it },
            onListen = { vm.speak(regMeaning) },
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
                    regShowError = false; editId = lm.id
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

        "U06" -> {
            // 음성 인식 후보 순위를 앞세우고, 지금 보던 뜻은 뺀다.
            val rank = v2.matchIds.withIndex().associate { (i, id) -> id to i }
            val list = meanings
                .filter { it.id != v2.meaningId }
                .sortedBy { rank[it.id] ?: (rank.size + meanings.indexOf(it)) }
            U06Pick(
                meanings = list,
                onPick = { picked -> vm.v2SelectMeaning(picked, V2Origin.CARD); route = "U03" },
                onNotHere = { route = "U03M" },
                onRespeak = { startMic() },
                onCancel = { route = if (v2.meaningId != null) "U03" else "U01" },
                onHelp = ::help,
            )
        }
    }
}
