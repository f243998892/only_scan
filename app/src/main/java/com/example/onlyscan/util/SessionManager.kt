package com.example.onlyscan.util

import android.content.Context
import android.content.SharedPreferences
import com.example.onlyscan.model.ProcessType

/**
 * 会话管理器
 * 用于管理用户登录状态和其他会话数据
 */
class SessionManager(context: Context) {
    companion object {
        private const val PREF_NAME = "OnlyScanPrefs"
        private const val KEY_USERNAME = "username"
        private const val KEY_PROCESS_TYPE = "process_type"
        
        private var instance: SessionManager? = null
        
        /**
         * 获取SessionManager实例
         */
        fun getInstance(context: Context): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    // SharedPreferences实例
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val editor = prefs.edit()
    
    /**
     * 保存用户名
     */
    fun saveUsername(username: String) {
        editor.putString(KEY_USERNAME, username)
        editor.apply()
    }
    
    /**
     * 获取已保存的用户名
     */
    fun getUsername(): String {
        return prefs.getString(KEY_USERNAME, "") ?: ""
    }
    
    /**
     * 清除用户会话数据（登出）
     */
    fun logout() {
        editor.clear()
        editor.apply()
    }
    
    /**
     * 是否已登录
     */
    fun isLoggedIn(): Boolean {
        return !getUsername().isNullOrEmpty()
    }
    
    /**
     * 保存当前选择的工序类型
     */
    fun saveProcessType(processType: ProcessType) {
        editor.putString(KEY_PROCESS_TYPE, processType.name)
        editor.apply()
    }
    
    /**
     * 获取当前选择的工序类型
     */
    fun getProcessType(): ProcessType {
        val typeName = prefs.getString(KEY_PROCESS_TYPE, ProcessType.DIP_COATING.name)
        return try {
            ProcessType.valueOf(typeName!!)
        } catch (e: Exception) {
            ProcessType.DIP_COATING // 默认值
        }
    }
} 