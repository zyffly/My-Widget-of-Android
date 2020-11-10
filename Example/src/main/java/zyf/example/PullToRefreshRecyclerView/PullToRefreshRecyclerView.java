package zyf.example.PullToRefreshRecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by zyf on 17-12-8.
 */

public class PullToRefreshRecyclerView extends ViewGroup {

    /**
     * 复位初始状态
     */
    private static final int STATUS_REFRESH_RETURNING = -4;
    /**
     * 正在刷新
     */
    private static final int STATUS_REFRESHING = -3;
    /**
     * 等待松手状态(下载状态，且下拉高度大于固定的HeaderView的高度)
     */
    private static final int STATUS_REFRESH_WAIT = -2;
    /**
     * 下拉状态
     */
    private static final int STATUS_REFRESH_SATART = -1;
    /**
     * 滑动RecyclerView的状态
     */
    private static final int STATUS_DEFAULT = 0;
    private static final int STATUS_LOADMORE_SATART = 1;
    private static final int STATUS_LOADMORE_WAIT = 2;
    //    private static final int STATUS_LOADMORE_RETURN_H = 3;
    private static final int STATUS_LOADMOREING = 3;
    private static final int STATUS_LOADMORE_RETURNING = 4;

    private RefreshHeaderView mHeaderView;
    private RecylerFooterView mFooterView;
    private RecyclerView mRecyclerView;

    private int mStatus;
    private int mTouchSlop;
    private int mMoveOffset = 0;

    private int mHearderViewHeight;
    private int mFooterViewHeight;

    private OnPullToRefreshListener mListener;
    private AutoScroller mAutoScroller;

    private boolean mNoMore;

    public PullToRefreshRecyclerView(Context context) {
        this(context, null);
    }

