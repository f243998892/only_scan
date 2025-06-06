package com.example.onlyscan.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.onlyscan.util.SessionManager
import kotlinx.coroutines.launch

/**
 * 登录界面视图模型
 * 处理登录相关的业务逻辑
 */
class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionManager = SessionManager.getInstance(application)

    private val _loginResult = MutableLiveData<Boolean>()
    val loginResult: LiveData<Boolean> = _loginResult

    /**
     * 执行登录操作
     */
    fun login(username: String) {
        viewModelScope.launch {
            // 简单的登录逻辑，只需保存用户名
            if (username.isNotBlank()) {
                sessionManager.saveUsername(username)
                _loginResult.value = true
            } else {
                _loginResult.value = false
            }
        }
    }

    /**
     * 检查用户是否已登录
     */
    fun isLoggedIn(): Boolean {
        return sessionManager.isLoggedIn()
    }

    /**
     * 获取当前登录用户名
     */
    fun getUsername(): String {
        return sessionManager.getUsername()
    }
} 