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
import androidx.camera.camera2.interop.Camera2Interop
import android.hardware.camera2.CaptureRequest
import android.util.Range
import android.os.Vibrator
import android.os.VibrationEffect
import android.media.AudioManager
import android.media.ToneGenerator
import android.view.WindowManager
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import com.google.android.material.button.MaterialButton

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
    private var camera: Camera? = null
    private var torchOn = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        binding = ActivitySingleScanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        cameraExecutor = Executors.newSingleThreadExecutor()
        // 屏幕常亮
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // 添加补光灯按钮
        val torchButton = MaterialButton(this).apply {
            text = "补光灯"
            setOnClickListener {
                toggleTorch()
            }
        }
        binding.root.addView(torchButton)
        
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
        val previewBuilder = Preview.Builder()
        Camera2Interop.Extender(previewBuilder)
            .setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            .setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(30, 30))
        val preview = previewBuilder
            .setTargetResolution(android.util.Size(1080, 1080))
            .build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
        
        // 图像分析用例
        val analysisBuilder = ImageAnalysis.Builder()
        Camera2Interop.Extender(analysisBuilder)
            .setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            .setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(30, 30))
        val imageAnalyzer = analysisBuilder
            .setTargetResolution(android.util.Size(1080, 1080))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build().also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    if (isScanning) {
                        try {
                            val (qrCode, aiTip) = QRCodeUtils.decodeQRCodeWithAIDetect(this, imageProxy)
                            runOnUiThread {
                                if (!aiTip.isNullOrEmpty()) {
                                    binding.aiTipTextView.text = aiTip
                                    binding.aiTipTextView.visibility = View.VISIBLE
                                } else {
                                    binding.aiTipTextView.visibility = View.GONE
                                }
                                if (!qrCode.isNullOrEmpty()) {
                                    isScanning = false // 暂停扫描
                                    showScanStatus("扫码成功")
                                    vibrateAndBeep()
                                    viewModel.processScannedCode(qrCode, false)
                                    binding.aiTipTextView.visibility = View.GONE
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
            camera = cameraProvider.bindToLifecycle(
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
    
    private fun vibrateAndBeep() {
        // 强烈震动，持续500ms
        try {
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator.vibrate(500)
            }
        } catch (e: Exception) {
            Log.e(TAG, "震动失败: "+e.message)
        }
        // 刺耳提示音，走铃声音量通道，持续1秒
        try {
            // STREAM_RING 走系统铃声音量，TONE_SUP_ERROR为刺耳警告音
            val toneGen = ToneGenerator(AudioManager.STREAM_RING, 100)
            toneGen.startTone(ToneGenerator.TONE_SUP_ERROR, 1000)
        } catch (e: Exception) {
            Log.e(TAG, "音效失败: "+e.message)
        }
    }
    
    private fun toggleTorch() {
        camera?.cameraControl?.enableTorch(!torchOn)
        torchOn = !torchOn
    }
} 