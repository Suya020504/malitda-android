package kr.voicemate.malitda.ui.vm

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kr.voicemate.malitda.data.db.AppDatabase
import kr.voicemate.malitda.data.settings.SettingsStore
import kr.voicemate.malitda.di.AppContainer
import kr.voicemate.malitda.domain.CandidateSource
import kr.voicemate.malitda.domain.EasyText
import kr.voicemate.malitda.domain.VoiceCandidateOptions
import kr.voicemate.malitda.stt.SttResult
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Real V2 state/repository integration, isolated DB and preferences; synthetic recognizer result, no microphone. */
@RunWith(AndroidJUnit4::class)
class CoreCommunicationIntegrationTest {
    private lateinit var db: AppDatabase
    private lateinit var c: AppContainer
    private lateinit var vm: SessionViewModel
    private lateinit var store: ViewModelStore
    private lateinit var prefsScope: CoroutineScope
    private lateinit var prefsFile: File
    private var pid = 0L

    @Before fun setUp(): Unit = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java).build()
        prefsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        prefsFile = File(app.cacheDir, "core-communication-${UUID.randomUUID()}.preferences_pb")
        val prefs = SettingsStore(app, PreferenceDataStoreFactory.create(scope = prefsScope, produceFile = { prefsFile }))
        prefs.setV2Seeded(true); prefs.acceptCurrentConsent(); prefs.setHaptics(false)
        c = AppContainer(app, db, prefs)
        pid = c.profiles.ensureCurrent()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            store = ViewModelStore(); vm = SessionViewModel(c, prepareSpeechOnInit = false); store.put("core", vm)
        }
        withTimeout(10000) { vm.profileId.filter { it == pid }.first(); vm.loadedSettings.filterNotNull().filter { it.consentAccepted }.first() }
    }

    @After fun tearDown(): Unit = runBlocking {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { store.clear(); c.tts.release() }
        c.appScope.cancel(); prefsScope.cancel(); delay(50); db.close(); prefsFile.delete(); Unit
    }

    private suspend fun hear(raw: String) = withContext(Dispatchers.Main) {
        vm.onSttFinal(SttResult(raw, listOf(raw), 0, 0))
    }

    private suspend fun approve(raw: String = "무를 주세요", corrected: String = "물을 주세요") {
        hear(raw)
        withContext(Dispatchers.Main) { vm.beginLiteral(raw); vm.updateDraft(corrected); assertTrue(vm.approveLiteral()) }
    }

    @Test fun approvalAloneDoesNotStoreAnUtterance() = runBlocking {
        approve()
        assertNotNull(vm.correctionLearningOffer()); assertTrue(db.correctionDao().all(pid).isEmpty())
        assertTrue(vm.canDeliverLiteral())
    }

    @Test fun explicitSaveBecomesNextCandidateButNeverNextApproval() = runBlocking {
        approve(); val request=vm.correctionLearningOffer()!!
        assertTrue(vm.rememberCorrection(request));assertTrue(vm.rememberCorrection(request))
        assertEquals(1, db.correctionDao().all(pid).size)
        hear("무를 주세요")
        val ui=vm.ui.value
        assertEquals("무를 주세요",ui.raw)
        assertEquals(CandidateSource.M1_EXACT,ui.candidates.first().source)
        assertEquals("물을 주세요",ui.candidates.first().text)
        assertFalse(vm.canDeliverLiteral())
        val choices=VoiceCandidateOptions.build(pid,ui.raw!!,emptyList(),ui.candidates,ui.alternatives,1)
        assertEquals("물을 주세요",choices.single().text)
    }

    @Test fun editedOrRevokedApprovalCannotBeSavedFromAnOldDialog() = runBlocking {
        approve();val request=vm.correctionLearningOffer()!!
        withContext(Dispatchers.Main){vm.updateDraft("약을 주세요")}
        assertTrue(runCatching { vm.rememberCorrection(request) }.isFailure)
        assertTrue(db.correctionDao().all(pid).isEmpty())
    }

    @Test fun switchingProfileDoesNotWriteOrReadAnotherPersonsCorrection() = runBlocking {
        approve();val request=vm.correctionLearningOffer()!!
        val other=c.profiles.add("합성 확인 B");c.profiles.switchTo(other)
        withTimeout(10000){vm.profileId.filter{it==other}.first()}
        assertNull(vm.ui.value.raw)
        assertEquals("",vm.ui.value.draft)
        assertTrue(runCatching{vm.rememberCorrection(request)}.isFailure)
        assertTrue(db.correctionDao().all(pid).isEmpty());assertTrue(db.correctionDao().all(other).isEmpty())
    }

    @Test fun consentWithdrawalBlocksAnOutstandingSaveRequest() = runBlocking {
        approve();val request=vm.correctionLearningOffer()!!
        c.settings.revokeConsent()
        assertTrue(runCatching{vm.rememberCorrection(request)}.isFailure)
        assertTrue(db.correctionDao().all(pid).isEmpty())
    }
    @Test fun dictionarySnapshotCarriesItsOwnerAndDistinguishesLoadingFromEmpty() = runBlocking {
        val initial=withTimeout(10000){vm.meaningSnapshot.filter{it.profileId==pid&&it.items!=null}.first()}
        assertTrue(initial.items!!.isEmpty())
        val other=c.profiles.add("합성 빈 사전 B")
        c.profiles.switchTo(other)
        val next=withTimeout(10000){vm.meaningSnapshot.filter{it.profileId==other&&it.items!=null}.first()}
        assertTrue(next.items!!.isEmpty());assertEquals(other,next.profileId)
    }

    @Test fun storedTextNeedsFreshConfirmationAndDeletionIsProfileBound() = runBlocking {
        approve();assertTrue(vm.rememberCorrection(vm.correctionLearningOffer()!!))
        val row=db.correctionDao().all(pid).single()
        assertFalse(c.corrections.deleteForProfile(pid+1,row.id))
        assertTrue(vm.useSavedCorrection(row));assertEquals(row.approvedText,vm.ui.value.draft)
        assertNull(vm.ui.value.raw);assertFalse(vm.canDeliverLiteral());assertNull(vm.correctionLearningOffer())
        assertTrue(vm.deleteSavedCorrection(pid,row.id));assertFalse(vm.useSavedCorrection(row))
    }

    @Test fun easierSuggestionMustBeAppliedAndConfirmedAsNewText() = runBlocking {
        approve("휴시글 취하고 싶습니다.","휴식을 취하고 싶습니다.")
        val suggestion=EasyText.suggest(vm.ui.value.draft)!!
        assertEquals("휴식을 취하고 싶습니다.",vm.approvedLiteralText())
        withContext(Dispatchers.Main){vm.updateDraft(suggestion.text)}
        assertFalse(vm.canDeliverLiteral());assertNull(vm.correctionLearningOffer())
        withContext(Dispatchers.Main){assertTrue(vm.approveLiteral())}
        assertEquals("쉬고 싶어요.",vm.approvedLiteralText())
        assertTrue(db.correctionDao().all(pid).isEmpty())
    }
}
