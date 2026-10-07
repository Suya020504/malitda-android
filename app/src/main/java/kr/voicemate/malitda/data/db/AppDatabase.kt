package kr.voicemate.malitda.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        ProfileEntity::class, ExpressionEntity::class, CorrectionEntity::class, CounterEntity::class,
        MeaningEntry::class, ExpressionAlias::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun expressionDao(): ExpressionDao
    abstract fun correctionDao(): CorrectionDao
    abstract fun counterDao(): CounterDao
    abstract fun meaningDao(): MeaningDao
    abstract fun expressionAliasDao(): ExpressionAliasDao

    companion object {
        const val NAME = "malitda.db"

        /** 기존 프로필·문장·교정·카운터를 그대로 두고 개인 의미 테이블만 추가한다. */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `meaning_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `profileId` INTEGER NOT NULL,
                        `displayText` TEXT NOT NULL,
                        `imageRef` TEXT,
                        `contextLabel` TEXT,
                        `status` TEXT NOT NULL,
                        `version` INTEGER NOT NULL DEFAULT 1,
                        `confirmationMethod` TEXT,
                        `confirmedBy` TEXT,
                        `confirmedAt` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`profileId`) REFERENCES `profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_meaning_entries_profileId_id` ON `meaning_entries` (`profileId`, `id`)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `expression_aliases` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `profileId` INTEGER NOT NULL,
                        `meaningId` INTEGER NOT NULL,
                        `rawAlias` TEXT NOT NULL,
                        `normalizedAlias` TEXT NOT NULL,
                        FOREIGN KEY(`profileId`, `meaningId`) REFERENCES `meaning_entries`(`profileId`, `id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_expression_aliases_profileId_normalizedAlias_meaningId` ON `expression_aliases` (`profileId`, `normalizedAlias`, `meaningId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expression_aliases_profileId_meaningId` ON `expression_aliases` (`profileId`, `meaningId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expression_aliases_meaningId` ON `expression_aliases` (`meaningId`)")
            }
        }

        /** SQLCipher로 암호화된 로컬 DB. 키는 Android Keystore가 감싼 무작위 32바이트. */
        fun build(context: Context, passphrase: ByteArray): AppDatabase {
            System.loadLibrary("sqlcipher")
            return Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME)
                .openHelperFactory(SupportOpenHelperFactory(passphrase))
                .addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}
