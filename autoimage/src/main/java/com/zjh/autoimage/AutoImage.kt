package com.zjh.autoimage

import android.content.ContentValues.TAG
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.LruCache
import android.widget.ImageView
import androidx.core.view.ViewCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.ImageViewTarget
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before

/**
 * 自动Image适配
 */
@Aspect
class AutoImage {

    companion object {
        @JvmStatic
        val instances: AutoImage by lazy { AutoImage() }
    }

    private var count = 0
    private val viewId by lazy { R.id.id_auto_image_view_converting }

    var adapter: AutoImageConverter? = object :
        AutoImageConverter {
        override fun convert(
            view: ImageView,
            drawable: Drawable,
            result: (drawable: Drawable?) -> Unit
        ) {
            if (count > 20) {
                return
            }
            count++
            Glide.with(view.context)
                .load(drawable)
                .placeholder(R.drawable.test_loading)
                .into(object : ImageViewTarget<Drawable>(view) {
                    override fun setResource(resource: Drawable?) {
                        result.invoke(resource)
                    }
                })
        }
    }

    /**
     * withincode 表示某个类的构造方法或方法中涉及到的JPoint
     * https://juejin.im/entry/588d45365c497d0056c471ef
     * //TODO:重写一个View的background方法，与这个的id区分开，因为src和background是不冲突的两个图层
     */
    @Around("execution(* android.widget.ImageView.setImageDrawable(..))")
    fun onSetImageDrawable(joinPoint: ProceedingJoinPoint) {
        val iv = joinPoint.target as ImageView
        val tag: Any? = iv.getTag(viewId)
        val converting = if (tag == null) {
            false
        } else {
            tag as Boolean
        }
        Log.d("啊是大飒飒", "${iv.id} 开始转换 是否正在转换：${converting}")
        if (!converting && adapter != null) {
            iv.setTag(viewId, true)
            for ((i, it) in joinPoint.args.withIndex()) {
                if (it is Drawable) {
                    adapter!!.convert(iv, it) {drawable ->
                        //修改参数值为已适配的，再执行view设置资源的方法
                        joinPoint.args[i] = drawable
                        joinPoint.proceed()
                        val finish = drawable != null
                        if (finish) {
                            iv.setTag(viewId, false)
                        }
                        Log.d("啊是大飒飒", "${iv.id} 转化完成:${finish} 转化前：$it 转换后：$drawable")
                    }
                    return
                }
            }
            iv.setTag(viewId, false)
        }
        //如无转换，则执行原方法
        joinPoint.proceed()
        Log.d("啊是大飒飒", "${iv.id} 无转换")
    }
}