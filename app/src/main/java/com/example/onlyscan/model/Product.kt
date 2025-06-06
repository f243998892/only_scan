package com.example.onlyscan.model

import java.util.*

/**
 * 产品数据模型
 * 对应数据库中的products表
 */
data class Product(
    val id: Int = 0,                      // 产品ID
    val productCode: String,              // 产品编码
    val productModel: String? = null,     // 产品型号
    
    // 焊线工序
    var weldingWorker: String? = null,    // 焊线员工
    var weldingTime: Date? = null,        // 焊线时间
    
    // 浸线工序
    var dipCoatingWorker: String? = null, // 浸漆员工
    var dipCoatingTime: Date? = null,     // 浸漆时间
    
    // 接线工序
    var connectingWorker: String? = null, // 接线员工
    var connectingTime: Date? = null,     // 接线时间
    
    // 压线工序
    var pressingWorker: String? = null,   // 压线员工
    var pressingTime: Date? = null,       // 压线时间
    
    // 车止口工序
    var carStopWorker: String? = null,    // 车止口员工
    var carStopTime: Date? = null,        // 车止口时间
    
    val createdAt: Date? = null           // 创建时间
) {
    override fun toString(): String {
        return "产品编码: $productCode"
    }
} 