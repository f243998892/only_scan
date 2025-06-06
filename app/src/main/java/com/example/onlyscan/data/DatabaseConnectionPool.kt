package com.example.onlyscan.data

import android.util.Log
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 数据库连接池
 * 管理和复用PostgreSQL数据库连接
 */
object DatabaseConnectionPool {
    private const val TAG = "DatabaseConnectionPool"
    
    // 数据库连接信息
    private const val DB_HOST = "s2.gnip.vip"
    private const val DB_PORT = "33946"
    private const val DB_NAME = "scan_db"
    private const val DB_USER = "fh"
    private const val DB_PASS = "yb123456"
    
    // JDBC URL
    private const val JDBC_URL = "jdbc:postgresql://$DB_HOST:$DB_PORT/$DB_NAME"
    
    // 连接池配置
    private const val MAX_POOL_SIZE = 5
    private const val CONNECTION_TIMEOUT = 30000L // 30秒
    
    // 连接池
    private val connectionPool = ConcurrentLinkedQueue<PooledConnection>()
    
    // 定期检查连接的调度器
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    
    init {
        // 加载PostgreSQL驱动
        try {
            Class.forName("org.postgresql.Driver")
            
            // 初始化连接池
            for (i in 0 until MAX_POOL_SIZE) {
                val connection = createConnection()
                if (connection != null) {
                    connectionPool.offer(PooledConnection(connection))
                }
            }
            
            // 定期检查连接有效性
            scheduler.scheduleAtFixedRate({
                checkConnections()
            }, 30, 30, TimeUnit.SECONDS)
            
            Log.d(TAG, "连接池初始化完成，连接数: ${connectionPool.size}")
        } catch (e: ClassNotFoundException) {
            Log.e(TAG, "PostgreSQL驱动加载失败: ${e.message}")
        }
    }
    
    /**
     * 获取一个连接
     */
    @Synchronized
    fun getConnection(): Connection? {
        var connection: PooledConnection? = connectionPool.poll()
        
        // 没有可用连接，创建新连接
        if (connection == null) {
            val newConnection = createConnection()
            return newConnection
        }
        
        // 检查连接是否有效
        try {
            if (connection.isValid()) {
                return connection.connection
            } else {
                // 无效连接，关闭并创建新连接
                try {
                    connection.connection.close()
                } catch (e: SQLException) {
                    Log.e(TAG, "关闭无效连接失败: ${e.message}")
                }
                
                return createConnection()
            }
        } catch (e: SQLException) {
            Log.e(TAG, "检查连接有效性失败: ${e.message}")
            return createConnection()
        }
    }
    
    /**
     * 归还连接到池中
     */
    @Synchronized
    fun releaseConnection(connection: Connection?) {
        if (connection != null && connectionPool.size < MAX_POOL_SIZE) {
            try {
                // 检查连接是否有效
                if (!connection.isClosed && connection.isValid(1)) {
                    connectionPool.offer(PooledConnection(connection))
                    return
                }
            } catch (e: SQLException) {
                Log.e(TAG, "归还连接时检查连接状态失败: ${e.message}")
            }
        }
        
        // 无效连接或池已满，关闭连接
        try {
            connection?.close()
        } catch (e: SQLException) {
            Log.e(TAG, "关闭连接失败: ${e.message}")
        }
    }
    
    /**
     * 创建新的数据库连接
     */
    private fun createConnection(): Connection? {
        return try {
            val properties = Properties()
            properties.setProperty("user", DB_USER)
            properties.setProperty("password", DB_PASS)
            properties.setProperty("connectTimeout", CONNECTION_TIMEOUT.toString())
            
            DriverManager.getConnection(JDBC_URL, properties)
        } catch (e: SQLException) {
            Log.e(TAG, "创建数据库连接失败: ${e.message}")
            null
        }
    }
    
    /**
     * 检查连接池中的连接
     */
    private fun checkConnections() {
        val iterator = connectionPool.iterator()
        while (iterator.hasNext()) {
            val connection = iterator.next()
            try {
                if (!connection.isValid()) {
                    iterator.remove()
                    try {
                        connection.connection.close()
                    } catch (e: SQLException) {
                        Log.e(TAG, "关闭无效连接失败: ${e.message}")
                    }
                }
            } catch (e: SQLException) {
                Log.e(TAG, "检查连接有效性失败: ${e.message}")
                iterator.remove()
                try {
                    connection.connection.close()
                } catch (e: SQLException) {
                    Log.e(TAG, "关闭无效连接失败: ${e.message}")
                }
            }
        }
        
        // 补充连接
        val size = connectionPool.size
        if (size < MAX_POOL_SIZE) {
            for (i in 0 until (MAX_POOL_SIZE - size)) {
                val connection = createConnection()
                if (connection != null) {
                    connectionPool.offer(PooledConnection(connection))
                }
            }
        }
    }
    
    /**
     * 关闭连接池
     */
    fun close() {
        scheduler.shutdown()
        
        val iterator = connectionPool.iterator()
        while (iterator.hasNext()) {
            val connection = iterator.next()
            iterator.remove()
            try {
                connection.connection.close()
            } catch (e: SQLException) {
                Log.e(TAG, "关闭连接失败: ${e.message}")
            }
        }
    }
    
    /**
     * 连接池中的连接包装类
     */
    private class PooledConnection(val connection: Connection) {
        private val creationTime = System.currentTimeMillis()
        private val maxLifeTime = 10 * 60 * 1000 // 10分钟
        
        /**
         * 检查连接是否有效
         */
        @Throws(SQLException::class)
        fun isValid(): Boolean {
            // 检查连接是否过期
            if (System.currentTimeMillis() - creationTime > maxLifeTime) {
                return false
            }
            
            // 检查连接是否关闭或无效
            return !connection.isClosed && connection.isValid(1)
        }
    }
} 