    public PullToRefreshRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PullToRefreshRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    private void initView() {

        mHeaderView = new RefreshHeaderView(getContext());
        mRecyclerView = new RecyclerView(getContext());
        mFooterView = new RecylerFooterView(getContext());
        addView(mHeaderView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        addView(mFooterView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        addView(mRecyclerView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));


        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        setStatus(STATUS_DEFAULT);
        mAutoScroller = new AutoScroller();
    }

    public boolean isNoMore() {
        return mNoMore;
    }

    public void setNoMore(boolean noMore) {
        this.mNoMore = noMore;
    }

    public void setAdapter(RecyclerView.Adapter adapter) {
        if (mRecyclerView != null) {
            mRecyclerView.setAdapter(adapter);
        }
    }

    public void setLayoutManager(RecyclerView.LayoutManager manager) {
        if (mRecyclerView != null) {
            mRecyclerView.setLayoutManager(manager);
        }
    }

    public void addItemDecoration(RecyclerView.ItemDecoration decoration) {
        if (mRecyclerView != null) {
            mRecyclerView.addItemDecoration(decoration);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthMeasure = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMeasure = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int tmpW = 0;
        int tmpH = 0;
        View child;
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            child = getChildAt(i);
            //先测量子View
            measureChild(child, widthMeasureSpec, heightMeasureSpec);

            int childW = child.getMeasuredWidth();
            int childH = child.getMeasuredHeight();
            if (tmpW < childW) {
                tmpW = childW;
            }

            tmpH = tmpH + childH;
        }

        tmpH = tmpH > heightMeasure ? heightMeasure : tmpH;
        setMeasuredDimension(
                widthMode != MeasureSpec.EXACTLY ? tmpW : widthMeasure,
                heightMode != MeasureSpec.EXACTLY ? tmpH : heightMeasure);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        layoutChildren();
    }

    private void layoutChildren() {

        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();

        int l = getPaddingLeft();
        int t = getPaddingTop();
        int r = getPaddingRight();
        int b = getPaddingBottom();

        int tmpW = width - l - r;

        if (mHeaderView != null) {
            int childW = mHeaderView.getMeasuredWidth();
            int childH = mHeaderView.getMeasuredHeight();

            if (mHearderViewHeight == 0) {
                mHearderViewHeight = childH;
            }

            int offset = mStatus < STATUS_DEFAULT ? mMoveOffset : 0;

            int tmpLeft = childW > tmpW ? l : (width - childW) / 2;
            int tmpTop = t - childH + offset;
            int tmpRight = childW > tmpW ? l + tmpW : (tmpW + childW) / 2;
            int tmpBottom = t + offset;
            mHeaderView.layout(tmpLeft, tmpTop, tmpRight, tmpBottom);
            t = tmpBottom;
        }

        if (mFooterView != null) {
            int childW = mFooterView.getMeasuredWidth();
            int childH = mFooterView.getMeasuredHeight();
            if (mFooterViewHeight == 0) {
                mFooterViewHeight = childH;
            }

            int offset = mStatus > STATUS_DEFAULT ? mMoveOffset : 0;

            int tmpLeft = childW > tmpW ? l : (width - childW) / 2;
            int tmpBottom = height - b + childH + offset;
            int tmpTop = height - b + offset;
            int tmpRight = childW > tmpW ? l + tmpW : (tmpW + childW) / 2;
            mFooterView.layout(tmpLeft, tmpTop, tmpRight, tmpBottom);
            b = b - offset;
            t = t - b;
        }


        if (mRecyclerView != null) {
            int childW = mRecyclerView.getMeasuredWidth();

            int tmpLeft = childW > tmpW ? l : (width - childW) / 2;
            int tmpBottom = height - b;

            int tmpTop = t;
            int tmpRight = childW > tmpW ? l + tmpW : (tmpW + childW) / 2;

            mRecyclerView.layout(tmpLeft, tmpTop, tmpRight, tmpBottom);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    int mActivePointerId;
    float mLastX, mLastY;
    float mInitDownX, mInitDownY;
    int INVALID_POINTER = -1;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (mStatus != STATUS_DEFAULT) {
            return true;
        }

        try {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:

                    mActivePointerId = event.getPointerId(0);
                    mInitDownY = mLastY = event.getY(mActivePointerId);
                    mInitDownX = mLastX = event.getX(mActivePointerId);
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mActivePointerId == INVALID_POINTER) {
                        return false;
                    }
                    float y = event.getY(mActivePointerId);
                    float x = event.getX(mActivePointerId);
                    final float yInitDiff = y - mInitDownY;
                    final float xInitDiff = x - mInitDownX;
                    mLastY = y;
                    mLastX = x;
                    boolean moved = Math.abs(yInitDiff) > Math.abs(xInitDiff);
                    boolean triggerCondition =
                            // refresh trigger condition
                            (yInitDiff > 0 && moved && onCheckCanRefresh()) ||
                                    //load more trigger condition
                                    (yInitDiff < 0 && moved && onCheckCanLoadMore());
                    if (triggerCondition) {
                        // if the refresh's or load more's trigger condition  is true,
                        // intercept the move action event and pass it to SwipeToLoadLayout#onTouchEvent()
                        return true;
                    }
                    break;
                case MotionEvent.ACTION_POINTER_UP: {
//                onSecondaryPointerUp(event);
//                mInitDownY = mLastY = getMotionEventY(event, mActivePointerId);
//                mInitDownX = mLastX = getMotionEventX(event, mActivePointerId);
                    break;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mActivePointerId = INVALID_POINTER;
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = event.getPointerId(0);
                break;
            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER) {
                    return false;
                }
                float y = event.getY(mActivePointerId);
                float x = event.getX(mActivePointerId);
                final float yInitDiff = y - mInitDownY;
                final float xInitDiff = x - mInitDownX;
                mLastY = y;
                mLastX = x;
                if (Math.abs(xInitDiff) > Math.abs(yInitDiff) && Math.abs(xInitDiff) > mTouchSlop && mStatus == STATUS_DEFAULT) {
                    return false;
                }

                if (mStatus == STATUS_DEFAULT) {
                    if (yInitDiff > 0 && onCheckCanRefresh()) {
                        setStatus(STATUS_REFRESH_SATART);
                    } else if (yInitDiff < 0 && onCheckCanLoadMore()) {
                        setStatus(STATUS_LOADMORE_SATART);
                    }
                } else if (mStatus == STATUS_REFRESHING || mStatus == STATUS_LOADMOREING) {
                    return false;
                }

                if (isUpdateLayout()) {
                    updataStatusLayout(yInitDiff);
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mActivePointerId == INVALID_POINTER) {
                    return false;
                }
                mActivePointerId = INVALID_POINTER;

                /**
                 *　下拉刷新状态改变
                 */
                if (mStatus == STATUS_REFRESH_SATART) {
                    setStatus(STATUS_REFRESH_RETURNING);
                    reset(mMoveOffset);
                    return true;
                } else if (mStatus == STATUS_REFRESH_WAIT) {
                    reset(mMoveOffset);
                    return true;
                } else if (mStatus == STATUS_LOADMORE_SATART) {  //上拉加载更多状态刷新
                    setStatus(STATUS_LOADMORE_RETURNING);
                    reset(mMoveOffset);
                    return true;
                } else if (mStatus == STATUS_LOADMORE_WAIT) {
                    reset(mMoveOffset);
                    return true;
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    /**
     * 是否重新布局
     *
     * @return
     */
    private boolean isUpdateLayout() {
//        if (mStatus == STATUS_REFRESH_SATART    //下拉状态
//                || mStatus == STATUS_REFRESH_WAIT   //下载状态，且下拉高度大于固定的HeaderView的高度
//                || mStatus == STATUS_REFRESH_RETURNING   //复位到初始状态
//                ) {
//            return true;
//        }
//        return false;

        switch (mStatus) {
            case STATUS_REFRESH_SATART:   //下拉状态
            case STATUS_REFRESH_WAIT:     //下载状态，且下拉高度大于固定的HeaderView的高度
            case STATUS_REFRESH_RETURNING:    //下拉，复位到初始状态
            case STATUS_LOADMORE_SATART:    //上拉状态
            case STATUS_LOADMORE_WAIT:    //拉状态，且下拉高度大于固定的HeaderView的高度
            case STATUS_LOADMORE_RETURNING:   //上拉，　复位到初始状态
                return true;
        }
        return false;
    }

    @SuppressLint("LongLogTag")
    private void updataStatusLayout(float dy) {
        if (dy == 0) {
            return;
        }
        switch (mStatus) {
            case STATUS_REFRESH_SATART:
                mMoveOffset = (int) (dy / 3 + 0.5f);
                if (mMoveOffset >= mHearderViewHeight) {
                    setStatus(STATUS_REFRESH_WAIT);
                }
                break;
            case STATUS_REFRESH_WAIT:
                if (mActivePointerId == INVALID_POINTER) {
                    mMoveOffset = mMoveOffset + (int) dy;
                    if (mMoveOffset < mHearderViewHeight) {
                        mMoveOffset = mHearderViewHeight;
                        setStatus(STATUS_REFRESHING);
                        if (mListener != null) {
                            mListener.onRefresh();
                        }
                    }
                } else {
                    mMoveOffset = (int) (dy / 3 + 0.5f);
                }
                break;
            case STATUS_REFRESH_RETURNING:
                mMoveOffset = mMoveOffset + (int) dy;
                if (mMoveOffset <= 0) {
                    mMoveOffset = 0;
                    setStatus(STATUS_DEFAULT);
                }
                break;
            case STATUS_DEFAULT:
                return;

            case STATUS_LOADMORE_SATART:
                mMoveOffset = (int) (dy / 3 - 0.5f);
                if (mMoveOffset <= -mFooterViewHeight) {
                    setStatus(STATUS_LOADMORE_WAIT);
                }
                break;
            case STATUS_LOADMORE_WAIT:
                if (mActivePointerId == INVALID_POINTER) {
                    mMoveOffset = mMoveOffset + (int) dy;
                    if (mMoveOffset > -mFooterViewHeight) {
                        mMoveOffset = -mFooterViewHeight;
                        setStatus(STATUS_LOADMOREING);
                        if (mListener != null) {
                            mListener.onLoadMore();
                        }
                    }
                } else {
                    mMoveOffset = (int) (dy / 3 - 0.5f);
                }
                break;
            case STATUS_LOADMORE_RETURNING:
                mMoveOffset = mMoveOffset + (int) dy;
                if (mMoveOffset >= 0) {
                    mMoveOffset = 0;
                    setStatus(STATUS_DEFAULT);
                }
                break;
        }
        layoutChildren();
        invalidate();
    }

    /**
     * 设置当前状态
     *
     * @param status
     */
    private void setStatus(int status) {
        if (status == mStatus) {
            return;
        }
        mStatus = status;
        switch (mStatus) {
            case STATUS_REFRESH_SATART:
                if (mHeaderView != null) {
                    mHeaderView.onPrepare();
                }
                break;
            case STATUS_REFRESH_WAIT:
                if (mHeaderView != null) {
                    mHeaderView.onWait();
                }
                break;
            case STATUS_REFRESHING:
                if (mHeaderView != null) {
                    mHeaderView.onRefreshing();
                }
                break;
            case STATUS_REFRESH_RETURNING:
                if (mHeaderView != null) {
                    mHeaderView.onRefreshComplete();
                }
                break;
            case STATUS_LOADMORE_SATART:
                if (mFooterView != null) {
                    mFooterView.onPrepare();
                }
                break;
            case STATUS_LOADMORE_WAIT:
                if (mFooterView != null) {
                    mFooterView.onWait();
                }
                break;
            case STATUS_LOADMOREING:
                if (mFooterView != null) {
                    mFooterView.onLoadMoreing();
                }
                break;
            case STATUS_LOADMORE_RETURNING:
                if (mFooterView != null) {
                    mFooterView.onLoadComplete();
                }
                break;
        }
    }


    /**
     * 复位
     */
    private void reset(int offset) {
        int time = 500;
        if (mStatus == STATUS_REFRESH_WAIT || mStatus == STATUS_LOADMORE_WAIT) {
            time = 200;
        }
        mAutoScroller.autoScroll(-offset, time);
    }


    private void autoScrollFinished() {
        if (mStatus == STATUS_REFRESH_RETURNING || mStatus == STATUS_LOADMORE_RETURNING) {
            if (mMoveOffset != 0) {
                mMoveOffset = 0;
                setStatus(STATUS_DEFAULT);
            }
        }
    }

    /**
     * check if it can refresh
     *
     * @return
     */
    protected boolean onCheckCanRefresh() {
        return !canChildScrollUp();
    }

    /**
     * check if it can load more
     *
     * @return
     */
    private boolean onCheckCanLoadMore() {

        return !canChildScrollDown() && !mNoMore;
    }

    /**
     * copy from {@link androidx.core.widget.SwipeRefreshLayout#canChildScrollUp()}
     *
     * @return Whether it is possible for the child view of this layout to
     * scroll up. Override this if the child view is a custom view.
     */
    protected boolean canChildScrollUp() {
        return mRecyclerView.canScrollVertically(-1);
    }

    /**
     * copy from {@link androidx.core.widget.SwipeRefreshLayout#canChildScrollUp()}
     *
     * @return Whether it is possible for the child view of this layout to
     * scroll up. Override this if the child view is a custom view.
     */
    protected boolean canChildScrollDown() {
        return mRecyclerView.canScrollVertically(1);
    }

    public void refreshComplete() {
        setStatus(STATUS_REFRESH_RETURNING);
        reset(mMoveOffset);
    }

    public void loadMoreComplete() {
        setStatus(STATUS_REFRESH_RETURNING);
        reset(mMoveOffset);
    }

    public void setOnPullToRefreshListener(OnPullToRefreshListener listener) {
        mListener = listener;
    }

    private class AutoScroller implements Runnable {

        private Scroller mScroller;

        private int mmLastY;

        private boolean mRunning = false;

        private boolean mAbort = false;

        public AutoScroller() {
            mScroller = new Scroller(getContext());
        }

        @Override
        public void run() {
            boolean finish = !mScroller.computeScrollOffset() || mScroller.isFinished();
            int currY = mScroller.getCurrY();
            int yDiff = currY - mmLastY;
            if (finish) {
                finish();
            } else {
                mmLastY = currY;
                updataStatusLayout(yDiff);
                post(this);
            }
        }

        /**
         * remove the post callbacks and reset default values
         */
        private void finish() {
            mmLastY = 0;
            mRunning = false;
            removeCallbacks(this);
            // if abort by user, don't call
            if (!mAbort) {
                autoScrollFinished();
            }
        }

        /**
         * abort scroll if it is scrolling
         */
        public void abortIfRunning() {
            if (mRunning) {
                if (!mScroller.isFinished()) {
                    mAbort = true;
                    mScroller.forceFinished(true);
                }
                finish();
                mAbort = false;
            }
        }

        /**
         * The param yScrolled here isn't final pos of y.
         * It's just like the yScrolled param in the
         *
         * @param yScrolled
         * @param duration
         */
        private void autoScroll(int yScrolled, int duration) {
            removeCallbacks(this);
            mmLastY = 0;
            if (!mScroller.isFinished()) {
                mScroller.forceFinished(true);
            }
            mScroller.startScroll(0, 0, 0, yScrolled, duration);
            post(this);
            mRunning = true;
        }
    }

    public interface OnPullToRefreshListener {
        void onRefresh();

        void onLoadMore();
    }
}
