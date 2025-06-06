package com.example.onlyscan.model

/**
 * 工序类型枚举
 * 定义了应用中所有支持的工序类型
 */
enum class ProcessType(val displayName: String) {
    DIP_COATING("浸漆"),
    CAR_STOP("车止口");
    
    companion object {
        /**
         * 根据名称查找对应的工序类型
         */
        fun fromDisplayName(name: String): ProcessType? {
            return values().find { it.displayName == name }
        }
    }
} 