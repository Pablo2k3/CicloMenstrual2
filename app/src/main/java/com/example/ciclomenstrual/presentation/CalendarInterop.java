package com.example.ciclomenstrual.presentation;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.applandeo.materialcalendarview.CalendarDay;
import com.applandeo.materialcalendarview.CalendarUtils;
import com.applandeo.materialcalendarview.CalendarView;

import java.util.Calendar;

/**
 * Java bridge for APIs hidden from Kotlin by the calendar library's legacy metadata.
 * Keeping this interop in one place avoids leaking the third-party limitation into presentation code.
 */
public final class CalendarInterop {
    private CalendarInterop() {}

    public interface DayCallback {
        void onDay(CalendarDay day);
    }

    public static void configureRange(
            CalendarView view,
            Calendar minimum,
            Calendar maximum,
            DayCallback callback
    ) {
        view.setMinimumDate(minimum);
        view.setMaximumDate(maximum);
        view.setOnCalendarDayClickListener(callback::onDay);
    }

    /**
     * The library's internal calendar root has a fixed white background.
     * Keep it aligned with the app surface in both light and dark themes.
     */
    public static void setSurfaceColor(CalendarView view, int color) {
        if (view.getChildCount() > 0) {
            View calendarRoot = view.getChildAt(0);
            calendarRoot.setBackgroundColor(color);
        }
    }

    public static void setBackground(CalendarDay day, int resource) {
        day.setBackgroundResource(resource);
    }

    public static void setLabelColor(CalendarDay day, int color) {
        day.setLabelColor(color);
    }

    public static Drawable textDrawable(
            Context context,
            String text,
            int color,
            int size
    ) {
        return CalendarUtils.getDrawableText(context, text, (Typeface) null, color, size);
    }
}
