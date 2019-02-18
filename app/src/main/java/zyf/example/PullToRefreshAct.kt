package zyf.example

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import zyf.example.ItemDecoration.RecycleViewDivider
import zyf.example.PullToRefreshRecyclerView.PullToRefreshRecyclerView2

class PullToRefreshAct : AppCompatActivity(), PullToRefreshRecyclerView2.OnPullToRefreshListener {
    override fun onRefresh() {
        window.decorView.postDelayed({
            mRecyclerView?.refreshComplete()
        },3000)
    }

    override fun onLoadMore() {
        window.decorView.postDelayed({
            mRecyclerView?.loadMoreComplete()
        },3000)
    }

    private var mRecyclerView : PullToRefreshRecyclerView2 ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mRecyclerView = PullToRefreshRecyclerView2(this)
        setContentView(mRecyclerView)

        initView()
    }

    private fun initView() {
        val list = ArrayList<String>()
        for (i in 0 until 5) {
            list.add(i.toString())
        }

        val divider = RecycleViewDivider(this)
        divider.setBackgroundColor(Color.BLACK)
        divider.setDividerHeight(1)
        divider.setMargin(5, 0, 5, 0)
        mRecyclerView?.setLayoutManager(LinearLayoutManager(this))
        mRecyclerView?.addItemDecoration(divider)

        mRecyclerView?.setOnPullToRefreshListener(this)

        val adapter = Adapter()
        adapter.setData(list)
        mRecyclerView?.setAdapter(adapter)
    }

    inner class Adapter : RecyclerView.Adapter<ViewHolder>() {

        private val mList = ArrayList<String>()

        fun setData(list: ArrayList<String>) {
            list.let {
                mList.clear()
                mList.addAll(it)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, position: Int): ViewHolder {

            val view = TextView(this@PullToRefreshAct)
            view.setPadding(30, 50, 30, 50)

            view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT)

            return ViewHolder(view)
        }

        override fun getItemCount(): Int = mList.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            (holder.itemView as TextView).text = mList[position]
            holder.itemView.setOnClickListener {
                System.out.println(mList[position])
                Log.e("onBindViewHolder",mList[position])
            }
        }

    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}