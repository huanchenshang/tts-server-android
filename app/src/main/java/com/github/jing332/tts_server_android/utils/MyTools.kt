package com.github.jing332.tts_server_android.utils

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.github.jing332.common.utils.longToast
import com.github.jing332.common.utils.toast
import com.github.jing332.tts_server_android.R
import splitties.systemservices.powerManager


object MyTools {
    const val TAG = "MyTools"

    fun Context.isIgnoringBatteryOptimizations(): Boolean{
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(packageName)
        } else {
            true
        }
    }

    @SuppressLint("BatteryLife")
     fun Context.killBattery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (powerManager.isIgnoringBatteryOptimizations(packageName)) {
                toast(R.string.added_battery_optimization_whitelist)
            } else {
                kotlin.runCatching {
                    startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    })
                }.onFailure {
                    toast(R.string.system_not_support_please_manual_set)
                }
            }
        }
    }

    /* 添加快捷方式 */
    @SuppressLint("UnspecifiedImmutableFlag")
    @Suppress("DEPRECATION")
    fun addShortcut(
        ctx: Context,
        name: String,
        id: String,
        iconResId: Int,
        launcherIntent: Intent
    ) {
        ctx.longToast(R.string.add_shortcut_if_fail_tips)
        if (Build.VERSION.SDK_INT < 26) { /* Android8.0 */
            val addShortcutIntent = Intent("com.android.launcher.action.INSTALL_SHORTCUT")
            // 不允许重复创建
            addShortcutIntent.putExtra("duplicate", false) // 经测试不是根据快捷方式的名字判断重复的
            addShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name)
            addShortcutIntent.putExtra(
                Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(
                    ctx, iconResId
                )
            )

            launcherIntent.action = Intent.ACTION_MAIN
            launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            addShortcutIntent
                .putExtra(Intent.EXTRA_SHORTCUT_INTENT, launcherIntent)

            // 发送广播
            ctx.sendBroadcast(addShortcutIntent)
        } else {
            val shortcutManager: ShortcutManager = ctx.getSystemService(ShortcutManager::class.java)
            if (shortcutManager.isRequestPinShortcutSupported) {
                launcherIntent.action = Intent.ACTION_VIEW
                val pinShortcutInfo = ShortcutInfo.Builder(ctx, id)
                    .setIcon(
                        Icon.createWithResource(ctx, iconResId)
                    )
                    .setIntent(launcherIntent)
                    .setShortLabel(name)
                    .build()
                val pinnedShortcutCallbackIntent = shortcutManager
                    .createShortcutResultIntent(pinShortcutInfo)
                //Get notified when a shortcut is pinned successfully//
                val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_IMMUTABLE
                } else {
                    0
                }
                val successCallback = PendingIntent.getBroadcast(
                    ctx, 0, pinnedShortcutCallbackIntent, pendingIntentFlags
                )
                shortcutManager.requestPinShortcut(
                    pinShortcutInfo, successCallback.intentSender
                )
            }
        }
    }

}
