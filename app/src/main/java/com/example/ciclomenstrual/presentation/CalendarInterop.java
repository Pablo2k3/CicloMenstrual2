package com.example.ciclomenstrual.presentation;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;

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

    public static void setBackground(CalendarDay day, int resource) {
        day.setBackgroundResource(resource);
    }

    @Nullable
    public static Calendar selectedDate(CalendarView view) {
        return view.getFirstSelectedDate();
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
