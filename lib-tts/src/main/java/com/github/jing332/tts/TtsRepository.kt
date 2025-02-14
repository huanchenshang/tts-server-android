package com.github.jing332.tts

import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.BgmConfiguration
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.tts.manager.ITtsRepository
import com.github.jing332.tts.manager.StandbyInfo
import com.github.jing332.tts.manager.TtsConfiguration
import com.github.jing332.tts.manager.TtsConfiguration.Companion.toVO

internal class TtsRepository(
    val context: ManagerContext,
) : ITtsRepository {

    override fun init() {

    }

    override fun destroy() {
    }


    override fun getAllTts(): Map<Long, TtsConfiguration> {
        val groupWithTts = dbm.systemTtsV2.getAllGroupWithTts()
        val map = mutableMapOf<Long, TtsConfiguration>()
        val standbyConfigs =
            groupWithTts.flatMap { it.list }
                .filter { it.isEnabled && (it.config as? TtsConfigurationDTO)?.speechRule?.isStandby == true }
                .map {
                    val config = it.config as TtsConfigurationDTO
                    config.toVO()
                }
        for (group in groupWithTts) {
            for (tts in group.list.sortedBy { it.order }) {
                if (!tts.isEnabled) continue
                val c = tts.config; if (c !is TtsConfigurationDTO) continue

                val standby = standbyConfigs.find {
                    it.speechInfo.target == c.speechRule.target &&
                            it.speechInfo.tagRuleId == c.speechRule.tagRuleId &&
                            it.speechInfo.tagName == c.speechRule.tagName
                }
                map[tts.id] = TtsConfiguration(
                    speechInfo = c.speechRule,
                    audioParams = c.audioParams,
                    audioFormat = c.audioFormat,
                    source = c.source,
                    standbyConfig = standby
                )
            }
        }

        return map
    }


    override fun getAllBgm(): List<BgmConfiguration> {
        return dbm.systemTtsV2.allEnabled.map { it.config }.filterIsInstance<BgmConfiguration>()
    }
}