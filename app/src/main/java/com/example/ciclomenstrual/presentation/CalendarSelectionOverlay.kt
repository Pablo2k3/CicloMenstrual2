package com.example.ciclomenstrual.presentation

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.ciclomenstrual.R

/**
 * Draws selection feedback without asking the calendar's PagerAdapter to rebuild its pages.
 */
class CalendarSelectionOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {
    private var pendingDayLabel: TextView? = null
    private var selectedCell: SelectedCell? = null

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> clearSelection()
            MotionEvent.ACTION_UP -> pendingDayLabel = findDayLabelAt(this, event.x, event.y)
        }
        return super.dispatchTouchEvent(event)
    }

    fun showSelection() {
        val label = pendingDayLabel ?: return
        selectedCell = SelectedCell(label, label.background, label.textColors)
        label.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(ContextCompat.getColor(context, R.color.selected_day))
        }
        label.setTextColor(ContextCompat.getColor(context, R.color.white))
        pendingDayLabel = null
    }

    private fun clearSelection() {
        selectedCell?.let {
            it.label.background = it.background
            it.label.setTextColor(it.textColors)
        }
        selectedCell = null
        pendingDayLabel = null
    }

    private fun findDayLabelAt(view: View, x: Float, y: Float): TextView? {
        if (view.id == com.applandeo.materialcalendarview.R.id.dayLabel) {
            return view as? TextView
        }
        if (view !is ViewGroup) return null

        for (index in view.childCount - 1 downTo 0) {
            val child = view.getChildAt(index)
            val childX = x + view.scrollX - child.left - child.translationX
            val childY = y + view.scrollY - child.top - child.translationY
            if (childX in 0f..child.width.toFloat() && childY in 0f..child.height.toFloat()) {
                findDayLabelAt(child, childX, childY)?.let { return it }
            }
        }
        return null
    }

    private data class SelectedCell(
        val label: TextView,
        val background: Drawable?,
        val textColors: ColorStateList,
    )
}
