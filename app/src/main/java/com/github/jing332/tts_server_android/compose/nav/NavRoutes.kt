package com.github.jing332.tts_server_android.compose.nav

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.constant.AppConst

sealed class NavRoutes(
    val id: String,
    @StringRes val strId: Int,
    val icon: @Composable () -> Unit = {},
) {
    companion object {
        val routes by lazy {
            listOf(
                SystemTTS,
                SystemTtsForwarder,
               Settings
            )
        }
    }

    data object SystemTTS : NavRoutes("system_tts", R.string.system_tts, icon = {
        Icon(modifier = Modifier.size(24.dp), painter = painterResource(id = R.drawable.ic_tts), contentDescription  = null)
    }){
        @Suppress("DEPRECATION")
        class ParamType: NavType<SystemTtsV2>(isNullableAllowed = false){
            override fun get(bundle: Bundle, key: String): SystemTtsV2? {
                return bundle.getParcelable(key)
            }

            override fun parseValue(value: String): SystemTtsV2 {
                return  AppConst.jsonBuilder.decodeFromString(value)
             }

            override fun put(bundle: Bundle, key: String, value: SystemTtsV2) {
                bundle.putParcelable(key, value)
            }

        }
    }

    data object SystemTtsForwarder :
        NavRoutes("system_tts_forwarder", R.string.forwarder_systts, icon = {
            Icon(modifier = Modifier.size(24.dp), painter = painterResource(id = R.drawable.ic_tts), contentDescription = null)
        })


    data object Settings : NavRoutes("settings", R.string.settings, icon = {
        Icon(Icons.Default.Settings, null)
    })

    // =============
    data object TtsEdit : NavRoutes("tts_edit", 0) {
        const val DATA = "data"
    }
}