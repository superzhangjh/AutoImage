package com.zjh.autoimage

import android.content.ContentValues.TAG
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.LruCache
import android.widget.ImageView
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

    private val convertingList by lazy { mutableListOf<Drawable>() }
    private val caches by lazy { mutableMapOf<Drawable, Drawable>() }
    private var count = 0

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
     */
    @Around("execution(* android.widget.ImageView.setImageDrawable(..)) && !withincode(* com.bumptech.glide.request.target.setDrawable(..))")
    fun onSetImageDrawable(joinPoint: ProceedingJoinPoint) {
        joinPoint.signature
        if (adapter != null) {
            for ((i, it) in joinPoint.args.withIndex()) {
//                if (it != null && it is Drawable && !isConverting(it)) {
                if (it != null && it is Drawable) {
                     //检查缓存
//                    val cache = caches[it]
//                    if (cache == null && !caches.values.contains(it)) {
//                        设置成正在转换
//                        setConvertState(it, true)
                        val iv = joinPoint.target as ImageView

                        //交由转换器处理
                        adapter!!.convert(iv, it) { drawable ->

                            //添加到缓存
                            if (drawable != null) {
                                caches[it] = drawable
                            }

                            //设置转换完成，注意这里要使用未转换的res，不是转换完成的
//                            setConvertState(it, false)

                            //修改参数值为已适配的，再执行view设置资源的方法
                            joinPoint.args[i] = drawable
                            joinPoint.proceed()
                            Log.d(TAG, "onSetImageDrawable setResource no cache ${iv.id} 转换前：$it 转换后：$drawable")
                        }
//                    } else {
                        //直接使用缓存
//                        joinPoint.args[i] = cache
//                        joinPoint.proceed()
//
//                        val iv = joinPoint.target as ImageView
//                        Log.d(TAG, "onSetImageDrawable setResource has cache ${iv.id} $it")
//                    }
                    return
                }
            }
        }
        //如无转换，则执行原方法
        joinPoint.proceed()
        val iv = joinPoint.target as ImageView
//        Log.d(TAG, "onSetImageDrawable null ${iv.id}")
    }

    private fun isConverting(res: Drawable): Boolean {
        return convertingList.contains(res)
    }

    private fun setConvertState(res: Drawable, convert: Boolean) {
        if (convert) {
            convertingList.add(res)
        } else {
            convertingList.remove(res)
        }
    }
}