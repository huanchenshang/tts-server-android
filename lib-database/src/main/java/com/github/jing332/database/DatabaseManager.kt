package com.github.jing332.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import com.github.jing332.database.dao.PluginDao
import com.github.jing332.database.dao.ReplaceRuleDao
import com.github.jing332.database.dao.SpeechRuleDao
import com.github.jing332.database.dao.SystemTtsDao
import com.github.jing332.database.dao.SystemTtsV2Dao
import com.github.jing332.database.entities.SpeechRule
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.database.entities.replace.ReplaceRule
import com.github.jing332.database.entities.replace.ReplaceRuleGroup
import com.github.jing332.database.entities.systts.SystemTtsGroup
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.v1.SystemTts
import splitties.init.appCtx

val dbm: DatabaseManager by lazy {
    Room.databaseBuilder(appCtx, DatabaseManager::class.java, "systts.db")
        .allowMainThreadQueries()
        .build()
}


@Database(
    version = 26,
    entities = [
        SystemTts::class,
        SystemTtsV2::class,
        SystemTtsGroup::class,
        ReplaceRule::class,
        ReplaceRuleGroup::class,
        Plugin::class,
        SpeechRule::class,
    ],
    autoMigrations = [
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 10, to = 11),
        AutoMigration(from = 11, to = 12),
        AutoMigration(from = 12, to = 13, DatabaseManager.DeleteSystemTtsColumn::class),
        AutoMigration(from = 13, to = 14),
        AutoMigration(from = 14, to = 15),
        // 15-16
        AutoMigration(from = 16, to = 17),
        AutoMigration(from = 17, to = 18),
        AutoMigration(from = 18, to = 19),
        AutoMigration(from = 19, to = 20),
        AutoMigration(from = 20, to = 21),
        AutoMigration(from = 21, to = 22),
        AutoMigration(from = 22, to = 23),
        AutoMigration(from = 23, to = 24),
        AutoMigration(from = 24, to = 25),
        AutoMigration(from = 25, to = 26),
    ]
)
abstract class DatabaseManager : RoomDatabase() {
    abstract val systemTtsDao: SystemTtsDao
    abstract val systemTtsV2: SystemTtsV2Dao
    abstract val replaceRuleDao: ReplaceRuleDao
    abstract val pluginDao: PluginDao
    abstract val speechRuleDao: SpeechRuleDao

    companion object {
        private const val DATABASE_NAME = "systts.db"


        fun createDatabase(context: Context) = Room
            .databaseBuilder(context, DatabaseManager::class.java, DATABASE_NAME)
            .allowMainThreadQueries()
            .build()
    }

    @DeleteColumn(tableName = "sysTts", columnName = "isBgm")
    class DeleteSystemTtsColumn : AutoMigrationSpec
}