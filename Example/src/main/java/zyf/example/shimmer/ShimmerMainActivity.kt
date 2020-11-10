package zyf.example.shimmer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_shimmer.*
import zyf.example.R


class ShimmerMainActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shimmer)

        sl_sl.startShimmerAnimation()
        text.onStartShimmerAnimation()
    }
}