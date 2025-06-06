package com.example.onlyscan.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.onlyscan.model.ProcessType
import com.example.onlyscan.model.Product
import com.example.onlyscan.repository.ProductRepository
import com.example.onlyscan.util.SessionManager
import kotlinx.coroutines.launch

/**
 * 扫码界面视图模型
 * 处理扫码和数据上传相关的业务逻辑
 */
class ScanViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ProductRepository()
    private val sessionManager = SessionManager.getInstance(application)
    
    // 多个扫描结果集合
    private val _scannedCodes = MutableLiveData<MutableList<String>>(mutableListOf())
    val scannedCodes: LiveData<MutableList<String>> = _scannedCodes
    
    // 最新的单个扫描结果
    private val _lastScannedCode = MutableLiveData<String>()
    val lastScannedCode: LiveData<String> = _lastScannedCode
    
    // 状态消息
    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage
    
    // 上传结果
    private val _uploadResult = MutableLiveData<Boolean>()
    val uploadResult: LiveData<Boolean> = _uploadResult
    
    /**
     * 处理新的扫码结果
     */
    fun processScannedCode(code: String, isContinuousMode: Boolean) {
        if (code.isEmpty()) return
        
        _lastScannedCode.value = code
        
        if (isContinuousMode) {
            // 连续扫码模式，将结果添加到列表
            val currentList = _scannedCodes.value ?: mutableListOf()
            if (!currentList.contains(code)) {
                currentList.add(code)
                _scannedCodes.value = currentList
            }
        } else {
            // 单个扫码模式，直接上传
            uploadSingleCode(code)
        }
    }
    
    /**
     * 上传单个扫码结果
     */
    private fun uploadSingleCode(code: String) {
        viewModelScope.launch {
            val processType = sessionManager.getProcessType()
            val username = sessionManager.getUsername()
            
            val product = repository.findProductByCode(code)
            if (product != null) {
                if (repository.isProcessAlreadyRecorded(product, processType)) {
                    _statusMessage.value = "已录入"
                    _uploadResult.value = false
                } else {
                    val result = repository.updateProductProcess(code, processType, username)
                    if (result) {
                        _statusMessage.value = "上传成功"
                        _uploadResult.value = true
                    } else {
                        _statusMessage.value = "上传失败"
                        _uploadResult.value = false
                    }
                }
            } else {
                _statusMessage.value = "未找到产品信息"
                _uploadResult.value = false
            }
        }
    }
    
    /**
     * 上传多个扫码结果
     */
    fun uploadMultiCodes() {
        viewModelScope.launch {
            val codes = _scannedCodes.value ?: return@launch
            if (codes.isEmpty()) {
                _statusMessage.value = "没有扫描结果"
                _uploadResult.value = false
                return@launch
            }
            
            val processType = sessionManager.getProcessType()
            val username = sessionManager.getUsername()
            
            var successCount = 0
            var alreadyRecordedCount = 0
            
            // 逐个处理每个条码
            for (code in codes) {
                val product = repository.findProductByCode(code)
                if (product != null) {
                    if (repository.isProcessAlreadyRecorded(product, processType)) {
                        alreadyRecordedCount++
                    } else {
                        val result = repository.updateProductProcess(code, processType, username)
                        if (result) {
                            successCount++
                        }
                    }
                }
            }
            
            // 汇总结果
            if (successCount > 0) {
                _statusMessage.value = "上传成功: $successCount 条，已录入: $alreadyRecordedCount 条"
                _uploadResult.value = true
            } else {
                _statusMessage.value = "上传失败，已录入: $alreadyRecordedCount 条"
                _uploadResult.value = false
            }
            
            // 清空列表
            _scannedCodes.value = mutableListOf()
        }
    }
    
    /**
     * 清空扫描结果列表
     */
    fun clearScannedCodes() {
        _scannedCodes.value = mutableListOf()
    }
} 