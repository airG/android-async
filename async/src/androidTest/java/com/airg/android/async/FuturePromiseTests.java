/*
 * ****************************************************************************
 *   Copyright  2017 airG Inc.                                                 *
 *                                                                             *
 *   Licensed under the Apache License, Version 2.0 (the "License");           *
 *   you may not use this file except in compliance with the License.          *
 *   You may obtain a copy of the License at                                   *
 *                                                                             *
 *       http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                             *
 *   Unless required by applicable law or agreed to in writing, software       *
 *   distributed under the License is distributed on an "AS IS" BASIS,         *
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *   See the License for the specific language governing permissions and       *
 *   limitations under the License.                                            *
 * ***************************************************************************
 */

package com.airg.android.async;

import android.support.test.runner.AndroidJUnit4;

import com.airg.android.async.future.FuturePromise;
import com.airg.android.async.future.Promise;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by mahramf.
 */
@RunWith(AndroidJUnit4.class)
public final class FuturePromiseTests {
    final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Test
    public void normalExecution() throws InterruptedException, ExecutionException {
        final AtomicBoolean onCompleteCalled = new AtomicBoolean(false);

        final FuturePromise<Boolean> task = new FuturePromise<>(new SleepTask(200));
        task.onComplete(new Promise.OnCompleteListener<Boolean>() {
            @Override
            public void onComplete(Boolean aBoolean) {
                synchronized (onCompleteCalled) {
                    onCompleteCalled.set(true);
                    onCompleteCalled.notifyAll();
                }
            }
        });

        assertFalse(task.isDone());
        executor.execute(task);

        // wait for task to finish
        synchronized (onCompleteCalled) {
            if (!onCompleteCalled.get())
                onCompleteCalled.wait(1000);
        }

        // We gave it enough time, didn't we? If not called by now, it never will
        assertTrue("Task should have completed", onCompleteCalled.get());
        assertTrue("Task should be done", task.isDone());
        assertTrue("Task should have succeeded", task.succeeded());
        assertTrue("Task should have returned true", task.get());
    }

    @Test
    public void cancelMidExecution() throws InterruptedException, ExecutionException {
        final AtomicBoolean lock = new AtomicBoolean ();
        final AtomicBoolean onCompleteCalled = new AtomicBoolean(false);
        final AtomicBoolean onCancelledCalled = new AtomicBoolean(false);

        final FuturePromise<Boolean> task = new FuturePromise<>(new SleepTask(1000));
        task.onComplete(new Promise.OnCompleteListener<Boolean>() {
            @Override
            public void onComplete(Boolean aBoolean) {
                synchronized (lock) {
                    onCompleteCalled.set(true);
                    lock.set(true);
                    lock.notifyAll();
                }
            }
        });
        task.onCancel(new Promise.OnCancelListener() {
            @Override
            public void onCancelled() {
                synchronized (lock) {
                    onCancelledCalled.set(true);
                    lock.set(true);
                    lock.notifyAll();
                }
            }
        });

        assertFalse(task.isDone());
        executor.execute(task);
        assertFalse(task.isDone());

        Thread.sleep(100);
        task.cancel(true);

        // wait for task to finish
        synchronized (lock) {
            if (!lock.get())
                lock.wait(2000);
        }

        // We gave it enough time, didn't we? If not called by now, it never will
        assertFalse("Task should NOT have completed", onCompleteCalled.get());
        assertTrue("Task should have cancelled", onCancelledCalled.get());
        assertTrue("Task should be done", task.isDone());
        assertFalse("Task should NOT have succeeded", task.succeeded());
        assertTrue("Task should have been cancelled", task.isCancelled());
    }

    @Test
    public void failedExecution() throws InterruptedException, ExecutionException {
        final AtomicBoolean lock = new AtomicBoolean ();
        final AtomicBoolean onCompleteCalled = new AtomicBoolean(false);
        final AtomicBoolean onFailedCalled = new AtomicBoolean(false);

        final FuturePromise<Boolean> task = new FuturePromise<>(new SleepTask(200, true));
        task.onComplete(new Promise.OnCompleteListener<Boolean>() {
            @Override
            public void onComplete(Boolean aBoolean) {
                synchronized (lock) {
                    onCompleteCalled.set(true);
                    lock.set(true);
                    lock.notifyAll();
                }
            }
        });

        task.onFail(new Promise.OnFailListener() {
            @Override
            public void onFailed(Throwable error) {
                synchronized (lock) {
                    onFailedCalled.set(true);
                    lock.set(true);
                    lock.notifyAll();
                }
            }
        });

        assertFalse(task.isDone());
        executor.execute(task);

        // wait for task to finish
        synchronized (lock) {
            if (!lock.get())
                lock.wait(1000);
        }

        // We gave it enough time, didn't we? If not called by now, it never will
        assertTrue("Task should have failed", onFailedCalled.get());
        assertTrue("Task should be done", task.isDone());
        assertFalse("Task should NOT have succeeded", task.succeeded());
    }

    private static class SleepTask implements Callable<Boolean> {
        private final long duration;
        private final boolean fail;

        SleepTask(final long dur) {
            this(dur, false);
        }

        SleepTask(final long dur, final boolean f) {
            duration = dur;
            fail = f;
        }

        @Override
        public Boolean call() throws Exception {
            Thread.sleep(duration);

            if (fail)
                throw new RuntimeException("I'm supposed to fail!");

            return true;
        }
    }
}
