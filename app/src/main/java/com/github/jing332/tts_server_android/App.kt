package com.github.jing332.tts_server_android

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Process
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.exoplayer.image.BitmapFactoryImageDecoder
import coil3.Bitmap
import coil3.BitmapImage
import coil3.Image
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.asImage
import coil3.fetch.ImageFetchResult
import coil3.imageLoader
import coil3.intercept.Interceptor
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.request.crossfade
import com.drake.net.Net.options
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.tts_server_android.compose.CoilStringFetcher
import com.github.jing332.tts_server_android.constant.AppConst
import com.github.jing332.tts_server_android.model.hanlp.HanlpManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.properties.Delegates
import kotlin.text.lowercase


val app: App
    inline get() = App.instance

@Suppress("DEPRECATION")
class App : Application() {
    companion object {
        const val TAG = "App"
        var instance: App by Delegates.notNull()
        val context: Context by lazy { instance }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base.apply { AppLocale.setLocale(base) })
    }

    @SuppressLint("SdCardPath")
    @OptIn(DelicateCoroutinesApi::class, DelicateCoilApi::class)
    override fun onCreate() {
        super.onCreate()
        instance = this
        CrashHandler(this)

        SystemTtsV2.Converters.json = AppConst.jsonBuilder

        SingletonImageLoader.setUnsafe(
            ImageLoader.Builder(context).components {
                add(CoilStringFetcher.Factory())
                add(object : Interceptor {
                    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
                        val data = chain.request.data
                        if (data is CharSequence) {
                            val drawId = when (data.toString().lowercase()) {
                                "male" -> R.drawable.male
                                "female" -> R.drawable.female
                                else -> null
                            }

                            val bitmap = drawId?.run {
                                ContextCompat.getDrawable(chain.request.context, this)?.toBitmap()
                            }

                            if (bitmap != null)
                                return SuccessResult(bitmap.asImage(), chain.request)
                        }

                        return chain.proceed()
                    }

                })

            }.crossfade(true).build()
        )

        GlobalScope.launch {
            HanlpManager.initDir(
                context.getExternalFilesDir("hanlp")?.absolutePath
                    ?: "/data/data/$packageName/files/hanlp"
            )
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    fun restart() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)!!
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        //杀掉以前进程
        Process.killProcess(Process.myPid());
    }
}