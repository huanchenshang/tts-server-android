package com.github.jing332.tts_server_android.ui.systts

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.constant.KeyConst.KEY_DATA
import com.github.jing332.tts_server_android.constant.KeyConst.KEY_POSITION
import com.github.jing332.tts_server_android.constant.TtsApiType
import com.github.jing332.tts_server_android.data.SysTtsConfigItem
import com.github.jing332.tts_server_android.databinding.ActivityTtsConfigEditBinding
import com.github.jing332.tts_server_android.ui.custom.BackActivity
import com.github.jing332.tts_server_android.ui.custom.SysTtsNumericalEditView
import com.github.jing332.tts_server_android.ui.custom.widget.WaitDialog
import com.github.jing332.tts_server_android.util.toastOnUi

class TtsConfigEditActivity : BackActivity(), AdapterView.OnItemSelectedListener {

    companion object {
        const val TAG = "TtsConfigEditActivity"
    }

    private val binding: ActivityTtsConfigEditBinding by lazy {
        ActivityTtsConfigEditBinding.inflate(layoutInflater)
    }
    private val model: TtsConfigEditViewModel by viewModels()

    private val spinnerRaTargetAdapter: ArrayAdapter<String> by lazy { buildSpinnerAdapter() }
    private val spinnerApiAdapter: ArrayAdapter<String> by lazy { buildSpinnerAdapter() }
    private val spinnerLanguageAdapter: ArrayAdapter<String> by lazy { buildSpinnerAdapter() }
    private val spinnerVoiceAdapter: ArrayAdapter<String> by lazy { buildSpinnerAdapter() }
    private val spinnerVoiceStyleAdapter: ArrayAdapter<String> by lazy { buildSpinnerAdapter() }
    private val spinnerVoiceRoleAdapter: ArrayAdapter<String> by lazy { buildSpinnerAdapter() }
    private val spinnerFormatAdapter: ArrayAdapter<String> by lazy { buildSpinnerAdapter() }

    /* -1表示添加 */
    private var position = -1

    @Suppress("DEPRECATION", "CAST_NEVER_SUCCEEDS")
    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        position = intent.getIntExtra(KEY_POSITION, -1)

        binding.spinnerReadAloudTarget.adapter = spinnerRaTargetAdapter
        binding.spinnerApi.adapter = spinnerApiAdapter
        binding.spinnerLanguage.adapter = spinnerLanguageAdapter
        binding.spinnerVoice.adapter = spinnerVoiceAdapter
        binding.spinnerVoiceStyle.adapter = spinnerVoiceStyleAdapter

        binding.spinnerVoiceRole.adapter = spinnerVoiceRoleAdapter
        binding.spinnerFormat.adapter = spinnerFormatAdapter

        binding.spinnerReadAloudTarget.onItemSelectedListener = this
        binding.spinnerApi.onItemSelectedListener = this
        binding.spinnerLanguage.onItemSelectedListener = this
        binding.spinnerVoice.onItemSelectedListener = this
        binding.spinnerVoiceStyle.onItemSelectedListener = this
        binding.spinnerVoiceRole.onItemSelectedListener = this
        binding.spinnerFormat.onItemSelectedListener = this

