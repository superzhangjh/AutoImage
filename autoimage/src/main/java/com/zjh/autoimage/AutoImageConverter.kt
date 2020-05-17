package com.zjh.autoimage

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView

/**
 * 资源转换器
 */
interface AutoImageConverter {

    /**
     * @return 是否已经处理
     */
    fun convert(view: ImageView, drawable: Drawable, result: (drawable: Drawable?) -> Unit)
}