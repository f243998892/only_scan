package com.example.onlyscan.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.onlyscan.model.ProcessType
import com.example.onlyscan.util.SessionManager

/**
 * 主界面视图模型
 * 处理主界面相关的业务逻辑
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionManager = SessionManager.getInstance(application)
    
    private val _username = MutableLiveData<String>()
    val username: LiveData<String> = _username
    
    private val _processType = MutableLiveData<ProcessType>()
    val processType: LiveData<ProcessType> = _processType
    
    init {
        // 加载用户信息和工序类型
        _username.value = sessionManager.getUsername()
        _processType.value = sessionManager.getProcessType()
    }
    
    /**
     * 更新选择的工序类型
     */
    fun updateProcessType(processType: ProcessType) {
        _processType.value = processType
        sessionManager.saveProcessType(processType)
    }
    
    /**
     * 获取当前选择的工序类型
     */
    fun getProcessType(): ProcessType {
        return processType.value ?: ProcessType.DIP_COATING
    }
    
    /**
     * 获取当前登录用户
     */
    fun getUsername(): String {
        return username.value ?: ""
    }
    
    /**
     * 执行登出操作
     */
    fun logout() {
        sessionManager.logout()
    }
} 