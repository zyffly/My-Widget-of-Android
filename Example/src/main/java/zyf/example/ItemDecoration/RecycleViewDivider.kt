package zyf.example.ItemDecoration

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * 可设置最后一条是否显示，分割线的边距，宽和高
 */

class RecycleViewDivider @JvmOverloads constructor(
    context: Context,
    orientation: Int = LinearLayoutManager.HORIZONTAL
) : RecyclerView.ItemDecoration() {

    private val mContext: Context = context

    private val mPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var mDivider: Drawable? = null
    private var mDividerHeight = 2//分割线高度，默认为1px
    private val mOrientation: Int = orientation//列表的方向：LinearLayoutManager.VERTICAL或LinearLayoutManager.HORIZONTAL
    private val mMargin = IntArray(4)

    private var mIsGrid: Boolean = false
    private var mIsDrawLast = false

    /**
     * 分割线的左右边距
     * @param l
     * @param t
     * @param r
     * @param b
     */
    fun setMargin(l: Int, t: Int, r: Int, b: Int) {
        mMargin[0] = l
        mMargin[1] = t
        mMargin[2] = r
        mMargin[3] = b
    }

    fun setDividerHeight(height: Int) {
        mDividerHeight = height
    }

    fun setBackgroundColor(dividerColor: Int) {
        mPaint.setColor(dividerColor)
    }

    fun setBackgroundDrawle(drawableId: Int) {
        mDivider = ContextCompat.getDrawable(mContext, drawableId)
        mDivider?.let {
            mDividerHeight = it.intrinsicHeight
        }
    }

    fun setGrid(isGrid: Boolean) {
        mIsGrid = isGrid
    }

    fun setIsDrawLast(isDrawLast : Boolean){
        mIsDrawLast = isDrawLast
    }

    //获取分割线尺寸
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        if (mIsGrid) {
            outRect.set(mDividerHeight / 2, 0, mDividerHeight / 2, mDividerHeight)
        } else {
            val pos = parent.getChildLayoutPosition(view)
            val itemCount = state.itemCount - 1
            if (pos == itemCount) {
                outRect.set(0, 0, 0, 0)
            } else {
                outRect.set(0, 0, 0, mDividerHeight)
            }

        }
    }

    //绘制分割线或者其他
    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDraw(c, parent, state)
        if (mOrientation == LinearLayoutManager.VERTICAL) {
            drawVertical(c, parent)
        } else {
            drawHorizontal(c, parent)
        }
    }

    //绘制横向 item 分割线
    private fun drawHorizontal(canvas: Canvas, parent: RecyclerView) {
        val left = parent.paddingLeft
        val right = parent.measuredWidth - parent.paddingRight
        var childSize = parent.childCount

        if(! mIsDrawLast){
            childSize -= 1
        }

        /**
         * 绘制
         */
        for (i in 0 until childSize) {
            val child = parent.getChildAt(i)
            val layoutParams = child.layoutParams as RecyclerView.LayoutParams
            val top = child.bottom + layoutParams.bottomMargin
            val bottom = top + mDividerHeight

            mDivider?.let {
                it.setBounds(left + mMargin[0], top + mMargin[1], right - mMargin[2], bottom - mMargin[3])
                it.draw(canvas)
            }

            canvas.drawRect(
                (left + mMargin[0]).toFloat(),
                (top + mMargin[1]).toFloat(),
                (right - mMargin[2]).toFloat(),
                (bottom - mMargin[3]).toFloat(),
                mPaint
            )

        }
    }

    //绘制纵向 item 分割线
    private fun drawVertical(canvas: Canvas, parent: RecyclerView) {
        val top = parent.paddingTop
        val bottom = parent.measuredHeight - parent.paddingBottom
        var childSize = parent.childCount

        if(! mIsDrawLast){
            childSize -= 1
        }

        for (i in 0 until childSize) {
            val child = parent.getChildAt(i)
            val layoutParams = child.layoutParams as RecyclerView.LayoutParams
            val left = child.right + layoutParams.rightMargin
            val right = left + mDividerHeight
            mDivider?.let {
                it.setBounds(left, top, right, bottom)
                it.draw(canvas)
            }

            canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), mPaint)

        }
    }
}