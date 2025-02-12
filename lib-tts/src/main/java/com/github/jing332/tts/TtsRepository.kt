package com.github.jing332.tts

import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.BgmConfiguration
import com.github.jing332.database.entities.systts.IConfiguration
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.tts.manager.ITtsRepository
import com.github.jing332.tts.manager.StandbyInfo
import com.github.jing332.tts.manager.TtsConfiguration

internal class TtsRepository(
    val context: ManagerContext,
) : ITtsRepository {

    private val cache = mutableMapOf<Long, IConfiguration>()
    override fun onInit() {
        val list = dbm.systemTtsV2.allEnabled
        for (tts in list) {
            cache[tts.id] = tts.config
        }
    }

    override fun onDestroy() {
        cache.clear()
    }

    private inline fun <reified T : IConfiguration> filterConfigType(): Map<Long, T> {
        val map = mutableMapOf<Long, T>()
        for (tts in cache) {
            if (tts.value is T) {
                map[tts.key] = tts.value as T
            }
        }
        return map
    }

    override fun getAllTts(): Map<Long, TtsConfiguration> {
        val groupWithTts = dbm.systemTtsV2.getAllGroupWithTts()
        val map = mutableMapOf<Long, TtsConfiguration>()
        val standbyConfigs =
            groupWithTts.flatMap { it.list }
                .filter { it.isEnabled  && (it.config as? TtsConfigurationDTO)?.speechRule?.isStandby == true }
                .map {
                    val config = it.config as TtsConfigurationDTO
                    TtsConfiguration(
                        speechInfo = config.speechRule,
                        audioParams = config.audioParams,
                        audioFormat = config.audioFormat,
                        source = config.source,
                        standbyInfo = null
                    )
                }
        for (group in groupWithTts) {
            for (tts in group.list.sortedBy { it.order }) {
                if (!tts.isEnabled) continue
                val c = tts.config; if (c !is TtsConfigurationDTO) continue

                map[tts.id] = TtsConfiguration(
                    speechInfo = c.speechRule,
                    audioParams = c.audioParams,
                    audioFormat = c.audioFormat,
                    source = c.source,
                    standbyInfo =
                    if (standbyConfigs.isEmpty()) null
                    else {
                        StandbyInfo(config = standbyConfigs.find {
                            it.speechInfo.target == c.speechRule.target &&
                                    it.speechInfo.tagRuleId == c.speechRule.tagRuleId &&
                                    it.speechInfo.tag == c.speechRule.tagName
                        })
                    }
                )
            }
        }

        return map
    }


    override fun getAllBgm(): Map<Long, BgmConfiguration> = filterConfigType<BgmConfiguration>()
}