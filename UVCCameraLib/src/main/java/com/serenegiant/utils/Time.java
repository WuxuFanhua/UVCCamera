package com.serenegiant.utils;

import android.annotation.SuppressLint;
import android.os.SystemClock;

/**
 * Your class notes
 *
 * @author gaoguanchao
 * @since 2023/6/19 0019
 */
public class Time {
    private static Time sTime;

    public static long nanoTime() {
        return sTime.timeNs();
    }

    public static void reset() {
        sTime = new Time();
    }

    private Time() {
    }

    protected long timeNs() {
        return SystemClock.elapsedRealtimeNanos();
    }

    static {
        reset();
    }
}
