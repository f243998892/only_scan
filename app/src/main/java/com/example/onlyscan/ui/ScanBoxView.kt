package com.example.onlyscan.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.example.onlyscan.R

/**
 * 自定义扫码框动画View
 * 1. 高亮边框和四角标记
 * 2. 动态扫描线动画
 * 3. 兼容主流扫码App风格
 */
class ScanBoxView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    // 扫码框尺寸
    private val boxWidth = 240f.dp
    private val boxHeight = 240f.dp
    // 边框和角标颜色
    private val borderColor = Color.parseColor("#00FF00") // 亮绿色
    private val cornerColor = Color.parseColor("#00FF00")
    private val scanLineColor = Color.parseColor("#00FF00")
    // 画笔
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = borderColor
        style = Paint.Style.STROKE
        strokeWidth = 4f.dp
    }
    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = cornerColor
        style = Paint.Style.STROKE
        strokeWidth = 8f.dp
    }
    private val scanLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = scanLineColor
        style = Paint.Style.FILL
        strokeWidth = 3f.dp
    }
    // 动画参数
    private var scanLinePosition = 0f
    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2000L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            scanLinePosition = it.animatedFraction
            invalidate()
        }
    }
    // dp转px扩展
    private val Float.dp: Float get() = this * resources.displayMetrics.density

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.start()
    }
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val left = (width - boxWidth) / 2f
        val top = (height - boxHeight) / 2f
        val right = left + boxWidth
        val bottom = top + boxHeight
        val rect = RectF(left, top, right, bottom)
        // 画边框
        canvas.drawRect(rect, borderPaint)
        // 画四角高亮
        drawCorners(canvas, rect)
        // 画扫描线
        val y = top + (boxHeight * scanLinePosition)
        canvas.drawLine(left + 10f.dp, y, right - 10f.dp, y, scanLinePaint)
    }

    // 四角高亮
    private fun drawCorners(canvas: Canvas, rect: RectF) {
        val cornerLen = 24f.dp
        // 左上
        canvas.drawLine(rect.left, rect.top, rect.left + cornerLen, rect.top, cornerPaint)
        canvas.drawLine(rect.left, rect.top, rect.left, rect.top + cornerLen, cornerPaint)
        // 右上
        canvas.drawLine(rect.right, rect.top, rect.right - cornerLen, rect.top, cornerPaint)
        canvas.drawLine(rect.right, rect.top, rect.right, rect.top + cornerLen, cornerPaint)
        // 左下
        canvas.drawLine(rect.left, rect.bottom, rect.left + cornerLen, rect.bottom, cornerPaint)
        canvas.drawLine(rect.left, rect.bottom, rect.left, rect.bottom - cornerLen, cornerPaint)
        // 右下
        canvas.drawLine(rect.right, rect.bottom, rect.right - cornerLen, rect.bottom, cornerPaint)
        canvas.drawLine(rect.right, rect.bottom, rect.right, rect.bottom - cornerLen, cornerPaint)
    }
} 