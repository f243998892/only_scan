package com.example.onlyscan

import android.app.Application
import android.util.Log
import com.example.onlyscan.data.DatabaseConnectionPool

/**
 * 应用程序类
 * 处理全局初始化和资源管理
 */
class OnlyScanApplication : Application() {
    
    companion object {
        private const val TAG = "OnlyScanApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化数据库连接池
        try {
            // 连接池会在第一次调用时自动初始化
            val connection = DatabaseConnectionPool.getConnection()
            if (connection != null) {
                Log.d(TAG, "数据库连接测试成功")
                DatabaseConnectionPool.releaseConnection(connection)
            } else {
                Log.e(TAG, "数据库连接测试失败")
            }
        } catch (e: Exception) {
            Log.e(TAG, "数据库连接初始化异常: ${e.message}")
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        
        // 关闭数据库连接池
        DatabaseConnectionPool.close()
    }
} 