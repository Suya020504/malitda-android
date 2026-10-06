package kr.voicemate.malitda.ui.vm

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kr.voicemate.malitda.data.db.MeaningWithAliases
import kr.voicemate.malitda.data.repo.MeaningRepository
import kr.voicemate.malitda.di.AppContainer
import kr.voicemate.malitda.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class MeaningSession(
    val profileId: Long = 0,
    val sessionId: Long = 0,
    val rawText: String = "",
    val inputSource: String = "",
    val candidates: List<MeaningWithAliases> = emptyList(),
    val selected: MeaningWithAliases? = null,
    val approval: MeaningApprovalSnapshot? = null,
    val busy: Boolean = false,
    val error: String? = null,
    val ttsError: Boolean = false,
) {
    val approved: Boolean get() = selected?.meaning?.let { m -> approval?.matches(profileId, m.id, m.version, m.displayText, m.imageRef, m.status) } == true
}

sealed interface MeaningEvent {
    data object Candidates : MeaningEvent
    data object Confirm : MeaningEvent
    data object Present : MeaningEvent
    data object Home : MeaningEvent
    data object Stale : MeaningEvent
    data class Notice(val text: String) : MeaningEvent
}

/** 의미 선택을 기존 STT 교정 저장과 분리한다. AI는 재정렬만 하며 학생 승인을 만들 수 없다. */
@OptIn(ExperimentalCoroutinesApi::class)
class MeaningViewModel(private val c: AppContainer, private val profile: StateFlow<Long>) : ViewModel() {
    val items = profile.flatMapLatest { c.meanings.observe(it) }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val _state = MutableStateFlow(MeaningSession())
    val state = _state.asStateFlow()
    private val _events = MutableSharedFlow<MeaningEvent>(extraBufferCapacity = 12)
    val events = _events.asSharedFlow()
    private val guard = MeaningOperationGuard()
    private var resolving: Job? = null

    init {
        viewModelScope.launch {
            profile.collect { pid ->
                val previous = _state.value.profileId
                if (pid != previous) {
                    reset(pid)
                    // 첫 프로필 로딩은 복원한 등록 화면을 덮어쓰지 않는다.
                    if (previous > 0L) _events.emit(MeaningEvent.Home)
                }
            }
        }
        viewModelScope.launch {
            items.collect { current ->
                val s = _state.value
                val old = s.selected?.meaning ?: return@collect
                val now = current.firstOrNull { it.meaning.id == old.id }?.meaning
                if (now == null || now.version != old.version || now.status != "CONFIRMED" || now.profileId != s.profileId) {
                    stale()
                }
            }
        }
    }

    private fun advance(pid: Long = profile.value): MeaningOperationGuard.Token {
        resolving?.cancel(); c.tts.stop()
        val token = guard.begin(pid)
        _state.update { it.copy(profileId = pid, sessionId = token.epoch, approval = null, busy = false) }
        return token
    }

    private fun current(token: MeaningOperationGuard.Token) = guard.accepts(token, profile.value)
    private fun token(s: MeaningSession = _state.value) = MeaningOperationGuard.Token(s.sessionId, s.profileId)

    private fun stale() {
        advance()
        _state.update { it.copy(selected = null, candidates = emptyList(), error = "등록한 뜻이 바뀌었어요. 다시 확인해 주세요.") }
        _events.tryEmit(MeaningEvent.Stale)
    }

    fun reset(pid: Long = profile.value) {
        val token = advance(pid)
        _state.value = MeaningSession(profileId = pid, sessionId = token.epoch)
    }

    fun resolveRecognition(raw: String, recognition: List<Candidate>) {
        reset()
        val requestToken = token(); val sid = requestToken.epoch; val pid = requestToken.profileId
        _state.update { it.copy(rawText = raw, inputSource = "들은 말: $raw", busy = true) }
        resolving = viewModelScope.launch {
            try {
                val all = c.meanings.observe(pid).first().filter { it.meaning.status == "CONFIRMED" }
                val texts = (listOf(raw) + recognition.map { it.text }).map(MeaningRepository::normalizeAlias).filter { it.isNotEmpty() }.toSet()
                val matched = all.filter { item -> item.aliases.any { it.normalizedAlias in texts } }
                val req = MeaningRankingRequest(pid, sid, raw, matched.map { x -> MeaningRankingCandidate(x.meaning.id, x.meaning.version, x.meaning.displayText, x.meaning.contextLabel, x.aliases.firstOrNull { it.normalizedAlias in texts }?.rawAlias.orEmpty()) }, null)
                val reply = if (matched.size < 2) null else withTimeoutOrNull(1_500) { try { c.meaningRanker.rank(req) } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { null } }
                val ranking = validateMeaningRankingResult(req, reply)
                if (!current(requestToken)) return@launch
                val fresh = c.meanings.observe(pid).first().associateBy { it.meaning.id }
                if (!current(requestToken)) return@launch
                if (matched.any { fresh[it.meaning.id]?.meaning?.let { m -> m.version != it.meaning.version || m.status != "CONFIRMED" } != false }) {
                    stale(); return@launch
                }
                val ordered = ranking.orderedMeaningIds.mapNotNull { id -> matched.firstOrNull { it.meaning.id == id } }
                _state.update { it.copy(candidates = ordered, busy = false) }
                if (ordered.size == 1) select(ordered.first().meaning.id) else _events.emit(MeaningEvent.Candidates)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                if (current(requestToken)) { _state.update { it.copy(busy = false, error = "등록한 뜻을 불러올 수 없어요. 다시 시도해 주세요.") }; _events.emit(MeaningEvent.Candidates) }
            }
        }
    }

