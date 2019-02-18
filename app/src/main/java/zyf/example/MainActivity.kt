package zyf.example

import android.content.Intent
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*
import zyf.example.ItemDecoration.RecycleViewDivider

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initView()
        initData()
    }

    private fun initView() {

        val divider = RecycleViewDivider(this)
        divider.setBackgroundColor(Color.BLACK)
        divider.setDividerHeight(1)
        divider.setMargin(5, 0, 5, 0)
        mRecyclerView.layoutManager = LinearLayoutManager(this)
        mRecyclerView.addItemDecoration(divider)
    }

    private fun initData() {
        val list = ArrayList<String>()
        list.add("PullToRefreshRecyclerView")

        val adapter = Adapter()
        adapter.setData(list)
        mRecyclerView.adapter = adapter
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

            val view = TextView(this@MainActivity)
            view.setPadding(30, 50, 30, 50)

            return ViewHolder(view)
        }

        override fun getItemCount(): Int = mList.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            (holder.itemView as TextView).text = mList[position]

            holder.itemView.setOnClickListener {
                startActivity(Intent(this@MainActivity,PullToRefreshAct::class.java))
            }
        }

    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
