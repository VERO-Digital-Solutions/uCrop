package com.yalantis.ucrop

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.doOnAttach
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentActivity

/**
 * To support android 15 changes on promoting edge to edge on apps we need to mock a view
 * in order to paint it since we cannot longer paint the status bar because the content is drawn
 * underneath it and not below as it used to in previous versions.
 *
 * Here we just add a dummy view to act as a status bar and then apply some insets to avoid
 * camera cutouts and display the content in a better way
 */
internal fun FragmentActivity.adjustEdgeToEdge(
    contentView: View,
    setContent: Boolean = true
): FrameLayout {
    WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
    val dummyParentLayout = createDummyParentLayout()
    val fakeBar = createFakeBar()
    dummyParentLayout.addView(contentView)
    dummyParentLayout.addView(fakeBar)
    if (setContent) setContentView(dummyParentLayout)
    var insetsApplied = false
    ViewCompat.setOnApplyWindowInsetsListener(contentView) { v, insets ->
        insetsApplied = true
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        adjustHeightOnFakeBar(fakeBar, systemBars)
        applyInsets(v, systemBars)
        insets
    }
    if (!insetsApplied) {
        contentView?.doOnAttach {
            it.requestApplyInsets()
        }
    }
    return dummyParentLayout
}

private fun FragmentActivity.createDummyParentLayout(): FrameLayout = FrameLayout(this).apply {
    layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )
}

private fun Context.createFakeBar(): View {
    return View(this).apply {
        tag = "status bar"
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            0, // placeholder height, will be updated with insets
            Gravity.TOP
        )
        setBackgroundColor(getColorRes(R.color.ucrop_color_statusbar))
    }
}

/**
 * Drawer layout requires special handling - we need to set the insets to content and drawer view
 */
private fun applyInsets(v: View, systemBars: Insets) {
    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
}

private fun adjustHeightOnFakeBar(fakeBar: View, systemBars: Insets) {
    val systemBarHeight =
        maxOf(systemBars.top, systemBars.bottom, systemBars.left, systemBars.right)
    val gravity = if (systemBars.top == systemBarHeight) {
        Gravity.TOP
    } else if (systemBars.left == systemBarHeight) {
        Gravity.START
    } else if (systemBars.bottom == systemBarHeight) {
        Gravity.BOTTOM
    } else {
        Gravity.END
    }
    fakeBar.updateLayoutParams<FrameLayout.LayoutParams> {
        if (gravity == Gravity.TOP || gravity == Gravity.END) {
            height = systemBarHeight
            width = ViewGroup.MarginLayoutParams.MATCH_PARENT
        } else { // gravity == Gravity.START || gravity == Gravity.END
            height = ViewGroup.MarginLayoutParams.MATCH_PARENT
            width = systemBarHeight
        }
        this.gravity = gravity
    }
}

internal fun Context.getColorRes(@ColorRes id: Int, alpha: Int? = null): Int {
    val baseColor = ContextCompat.getColor(this, id)
    return if (alpha == null) {
        baseColor
    } else {
        ColorUtils.setAlphaComponent(baseColor, alpha)
    }
}