        /* 显示名称*/
        model.displayNameLiveData.observe(this) { text ->
            binding.etDisplayName.setText(text)
        }
        /* 朗读目标 */
        model.readAloudTargetLiveData.observe(this) { data ->
            updateSpinner(binding.spinnerReadAloudTarget, data)
        }
        /* 接口列表 */
        model.apiLiveData.observe(this) { data ->
            updateSpinner(binding.spinnerApi, data)
        }
        /* 语言列表 */
        model.languageLiveData.observe(this) { data ->
            Log.d(TAG, "languageList size:${data.list.size}")
            updateSpinner(binding.spinnerLanguage, data)
        }
        /* 声音列表 */
        model.voiceLiveData.observe(this) { data ->
            Log.d(TAG, "voiceList size:${data.list.size}")
            updateSpinner(binding.spinnerVoice, data)
        }
        /* 风格 */
        model.voiceStyleLiveData.observe(this) { data ->
            Log.d(TAG, "styleList size:${data.list.size}")
            updateSpinner(binding.spinnerVoiceStyle, data)
        }
        /* 角色 */
        model.voiceRoleLiveData.observe(this) { data ->
            Log.d(TAG, "roleList size:${data.list.size}")
            updateSpinner(binding.spinnerVoiceRole, data)
        }
        /* 音频格式列表 */
        model.audioFormatLiveData.observe(this) { data ->
            Log.d(TAG, "audioFormatList size:${data.list.size}")
            updateSpinner(binding.spinnerFormat, data)
            spinnerFormatAdapter.clear()
            data.list.forEach {
                spinnerFormatAdapter.add(it.displayName)
            }
            binding.spinnerFormat.setSelection(data.position)
        }
        /* 语速 */
        model.rateLiveData.observe(this) {
            Log.d(TAG, "rate:$it")
//            binding.seekBarRate.seekBar.progress = it
            binding.sysTtsNumericalEditView.setRate(it)
        }
        /* 音量 */
        model.volumeLiveData.observe(this) {
            Log.d(TAG, "volume:$it")

            binding.sysTtsNumericalEditView.setVolume(it)
        }
        /* 风格强度 */
        model.voiceStyleDegreeLiveData.observe(this) {
            binding.sysTtsNumericalEditView.setStyleDegree(it)
        }

        binding.sysTtsNumericalEditView.callback = object : SysTtsNumericalEditView.Callback {
            override fun onRateChanged(rate: Int) {
                model.rateChanged(rate)
            }

            override fun onVolumeChanged(volume: Int) {
                model.volumeChanged(volume)
            }

            override fun onStyleDegreeChanged(degree: Float) {
                model.onStyleDegreeChanged(degree)
            }
        }

        /* 加载数据并更新列表 */
        var cfg =
            intent.getSerializableExtra(KEY_DATA)?.let { it as SysTtsConfigItem }
        if (cfg == null) cfg = SysTtsConfigItem()
        model.loadData(this, cfg)
    }

    private var isInit = 0

    /* Spinner选择变更 */
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (isInit >= 2)
            binding.etDisplayName.setText("")
        when (parent?.id) {
            R.id.spinner_readAloudTarget -> {
                model.onReadAloudTargetSelected(position)
            }
            R.id.spinner_api -> {
                binding.sysTtsNumericalEditView.isStyleDegreeVisible = position != TtsApiType.EDGE
                val waitDialog = WaitDialog(this)
                waitDialog.show()
                model.apiSelected(position) { waitDialog.dismiss() }
            }
            R.id.spinner_language -> model.languageSelected(position)
            R.id.spinner_voice -> {
                model.voiceSelected(position)
                isInit++
            }
            R.id.spinner_voiceStyle -> {
                model.voiceStyleSelected(position)
            }
            R.id.spinner_voiceRole -> {
                model.voiceRoleSelected(position)
                isInit++
            }
            R.id.spinner_format -> {
                if (model.formatSelected(position)) toastOnUi(R.string.raw_format_is_play_while_downloading)
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    private fun buildSpinnerAdapter(): ArrayAdapter<String> {
        return ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_1
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun updateSpinner(spinner: Spinner, data: TtsConfigEditViewModel.SpinnerData) {
        spinner.also {
            val adapter = it.adapter as ArrayAdapter<String>
            adapter.clear()
            data.list.forEach { v ->
                adapter.add(v.displayName)
            }
            /* 解决position相同时不调用onItemSelectedListener */
            if (it.selectedItemPosition == data.position) {
                val cb = spinner.onItemSelectedListener
                cb?.onItemSelected(
                    spinner,
                    null,
                    data.position,
                    data.position.toLong()
                )
            } else {
                it.setSelection(data.position)
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_systts_config_edit, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.systts_config_edit_save -> {
                val intent = Intent()
                intent.putExtra(
                    KEY_DATA,
                    model.getTtsConfigItem(binding.etDisplayName.text.toString())
                )
                intent.putExtra(KEY_POSITION, position)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }

        return super.onOptionsItemSelected(item)
    }
}