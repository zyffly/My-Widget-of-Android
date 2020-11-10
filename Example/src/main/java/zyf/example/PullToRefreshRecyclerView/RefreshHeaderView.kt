package zyf.example.PullToRefreshRecyclerView

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_header_footer.view.*
import zyf.example.R

/**
 * Created by zyf on 17-12-9.
 */

class RefreshHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        initView()
    }

    private fun initView() {
        orientation = LinearLayout.VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_header_footer, this, true)
        onPrepare()
    }


    fun onPrepare() {
        mProgressBar.visibility = View.GONE
        mTextView.setText(R.string.pull_header_refresh)
    }

    fun onWait() {
        mProgressBar.visibility = View.GONE
        mTextView.setText(R.string.pull_header_let_go_refresh)
    }

    fun onRefreshing() {
        mProgressBar.visibility = View.VISIBLE
        mTextView.setText(R.string.pull_header_refreshing)
    }

    fun onRefreshComplete() {
        mProgressBar.visibility = View.GONE
        mTextView.setText(R.string.pull_header_refresh_suc)
    }

}
