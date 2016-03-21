package com.salat.viralcam.app.activities;

import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.test.TouchUtils;
import android.test.UiThreadTest;
import android.view.MotionEvent;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Marek on 20.03.2016.
 */
//@RunWith(AndroidJUnit4.class)
public class TrimapActivityTest extends ActivityInstrumentationTestCase2<TrimapActivity> {

    private TrimapActivity activity;

    public TrimapActivityTest() {
        super(TrimapActivity.class);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();

        activity = getActivity();
    }

    @After
    public void tearDown() {
        activity.finish();
    }

    @Test
//    @UiThreadTest
    public void testCalculateAlphaMask() {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        getInstrumentation().waitForIdleSync();

//        TouchUtils.drag(this, 0f, 0f, 250f, 250f, 30);
//        getInstrumentation().waitForIdleSync();

        Instrumentation inst = getInstrumentation();

        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();

        List<Point> path = new ArrayList<>();
        path.add(new Point(250, 250));
        path.add(new Point(500, 250));
        path.add(new Point(500, 500));
        path.add(new Point(250, 500));

        MotionEvent event = MotionEvent.obtain(downTime, eventTime,
                MotionEvent.ACTION_DOWN, path.get(0).x, path.get(0).y, 0);
        inst.sendPointerSync(event);
        inst.waitForIdleSync();

        for (int i = 0; i < path.size(); ++i) {
            Point point = path.get(i);
            eventTime = SystemClock.uptimeMillis();
            event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE, point.x, point.y, 0);
            inst.sendPointerSync(event);
            inst.waitForIdleSync();
        }

        eventTime = SystemClock.uptimeMillis();
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, path.get(path.size()-1).x, path.get(path.size()-1).y, 0);
        inst.sendPointerSync(event);
        inst.waitForIdleSync();

        Instrumentation.ActivityMonitor monitor = getInstrumentation()
                .addMonitor(Instrumentation.ActivityMonitor.class.getName(),
                        null, false);
        monitor.waitForActivityWithTimeout(10000);
    }
}