    fun useCard(id: Long) { reset(); _state.update { it.copy(inputSource = "카드로 고른 뜻") }; select(id) }
    fun select(id: Long) {
        val requestToken = advance()
        _state.update { it.copy(selected = null, ttsError = false, error = null) }
        viewModelScope.launch {
            try {
                val item = c.meanings.get(requestToken.profileId, id)
                if (!current(requestToken)) return@launch
                if (item == null || item.meaning.status != "CONFIRMED") { stale(); return@launch }
                _state.update { it.copy(selected = item) }
                _events.emit(MeaningEvent.Confirm)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (current(requestToken)) stale() }
        }
    }

    fun chooseAgain() {
        advance()
        _state.update { it.copy(selected = null, ttsError = false) }
        _events.tryEmit(if (_state.value.rawText.isBlank()) MeaningEvent.Home else MeaningEvent.Candidates)
    }
    fun approve() {
        val s = _state.value; val m = s.selected?.meaning ?: return
        val requestToken = advance()
        viewModelScope.launch {
            try {
                val fresh = c.meanings.get(s.profileId, m.id)?.meaning
                if (!current(requestToken)) return@launch
                if (fresh == null || fresh.version != m.version || fresh.status != "CONFIRMED") { stale(); return@launch }
                _state.update { it.copy(approval = MeaningApprovalSnapshot.capture(s.profileId, m.id, m.version, m.displayText, m.imageRef)) }
                _events.emit(MeaningEvent.Present)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (current(requestToken)) stale() }
        }
    }

    private suspend fun deliveryText(): String? {
        val s = _state.value; val m = s.selected?.meaning ?: return null
        val requestToken = token(s)
        val fresh = c.meanings.get(profile.value, m.id)?.meaning
        if (!current(requestToken)) return null
        if (fresh == null || _state.value.approval != s.approval || s.approval?.matches(profile.value, fresh.id, fresh.version, fresh.displayText, fresh.imageRef, fresh.status) != true) {
            stale(); return null
        }
        return fresh.displayText
    }

    fun speakPreview(text: String) {
        val requestToken = token()
        viewModelScope.launch {
            try {
                val rate = c.settings.current().ttsRate
                if (!current(requestToken)) return@launch
                c.tts.stop()
                if (!c.tts.speak(text, rate)) {
                    _state.update { it.copy(ttsError = true) }
                    _events.emit(MeaningEvent.Notice("소리를 사용할 수 없어요. 그림과 문장으로 확인해 주세요."))
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                if (current(requestToken)) {
                    _state.update { it.copy(ttsError = true) }
                    _events.emit(MeaningEvent.Notice("소리를 사용할 수 없어요. 그림과 문장으로 확인해 주세요."))
                }
            }
        }
    }
    fun speakApproved() {
        val requestToken = token()
        viewModelScope.launch {
            try {
                val rate = c.settings.current().ttsRate
                if (!current(requestToken)) return@launch
                val text = deliveryText() ?: return@launch
                if (!current(requestToken)) return@launch
                // 마지막 검증과 실제 출력 사이에는 중단 가능한 작업을 두지 않는다.
                c.tts.stop(); _state.update { it.copy(ttsError = !c.tts.speak(text, rate)) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (current(requestToken)) stale() }
        }
    }
    fun shareApproved(context: Context) {
        val requestToken = token()
        viewModelScope.launch {
            try {
                if (!current(requestToken)) return@launch
                val text = deliveryText() ?: return@launch
                if (!current(requestToken)) return@launch
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, "공유할 앱 선택").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                // 공유창을 연 사실은 전송 성공이 아니다. 돌아와도 승인된 세션을 유지한다.
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (current(requestToken)) _events.emit(MeaningEvent.Notice("공유할 수 있는 앱이 없어요. 화면으로 보여 주세요.")) }
        }
    }

    override fun onCleared() { resolving?.cancel(); c.tts.stop() }
    companion object {
        fun factory(c: AppContainer, profile: StateFlow<Long>) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T = MeaningViewModel(c, profile) as T
        }
    }
}
