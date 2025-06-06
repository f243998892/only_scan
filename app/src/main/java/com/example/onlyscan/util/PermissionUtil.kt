package com.example.onlyscan.util

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * 权限工具类
 * 简化权限请求和检查
 */
object PermissionUtil {

    /**
     * 检查是否拥有指定权限
     */
    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 检查是否拥有多个权限
     */
    fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (!hasPermission(context, permission)) {
                return false
            }
        }
        return true
    }

    /**
     * 请求权限
     */
    fun requestPermissions(activity: Activity, permissions: Array<String>, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }

    /**
     * 检查权限授予结果
     */
    fun verifyPermissionResults(grantResults: IntArray): Boolean {
        if (grantResults.isEmpty()) return false

        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }
} 