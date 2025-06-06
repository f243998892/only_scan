package com.example.onlyscan.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.onlyscan.R
import com.example.onlyscan.databinding.ActivityLoginBinding

/**
 * 登录界面
 * 用户输入姓名后即可登录
 */
class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 检查是否已登录
        if (viewModel.isLoggedIn()) {
            navigateToMainActivity()
            return
        }
        
        setupListeners()
        observeViewModel()
    }
    
    /**
     * 设置监听器
     */
    private fun setupListeners() {
        binding.loginButton.setOnClickListener {
            val username = binding.nameEditText.text.toString().trim()
            if (username.isNotEmpty()) {
                viewModel.login(username)
            } else {
                Toast.makeText(this, R.string.enter_name, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 观察ViewModel数据变化
     */
    private fun observeViewModel() {
        viewModel.loginResult.observe(this) { success ->
            if (success) {
                navigateToMainActivity()
            }
        }
    }
    
    /**
     * 跳转到主界面
     */
    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // 结束登录界面
    }
} 