package kr.voicemate.malitda.ui.v2

import kr.voicemate.malitda.BuildConfig

import android.Manifest
import android.app.Activity
import android.app.KeyguardManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kr.voicemate.malitda.data.settings.SettingsStore
import kr.voicemate.malitda.data.settings.ConfirmationMode
import kr.voicemate.malitda.domain.TrialCsv
import kotlinx.coroutines.Job
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kr.voicemate.malitda.domain.StudentMeaningConfirmation
import kr.voicemate.malitda.domain.StudentMeaningResponse
import kr.voicemate.malitda.domain.StudentConfirmationMethod
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
    var guardianUnlocked by remember { mutableStateOf(false) }
    var credentialRecoveryPending by remember { mutableStateOf(false) }
    var generationJob by remember { mutableStateOf<Job?>(null) }
    var regCategory by rememberSaveable { mutableStateOf("often") }
    var literalReturn by rememberSaveable { mutableStateOf("U01") }
    var pilotEndReturn by rememberSaveable { mutableStateOf("U01") }
    var feedbackReturn by rememberSaveable { mutableStateOf("U04") }
    var privacyReturn by rememberSaveable { mutableStateOf("U12") }
    var onboardingDestination by rememberSaveable { mutableStateOf("U01") }
    var formRevision by remember { mutableStateOf(0L) }
    var photoProfile by remember { mutableStateOf(-1L) }
    var photoFormRevision by remember { mutableStateOf(-1L) }
    val owner = LocalLifecycleOwner.current
    // 말하기·뜻 고르기·확인·전달 중에는 화면이 꺼지지 않게 한다 — 상대가 읽는 도중 꺼지면 전달이 끊긴다.
    val view = LocalView.current
    val keepAwake = listOf("U02", "U03", "U04", "U06", "U13").any { route.startsWith(it) }
    DisposableEffect(keepAwake) {
        view.keepScreenOn = keepAwake
        onDispose { view.keepScreenOn = false }
    }
    var soundless by rememberSaveable { mutableStateOf(false) }
    // 지원자 등록 폼(U05) 작성 상태
    var regExpression by rememberSaveable { mutableStateOf("") }
    var regMeaning by rememberSaveable { mutableStateOf("") }
    var regImageKey by rememberSaveable { mutableStateOf<String?>(null) }
    var regSituation by rememberSaveable { mutableStateOf("") }
    var regShowError by rememberSaveable { mutableStateOf(false) }
    var regSavedId by rememberSaveable { mutableStateOf(-1L) }
    // 학생 반응은 프로세스 종료 뒤 복원하지 않는다. 다시 확인해야 활성화할 수 있다.
    var regConfirmation by remember { mutableStateOf(StudentMeaningConfirmation()) }
    var regSavedUpdatedAt by rememberSaveable { mutableStateOf(-1L) }
    var regPersisting by remember { mutableStateOf(false) }
    var confirming by remember { mutableStateOf(false) }
    var learning by remember { mutableStateOf(false) }
    // 수정 모드(U09-E)면 원본 id, 아니면 -1
    var editId by rememberSaveable { mutableStateOf(-1L) }
    // U10·U07-S·U13을 어디서 열었는지(돌아갈 곳)
    var u10Return by rememberSaveable { mutableStateOf("U07") }
    // 뜻 상세(U09)를 연 곳: 내 표현(U08) 또는 미확인 보관함(U11).
    var u09Return by rememberSaveable { mutableStateOf("U08") }
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
    var dataCounts by remember { mutableStateOf<Map<String,Int>>(emptyMap()) }
    var dataBusy by remember { mutableStateOf(false) }
    var dataError by remember { mutableStateOf<String?>(null) }
    var dataRefresh by remember { mutableStateOf(0) }
    var aliases by remember { mutableStateOf<Map<Long,String>>(emptyMap()) }

    var regConflict by rememberSaveable { mutableStateOf<String?>(null) }
    // 저장 대상이 아니다: 앱이 재시작되면 진행 중이던 요청도 사라지므로 '만드는 중'에 갇히지 않게.
    var generating by remember { mutableStateOf(false) }
    // 그림 생성 회차. 그만두거나 등록을 끝내면 올려서, 늦게 도착한 그림이 다른 폼에 붙지 않게 한다.
    var genRound by remember { mutableStateOf(0) }
    // R4 서버(팀원 그림 생성 서버) 상태·동의·그림체 미리보기
    var r4Status by remember { mutableStateOf<R4.Status?>(null) }
    var r4CheckRound by remember { mutableStateOf(0) }
    var genError by remember { mutableStateOf<String?>(null) }
    var r4Consent by remember { mutableStateOf(false) }
    var stylePreviews by remember { mutableStateOf<Map<String, ByteArray>>(emptyMap()) }
    var u15Return by rememberSaveable { mutableStateOf("U12") }

    fun resetRegForm() {
        formRevision++
        regExpression = ""; regMeaning = ""; regImageKey = null; regSituation = ""; regCategory = "often"
        regShowError = false; regSavedId = -1L; regSavedUpdatedAt = -1L
        regConfirmation = StudentMeaningConfirmation(); editId = -1L; regConflict = null
        genRound++; generationJob?.cancel(); generationJob = null; generating = false; r4Consent = false
    }

    /** 표현 칸은 쉼표·줄바꿈으로 여러 개를 받는다. */
    fun parseExpressions(s: String): List<String> =
        s.split(",", "\n").map { it.trim() }.filter { it.isNotBlank() }.distinct()

    val ui by vm.ui.collectAsStateWithLifecycle()
    val settingsSnapshot by vm.loadedSettings.collectAsStateWithLifecycle()
    val settings = settingsSnapshot ?: return
    val pilotToolsEnabled = vm.pilotToolsEnabled
    val trialLoggingEnabled = pilotToolsEnabled && settings.trialLogging
    val v2 by vm.v2.collectAsStateWithLifecycle()
    val meanings by vm.meanings.collectAsStateWithLifecycle()
    val name by vm.profileName.collectAsStateWithLifecycle()
    val speaking by vm.ttsSpeaking.collectAsStateWithLifecycle()
    val artStyle by vm.artStyle.collectAsStateWithLifecycle()
    val preferences by vm.profilePreferences.collectAsStateWithLifecycle()
    val pilot by vm.pilotConfig.collectAsStateWithLifecycle()
    val ttsState by vm.ttsState.collectAsStateWithLifecycle()
    val voicePreparation by vm.prepareState.collectAsStateWithLifecycle()

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
    LaunchedEffect(meanings, profileId) { aliases = meanings.associate { it.id to vm.v2ExpressionsText(it.id) } }

    /** 보관함·사용 중지 뜻을 U10 재확인으로 보낼 준비. */
    fun startReconfirm(m: MeaningEntity, from: String) {
        scope.launch {
            regExpression = vm.v2ExpressionsText(m.id)
            regMeaning = m.text; regImageKey = m.imageKey; regSituation = m.situation.orEmpty()
            regSavedId = m.id; regSavedUpdatedAt = m.updatedAt
            regConfirmation = StudentMeaningConfirmation(); u10Return = from
            route = "U10"
        }
    }

    fun endSupporterSession() {
        guardianUnlocked = false
        genRound++; generationJob?.cancel(); generationJob = null
        generating = false; r4Consent = false
    }

    fun goHome() {
        endSupporterSession()
        vm.resetSession()
        soundless = false
        route = "U01"
    }

    fun goLibrary() {
        endSupporterSession()
        vm.resetSession()
        route = "U08"
    }

    fun activateRegistration(homeAfterSave: Boolean) {
        if (!regConfirmation.canActivate || regPersisting) return
        regPersisting = true
        scope.launch {
            try {
                val id = regSavedId
                val activated = id > 0 && vm.v2ActivateMeaning(id, regSavedUpdatedAt, regConfirmation)
                if (!activated) {
                    regConfirmation = StudentMeaningConfirmation()
                    toast("등록한 내용이 바뀌었습니다. 다시 확인합니다.")
                    vm.v2MeaningById(id)?.let { startReconfirm(it, u10Return) }
                    return@launch
                }
                if (homeAfterSave) {
                    resetRegForm(); goHome()
                } else if (u10Return == "U09") {
                    toast("학생과 확인한 뜻을 저장했습니다.")
                    resetRegForm(); route = "U09"
                } else {
                    val fresh = vm.v2MeaningById(id)
                    fresh?.let { vm.v2SelectMeaning(it, V2Origin.CARD) }
                    endSupporterSession()
                    resetRegForm(); route = if (fresh != null) "U03" else "U01"
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                toast("확인 내용을 저장하지 못했습니다. 다시 시도해 주십시오.")
            } finally { regPersisting = false }
        }
    }

    fun saveRegistration(confirmNext: Boolean) {
        if (regPersisting || generating) return
        val expressions = parseExpressions(regExpression)
        if (expressions.isEmpty() || regMeaning.isBlank() || regMeaning.trim().length > 100) { regShowError = true; regConflict = "평소 표현과 전할 뜻을 입력해야 합니다. 전할 뜻은 100자까지 등록합니다."; return }
        regPersisting = true
        regConflict = null
        val targetProfile = profileId
        val requestedForm = formRevision
        val requestedEdit = editId
        val requestedText = regMeaning
        val requestedImage = regImageKey
        val requestedSituation = regSituation
        val requestedCategory = regCategory
        scope.launch {
            try {
                val conflicts = vm.v2FindConflicts(expressions, excludeMeaningId = requestedEdit.coerceAtLeast(0L))
                if (conflicts.isNotEmpty()) {
                    toast("같은 표현의 다른 뜻이 있습니다. 상황을 구분해 후보로 함께 보여 줍니다.")
                }
                if (targetProfile != vm.profileId.value || requestedForm != formRevision) return@launch
                val wasEdit = requestedEdit > 0
                val saved = if (wasEdit) {
                    vm.v2UpdateMeaning(requestedEdit, expressions, requestedText, requestedImage, requestedSituation, requestedCategory)
                    requestedEdit
                } else vm.v2RegisterMeaning(expressions, requestedText, requestedImage, requestedSituation, requestedCategory)
                if (vm.profileId.value != targetProfile || requestedForm != formRevision) return@launch
                if (confirmNext) {
                    val fresh = vm.v2MeaningById(saved) ?: error("저장한 뜻을 다시 불러오지 못했습니다.")
                    regSavedId = saved; regSavedUpdatedAt = fresh.updatedAt
                    regConfirmation = StudentMeaningConfirmation()
                    u10Return = if (wasEdit) "U09" else "U07"
                    route = "U10"
                } else {
                    toast("미확인으로 저장했습니다. 학생과 확인한 뒤 사용할 수 있습니다.")
                    resetRegForm(); goHome()
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                regConflict = when (e) {
                    is kr.voicemate.malitda.data.db.MeaningLimitReachedException -> "표현은 50개까지 보관합니다. 내 표현에서 사용하지 않는 뜻을 삭제한 뒤 다시 저장합니다."
                    is kr.voicemate.malitda.data.db.MeaningAlreadyExistsException -> "같은 문장·그림·상황의 뜻이 이미 있습니다. 내 표현에서 기존 뜻을 확인합니다."
                    else -> "저장하지 못했습니다. 입력한 내용은 그대로 있습니다. 다시 저장해 주십시오."
                }
            } finally { regPersisting = false }
        }
    }

    // 첫 실행에만 안내(U00)로 시작한다.
    // Hidden hand-off entry exists only in a deliberately opted-in private debug build.
    LaunchedEffect(Unit) {
        if (vm.c.settings.current().onboarded) {
            val privateTool = pilotToolsEnabled && (ctx as? Activity)?.intent?.getStringExtra("malitda.debug.tool") == "pilot"
            route = if (privateTool) "U14" else "U01"
        }
    }
    // 화면을 떠나면 읽어주던 소리를 멈춘다.
    LaunchedEffect(route) { vm.stopSpeaking() }

    val micPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { route = "U02"; vm.startListening() } else route = "U02D"
    }
    // 의미 그림으로 쓸 사진 선택(앱 전용 저장소로 복사해 참조만 보관).
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && photoProfile == vm.profileId.value && photoFormRevision == formRevision) scope.launch {
            val expectedProfile = photoProfile; val expectedForm = photoFormRevision
            runCatching { kr.voicemate.malitda.data.repo.MeaningImageStore.import(ctx, uri) }
                .onSuccess { key ->
                    if (expectedProfile == vm.profileId.value && expectedForm == formRevision && vm.settings.value.consentAccepted) regImageKey = key
                    else vm.v2CleanupImages(setOfNotNull(regImageKey))
                }
                .onFailure { e -> toast(e.message ?: "사진을 불러오지 못했어요") }
        }
    }
    fun hasMic() = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    fun startMic() {
        vm.prepareRespeak()
        vm.v2ClearHeard()
        vm.trialStartVoice()
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
        scope.launch {
            runCatching {
                val fresh = vm.v2MeaningById(m.id)
                check(fresh != null && vm.v2CanDeliver(fresh) && fresh.updatedAt == m.updatedAt && route == "U04")
                CommunicationIntents.shareText(ctx, fresh.text)
            }.onSuccess { route = "U04S" }.onFailure {
                if (it is CancellationException) throw it
                toast("공유할 뜻과 승인을 다시 확인합니다. 큰 문장이나 복사를 사용할 수 있습니다.")
            }
        }
    }

    // 인식 이벤트 → v2 라우팅. 인식 결과는 말하기 화면(U02)에 있을 때만 받는다 —
    // 학생이 그사이 카드로 고른 뜻을 늦게 도착한 결과가 덮어쓰면 안 된다.
    LaunchedEffect(Unit) {
        vm.events.collect { e ->
            when (e) {
                SessionEvent.GoResult -> if (route == "U02") {
                    val matches = vm.v2ResolveFromStt()
                    if (route == "U02") route = "U16"
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
        if (regPersisting || dataBusy) return@BackHandler
        when (route) {
            "U00" -> goHome()
            "U02" -> { vm.cancelListening(); goHome() }
            "U04" -> { vm.v2ClearApproval(); route = "U03" }
            "U03H" -> route = "U03"
            "U04F", "U04S" -> route = "U04"
            "U06" -> if (v2.meaningId != null) route = "U03" else goHome()
            "U05" -> route = if (editId > 0) "U09" else "U07"
            "U05M" -> route = "U05"
            "U07S" -> route = u07sReturn
            "U10" -> { toast("미확인으로 두었어요"); val r = u10Return; resetRegForm(); route = r }
            "U08" -> goHome()
            "U09" -> if (u09Return == "U08") goLibrary() else { route = u09Return }
            "U09D" -> route = "U09"
            "U12", "U07" -> goHome()
            "U12P", "U12T", "U12PF", "U12INFO" -> route = "U12"
            "U11", "U14", "U12DATA" -> route = "U07"
            "U12PR" -> route = privacyReturn
            "U14SETUP" -> route = "U14"
            "U14FEEDBACK" -> route = feedbackReturn
            "U14END" -> route = pilotEndReturn
            "U16" -> goHome()
            "U17" -> { vm.revokeLiteral(); route = literalReturn }
            "U18" -> { vm.revokeLiteral(); route = "U17" }
            "U15" -> route = u15Return
            "U13" -> route = u13Return
            else -> goHome()
        }
    }

    // U03·U04에서 보던 뜻이 수정되면(U03-V) 알린다. 선택 당시 updatedAt과 비교.
    val m = meaning
    val changed = m != null && v2.snapshotUpdatedAt != null &&
        (m.updatedAt != v2.snapshotUpdatedAt || !(m.confirmed || m.isSample) || !m.active || m.profileId != profileId)
    // U04S is a post-share navigation screen. It cannot send or speak; a new delivery still requires U03 approval.
    LaunchedEffect(route, m, v2.deliveryApproval) {
        if (route in listOf("U04", "U04F") && m != null && !vm.v2CanDeliver(m)) route = "U03"
    }
    val trialDraft by vm.trialDraft.collectAsStateWithLifecycle()
    val trialLogs by vm.trialLogs.collectAsStateWithLifecycle()
    LaunchedEffect(route, profileId, dataRefresh) {
        if (route == "U12DATA") {
            dataCounts = emptyMap(); dataError = null
            runCatching { vm.dataCounts() }.onSuccess { dataCounts = it }
                .onFailure { dataError = "자료 현황을 읽지 못했습니다. 다시 들어와 확인해야 합니다." }
        }
    }
    fun dataAction(action: suspend () -> Unit) {
        if (dataBusy) return
        dataBusy = true; dataError = null; resetRegForm()
        scope.launch {
            try { action(); dataRefresh++ }
            catch (e: Exception) { if (e is CancellationException) throw e; dataError = "자료 처리를 마치지 못했습니다. 다시 확인해야 합니다." }
            finally { dataBusy = false }
        }
    }

    val recoveryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (credentialRecoveryPending && result.resultCode == Activity.RESULT_OK) {
            scope.launch { vm.c.settings.clearGuardianPinWithDeviceCredential() }
        }
        credentialRecoveryPending = false
    }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                guardianUnlocked = false; helpOpen = false; r4Consent = false
                generationJob?.cancel(); generationJob = null; genRound++; generating = false
                vm.pauseForBackground()
                if (route == "U13") { u13Text = ""; u13ImageKey = null; route = "U01" }
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    fun beginLiteral(text: String = "", from: String = route) {
        vm.trialStartText(); vm.beginLiteral(text); literalReturn = from; route = "U17"
    }
    if (!pilotToolsEnabled && route in setOf("U14", "U14SETUP", "U14FEEDBACK", "U14END")) {
        LaunchedEffect(route) { vm.resetSession(); route = "U01" }
        return
    }
    val protectedRoutes = setOf("U07", "U07S", "U05", "U05M", "U10", "U09", "U09D", "U11",
        "U12", "U12DATA", "U14", "U14SETUP", "U14FEEDBACK", "U14END", "U15", "U12PR", "U12PF", "U12INFO")
    val needsConsent = !settings.consentAccepted || settings.consentVersion != SettingsStore.CURRENT_CONSENT_VERSION
    if (needsConsent && route == "U00") BackHandler { (ctx as? Activity)?.finish() }
    if (OnboardingPolicy.requiresConsent(needsConsent, route)) {
        BackHandler { (ctx as? Activity)?.finish() }
        V2ConsentScreen(onAccept = { scope.launch {
            vm.c.settings.acceptCurrentConsent(); vm.c.settings.setOnboarded(true); route = onboardingDestination
        } }, onDecline = { (ctx as? Activity)?.finish() }, onManageData = { route = "U12DATA" })
        return
    }
    if ((route in protectedRoutes || (route == "U13" && u13Return in protectedRoutes)) && !guardianUnlocked) {
        BackHandler { goHome() }
        GuardianGateScreen(settingsStore = vm.c.settings,
            onUnlocked = { scope.launch { if (vm.c.settings.current().guardianConfigured && owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) guardianUnlocked = true } }, onCancel = { goHome() },
            onDeviceRecovery = {
                val manager = ctx.getSystemService(KeyguardManager::class.java)
                @Suppress("DEPRECATION") val intent = manager.createConfirmDeviceCredentialIntent("말잇다 지원자 PIN 복구", "기기 잠금을 확인한 뒤 지원자 PIN을 다시 정합니다.")
                if (intent == null) toast("기기 잠금이 설정되지 않아 복구할 수 없습니다. 기존 PIN으로 확인해야 합니다.")
                else { credentialRecoveryPending = true; recoveryLauncher.launch(intent) }
            })
        return
    }


    if (helpOpen) V2HelpDialog(helpTextFor(route)) { helpOpen = false }

    androidx.compose.runtime.CompositionLocalProvider(LocalPilotStop provides
        (if (trialLoggingEnabled && pilot.canCollect && trialDraft != null) ({ vm.pausePilot(); goHome() }) else null),
        LocalPilotComplete provides (if (trialLoggingEnabled && pilot.canCollect && trialDraft != null && route != "U14END")
            ({ pilotEndReturn = route; route = "U14END" }) else null)) {
    when (route) {
        "U00C" -> SettingsLoadingScreen()
        "U00" -> U00Guide(
            onStart = {
                onboardingDestination = "U01"
                if (needsConsent) route = OnboardingPolicy.destination(false, false) else { goHome(); scope.launch { vm.c.settings.setOnboarded(true) } }
            },
            onPrepare = {
                onboardingDestination = "U07"
                route = OnboardingPolicy.destination(true, !needsConsent)
                if (!needsConsent) scope.launch { vm.c.settings.setOnboarded(true) }
            },
            onBack = if (settings.onboarded) ::goHome else null,
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
            onLibrary = ::goLibrary,
            onSettings = { route = "U12" },
            onDirectInput = { beginLiteral() },
            totalRegistered = (meanings + archived).count { !it.isSample },
            lastModified = (meanings + archived).filter { !it.isSample }.maxOfOrNull { it.updatedAt },
            voiceStatus = when (voicePreparation) {
                is kr.voicemate.malitda.stt.PrepareState.Ready -> "음성인식은 이 휴대전화 안에서 처리합니다."
                is kr.voicemate.malitda.stt.PrepareState.Failed -> "음성인식을 준비하지 못했습니다. 카드나 직접 입력을 사용할 수 있습니다."
                else -> "음성인식을 준비 중입니다. 카드와 직접 입력은 바로 사용할 수 있습니다."
            },
            onRetryVoice = if (voicePreparation is kr.voicemate.malitda.stt.PrepareState.Failed) vm::retryPrepare else null,
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
            m == null -> V2MissingMeaning({ route = "U06" }, ::goHome, ::help) // 불러오는 중(순간)
            changed -> U03Changed(
                onRecheck = { scope.launch { route = if (vm.v2RefreshSnapshot()) "U03" else "U06" } },
                onHome = { goHome() },
                onHelp = ::help,
            )
            else -> U03Confirm(
                meaning = m,
                origin = v2.origin,
                preferredMode = if (trialLoggingEnabled && pilot.canCollect && pilot.condition == "B") ConfirmationMode.TEXT else preferences.confirmationMode,
                heard = if (v2.origin == V2Origin.VOICE) ui.raw else null,
                onEdit = { beginLiteral(m.text, "U03") },
                studentName = name,
                onListen = { openSound(m.text, m.imageKey, from = "U03") },
                confirming = confirming,
                onYes = { if (!confirming) {
                    confirming = true
                    scope.launch {
                        try {
                            val approved = vm.v2ConfirmUse(m)
                            if (route == "U03" && approved && vm.v2CanDeliver(m)) { soundless = false; route = "U04" }
                            else if (route == "U03" && !approved) { toast("뜻이 바뀌었거나 사용할 수 없습니다. 다시 확인합니다."); vm.v2RefreshSnapshot() }
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            if (route == "U03") toast("뜻을 불러오지 못했습니다. 다시 시도해 주십시오.")
                        } finally { confirming = false }
                    }
                } },
                onOther = { vm.v2ClearApproval(); vm.trialOtherMeaning(); route = "U06" },
                onUnsure = { vm.v2ClearApproval(); route = "U03H" },
                onBack = { goHome() },
                onHelp = ::help,
            )
        }

        "U03H" -> U03Unsure(
            meaning = m,
            onListen = { m?.let { openSound(it.text, it.imageKey, from = "U03H") } },
            onRecheck = { route = "U03" }, onPick = { route = "U06" },
            onStop = { goHome() }, onHelp = ::help,
        )

        "U03E" -> U03NoHear(
            onRespeak = { startMic() },
            onPickCards = { route = "U06" },
            onCancel = { goHome() },
            onHelp = ::help, onDirect = { beginLiteral() },
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
            onHelp = ::help, onDirect = { beginLiteral(ui.raw.orEmpty()) },
        )

        "U04" -> when {
            m == null -> V2MissingMeaning({ route = "U06" }, ::goHome, ::help)
            changed -> U03Changed(
                onRecheck = { scope.launch { route = if (vm.v2RefreshSnapshot()) "U03" else "U06" } },
                onHome = { goHome() },
                onHelp = ::help,
            )
            else -> U04Deliver(
                meaning = m,
                speaking = speaking,
                soundless = soundless || ttsState is TtsState.Unavailable,
                onSound = { if (!vm.v2CanDeliver(m)) route = "U03" else if (ttsState is TtsState.Unavailable) route = "U04F" else openSound(m.text, m.imageKey, from = "U04") },
                onRepick = { vm.v2ClearApproval(); route = "U06" },
                onShare = { shareMeaning(m) },
                onBack = { vm.v2ClearApproval(); route = "U03" },
                onHelp = ::help,
                // 말로 시도했는데 학생이 카드로 뜻을 고쳐 확정한 경우에만 제안한다.
                learnHeard = if (guardianUnlocked && v2.origin == V2Origin.CARD) v2.heard else null,
                onLearn = {
                    if (guardianUnlocked && !learning && vm.v2CanDeliver(m)) {
                        learning = true
                        scope.launch {
                            try {
                                val heard = vm.v2.value.heard
                                when (val result = vm.v2LearnHeard()) {
                                    is kr.voicemate.malitda.data.repo.MeaningRepository.AddExpressionResult.Added ->
                                        toast("등록했습니다. 다음부터 '$heard'도 이 뜻의 후보로 찾습니다.")
                                    is kr.voicemate.malitda.data.repo.MeaningRepository.AddExpressionResult.AlreadyThere ->
                                        toast("이미 이 뜻에 등록된 표현입니다.")
                                    is kr.voicemate.malitda.data.repo.MeaningRepository.AddExpressionResult.TakenBy ->
                                        toast("다른 뜻에 등록된 표현입니다: ${result.meaningText}")
                                    null -> Unit
                                }
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                                toast("표현을 추가하지 못했습니다. 다시 시도해 주십시오.")
                            } finally { learning = false }
                        }
                    }
                },
                trialUnderstood = if (guardianUnlocked && trialLoggingEnabled && pilot.canCollect) trialDraft?.understood else null,
                trialHelped = trialDraft?.supporterHelped ?: false,
                onTrialUnderstood = { vm.trialSetUnderstood(it) },
                onTrialHelped = { vm.trialSetHelped(it) },
                onDone = { vm.completePilotTask(); goHome() },
                onFeedback = if (trialLoggingEnabled && pilot.canCollect) ({ feedbackReturn = "U04"; route = "U14FEEDBACK" }) else null,
                onStopSound = { vm.stopSpeaking() },
                onCopy = { if (vm.v2CanDeliver(m)) runCatching { CommunicationIntents.copySensitive(ctx, m.text) }.onFailure { toast("복사하지 못했습니다.") } },
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
        } ?: V2MissingMeaning({ goLibrary() }, ::goHome, ::help)

        "U04S" -> m?.let {
            U04Share(
                meaning = it,
                onReturn = { vm.v2ClearApproval(); route = "U03" },
                onHome = ::goHome,
                onHelp = ::help,
            )
        } ?: V2MissingMeaning({ goLibrary() }, ::goHome, ::help)

        // ---------- 지원자 등록 흐름 (U07 → U05 → U10) ----------
        "U07" -> U07Entry(
            studentName = name,
            onRegister = { resetRegForm(); route = "U05" },
            onMyExpressions = ::goLibrary,
            onStudentChip = { u07sReturn = "U07"; route = "U07S" },
            onBack = ::goHome,
            onHelp = ::help,
            onArchive = { route = "U11" },
            onSettings = { route = "U12" },
            onData = { route = "U12DATA" },
            onTrialLog = if (pilotToolsEnabled) ({ route = "U14" }) else null,
            pendingCount = archived.size,
        )

        "U07S" -> U07NewSession(
            studentName = name,
            others = profilesList.filter { it.id != profileId }.map { it.id to it.name },
            onRestart = { resetRegForm(); toast("새로 시작해요"); goHome() },
            onSwitch = { id -> vm.v2SwitchStudent(id); resetRegForm(); toast("학생을 바꿨어요"); goHome() },
            onAdd = { newName -> vm.v2AddStudent(newName); resetRegForm(); toast("${newName} 학생으로 시작해요"); goHome() },
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
            category = regCategory, onCategory = { regCategory = it },
            similarMeanings = if (regMeaning.trim().length < 2) emptyList() else (meanings + archived).filter {
                it.id != editId && kr.voicemate.malitda.domain.MeaningResolver.stringSimilarity(
                    kr.voicemate.malitda.domain.TextNormalizer.comparison(regMeaning),
                    kr.voicemate.malitda.domain.TextNormalizer.comparison(it.text)) >= 0.8
            }.take(3),
            onOpenExisting = { libId = it.id; u09Return = "U05"; route = "U09" },
            onPreview = { if (regMeaning.isNotBlank()) vm.speak(regMeaning) }, onPreviewStop = vm::stopSpeaking,
            conflictMessage = regConflict,
            imageGenerating = generating,
            saving = regPersisting,
            onExpression = { if (!regPersisting) { regExpression = it; regConflict = null } },
            onMeaning = { if (!regPersisting) regMeaning = it },
            onSituation = { if (!regPersisting) regSituation = it },
            onPickImage = { if (!regPersisting) route = "U05M" },
            onConfirmWithStudent = { saveRegistration(confirmNext = true) },
            onSaveUnconfirmed = { saveRegistration(confirmNext = false) },
            onRetry = { regShowError = false },
            onCancel = { if (!regPersisting) { val back = if (editId > 0) "U09" else "U07"; resetRegForm(); route = back } },
            onHelp = ::help,
        )

        "U05M" -> {
          // 키가 든 지원자용 빌드는 앱이 직접 만든다 — PC 서버 확인이 필요 없다.
          val direct = kr.voicemate.malitda.data.ai.DirectImageClient.isAvailable
          // 화면에 들어올 때 PC 서버 상태를 확인한다(꺼짐·점검 모드면 생성 대신 안내).
          LaunchedEffect(r4CheckRound) { if (!direct && R4.isEnabled) r4Status = R4.status() }
          U05PickImage(
            meaningText = regMeaning,
            selectedKey = regImageKey,
            onSelect = {
                // A deliberate local choice wins over an older asynchronous AI response.
                genRound++; generationJob?.cancel(); generationJob = null
                generating = false; r4Consent = false; regImageKey = it
            },
            onPickPhoto = { photoProfile = profileId; photoFormRevision = formRevision; photoLauncher.launch("image/*") },
            onUse = { route = "U05" },
            onCancel = { route = "U05" },
            onHelp = ::help,
            serverNote = if (direct) {
                kr.voicemate.malitda.data.ai.DirectImageClient.remainingToday(ctx)
                    .takeIf { it == 0 }?.let { "오늘 만들 수 있는 그림을 다 썼습니다. 내일 다시 만들 수 있습니다." }
            } else r4Note(r4Status), demoMode = !direct && r4Status?.demoMode == true,
            lastError = genError,
            styleLabel = kr.voicemate.malitda.domain.ArtStyles.label(artStyle),
            onChangeStyle = { u15Return = "U05M"; route = "U15" },
            consent = r4Consent,
            onConsent = { r4Consent = it },
            onRecheckServer = { r4CheckRound++ },
            onGenerate = if (!direct && !R4.isEnabled) null else ({
                if (!guardianUnlocked || !r4Consent) {
                    toast("전송과 비용 안내에 먼저 동의해야 합니다.")
                } else if (regMeaning.isBlank()) {
                    toast("먼저 '전할 뜻'을 입력해 주세요")
                } else if (!generating) {
                    generating = true
                    genError = null
                    val round = ++genRound
                    val requestingProfile = profileId
                    val requestedMeaning = regMeaning
                    r4Consent = false
                    generationJob = scope.launch {
                        val result = runCatching { vm.r4GenerateMeaningImage(ctx, requestedMeaning) }
                        // Never clear a newer request's progress. This request is finished even if
                        // its result is discarded because the person or prompt has changed.
                        if (round != genRound) return@launch
                        generating = false; generationJob = null; r4Consent = false
                        if (result.exceptionOrNull() is CancellationException) return@launch
                        if (profileId != requestingProfile || regMeaning != requestedMeaning || !guardianUnlocked) {
                            toast("등록 내용이 바뀌어 이전 그림을 적용하지 않았습니다. 다시 확인한 뒤 만들 수 있습니다.")
                            return@launch
                        }
                        result.onSuccess { key -> regImageKey = key; toast(if (!direct && r4Status?.demoMode == true) "모의 서버의 예시 그림입니다. 실제 AI 생성 결과가 아닙니다." else "그림을 받았습니다. 학생과 함께 확인합니다.") }
                            .onFailure { e ->
                                genError = e.message ?: "그림을 만들지 못했어요"
                                toast(e.message ?: "그림을 만들지 못했어요")
                                r4CheckRound++
                            }
                    }
                }
            }),
            generating = generating,
            onCancelGenerate = { genRound++; generationJob?.cancel(); generationJob = null; generating = false; r4Consent = false; toast("결과 받기를 중단했습니다. 이미 시작한 서버 작업이나 비용은 취소되지 않을 수 있습니다.") },
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
                serverNote = if (kr.voicemate.malitda.data.ai.DirectImageClient.isAvailable) null
                    else if (!R4.isEnabled) "이 배포본에는 그림 만들기가 들어 있지 않아요." else if (r4Status?.reachable == false) "PC 서버에 연결되지 않아 미리보기 그림이 보이지 않아요. 그래도 고를 수 있어요." else null,
                onSelect = { vm.setArtStyle(it) },
                onBack = { route = u15Return },
                onHelp = ::help,
            )
        }

        "U10" -> U10Activate(
            studentName = name, expression = regExpression, meaningText = regMeaning, imageKey = regImageKey,
            confirmation = regConfirmation, saving = regPersisting,
            onResponse = {
                regConfirmation = StudentMeaningConfirmation(response = it)
                if (it != StudentMeaningResponse.MATCHED && regSavedId > 0) scope.launch {
                    vm.v2RecordResponse(regSavedId, regSavedUpdatedAt, when (it) {
                        StudentMeaningResponse.DIFFERENT -> "different"
                        StudentMeaningResponse.UNSURE -> "unsure"
                        else -> "no_response"
                    })
                }
            },
            onMethod = { regConfirmation = regConfirmation.copy(method = it) },
            onListen = { openSound(regMeaning, regImageKey, from = "U10") },
            onUse = { activateRegistration(false) },
            onUseAndHome = { activateRegistration(true) },
            onLater = { if (!regPersisting) { toast("미확인 보관함에 두었습니다."); val back = u10Return; resetRegForm(); route = back } },
            onBack = { if (!regPersisting) { val back = u10Return; resetRegForm(); route = back } },
            onHelp = ::help,
        )

        // ---------- 내 표현(U08·U09)·보관함(U11)·설정(U12) ----------
        "U08" -> U08Library(
            studentName = name, profileId = profileId,
            meanings = meanings,
            onOpen = { picked -> libId = picked.id; u09Return = "U08"; route = "U09" },
            onUse = { picked -> vm.trialStartCard(); vm.v2SelectMeaning(picked, V2Origin.CARD); route = "U03" },
            onListen = { vm.speak(it.text) }, onFavorite = { vm.toggleMeaningFavorite(it.id) }, aliases = aliases,
            onRegister = { route = "U07" },
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
                    regImageKey = lm.imageKey; regSituation = lm.situation.orEmpty(); regCategory = lm.category
                    regShowError = false; regConflict = null; editId = lm.id
                    route = "U05"
                },
                onDeactivate = { vm.v2SetActive(lm.id, false); toast("잠깐 사용하지 않아요") },
                onReconfirm = { startReconfirm(lm, from = "U09") },
                onDelete = { route = "U09D" },
                onToLibrary = ::goLibrary,
                onBack = { if (u09Return == "U08") goLibrary() else route = u09Return },
                onHelp = ::help,
            )
        } ?: V2MissingMeaning({ goLibrary() }, ::goHome, ::help)

        "U09D" -> libMeaning?.let { lm ->
            U09Delete(
                meaning = lm,
                onConfirmDelete = {
                    scope.launch {
                        runCatching { vm.v2DeleteMeaning(lm.id) }.onSuccess {
                            toast("뜻을 삭제했습니다."); libId = -1L; if (u09Return == "U08") goLibrary() else { route = u09Return }
                        }.onFailure { toast("삭제를 마치지 못했습니다. 자료를 확인하고 다시 시도해야 합니다.") }
                    }
                },
                onKeep = { route = "U09" },
                onHelp = ::help,
            )
        } ?: V2MissingMeaning({ goLibrary() }, ::goHome, ::help)

        "U11" -> U11Archive(
            meanings = archived,
            onConfirm = { picked -> libId = picked.id; startReconfirm(picked, from = "U11") },
            onOpen = { picked -> libId = picked.id; u09Return = "U11"; route = "U09" },
            onToLibrary = ::goLibrary,
            onBack = { route = "U07" },
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
            onLibrary = ::goLibrary,
            onHelp = ::help,
            largeText = settings.fontScale > 1.05f,
            onLargeText = { large -> vm.setFontScale(if (large) 2.0f else 1.0f) },
            onPreferences = { route = "U12PF" }, onPrivacy = { route = "U12DATA" },
            onPrivacyGuide = { privacyReturn = "U12"; route = "U12PR" },
            onAppInfo = { route = "U12INFO" },
            onDeleteAll = { scope.launch { CommunicationIntents.clearOwnClipboard(ctx); resetRegForm(); runCatching { vm.wipeAllData() }
                .onSuccess { guardianUnlocked = false; resetRegForm(); route = "U01" }
                .onFailure { toast("일부 자료를 삭제하지 못했습니다. 다시 시도해야 합니다.") } } },
            onResetRanking = { scope.launch { vm.resetPersonalRanking(); toast("이 학생의 사용·교정 가중치를 초기화했습니다.") } },
            onLock = { guardianUnlocked = false; goHome() },
            trialLogging = trialLoggingEnabled,
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
            enabled = trialLoggingEnabled,
            summary = kr.voicemate.malitda.domain.TrialSummary.of(trialLogs),
            onToggle = { if (it && !pilot.canCollect) route = "U14SETUP" else vm.setTrialLogging(it) },
            config = pilot,
            exportCount = TrialCsv.exportable(trialLogs, vm.trialExportScope()).size,
            onConfigure = { route = "U14SETUP" },
            onWithdraw = { scope.launch { runCatching { vm.withdrawPilot() }
                .onSuccess { toast("동의를 철회하고 현재 학생의 기록과 임시 CSV를 정리했습니다.") }
                .onFailure { toast("철회 또는 자료 정리를 마치지 못했습니다. 다시 확인해야 합니다.") } } },
            onPause = { vm.pausePilot() }, onResume = { vm.resumePilot() }, onPauseReason = vm::pausePilot,
            onExport = {
                scope.launch {
                    runCatching {
                        val exportingProfile = profileId
                        val csv = vm.trialCsv()
                        check(guardianUnlocked && route == "U14" && vm.profileId.value == exportingProfile && owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
                        CommunicationIntents.shareAnonymousCsv(ctx, csv)
                    }
                        .onFailure { toast("CSV 파일을 공유하지 못했습니다. 다시 확인해야 합니다.") }
                }
            },
            onClear = { vm.clearTrialLogs(); toast("이 학생 기록을 지웠어요") },
            onBack = { route = "U07" },
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

        "U12DATA" -> DataControlScreen(dataCounts,
            onClearMeanings = { dataAction { vm.clearMeaningDictionary() } },
            onResetRanking = { dataAction { vm.resetPersonalRanking() } },
            onClearTrials = { dataAction { vm.deleteCurrentTrialRecords() } },
            onWipeAll = { dataAction { CommunicationIntents.clearOwnClipboard(ctx); vm.wipeAllData(); guardianUnlocked = false; route = "U01" } },
            onBack = { route = if (needsConsent) "U00" else "U07" }, onHelp = ::help,
            studentName = name, busy = dataBusy, error = dataError,
            onPrivacy = { privacyReturn = "U12DATA"; route = "U12PR" })
        "U12PF" -> V2ProfilePreferencesScreen(name, preferences, vm::setProfilePreferences,
            settings.fontScale, vm::setFontScale, settings.visualEmphasis, vm::setVisualEmphasis,
            { route = "U12" }, ::help, { vm.speak("이 속도로 소리를 확인합니다.") })
        "U12PR" -> V2ConsentScreen(onAccept = {}, onDecline = {}, onBack = { route = privacyReturn }, readOnly = true,
            onRevoke = { scope.launch { vm.withdrawPilot(); vm.c.settings.revokeConsent(); guardianUnlocked = false; vm.resetSession(); route = "U01" } })
        "U12INFO" -> V2AppInfoScreen(onBack = { route = "U12" }, onHelp = ::help,
            onProject = { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/choihyungyu0/malitda-android/issues"))) }.onFailure { toast("문의 페이지를 열지 못했습니다.") } })
        "U14SETUP" -> PilotSetupScreen(pilot, meanings,
            onSave = { config -> scope.launch { runCatching { vm.configurePilot(config) }
                .onSuccess { route = "U14" }.onFailure { toast(it.message ?: "실증 설정을 저장하지 못했습니다.") } } },
            onBack = { route = "U14" }, onHelp = ::help)
        "U14END" -> trialDraft?.let { draft -> PilotTaskCompletion(draft.delivered, draft.observedCorrect, draft.understood, draft.supporterHelped,
            vm::trialSetJudgedCorrect, vm::trialSetUnderstood, vm::trialSetHelped,
            onFinish = { vm.completePilotTask(); goHome() },
            onStop = { vm.pausePilot(); goHome() }, onBack = { route = pilotEndReturn }, onHelp = ::help) }
            ?: V2TaskEnded(onBack = { route = "U14" }, onHelp = ::help)
        "U14FEEDBACK" -> if (trialDraft != null) PilotFeedbackScreen(current = trialDraft!!.feedback,
            onSave = { if (vm.trialDraft.value != null) vm.trialSetFeedback(it); route = feedbackReturn }, onBack = { route = feedbackReturn }, onHelp = ::help,
            understood = trialDraft!!.understood, helped = trialDraft!!.supporterHelped,
            onUnderstood = vm::trialSetUnderstood, onHelped = vm::trialSetHelped,
            onAccidentalSend = { vm.trialAccidentalSendObserved(); toast("실증을 즉시 중단했습니다. 전달 상황을 확인해야 합니다."); route = "U14" })
        else V2TaskEnded(onBack = { route = "U14" }, onHelp = ::help)
        "U16" -> U16SpeechCandidates(raw = ui.raw.orEmpty(),
            meanings = if (trialLoggingEnabled && pilot.canCollect && pilot.condition == "A") emptyList()
                       else v2.matchIds.mapNotNull { id -> meanings.firstOrNull { it.id == id } },
            alternatives = if (trialLoggingEnabled && pilot.canCollect && pilot.condition == "A") emptyList() else (ui.candidates.map { it.text } + ui.alternatives).distinct(),
            candidateCount = preferences.candidateCount, context = v2.context,
            onContext = vm::setSituationContext,
            onRerank = { scope.launch { vm.v2ResolveFromStt() } },
            onMeaning = { vm.v2SelectMeaning(it, V2Origin.VOICE); route = "U03" },
            onLiteral = { beginLiteral(it, "U16") }, onListen = vm::speak,
            onCards = { route = "U06" }, onRespeak = ::startMic, onBack = ::goHome, onHelp = ::help)
        "U17" -> U17LiteralEdit(ui.draft, ui.raw, vm::updateDraft,
            onListen = { vm.speak(ui.draft) }, onConfirm = { if (vm.approveLiteral()) route = "U18" },
            onCards = { vm.revokeLiteral(); route = "U06" }, onBack = { vm.revokeLiteral(); if (literalReturn == "U01") goHome() else route = literalReturn }, onHelp = ::help)
        "U18" -> if (!vm.canDeliverLiteral()) {
            U17LiteralEdit(ui.draft, ui.raw, vm::updateDraft, { vm.speak(ui.draft) },
                { if (vm.approveLiteral()) route = "U18" }, { route = "U06" }, ::goHome, ::help)
        } else U18LiteralDeliver(ui.draft,
            onListen = { vm.approvedLiteralText()?.let(vm::speak) }, onStopSound = vm::stopSpeaking,
            onShare = { vm.approvedLiteralText()?.let { approved -> runCatching { CommunicationIntents.shareText(ctx, approved) }
                .onFailure { toast("공유하지 못했습니다.") } } },
            onCopy = { vm.approvedLiteralText()?.let { approved -> runCatching { CommunicationIntents.copySensitive(ctx, approved) }
                .onSuccess { toast("복사했습니다. 다른 앱에 붙여 넣을 수 있습니다.") }.onFailure { toast("복사하지 못했습니다.") } } },
            onSave = { val text = ui.draft; vm.revokeLiteral(); resetRegForm(); regMeaning = text; route = "U05" },
            onEdit = { vm.revokeLiteral(); route = "U17" }, onDone = { vm.completePilotTask(); goHome() }, onHelp = ::help,
            onFeedback = if (trialLoggingEnabled && trialDraft != null) ({ feedbackReturn = "U18"; route = "U14FEEDBACK" }) else null)

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
                onCancel = { if (v2.meaningId != null) route = "U03" else goHome() },
                onHelp = ::help,
            )
        }
    }
    }
}
