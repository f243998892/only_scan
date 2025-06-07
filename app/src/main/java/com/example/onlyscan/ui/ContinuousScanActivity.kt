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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onlyscan.R
import com.example.onlyscan.databinding.ActivityContinuousScanBinding
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
import com.google.android.material.button.MaterialButton

/**
 * 连续扫码界面
 * 连续扫描多个二维码，然后一次性上传
 */
class ContinuousScanActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "ContinuousScanActivity"
        private const val REQUEST_CAMERA_PERMISSION = 10
    }
    
    private lateinit var binding: ActivityContinuousScanBinding
    private val viewModel: ScanViewModel by viewModels()
    
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var scanResultAdapter: ScanResultAdapter
    
    // 防止重复扫描同一个码
    private var lastScannedCode: String = ""
    private var lastScannedTime: Long = 0
    
    private var camera: Camera? = null
    private var torchOn = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        binding = ActivityContinuousScanBinding.inflate(layoutInflater)
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
        setupRecyclerView()
        setupButtons()
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
     * 设置RecyclerView
     */
    private fun setupRecyclerView() {
        scanResultAdapter = ScanResultAdapter()
        binding.resultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ContinuousScanActivity)
            adapter = scanResultAdapter
        }
    }
    
    /**
     * 设置按钮点击事件
     */
    private fun setupButtons() {
        binding.uploadButton.setOnClickListener {
            viewModel.uploadMultiCodes()
        }
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
        val previewBuilder = Preview.Builder()
        Camera2Interop.Extender(previewBuilder)
            .setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            .setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(30, 30))
        val preview = previewBuilder
            .setTargetResolution(android.util.Size(1080, 1080))
            .build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
        val analysisBuilder = ImageAnalysis.Builder()
        Camera2Interop.Extender(analysisBuilder)
            .setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            .setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(30, 30))
        val imageAnalyzer = analysisBuilder
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build().also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
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
                                val currentTime = System.currentTimeMillis()
                                if (qrCode != lastScannedCode || currentTime - lastScannedTime > 2000) {
                                    lastScannedCode = qrCode
                                    lastScannedTime = currentTime
                                    showScanStatus("扫码成功")
                                    vibrateAndBeep()
                                    viewModel.processScannedCode(qrCode, true)
                                    binding.aiTipTextView.visibility = View.GONE
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "ZXing解析异常: ${e.message}")
                    } finally {
                        imageProxy.close()
                    }
                }
            }
        try {
            cameraProvider.unbindAll()
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
        // 2秒后隐藏状态提示
        binding.scanStatusTextView.postDelayed({
            binding.scanStatusTextView.visibility = View.INVISIBLE
        }, 2000)
    }
    
    /**
     * 观察ViewModel数据变化
     */
    private fun observeViewModel() {
        Log.d(TAG, "observeViewModel")
        viewModel.scannedCodes.observe(this) { codes ->
            Log.d(TAG, "ViewModel扫描结果: $codes")
            scanResultAdapter.updateResults(codes)
        }
        viewModel.statusMessage.observe(this) { message ->
            Log.d(TAG, "ViewModel状态消息: $message")
            showScanStatus(message)
        }
        viewModel.uploadResult.observe(this) { success ->
            Log.d(TAG, "ViewModel上传结果: $success")
            if (success) {
                finish()
            }
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