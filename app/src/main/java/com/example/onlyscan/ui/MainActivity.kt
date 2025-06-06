package com.example.onlyscan.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.onlyscan.R
import com.example.onlyscan.databinding.ActivityMainBinding
import com.example.onlyscan.model.ProcessType

/**
 * 主界面
 * 显示用户信息，选择工序类型，提供扫码功能入口
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupProcessTypeSpinner()
        setupButtons()
        observeViewModel()
    }
    
    /**
     * 设置工序类型下拉菜单
     */
    private fun setupProcessTypeSpinner() {
        val processTypes = ProcessType.values().map { it.displayName }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, processTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.processTypeSpinner.adapter = adapter
        
        // 设置当前选中的工序类型
        val currentProcessType = viewModel.getProcessType()
        val position = ProcessType.values().indexOfFirst { it == currentProcessType }
        if (position != -1) {
            binding.processTypeSpinner.setSelection(position)
        }
        
        // 监听选择变化
        binding.processTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedName = processTypes[position]
                ProcessType.fromDisplayName(selectedName)?.let {
                    viewModel.updateProcessType(it)
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // 不处理
            }
        }
    }
    
    /**
     * 设置按钮点击事件
     */
    private fun setupButtons() {
        binding.singleScanButton.setOnClickListener {
            val intent = Intent(this, SingleScanActivity::class.java)
            startActivity(intent)
        }
        
        binding.continuousScanButton.setOnClickListener {
            val intent = Intent(this, ContinuousScanActivity::class.java)
            startActivity(intent)
        }
    }
    
    /**
     * 观察ViewModel数据变化
     */
    private fun observeViewModel() {
        viewModel.username.observe(this) { username ->
            binding.welcomeTextView.text = "姓名：$username"
        }
    }
} 