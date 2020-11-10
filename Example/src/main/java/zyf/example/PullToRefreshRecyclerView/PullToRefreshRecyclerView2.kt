package zyf.example.PullToRefreshRecyclerView

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.Scroller
import androidx.recyclerview.widget.RecyclerView

class PullToRefreshRecyclerView2 : ViewGroup {

    private enum class Status(var value: Int) {
        /**
         * 复位初始状态
         */
        REFRESH_RETURNING(-4),
        /**
         * 正在刷新
         */
        REFRESHING(-3),
        /**
         * 等待松手状态(下载状态，且下拉高度大于固定的HeaderView的高度)
         */
        REFRESH_WAIT(-4),
        /**
         * 下拉状态
         */
        REFRESH_SATART(-1),
        DEFAULT(0),
        LOADMORE_SATART(1),
        LOADMORE_WAIT(2),
        LOADMOREING(3),
        LOADMORE_RETURNING(4)
    }

    private lateinit var mHeaderView: RefreshHeaderView
    private lateinit var mFooterView: RecylerFooterView
    private lateinit var mRecyclerView: RecyclerView

    private var mStatus = Status.DEFAULT
    private var mTouchSlop: Int = 0
    private var mMoveOffset = 0

    private var mNoMore: Boolean = false
    private lateinit var mAutoScroller: AutoScroller
    private var mListener: OnPullToRefreshListener? = null

    private var mHearderViewHeight: Int = 0
    private var mFooterViewHeight: Int = 0

