package com.example.onlyscan.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 上传二维码工序的请求体
 */
data class UpdateProductProcessRequest(
    val productCode: String,
    val processType: String,
    val employeeName: String,
    val timeField: String,
    val employeeField: String,
    val timestamp: String
)

/**
 * API通用响应体
 */
data class ApiResponse(
    val success: Boolean? = null,
    val detail: String? = null
)

/**
 * 产品相关API接口定义
 */
interface ProductApiService {
    @POST("/api/updateProductProcess")
    suspend fun updateProductProcess(
        @Body request: UpdateProductProcessRequest
    ): Response<ApiResponse>
} 