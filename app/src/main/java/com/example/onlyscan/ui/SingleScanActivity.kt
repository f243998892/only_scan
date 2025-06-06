package com.example.onlyscan.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.onlyscan.R
import com.example.onlyscan.databinding.ActivitySingleScanBinding
import com.example.onlyscan.util.QRCodeUtils
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 单个扫码界面
 * 扫描单个二维码并自动上传
 */
class SingleScanActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "SingleScanActivity"
        private const val REQUEST_CAMERA_PERMISSION = 10
    }
    
    private lateinit var binding: ActivitySingleScanBinding
    private val viewModel: ScanViewModel by viewModels()
    
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraExecutor: ExecutorService
    
    private var isScanning = true
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        binding = ActivitySingleScanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        // 检查相机权限
        if (allPermissionsGranted()) {
            Log.d(TAG, "权限已授予，启动相机")
            startCamera()
        } else {
            Log.d(TAG, "未授予权限，请求权限")
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION
            )
        }
        
        observeViewModel()
    }
    
    /**
     * 检查是否已授予所有必要权限
     */
    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        baseContext, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
    
    /**
     * 权限请求结果处理
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "onRequestPermissionsResult: requestCode=$requestCode, grantResults=${grantResults.joinToString()}")
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (allPermissionsGranted()) {
                Log.d(TAG, "权限请求通过，启动相机")
                startCamera()
            } else {
                Log.e(TAG, "相机权限被拒绝，关闭页面")
                Toast.makeText(
                    this, "需要相机权限才能扫描二维码", Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }
    
    /**
     * 启动相机
     */
    private fun startCamera() {
        Log.d(TAG, "startCamera")
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                Log.d(TAG, "获取CameraProvider成功")
                bindCameraUseCases(cameraProvider)
            } catch (e: Exception) {
                Log.e(TAG, "获取CameraProvider失败: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }
    
    /**
     * 绑定相机用例
     */
    private fun bindCameraUseCases(cameraProvider: ProcessCameraProvider) {
        Log.d(TAG, "bindCameraUseCases")
        // 预览用例
        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
        
        // 图像分析用例
        val imageAnalyzer = ImageAnalysis.Builder()
            .setTargetResolution(android.util.Size(1080, 1080))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    if (isScanning) {
                        try {
                            val qrCode = QRCodeUtils.decodeQRCodeFromImage(this, imageProxy)
                            Log.d(TAG, "ZXing解析结果: $qrCode")
                            if (!qrCode.isNullOrEmpty()) {
                                isScanning = false // 暂停扫描
                                runOnUiThread {
                                    showScanStatus("扫码成功")
                                    viewModel.processScannedCode(qrCode, false)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "ZXing解析异常: ${e.message}")
                        }
                    }
                    imageProxy.close()
                }
            }
        
        try {
            // 解除之前的绑定
            cameraProvider.unbindAll()
            
            // 绑定用例到相机
            cameraProvider.bindToLifecycle(
                this as LifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalyzer
            )
            
            Log.d(TAG, "相机用例绑定成功")
        } catch (e: Exception) {
            Log.e(TAG, "相机绑定失败: ${e.message}")
        }
    }
    
    /**
     * 显示扫描状态
     */
    private fun showScanStatus(message: String) {
        Log.d(TAG, "showScanStatus: $message")
        binding.scanStatusTextView.text = message
        binding.scanStatusTextView.visibility = View.VISIBLE
    }
    
    /**
     * 观察ViewModel数据变化
     */
    private fun observeViewModel() {
        Log.d(TAG, "observeViewModel")
        viewModel.statusMessage.observe(this) { message ->
            Log.d(TAG, "ViewModel状态消息: $message")
            showScanStatus(message)
        }
        
        viewModel.uploadResult.observe(this) { success ->
            Log.d(TAG, "ViewModel上传结果: $success")
            // 延迟2秒后关闭界面，让用户有时间看到结果
            binding.scanStatusTextView.postDelayed({
                Log.d(TAG, "finish Activity")
                finish()
            }, 2000)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy，关闭cameraExecutor")
        cameraExecutor.shutdown()
    }
} 