    private var mActivePointerId: Int = 0
    private var mLastX: Float = 0.toFloat()
    private var mLastY: Float = 0.toFloat()
    private var mInitDownX: Float = 0.toFloat()
    private var mInitDownY: Float = 0.toFloat()
    private var INVALID_POINTER = -1
    private var mIsTouch = false

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initView()
    }

    private fun initView() {
        mHeaderView = RefreshHeaderView(context)
        mRecyclerView = RecyclerView(context)
        mFooterView = RecylerFooterView(context)

        addView(
            mHeaderView,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        addView(
            mFooterView,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        addView(
            mRecyclerView,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )

        mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
        setStatus(Status.DEFAULT)
        mAutoScroller = AutoScroller()
    }

    fun isNoMore(): Boolean {
        return mNoMore
    }

    fun setNoMore(noMore: Boolean) {
        this.mNoMore = noMore
    }

    fun setAdapter(adapter: RecyclerView.Adapter<*>) {
        mRecyclerView.adapter = adapter
    }

    fun setLayoutManager(manager: RecyclerView.LayoutManager) {
        mRecyclerView.layoutManager = manager
    }

    fun addItemDecoration(decoration: RecyclerView.ItemDecoration) {
        mRecyclerView.addItemDecoration(decoration)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val widthMeasure = View.MeasureSpec.getSize(widthMeasureSpec)
        val widthMode = View.MeasureSpec.getMode(widthMeasureSpec)
        val heightMeasure = View.MeasureSpec.getSize(heightMeasureSpec)
        val heightMode = View.MeasureSpec.getMode(heightMeasureSpec)

        var tmpW = 0
        var tmpH = 0
        var child: View
        val count = childCount
        for (i in 0 until count) {
            child = getChildAt(i)
            //先测量子View
            measureChild(child, widthMeasureSpec, heightMeasureSpec)

            val childW = child.measuredWidth
            val childH = child.measuredHeight
            if (tmpW < childW) {
                tmpW = childW
            }

            tmpH += childH
        }

        tmpH = if (tmpH > heightMeasure) heightMeasure else tmpH
        setMeasuredDimension(
            if (widthMode != View.MeasureSpec.EXACTLY) tmpW else widthMeasure,
            if (heightMode != View.MeasureSpec.EXACTLY) tmpH else heightMeasure
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        layoutChildren()
    }

    private fun layoutChildren() {

        val width = measuredWidth
        val height = measuredHeight

        val l = paddingLeft
        var t = paddingTop
        val r = paddingRight
        var b = paddingBottom

        val tmpW = width - l - r

        val hW = mHeaderView.measuredWidth
        val hH = mHeaderView.measuredHeight

        if (mHearderViewHeight == 0) {
            mHearderViewHeight = hH
        }

        val hOffset = if (mStatus.value < Status.DEFAULT.value) mMoveOffset else 0
        val hLeft = if (hW > tmpW) l else (width - hW) / 2
        val hTop = t - hH + hOffset
        val hRight = if (hW > tmpW) l + tmpW else (tmpW + hW) / 2
        val hBottom = t + hOffset
        mHeaderView.layout(hLeft, hTop, hRight, hBottom)
        t = hBottom


        val fW = mFooterView.measuredWidth
        val fH = mFooterView.measuredHeight
        if (mFooterViewHeight == 0) {
            mFooterViewHeight = fH
        }
        val fOffset = if (mStatus.value > Status.DEFAULT.value) mMoveOffset else 0
        val fLeft = if (fW > tmpW) l else (width - fW) / 2
        val fBottom = height - b + fH + fOffset
        val fTop = height - b + fOffset
        val fRight = if (fW > tmpW) l + tmpW else (tmpW + fW) / 2
        mFooterView.layout(fLeft, fTop, fRight, fBottom)
        b -= fOffset
        t -= b


        val rW = mRecyclerView.measuredWidth
        val rLeft = if (rW > tmpW) l else (width - rW) / 2
        val rBottom = height - b
        val rTop = t
        val rRight = if (rW > tmpW) l + tmpW else (tmpW + rW) / 2
        mRecyclerView.layout(rLeft, rTop, rRight, rBottom)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
//        if (mStatus != Status.DEFAULT) {
//            Log.e("dispatchTouchEvent",   "0000.")
//            return true
//        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mActivePointerId = event.getPointerId(0)
                mLastY = event.getY(0)
                mInitDownY = mLastY
                mLastX = event.getX(0)
                mInitDownX = mLastX
                mIsTouch = true
            }

            MotionEvent.ACTION_MOVE -> {
                if (mActivePointerId == INVALID_POINTER) {
                    Log.e("dispatchTouchEvent", "Got ACTION_MOVE event but don't have an active pointer id.")
                    return super.dispatchTouchEvent(event)
                }

                val index = event.findPointerIndex(mActivePointerId)
                if (index < 0) {
                    Log.e("dispatchTouchEvent", "Got ACTION_MOVE event but don't have an active pointer id.")
                    return super.dispatchTouchEvent(event)
                }

                val y = event.getY(index)
                val x = event.getX(index)
                val yInitDiff = y - mLastY
                val xInitDiff = x - mLastX
                mLastY = y
                mLastX = x

                if (Math.abs(xInitDiff) > Math.abs(yInitDiff) &&
                    Math.abs(xInitDiff) > mTouchSlop &&
                    mStatus == Status.DEFAULT
                ) {
                    return super.dispatchTouchEvent(event)
                }

                if (mStatus == Status.DEFAULT) {
                    if (yInitDiff > 0 && onCheckCanRefresh()) {
                        setStatus(Status.REFRESH_SATART)
                    } else if (yInitDiff < 0 && onCheckCanLoadMore()) {
                        setStatus(Status.LOADMORE_SATART)
                    }
                } else if (mStatus == Status.REFRESHING ||
                    mStatus == Status.REFRESH_RETURNING ||
                    mStatus == Status.LOADMOREING ||
                    mStatus == Status.LOADMORE_RETURNING

                ) {
                    return true
                }

                if (isUpdateLayout()) {
                    updataStatusLayout(yInitDiff)
                    return true
                }
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                val pointerIndex = event.actionIndex
                if (pointerIndex < 0) {
                    Log.e("dispatchTouchEvent", "Got ACTION_POINTER_DOWN event but have an invalid action index.")
                    return super.dispatchTouchEvent(event)
                }
                mLastX = event.getX(pointerIndex)
                mLastY = event.getY(pointerIndex)
//                lastEvent = event
                mActivePointerId = event.getPointerId(pointerIndex)
            }

            MotionEvent.ACTION_POINTER_UP -> {
                var pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                if (pointerId == mActivePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    pointerIndex = if (pointerIndex == 0) 1 else 0
                    mLastY = event.getY(pointerIndex)
                    mLastX = event.getX(pointerIndex)
                    mActivePointerId = event.getPointerId(pointerIndex)
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
//                isTouch = false
//                if (currentTargetOffsetTop > START_POSITION) {
//                    finishSpinner()
//                }
                mIsTouch = false
                if (mActivePointerId == INVALID_POINTER) {
                    return false
                }
                mActivePointerId = INVALID_POINTER

                /**
                 *　下拉刷新状态改变
                 */
                when (mStatus) {
                    Status.REFRESH_SATART -> {
//                        setStatus(Status.REFRESH_RETURNING)
                        reset(mMoveOffset)
                        return true
                    }
                    Status.REFRESH_WAIT -> {
                        if(mMoveOffset == 0){
                            setStatus(Status.DEFAULT)
                            return true
                        }else if (mMoveOffset < mHearderViewHeight) {
                            setStatus(Status.REFRESH_RETURNING)
                        }
                        reset(mMoveOffset)
                        return true
                    }
                    Status.LOADMORE_SATART -> {  //上拉加载更多状态刷新
//                        setStatus(Status.LOADMORE_RETURNING)
                        reset(mMoveOffset)
                        return true
                    }
                    Status.LOADMORE_WAIT -> {
                        if(mMoveOffset == 0){
                            setStatus(Status.DEFAULT)
                            return true
                        }else if (mMoveOffset > -mFooterViewHeight) {
                            setStatus(Status.LOADMORE_RETURNING)

                        }
                        reset(mMoveOffset)
                        return true
                    }
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

//    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
//        if (mStatus != Status.DEFAULT) {
//            return true
//        }
//
//        try {
//            when (event.action) {
//                MotionEvent.ACTION_DOWN -> {
//
//                    mActivePointerId = event.getPointerId(0)
//                    mLastY = event.getY(mActivePointerId)
//                    mInitDownY = mLastY
//                    mLastX = event.getX(mActivePointerId)
//                    mInitDownX = mLastX
//                }
//                MotionEvent.ACTION_MOVE -> {
//                    if (mActivePointerId == INVALID_POINTER) {
//                        return false
//                    }
//                    val y = event.getY(mActivePointerId)
//                    val x = event.getX(mActivePointerId)
//                    val yInitDiff = y - mInitDownY
//                    val xInitDiff = x - mInitDownX
//                    mLastY = y
//                    mLastX = x
//                    val moved = Math.abs(yInitDiff) > Math.abs(xInitDiff)
//                    val triggerCondition =
//                    // refresh trigger condition
//                        yInitDiff > 0 && moved && onCheckCanRefresh() ||
//                                //load more trigger condition
//                                yInitDiff < 0 && moved && onCheckCanLoadMore()
//                    if (triggerCondition) {
//                        // if the refresh's or load more's trigger condition  is true,
//                        // intercept the move action event and pass it to SwipeToLoadLayout#onTouchEvent()
//                        return true
//                    }
//                }
//                MotionEvent.ACTION_POINTER_UP -> {
//                    //                onSecondaryPointerUp(event);
//                    //                mInitDownY = mLastY = getMotionEventY(event, mActivePointerId);
//                    //                mInitDownX = mLastX = getMotionEventX(event, mActivePointerId);
//                }
//                MotionEvent.ACTION_UP,
//                MotionEvent.ACTION_CANCEL -> mActivePointerId = INVALID_POINTER
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//
//        return super.onInterceptTouchEvent(event)
//    }
//
//    override fun onTouchEvent(event: MotionEvent): Boolean {
//        when (event.action) {
//            MotionEvent.ACTION_DOWN -> mActivePointerId = event.getPointerId(0)
//            MotionEvent.ACTION_MOVE -> {
//                if (mActivePointerId == INVALID_POINTER) {
//                    return false
//                }
//                val y = event.getY(mActivePointerId)
//                val x = event.getX(mActivePointerId)
//                val yInitDiff = y - mInitDownY
//                val xInitDiff = x - mInitDownX
//                mLastY = y
//                mLastX = x
//                if (Math.abs(xInitDiff) > Math.abs(yInitDiff) && Math.abs(xInitDiff) > mTouchSlop && mStatus == Status.DEFAULT) {
//                    return false
//                }
//
//                if (mStatus == Status.DEFAULT) {
//                    if (yInitDiff > 0 && onCheckCanRefresh()) {
//                        setStatus(Status.REFRESH_SATART)
//                    } else if (yInitDiff < 0 && onCheckCanLoadMore()) {
//                        setStatus(Status.LOADMORE_SATART)
//                    }
//                } else if (mStatus == Status.REFRESHING || mStatus == Status.LOADMOREING) {
//                    return false
//                }
//
//                if (isUpdateLayout()) {
//                    updataStatusLayout(yInitDiff)
//                    return true
//                }
//            }
//            MotionEvent.ACTION_UP -> {
//                if (mActivePointerId == INVALID_POINTER) {
//                    return false
//                }
//                mActivePointerId = INVALID_POINTER
//
//                /**
//                 * 　下拉刷新状态改变
//                 */
//                if (mStatus == Status.REFRESH_SATART) {
//                    setStatus(Status.REFRESH_RETURNING)
//                    reset(mMoveOffset)
//                    return true
//                } else if (mStatus == Status.REFRESH_WAIT) {
//                    reset(mMoveOffset)
//                    return true
//                } else if (mStatus == Status.LOADMORE_SATART) {  //上拉加载更多状态刷新
//                    setStatus(Status.LOADMORE_RETURNING)
//                    reset(mMoveOffset)
//                    return true
//                } else if (mStatus == Status.LOADMORE_WAIT) {
//                    reset(mMoveOffset)
//                    return true
//                }
//            }
//        }
//        return super.onTouchEvent(event)
//    }

    /**
     * 是否重新布局
     *
     * @return
     */
    private fun isUpdateLayout(): Boolean = when (mStatus) {
        Status.REFRESH_SATART,  //下拉状态
        Status.REFRESH_WAIT,     //下载状态，且下拉高度大于固定的HeaderView的高度
        Status.REFRESH_RETURNING,    //下拉，复位到初始状态
        Status.LOADMORE_SATART,    //上拉状态
        Status.LOADMORE_WAIT,    //拉状态，且下拉高度大于固定的HeaderView的高度
        Status.LOADMORE_RETURNING   //上拉，　复位到初始状态
        -> true
        else -> false
    }

    private fun updataStatusLayout(dy: Float) {

        var offset = Math.round(dy)
        if (offset == 0) {
            return
        }

        offset = (offset * (1 - getSlingOffet(offset))).toInt()

        when (mStatus) {
            Status.REFRESH_SATART -> {
                mMoveOffset += offset
                if (mMoveOffset >= mHearderViewHeight) {
                    setStatus(Status.REFRESH_WAIT)
                }
            }
            Status.REFRESH_WAIT -> if (!mIsTouch) {
                mMoveOffset += offset
                if (mMoveOffset >= mHearderViewHeight) {
                    mMoveOffset = mHearderViewHeight
                    setStatus(Status.REFRESHING)
                    if (mListener != null) {
                        mListener?.onRefresh()
                    }
                }
            } else {
                mMoveOffset += offset
                if (mMoveOffset <= 0) {
                    mMoveOffset = 0
                }
            }
            Status.REFRESH_RETURNING -> {
                mMoveOffset += offset
                if (mMoveOffset <= 0) {
                    mMoveOffset = 0
                    setStatus(Status.DEFAULT)
                }
            }
            Status.DEFAULT -> return

            Status.LOADMORE_SATART -> {
                mMoveOffset += offset
                if (mMoveOffset <= -mFooterViewHeight) {
                    setStatus(Status.LOADMORE_WAIT)
                }
            }
            Status.LOADMORE_WAIT -> if (mActivePointerId == INVALID_POINTER) {
                mMoveOffset += offset
                if (mMoveOffset <= -mFooterViewHeight) {
                    mMoveOffset = -mFooterViewHeight
                    setStatus(Status.LOADMOREING)
                    if (mListener != null) {
                        mListener?.onLoadMore()
                    }
                }
            } else {
//                offset = (offset * (1 - getSlingOffet(offset))).toInt()
                mMoveOffset += offset
                if (mMoveOffset >= 0) {
                    mMoveOffset = 0
                }
            }
            Status.LOADMORE_RETURNING -> {
                mMoveOffset += offset
                if (mMoveOffset >= 0) {
                    mMoveOffset = 0
                    setStatus(Status.DEFAULT)
                }
            }
            else -> {
            }
        }
        layoutChildren()
        invalidate()
    }

    private fun getSlingOffet(offset: Int): Float {
        val targetY = Math.max(0, Math.abs(mMoveOffset + offset))
        // y = x - (x/2)^2
        val extraOS = (targetY - mHearderViewHeight).toFloat()
        val slingshotDist = mHearderViewHeight.toFloat()
        val tensionSlingshotPercent = Math.max(0f, Math.min(extraOS, slingshotDist * 2) / slingshotDist)

        return (tensionSlingshotPercent - Math.pow((tensionSlingshotPercent / 2).toDouble(), 2.0)).toFloat()
    }

    /**
     * 设置当前状态
     *
     * @param status
     */
    private fun setStatus(status: Status) {
        if (status == mStatus) {
            return
        }
        mStatus = status
        when (mStatus) {
            Status.REFRESH_SATART -> mHeaderView.onPrepare()
            Status.REFRESH_WAIT -> mHeaderView.onWait()
            Status.REFRESHING -> mHeaderView.onRefreshing()
            Status.REFRESH_RETURNING -> mHeaderView.onRefreshComplete()
            Status.LOADMORE_SATART -> mFooterView.onPrepare()
            Status.LOADMORE_WAIT -> mFooterView.onWait()
            Status.LOADMOREING -> mFooterView.onLoadMoreing()
            Status.LOADMORE_RETURNING -> mFooterView.onLoadComplete()
            Status.DEFAULT -> {
            }
        }
    }

    /**
     * 复位
     */
    private fun reset(offset: Int) {
        var time = 500
        if (mStatus == Status.REFRESH_WAIT || mStatus == Status.LOADMORE_WAIT) {
            time = 200
        }
        mAutoScroller.autoScroll(-offset, time)
    }

    private fun autoScrollFinished() {
        if (mStatus == Status.REFRESH_RETURNING || mStatus == Status.LOADMORE_RETURNING) {
            if (mMoveOffset != 0) {
                mMoveOffset = 0
                setStatus(Status.DEFAULT)
            }
        }
    }

    /**
     * check if it can refresh
     *
     * @return
     */
    private fun onCheckCanRefresh(): Boolean {
        return !canChildScrollUp()
    }

    /**
     * check if it can load more
     *
     * @return
     */
    private fun onCheckCanLoadMore(): Boolean {

        return !canChildScrollDown() && !mNoMore
    }

    /**
     * copy from [android.support.v4.widget.SwipeRefreshLayout.canChildScrollUp]
     *
     * @return Whether it is possible for the child view of this layout to
     * scroll up. Override this if the child view is a custom view.
     */
    private fun canChildScrollUp(): Boolean {
        return mRecyclerView.canScrollVertically(-1)
    }

    /**
     * copy from [android.support.v4.widget.SwipeRefreshLayout.canChildScrollUp]
     *
     * @return Whether it is possible for the child view of this layout to
     * scroll up. Override this if the child view is a custom view.
     */
    private fun canChildScrollDown(): Boolean {
        return mRecyclerView.canScrollVertically(1)
    }

    fun refreshComplete() {
        setStatus(Status.REFRESH_RETURNING)
        reset(mMoveOffset)
    }

    fun loadMoreComplete() {
        setStatus(Status.REFRESH_RETURNING)
        reset(mMoveOffset)
    }

    fun setOnPullToRefreshListener(listener: OnPullToRefreshListener) {
        mListener = listener
    }

    private inner class AutoScroller : Runnable {

        private val mScroller: Scroller = Scroller(context)

        private var mmLastY: Int = 0

        private var mRunning = false

        private var mAbort = false

        override fun run() {
            val finish = !mScroller.computeScrollOffset() || mScroller.isFinished
            val currY = mScroller.currY
            val yDiff = currY - mmLastY
            if (finish) {
                finish()
            } else {
                mmLastY = currY
                updataStatusLayout(yDiff.toFloat())
                post(this)
            }
        }

        /**
         * remove the post callbacks and reset default values
         */
        private fun finish() {
            mmLastY = 0
            mRunning = false
            removeCallbacks(this)
            // if abort by user, don't call
            if (!mAbort) {
                autoScrollFinished()
            }
        }

        /**
         * abort scroll if it is scrolling
         */
        fun abortIfRunning() {
            if (mRunning) {
                if (!mScroller.isFinished) {
                    mAbort = true
                    mScroller.forceFinished(true)
                }
                finish()
                mAbort = false
            }
        }

        /**
         * The param yScrolled here isn't final pos of y.
         * It's just like the yScrolled param in the
         *
         * @param yScrolled
         * @param duration
         */
        fun autoScroll(yScrolled: Int, duration: Int) {
            removeCallbacks(this)
            mmLastY = 0
            if (!mScroller.isFinished) {
                mScroller.forceFinished(true)
            }
            mScroller.startScroll(0, 0, 0, yScrolled, duration)
            post(this)
            mRunning = true
        }
    }

    interface OnPullToRefreshListener {
        fun onRefresh()

        fun onLoadMore()
    }
}