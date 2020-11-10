package com.felix.shimmer

import android.graphics.Color

object ShimmerUtils {
    /**
     * 去掉颜色中的透明度
     */
    fun clearColorAlpha(color: Int) =
        Color.argb(
            0,
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )


}