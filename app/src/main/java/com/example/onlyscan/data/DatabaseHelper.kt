package com.example.onlyscan.data

import android.util.Log
import com.example.onlyscan.model.ProcessType
import com.example.onlyscan.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp
import java.util.Date

/**
 * 数据库辅助类
 * 处理PostgreSQL数据库的连接和操作
 */
class DatabaseHelper {
    companion object {
        private const val TAG = "DatabaseHelper"
    }
    
    /**
     * 获取数据库连接
     */
    private suspend fun getConnection(): Connection? {
        return withContext(Dispatchers.IO) {
            DatabaseConnectionPool.getConnection()
        }
    }
    
    /**
     * 根据产品编码查找产品
     */
    suspend fun findProductByCode(productCode: String): Product? {
        return withContext(Dispatchers.IO) {
            var connection: Connection? = null
            var statement: PreparedStatement? = null
            var resultSet: ResultSet? = null
            
            try {
                connection = getConnection()
                if (connection == null) return@withContext null
                
                val query = "SELECT * FROM public.products WHERE 产品编码 = ?"
                statement = connection.prepareStatement(query)
                statement.setString(1, productCode)
                resultSet = statement.executeQuery()
                
                if (resultSet.next()) {
                    Product(
                        id = resultSet.getInt("id"),
                        productCode = resultSet.getString("产品编码"),
                        productModel = resultSet.getString("产品型号"),
                        weldingWorker = resultSet.getString("焊线员工"),
                        weldingTime = resultSet.getTimestamp("焊线时间"),
                        dipCoatingWorker = resultSet.getString("浸线员工"),
                        dipCoatingTime = resultSet.getTimestamp("浸线时间"),
                        connectingWorker = resultSet.getString("接线员工"),
                        connectingTime = resultSet.getTimestamp("接线时间"),
                        pressingWorker = resultSet.getString("压线员工"),
                        pressingTime = resultSet.getTimestamp("压线时间"),
                        carStopWorker = resultSet.getString("车止口员工"),
                        carStopTime = resultSet.getTimestamp("车止口时间"),
                        createdAt = resultSet.getTimestamp("created_at")
                    )
                } else null
            } catch (e: Exception) {
                Log.e(TAG, "查询产品失败: ${e.message}")
                null
            } finally {
                try {
                    resultSet?.close()
                    statement?.close()
                    connection?.let { DatabaseConnectionPool.releaseConnection(it) }
                } catch (e: SQLException) {
                    Log.e(TAG, "关闭连接失败: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 更新产品工序信息
     */
    suspend fun updateProductProcess(
        productCode: String, 
        processType: ProcessType, 
        worker: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            var connection: Connection? = null
            var statement: PreparedStatement? = null
            
            try {
                connection = getConnection()
                if (connection == null) return@withContext false
                
                // 根据工序类型确定要更新的字段
                val (workerField, timeField) = when(processType) {
                    ProcessType.DIP_COATING -> Pair("浸线员工", "浸线时间")
                    ProcessType.CAR_STOP -> Pair("车止口员工", "车止口时间")
                }
                
                val currentTime = Timestamp(System.currentTimeMillis())
                val query = "UPDATE public.products SET $workerField = ?, $timeField = ? WHERE 产品编码 = ?"
                
                statement = connection.prepareStatement(query)
                statement.setString(1, worker)
                statement.setTimestamp(2, currentTime)
                statement.setString(3, productCode)
                
                val affectedRows = statement.executeUpdate()
                affectedRows > 0
            } catch (e: Exception) {
                Log.e(TAG, "更新产品数据失败: ${e.message}")
                false
            } finally {
                try {
                    statement?.close()
                    connection?.let { DatabaseConnectionPool.releaseConnection(it) }
                } catch (e: SQLException) {
                    Log.e(TAG, "关闭连接失败: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 检查工序字段是否已有数据
     */
    fun isProcessAlreadyRecorded(product: Product, processType: ProcessType): Boolean {
        return when (processType) {
            ProcessType.DIP_COATING -> product.dipCoatingWorker != null && product.dipCoatingTime != null
            ProcessType.CAR_STOP -> product.carStopWorker != null && product.carStopTime != null
        }
    }
} 