package com.github.jing332.tts_server_android

import android.content.Context
import com.github.jing332.compose.widgets.InterceptorResult
import com.github.jing332.compose.widgets.ModelInterceptor

object AsyncImageInterceptor : ModelInterceptor {
    private fun modelInterceptor(context: Context, model: Any?): Pair<Any?, Int>? {
        return when (model) {
            is CharSequence -> {
                when (model.toString().lowercase()) {
                    "male" -> R.drawable.male to R.string.male
                    "female" -> R.drawable.female to R.string.female
                    else -> null
                }
            }

            else -> null
        }
    }


    override fun apply(
        context: Context,
        model: Any?,
        contentDescription: String?,
    ): InterceptorResult {
        val ret = modelInterceptor(context, model)
        val m = if (ret == null) model else ret.first
        val desc = if (ret == null) contentDescription else context.getString(ret.second)

        return InterceptorResult(m, desc)
    }
}