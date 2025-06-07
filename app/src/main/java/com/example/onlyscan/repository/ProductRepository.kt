package com.example.onlyscan.repository

import com.example.onlyscan.api.ProductApiService
import com.example.onlyscan.api.UpdateProductProcessRequest
import com.example.onlyscan.api.ApiResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * 产品仓库，负责与后端API交互，上传二维码及工序信息
 */
class ProductRepository {
    // Retrofit API接口
    private val api: ProductApiService

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.fanghui8131.fun")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofit.create(ProductApiService::class.java)
    }

    /**
     * 上传二维码（工序录入）
     * @param code 二维码内容（产品编码）
     * @param processType 工序类型（如"浸漆"或"车止口"）
     * @param employeeName 员工姓名
     * @return true=上传成功，false=失败或已录入
     */
    suspend fun updateProductProcess(
        code: String,
        processType: String,
        employeeName: String
    ): Boolean {
        // 自动映射工序字段
        val (timeField, employeeField) = when (processType) {
            "浸漆" -> Pair("浸漆时间", "浸漆员工")
            "车止口" -> Pair("车止口时间", "车止口员工")
            else -> return false
        }
        val timestamp = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val request = UpdateProductProcessRequest(
            productCode = code,
            processType = processType,
            employeeName = employeeName,
            timeField = timeField,
            employeeField = employeeField,
            timestamp = timestamp
        )
        val response = api.updateProductProcess(request)
        // 只要API返回success=true即视为成功，否则失败
        return response.isSuccessful && response.body()?.success == true
    }

    // 下面两个方法为兼容旧ViewModel逻辑，实际可省略或直接返回null/false
    fun findProductByCode(code: String): Any? = code // 占位
    fun isProcessAlreadyRecorded(product: Any, processType: String): Boolean = false // 占位
} 