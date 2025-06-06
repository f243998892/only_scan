package com.example.onlyscan.repository

import com.example.onlyscan.data.DatabaseHelper
import com.example.onlyscan.model.ProcessType
import com.example.onlyscan.model.Product

/**
 * 产品数据仓库
 * 作为ViewModel和数据源之间的中间层
 */
class ProductRepository {
    private val databaseHelper = DatabaseHelper()
    
    /**
     * 根据产品编码查找产品
     */
    suspend fun findProductByCode(productCode: String): Product? {
        return databaseHelper.findProductByCode(productCode)
    }
    
    /**
     * 更新产品工序信息
     */
    suspend fun updateProductProcess(productCode: String, processType: ProcessType, worker: String): Boolean {
        return databaseHelper.updateProductProcess(productCode, processType, worker)
    }
    
    /**
     * 检查工序字段是否已有数据
     */
    fun isProcessAlreadyRecorded(product: Product, processType: ProcessType): Boolean {
        return databaseHelper.isProcessAlreadyRecorded(product, processType)
    }
} 