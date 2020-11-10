package com.felix.shimmer

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat

/**
 * 不支持带透明色的扫光特效
 */
class ShimmerTextView : AppCompatTextView {

    /**
     * 扫光特效的画笔
     */
    private var mShimmerPaint: Paint? = null

    /**
     * 扫光特效的宽度比例。0~1之间，0表示不显示扫光效果，1表示整个控件都是扫光效果。默认0.2
     */
    private var mShimmerWithRatio = 0.2f

    /**
     * 扫光特效的角度，角度返回时0-360，默认值是20
     */
    private var mShimmerAngle = 20

    /**
     * 扫光特效的颜色
     */
    private var mShimmerColor: Int = 0


    private var mShimmerAnimation: ValueAnimator? = null


    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        mShimmerColor = ContextCompat.getColor(context, R.color.shimmer_color)

        postDelayed({
            onCreateShimmerPaint()
            invalidate()
        },500)
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        onShimmerDraw(canvas)
    }

    private fun onShimmerDraw(canvas: Canvas){
//        mShimmerAnimation?.let {
//            if(it.isRunning){


//                canvas.translate(maskOffsetX.toFloat(), 0f)
        onCreateShimmerPaint()
                mShimmerPaint?.let {paint ->
                    canvas.save()
                    canvas.drawRect(
                        0f,
                        0f,
                        height.toFloat(),
                        height.toFloat(),
                        paint
                    )
                    canvas.restore()
                }
//            }
//        }
    }

    /**
     * 开启扫光动画
     */
    fun onStartShimmerAnimation() {
        mShimmerAnimation?.let {
            if (!it.isRunning) {
                it.start()
            }
            return
        }

        mShimmerAnimation = ValueAnimator.ofFloat(0f, width.toFloat()).apply {
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            addUpdateListener { animation ->

            }
            start()
        }
    }

    /**
     * 关闭扫光动画
     */
    private fun onStopShimmerAnimation() {

    }

    /**
     * 创建扫光特效的画笔
     */
    private fun onCreateShimmerPaint() {
        //扫光特效的宽度为0时，则不需要该特效
        if (mShimmerWithRatio == 0f) {
            return
        }
        mShimmerPaint?.let {
            return
        }
        val edgeColor: Int = ShimmerUtils.clearColorAlpha(mShimmerColor)
        val shimmerLineWidth: Float = width * mShimmerWithRatio
        val yPosition: Float = 0f


        val gradient = LinearGradient(
            0f,
            50f,
            50f,
            50f,
            intArrayOf(edgeColor, mShimmerColor, mShimmerColor, edgeColor),
            getGradientColorDistribution(),
            Shader.TileMode.CLAMP
        )

//        val maskBitmapShader =
//            BitmapShader(getMaskBitmap(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
//
//        val composeShader =
//            ComposeShader(gradient, maskBitmapShader, PorterDuff.Mode.DST_IN)

        mShimmerPaint = Paint().apply {
            isAntiAlias = true
            isDither = true
            isFilterBitmap = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            shader = gradient
        }
    }

    private fun getGradientColorDistribution(): FloatArray? {
        val colorDistribution = FloatArray(4)
        colorDistribution[0] = 0f
        colorDistribution[3] = 1f
        colorDistribution[1] = 0.5f - mShimmerWithRatio / 2f
        colorDistribution[2] = 0.5f + mShimmerWithRatio / 2f
        return colorDistribution
    }
}