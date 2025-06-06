package com.example.onlyscan.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min
import android.os.Environment

/**
 * 二维码工具类
 * 处理二维码图像处理和识别
 */
object QRCodeUtils {

    /**
     * 将ImageProxy（YUV_420_888）转换为Bitmap，兼容所有CameraX 1.3.x版本
     * @param imageProxy CameraX的图像帧
     * @return 转换后的Bitmap
     */
    @OptIn(androidx.camera.core.ExperimentalGetImage::class)
    fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val image = imageProxy.image ?: return null
        val width = imageProxy.width
        val height = imageProxy.height
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer
        val yRowStride = image.planes[0].rowStride
        val uvRowStride = image.planes[1].rowStride
        val uvPixelStride = image.planes[1].pixelStride
        val argb = IntArray(width * height)
        var yp = 0
        for (j in 0 until height) {
            val pY = yRowStride * j
            val uvRow = uvRowStride * (j shr 1)
            for (i in 0 until width) {
                val y = 0xff and yBuffer.get(pY + i).toInt()
                val u = 0xff and uBuffer.get(uvRow + (i shr 1) * uvPixelStride).toInt()
                val v = 0xff and vBuffer.get(uvRow + (i shr 1) * uvPixelStride).toInt()
                // YUV转RGB
                val yNew = if (y - 16 < 0) 0 else y - 16
                val uNew = u - 128
                val vNew = v - 128
                var r = (1.164f * yNew + 1.596f * vNew).toInt()
                var g = (1.164f * yNew - 0.813f * vNew - 0.391f * uNew).toInt()
                var b = (1.164f * yNew + 2.018f * uNew).toInt()
                r = r.coerceIn(0, 255)
                g = g.coerceIn(0, 255)
                b = b.coerceIn(0, 255)
                argb[yp++] = -0x1000000 or (r shl 16) or (g shl 8) or b
            }
        }
        return Bitmap.createBitmap(argb, width, height, Bitmap.Config.ARGB_8888)
    }

    /**
     * 裁剪图像中心区域
     */
    fun cropCenterBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        // 计算裁剪区域（取中心800x800像素区域，或尽可能大的正方形）
        val cropSize = minOf(width, height, 800)
        val startX = (width - cropSize) / 2
        val startY = (height - cropSize) / 2
        
        return Bitmap.createBitmap(bitmap, startX, startY, cropSize, cropSize)
    }
    
    /**
     * 灰度化处理
     */
    fun convertToGrayscale(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        for (i in pixels.indices) {
            val pixel = pixels[i]
            
            // 提取RGB各分量
            val r = (pixel shr 16) and 0xff
            val g = (pixel shr 8) and 0xff
            val b = pixel and 0xff
            
            // 转换为灰度值 (0.3R + 0.59G + 0.11B)
            val gray = (0.3 * r + 0.59 * g + 0.11 * b).toInt()
            
            // 设置RGB分量相同的值，保留原透明度
            pixels[i] = (pixel and 0xff000000.toInt()) or (gray shl 16) or (gray shl 8) or gray
        }
        
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }
    
    /**
     * 二值化处理
     */
    fun binarize(bitmap: Bitmap, threshold: Int = 128): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        for (i in pixels.indices) {
            val pixel = pixels[i]
            
            // 提取灰度值
            val gray = (pixel shr 16) and 0xff
            
            // 二值化处理
            val binaryValue = if (gray > threshold) 255 else 0
            
            // 设置二值化后的像素值
            pixels[i] = (pixel and 0xff000000.toInt()) or 
                    (binaryValue shl 16) or 
                    (binaryValue shl 8) or 
                    binaryValue
        }
        
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }
    
    /**
     * 旋转Bitmap
     */
    fun rotateBitmapIfNeeded(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        if (rotationDegrees == 0) return bitmap
        val matrix = Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    /**
     * 从Bitmap识别二维码（用RGBLuminanceSource）
     */
    fun decodeQRCode(bitmap: Bitmap): String? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val source = RGBLuminanceSource(width, height, pixels)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val hints = mapOf(
            DecodeHintType.TRY_HARDER to true,
            DecodeHintType.POSSIBLE_FORMATS to listOf(com.google.zxing.BarcodeFormat.QR_CODE)
        )
        return try {
            val reader = MultiFormatReader()
            val result = reader.decode(binaryBitmap, hints)
            result.text
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 从ImageProxy直接识别二维码（自动旋转+灰度化，需传context）
     * @param context 上下文
     * @param imageProxy CameraX的图像帧
     * @return 识别到的二维码内容，失败返回null
     */
    fun decodeQRCodeFromImage(context: Context, imageProxy: ImageProxy): String? {
        val bitmap = imageProxyToBitmap(imageProxy) ?: return null
        val rotatedBitmap = rotateBitmapIfNeeded(bitmap, imageProxy.imageInfo.rotationDegrees)
        android.util.Log.d("QRCodeUtils", "imageToBitmap成功，尺寸: ${bitmap.width}x${bitmap.height}，旋转: ${imageProxy.imageInfo.rotationDegrees}")
        // 保存一帧图片到本地，便于人工调试
        if (System.currentTimeMillis() % 1000 < 50) {
            saveBitmapToDCIM(rotatedBitmap, "debug_${System.currentTimeMillis()}.jpg")
        }
        val grayscaleBitmap = convertToGrayscale(rotatedBitmap)
        android.util.Log.d("QRCodeUtils", "灰度化处理成功")
        val result = decodeQRCode(grayscaleBitmap)
        android.util.Log.d("QRCodeUtils", "decodeQRCode结果: $result")
        return result
    }
    
    /**
     * 将ARGB像素数据转换为YUV格式
     */
    private fun convertToYUV(pixels: IntArray, width: Int, height: Int): ByteArray {
        val yuv = ByteArray(width * height)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = pixels[y * width + x]
                
                // 提取RGB分量
                val r = (pixel shr 16) and 0xff
                val g = (pixel shr 8) and 0xff
                val b = pixel and 0xff
                
                // 转换为Y分量
                val yValue = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                yuv[y * width + x] = yValue.toByte()
            }
        }
        
        return yuv
    }
    
    /**
     * 取两数中的较小值，但不超过最大限制
     */
    private fun min(a: Int, b: Int, maxLimit: Int = Int.MAX_VALUE): Int {
        return min(min(a, b), maxLimit)
    }

    /**
     * 保存Bitmap到本地DCIM/OnlyScan目录，便于人工调试
     */
    fun saveBitmapToDCIM(bitmap: Bitmap, fileName: String) {
        try {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "OnlyScan")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, fileName)
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()
            android.util.Log.d("QRCodeUtils", "已保存调试图片: ${file.absolutePath}")
        } catch (e: Exception) {
            android.util.Log.e("QRCodeUtils", "保存Bitmap失败: ${e.message}")
        }
    }
} 