package kr.voicemate.malitda.data.repo

import androidx.room.withTransaction
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.voicemate.malitda.data.db.AppDatabase

/**
 * 명시적으로 확인한 삭제 요청만 실행한다. 호출 전에 녹음·그림 생성·진행 중 저장을 종료해야 한다.
 * 설정(DataStore)과 실증 조건은 호출자가 함께 지운다. 앱에 포함된 인식 모델은 사용자 자료가 아니다.
 */
class DataManagementRepository(
    private val db: AppDatabase,
    private val filesDir: File,
    private val cacheDir: File? = null,
) {
    /**
     * Clear this person's complete dictionary, including practice cards and legacy expressions.
     * Keep their profile, preferences and pilot results. Historical trial meaning IDs are not foreign keys.
     * The caller must first discard active forms/generation and pause any task that targets a deleted meaning.
     */
    suspend fun clearMeanings(profileId: Long) = withContext(Dispatchers.IO) {
        require(profileId > 0)
        db.withTransaction {
            db.dataManagementDao().run {
                deleteConfirmations(profileId)
                deleteMeaningExpressions(profileId)
                deleteMeanings(profileId)
                deleteExpressions(profileId)
                deleteCorrections(profileId)
            }
        }
        // Retain pictures still referenced by any other person's dictionary.
        MeaningImageStore.removeUnreferenced(filesDir, db.meaningDao().allImageKeys().toSet())
        // Old technical exports can contain the dictionary text that was just removed.
        clearPrivateExports()
    }

    suspend fun wipeProfile(profileId: Long) = withContext(Dispatchers.IO) {
        require(profileId > 0)
        db.withTransaction {
            db.dataManagementDao().run {
                deleteConfirmations(profileId)
                deleteMeaningExpressions(profileId)
                deleteMeanings(profileId)
                deleteTrials(profileId)
                deleteExpressions(profileId)
                deleteCorrections(profileId)
                deleteCounters(profileId)
                deleteProfile(profileId)
            }
        }
        MeaningImageStore.removeUnreferenced(filesDir, db.meaningDao().allImageKeys().toSet())
        // 이전 내보내기 파일은 특정 프로필 자료가 섞였을 수 있어 공유 캐시 전체를 비운다.
        clearPrivateExports()
    }

    suspend fun wipeAll() = withContext(Dispatchers.IO) {
        db.clearAllTables()
        listOf("meaning_media", "means_media", "testaudio", "eval").forEach { clearOwnedDirectory(filesDir, it) }
        clearPrivateExports()
    }

    suspend fun resetRanking(profileId: Long) = db.withTransaction {
        db.meaningDao().resetUseCount(profileId)
        db.correctionDao().deleteAll(profileId)
    }

    /** Generated CSV copies are temporary; primary records in other profiles are untouched. */
    suspend fun clearTrialExportCache() = withContext(Dispatchers.IO) {
        cacheDir?.let { clearOwnedDirectory(it, "trial_exports") }
    }

    /** 프로필별 삭제에서도 이름·문장이 든 이전 시험 CSV를 남기지 않는다. */
    private fun clearPrivateExports() {
        clearOwnedDirectory(filesDir, "eval")
        cacheDir?.let { root ->
            listOf("exports", "shares", "trial_exports", "csv_exports").forEach { clearOwnedDirectory(root, it) }
        }
    }

    private fun clearOwnedDirectory(parent: File, name: String) {
        val base = parent.canonicalFile
        val dir = File(base, name).canonicalFile
        require(dir.parentFile == base) { "잘못된 앱 자료 경로입니다." }
        if (!dir.exists()) return
        dir.walkBottomUp().forEach { entry ->
            val canonical = entry.canonicalFile
            require(canonical == dir || canonical.path.startsWith(dir.path + File.separator))
            check(entry.delete() || !entry.exists()) { "일부 자료를 지우지 못했습니다. 다시 시도해야 합니다." }
        }
    }
}
