package kr.voicemate.malitda.data.migration

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.util.UUID
import kr.voicemate.malitda.data.db.DbKeyManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DbKeyRecoveryTest {
    private class IsolatedContext(base: Context) : ContextWrapper(base) {
        private val prefix = "key-recovery-test-" + UUID.randomUUID()
        val database = File(base.cacheDir, prefix + ".db")
        override fun getApplicationContext(): Context = this
        override fun getDatabasePath(name: String): File = database
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            baseContext.getSharedPreferences(prefix + "-" + name, mode)
    }

    @Test fun existingDatabaseWithoutKeyIsNeverAssignedANewKey() {
        val context = IsolatedContext(ApplicationProvider.getApplicationContext())
        val prefs = context.getSharedPreferences("malitda_keys", Context.MODE_PRIVATE)
        try {
            context.database.writeBytes(ByteArray(100) { 7 })
            assertTrue(runCatching { DbKeyManager(context).getOrCreateKey() }.exceptionOrNull() is IllegalStateException)
            assertTrue(prefs.all.isEmpty())
            assertEquals(100L, context.database.length())
        } finally { context.database.delete(); prefs.edit().clear().commit() }
    }

    @Test fun damagedWrappedKeyIsPreservedForRecovery() {
        val context = IsolatedContext(ApplicationProvider.getApplicationContext())
        val prefs = context.getSharedPreferences("malitda_keys", Context.MODE_PRIVATE)
        try {
            DbKeyManager(context).getOrCreateKey()
            prefs.edit().putString("db_key_wrapped", "damaged-key").commit()
            val before = prefs.all.toMap()
            assertTrue(runCatching { DbKeyManager(context).getOrCreateKey() }.exceptionOrNull() is IllegalStateException)
            assertEquals(before, prefs.all)
        } finally { prefs.edit().clear().commit() }
    }

    @Test fun normalWrappedKeyCanBeReadAgain() {
        val context = IsolatedContext(ApplicationProvider.getApplicationContext())
        val prefs = context.getSharedPreferences("malitda_keys", Context.MODE_PRIVATE)
        try {
            val first = DbKeyManager(context).getOrCreateKey()
            assertArrayEquals(first, DbKeyManager(context).getOrCreateKey())
        } finally { prefs.edit().clear().commit() }
    }
}
