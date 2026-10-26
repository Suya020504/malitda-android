package kr.voicemate.malitda.ui.vm

import android.app.Application
import android.graphics.Bitmap
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.InputStream
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kr.voicemate.malitda.data.db.AppDatabase
import kr.voicemate.malitda.data.settings.SettingsStore
import kr.voicemate.malitda.di.AppContainer
import kr.voicemate.malitda.stt.*
import kr.voicemate.malitda.ui.testing.CoreQaActivity
import kr.voicemate.malitda.ui.theme.MalitdaTheme
import kr.voicemate.malitda.ui.v2.MalitdaV2App
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the actual V2 composables using synthetic speech and isolated storage; no live mic or external send. */
@RunWith(AndroidJUnit4::class)
class CoreCommunicationUiTest {
    private val inst = InstrumentationRegistry.getInstrumentation()
    private val args = InstrumentationRegistry.getArguments()
    private val output by lazy { File(inst.targetContext.getExternalFilesDir(null), "core-ui-" + (args.getString("captureLabel") ?: "default")).apply { mkdirs() } }
    private val raw = "휴시글 취하고 싶습니다."
    private val formal = "휴식을 취하고 싶습니다."
    private val easy = "쉬고 싶어요."

    private fun nodes(n: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> =
        if (n == null) emptyList() else listOf(n) + (0 until n.childCount).flatMap { nodes(n.getChild(it)) }
    private fun current() = nodes(inst.uiAutomation.rootInActiveWindow)
    private fun scrollContainer() = current().filter { it.isScrollable && it.isVisibleToUser }.maxByOrNull {
        val bounds=android.graphics.Rect();it.getBoundsInScreen(bounds);bounds.width()*bounds.height()
    }
    private fun swipeContainer(forward: Boolean) {
        val container=scrollContainer() ?: return
        val bounds=android.graphics.Rect();container.getBoundsInScreen(bounds)
        val x=bounds.centerX().toFloat();val lower=(bounds.bottom-bounds.height()*0.18f);val upper=(bounds.top+bounds.height()*0.18f)
        val from=if(forward)lower else upper;val to=if(forward)upper else lower
        val down=SystemClock.uptimeMillis()
        fun send(action:Int,y:Float) {val e=android.view.MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y,0);e.source=android.view.InputDevice.SOURCE_TOUCHSCREEN;inst.uiAutomation.injectInputEvent(e,true);e.recycle()}
        send(android.view.MotionEvent.ACTION_DOWN,from)
        repeat(12){step->SystemClock.sleep(25);send(android.view.MotionEvent.ACTION_MOVE,from+(to-from)*(step+1)/12)}
        send(android.view.MotionEvent.ACTION_UP,to);SystemClock.sleep(400)
    }
    private fun found(text: String) = current().firstOrNull { it.isVisibleToUser && (it.text?.toString() == text || it.contentDescription?.toString() == text) }
    private fun waitFor(text: String): AccessibilityNodeInfo {
        repeat(50) { found(text)?.let { return it }; SystemClock.sleep(160) }
        error("Missing visible UI: $text; seen=" + current().mapNotNull { it.text?.toString() }.joinToString(" | "))
    }
    private fun scrollTo(text: String): AccessibilityNodeInfo {
        found(text)?.let { return it }
        for (direction in listOf(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)) {
            repeat(9) {
                swipeContainer(direction==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                found(text)?.let { return it }
            }
        }
        capture("failure-scroll")
        File(output,"failure-scroll-nodes.txt").writeText(current().filter{it.isScrollable}.joinToString("\n"){
            val bounds=android.graphics.Rect();it.getBoundsInScreen(bounds);"${it.className} ${bounds} ${it.actionList}"
        })
        return waitFor(text)
    }
    private fun click(text: String) {
        var node: AccessibilityNodeInfo? = scrollTo(text)
        while (node != null && !node.isClickable) node = node.parent
        assertNotNull("No clickable parent: $text", node)
        assertTrue("Click failed: $text",node!!.performAction(AccessibilityNodeInfo.ACTION_CLICK));SystemClock.sleep(220)
    }
    private fun capture(name: String) {
        SystemClock.sleep(220)
        val shot=inst.uiAutomation.takeScreenshot() ?: error("screenshot unavailable")
        File(output,"$name.png").outputStream().use { shot.compress(Bitmap.CompressFormat.PNG,100,it) };shot.recycle()
        File(output,"$name.txt").writeText(current().mapNotNull { it.text?.toString() }.joinToString("\n"))
    }
    private fun edit(text: String) {
        val node=current().firstOrNull { it.isEditable } ?: error("No text input")
        assertTrue(node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,Bundle().apply {putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,text)}))
        SystemClock.sleep(180)
    }

    @Test fun inputRecoveryAndPersonalCorrectionStayUnderThePersonsControl() = runBlocking {
        val app=ApplicationProvider.getApplicationContext<Application>()
        val db=Room.inMemoryDatabaseBuilder(app,AppDatabase::class.java).build()
        val prefsScope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
        val prefsFile=File(app.cacheDir,"core-ui-${UUID.randomUUID()}.preferences_pb")
        val prefs=SettingsStore(app,PreferenceDataStoreFactory.create(scope=prefsScope,produceFile={prefsFile}))
        prefs.setV2Seeded(true);prefs.setOnboarded(true);prefs.acceptCurrentConsent()
        val fake=object:SttEngine {
            override val name="Synthetic UI fixture"
            override val prepareState=MutableStateFlow<PrepareState>(PrepareState.Ready(0,0))
            var listener:SttCallback?=null
            override suspend fun prepare() {}
            override fun startListening(callback:SttCallback):Boolean {listener=callback;return true}
            override fun stop() {val callback=listener;listener=null;callback?.onFinal(SttResult(raw,listOf(raw),0,0))}
            override fun cancel(){listener=null}
            override suspend fun recognizeWav(input:InputStream)=error("No audio used in this UI fixture")
            override fun release(){listener=null}
        }
        val c=AppContainer(app,db,prefs,fake)
        val pid=c.profiles.ensureCurrent();c.profiles.rename(pid,"기본 흐름 검증")
        lateinit var vm:SessionViewModel
        val scenario=ActivityScenario.launch(CoreQaActivity::class.java)
        try {
            scenario.onActivity { activity ->
                vm=SessionViewModel(c);activity.viewModelStore.put("core-ui",vm);activity.qaViewModel=vm
                activity.setContent { MalitdaTheme { MalitdaV2App(vm) } }
            }
            withTimeout(10000){vm.profileId.filter{it==pid}.first()}
            waitFor("말잇다");SystemClock.sleep(300)
            assertNotNull("Speech should be visible before dictionary registration",found("말하기"))
            assertNotNull("Direct input should be visible before dictionary registration",found("문장 직접 입력"))
            capture("00-empty-home")
            click("말하기");waitFor("■  말하기 끝내기");click("■  말하기 끝내기")
            waitFor("전할 뜻을 골라 주세요");capture("01-raw-candidates")
            click("직접 고치기");waitFor("전할 문장을 확인해 주세요");edit(formal)
            click("쉬운 말 제안 보기");scrollTo("이 문장으로 고치기");capture("02-easy-comparison")
            assertEquals(formal,vm.ui.value.draft);assertFalse(vm.canDeliverLiteral())
            click("원문 유지");assertEquals(formal,vm.ui.value.draft)
            click("쉬운 말 제안 보기")
            click("이 문장으로 고치기");assertEquals(easy,vm.ui.value.draft)
            click("잘 모르겠어요");waitFor("천천히 다시 확인해요");capture("03-unsure")
            assertFalse(vm.canDeliverLiteral());click("함께 다시 확인")
            click("이 문장이 맞아요");waitFor("이 문장으로 전합니다");capture("04-approved")
            assertEquals(easy,vm.approvedLiteralText());assertTrue(db.correctionDao().all(pid).isEmpty())
            click("다음에도 이 수정 사용");waitFor("이 수정을 다음에도 쓸까요?");capture("05-save-choice")
            click("이번만 사용");assertTrue(db.correctionDao().all(pid).isEmpty())
            if(args.getString("layoutOnly")=="true") return@runBlocking
            click("다음에도 이 수정 사용");click("수정 저장")
            withTimeout(10000){while(db.correctionDao().all(pid).isEmpty())delay(50)}
            click("저장한 수정 보기");waitFor("저장한 수정");capture("06-saved-correction")
            click("이 문장 확인하기");waitFor("전할 문장을 확인해 주세요")
            assertFalse(vm.canDeliverLiteral());assertNull(vm.ui.value.raw)
            click("이 문장이 맞아요");click("공유할 앱 선택")
            SystemClock.sleep(600);capture("07-share-chooser-no-send")
            inst.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);SystemClock.sleep(450)
            assertEquals(easy,vm.ui.value.draft)
            if(found("전할 문장을 확인해 주세요")!=null)click("이 문장이 맞아요")
            click("마치고 홈으로");click("말하기");waitFor("■  말하기 끝내기");click("■  말하기 끝내기")
            waitFor("전에 확인하고 저장한 수정");capture("08-next-candidate")
            assertFalse(vm.canDeliverLiteral());assertEquals(raw,vm.ui.value.raw)
            // Clear the communication state before opening the personal list.
            inst.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);SystemClock.sleep(220)
            click("내 표현");click("저장한 수정 · 1개");click("이 수정 기록 삭제")
            waitFor("이 수정 기록을 지울까요?");capture("09-delete-confirmation");click("기록 삭제")
            withTimeout(10000){while(db.correctionDao().all(pid).isNotEmpty())delay(50)}
            waitFor("저장한 수정이 없습니다. 말한 문장을 고쳐 확인한 뒤, 다음에도 쓸 수정을 따로 저장할 수 있습니다.")
            capture("10-empty-history")
        } finally {
            scenario.close();c.tts.release();c.appScope.cancel();prefsScope.cancel();delay(50);db.close();prefsFile.delete()
        }
    }
}
