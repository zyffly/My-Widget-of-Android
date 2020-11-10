package zyf.example

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import zyf.example.ItemDecoration.RecycleViewDivider
import zyf.example.shimmer.ShimmerMainActivity

class MainActivity : AppCompatActivity() {

    enum class Type(val value:String){
        PULL_TO_REFRESH_RECYCLERVIEW("PullToRefreshRecyclerView"),
        SHIMMER("Shimmer")
    }

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
//        val list = ArrayList(Type.values())
//        list.add("PullToRefreshRecyclerView")
//        list.add("shimmer")
//        Type.values()

        val adapter = Adapter()
        adapter.setData(ArrayList<Type>().apply {
            addAll(Type.values())
        })
        mRecyclerView.adapter = adapter
    }

    inner class Adapter : RecyclerView.Adapter<ViewHolder>() {

        private val mList = ArrayList<Type>()

        fun setData(list: ArrayList<Type>) {
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
            val type = mList[position]
            (holder.itemView as TextView).text = type.value

            holder.itemView.setOnClickListener {
                when(type){
                    Type.PULL_TO_REFRESH_RECYCLERVIEW -> {
                        startActivity(Intent(this@MainActivity, PullToRefreshAct::class.java))
                    }
                    Type.SHIMMER -> {
                        startActivity(Intent(this@MainActivity,ShimmerMainActivity::class.java))
                    }
                }
            }
        }

    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
