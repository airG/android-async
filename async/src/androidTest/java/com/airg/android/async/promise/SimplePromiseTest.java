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

package com.airg.android.async.promise;

import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.airg.android.async.ThreadPool;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Mahram Z. Foadi
 */
@RunWith(AndroidJUnit4.class)
public class SimplePromiseTest extends BaseExecutorTest {
    @Test
    public void successfulExecution() throws InterruptedException, ExecutionException {
        final AtomicBoolean onCompleteCalled = new AtomicBoolean(false);
        final AtomicReference<String> result = new AtomicReference<>();

        final String secret = "this is a test";

        final SimplePromise<String> promise = new SimplePromise<>();
        promise.onComplete(new Promise.OnCompleteListener<String>() {
            @Override
            public void onComplete(final String response) {
                synchronized (onCompleteCalled) {
                    result.set(response);
                    onCompleteCalled.set(true);
                    onCompleteCalled.notifyAll();
                }
            }
        });

        assertFalse(promise.isDone());
        ThreadPool.bg(new EchoTaskRunner<>(new EchoTask<>(secret, 200), promise));

        // wait for task to finish
        synchronized (onCompleteCalled) {
            if (!onCompleteCalled.get())
                onCompleteCalled.wait(1000);
        }

        // We gave it enough time, didn't we? If not called by now, it never will
        assertTrue("Task should have completed", onCompleteCalled.get());
        assertTrue("Task should be done", promise.isDone());
        assertFalse("Task should have succeeded", promise.isFailed());
        assertEquals("Task should have returned the original secret", secret, result.get());
    }

    @Test
    public void lateListenerSuccessExecution() throws InterruptedException, ExecutionException {
        final AtomicBoolean onCompleteCalled = new AtomicBoolean(false);
        final AtomicReference<String> result = new AtomicReference<>();

        final String secret = "My listener is late";

        final SimplePromise<String> promise = new SimplePromise<>();

        assertFalse(promise.isDone());

        ThreadPool.bg(new EchoTaskRunner<>(new EchoTask<>(secret, 300), promise));

        // wait for task to finish
        Thread.sleep(1000);

        assertTrue("Task should be done", promise.isDone());

        promise.onComplete(new Promise.OnCompleteListener<String>() {
            @Override
            public void onComplete(final String response) {
                result.set(response);
                onCompleteCalled.set(true);
            }
        });

        // We gave it enough time, didn't we? If not called by now, it never will
        assertTrue("Task should have completed", onCompleteCalled.get());
        assertFalse("Task should have succeeded", promise.isFailed());
        assertEquals("Task should have returned the original secret", secret, result.get());
    }

    @Test
    public void cancelMidExecution() throws InterruptedException, ExecutionException {
        final AtomicBoolean lock = new AtomicBoolean();
        final AtomicBoolean onCompleteCalled = new AtomicBoolean(false);
        final AtomicBoolean onCancelledCalled = new AtomicBoolean(false);

        final SimplePromise<Void> promise = new SimplePromise<>();
        final long taskRuntime = 1000;

        promise.onComplete(new Promise.OnCompleteListener<Void>() {
            @Override
            public void onComplete(final Void ignore) {
                synchronized (lock) {
                    onCompleteCalled.set(true);
                    lock.set(true);
                    lock.notifyAll();
                }
            }
        }).onCancel(new Promise.OnCancelListener() {
            @Override
            public void onCancelled() {
                synchronized (lock) {
                    onCancelledCalled.set(true);
                    lock.set(true);
                    lock.notifyAll();
                }
            }
        });

        assertFalse(promise.isDone());

        final EchoTaskRunner<Void> task = new EchoTaskRunner<>(new EchoTask<Void>(null, taskRuntime), promise);
        ThreadPool.bg(task);

        assertFalse(promise.isDone());

        Thread.sleep(taskRuntime / 4);
        task.cancel();

        // wait for task to finish
        synchronized (lock) {
            if (!lock.get())
                lock.wait(taskRuntime);
        }

        // We gave it enough time, didn't we? If not called by now, it never will
        assertFalse("Task should NOT have completed", onCompleteCalled.get());
        assertTrue("Task should have cancelled", onCancelledCalled.get());
        assertTrue("Task should be done", promise.isDone());
        assertFalse("Task should NOT have failed", promise.isFailed());
        assertTrue("Task should have been cancelled", promise.isCancelled());
    }

    @Test
    public void lateListenerCancelMidExecution() throws InterruptedException, ExecutionException {
        final AtomicBoolean onCompleteCalled = new AtomicBoolean(false);
        final AtomicBoolean onCancelledCalled = new AtomicBoolean(false);

        final SimplePromise<Void> promise = new SimplePromise<>();
        final long taskRuntime = 1000;
        final EchoTaskRunner<Void> task = new EchoTaskRunner<>(new EchoTask<Void>(null, taskRuntime), promise);

        assertFalse(promise.isDone());
        ThreadPool.bg(task);
        assertFalse(promise.isDone());

        Thread.sleep(taskRuntime / 4);
        task.cancel();

        // should be more than enough to properly cancel
        Thread.sleep(taskRuntime);

        promise.onComplete(new Promise.OnCompleteListener<Void>() {
            @Override
            public void onComplete(final Void ignore) {
                onCompleteCalled.set(true);
            }
        }).onCancel(new Promise.OnCancelListener() {
            @Override
            public void onCancelled() {
                onCancelledCalled.set(true);
            }
        });

        assertTrue("Task should be done", promise.isDone());
        assertTrue("Task should have been cancelled", promise.isCancelled());
        assertFalse("Task should NOT have failed", promise.isFailed());
        assertTrue("Task should have cancelled", onCancelledCalled.get());
        assertFalse("Task should NOT have completed", onCompleteCalled.get());
    }

    @Test
    public void lateListenerCancelAfterExecution() throws InterruptedException, ExecutionException {
        final AtomicBoolean onCompleteCalled = new AtomicBoolean(false);
        final AtomicBoolean onCancelledCalled = new AtomicBoolean(false);

        final SimplePromise<Void> promise = new SimplePromise<>();

        final long taskRuntime = 1000;
        final EchoTaskRunner<Void> task = new EchoTaskRunner<>(new EchoTask<Void>(null, taskRuntime), promise);

        promise.onComplete(new Promise.OnCompleteListener<Void>() {
            @Override
            public void onComplete(final Void ignore) {
                synchronized (onCompleteCalled) {
                    onCompleteCalled.set(true);
                    task.cancel();
                    safeSleep(200);
                    onCompleteCalled.notifyAll();
                }
            }
        });

        assertFalse(promise.isDone());
        ThreadPool.bg(task);
        assertFalse(promise.isDone());

        synchronized (onCompleteCalled) {
            if (!onCancelledCalled.get())
                onCompleteCalled.wait();

            safeSleep(200);
        }

        promise.onCancel(new Promise.OnCancelListener() {
            @Override
            public void onCancelled() {
                onCancelledCalled.set(true);
            }
        });

        assertTrue("Task should be done", promise.isDone());
        assertFalse("Task should NOT have been cancelled (because it finished first)", promise.isCancelled());
        assertFalse("onCancel should NOT have been called", onCancelledCalled.get());
        assertFalse("Task should NOT have failed", promise.isFailed());
        assertTrue("Task should have completed", onCompleteCalled.get());
    }

    private void safeSleep(final long dur) {
        try {
            Thread.sleep(dur);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test(expected = RuntimeException.class)
    public void failedExecution() throws InterruptedException {
        final AtomicBoolean lock = new AtomicBoolean();
        final AtomicBoolean onCompleteCalled = new AtomicBoolean(false);
        final AtomicBoolean onFailedCalled = new AtomicBoolean(false);
        final AtomicReference<Throwable> error = new AtomicReference<>(null);

        final String failMsg = "Failz";
        final SimplePromise<Boolean> promise = new SimplePromise<>();

        promise.onComplete(new Promise.OnCompleteListener<Boolean>() {
            @Override
            public void onComplete(Boolean aBoolean) {
                synchronized (lock) {
                    onCompleteCalled.set(true);
                    lock.set(true);
                    lock.notifyAll();
                }
            }
        }).onFail(new Promise.OnFailListener() {
            @Override
            public void onFailed(Throwable t) {
                synchronized (lock) {
                    onFailedCalled.set(true);
                    error.set(t);
                    lock.set(true);
                    lock.notifyAll();
                }
            }
        });

        assertFalse(promise.isDone());
        ThreadPool.bg(new EchoTaskRunner<>(new EchoTask<>(true, 200, new RuntimeException(failMsg)), promise));

        // wait for task to finish
        synchronized (lock) {
            if (!lock.get())
                lock.wait(1000);
        }

        // We gave it enough time, didn't we? If not called by now, it never will
        assertTrue("Task should have failed", onFailedCalled.get());
        assertTrue("Task should be done", promise.isDone());
        assertFalse("Task should NOT have succeeded", promise.succeeded());

        final RuntimeException t = (RuntimeException) error.get();
        assertEquals(failMsg, t.getMessage());
        throw t;
    }

    @Test(expected = RuntimeException.class)
    public void lateListenerFailedExecution() throws InterruptedException {
        final AtomicBoolean onCompleteCalled = new AtomicBoolean(false);
        final AtomicBoolean onFailedCalled = new AtomicBoolean(false);
        final AtomicReference<Throwable> error = new AtomicReference<>(null);

        final String failMsg = "Failz";
        final SimplePromise<Boolean> promise = new SimplePromise<>();
        final EchoTaskRunner<Boolean> task = new EchoTaskRunner<>(new EchoTask<>(true, 200, new RuntimeException(failMsg)), promise);

        assertFalse(promise.isDone());
        ThreadPool.bg(task);

        Thread.sleep(1000);

        assertTrue("Task should be done", promise.isDone());
        assertFalse("Task should NOT have succeeded", promise.succeeded());

        promise.onComplete(new Promise.OnCompleteListener<Boolean>() {
            @Override
            public void onComplete(Boolean aBoolean) {
                onCompleteCalled.set(true);
            }
        }).onFail(new Promise.OnFailListener() {
            @Override
            public void onFailed(Throwable t) {
                onFailedCalled.set(true);
                error.set(t);
            }
        });

        // We gave it enough time, didn't we? If not called by now, it never will
        assertTrue("Task should have failed", onFailedCalled.get());


        final RuntimeException t = (RuntimeException) error.get();
        assertEquals(failMsg, t.getMessage());
        throw t;
    }

    private static class EchoTaskRunner<VALUE> implements Runnable {

        private final EchoTask<VALUE> task;
        private final SimplePromise<VALUE> promise;

        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        EchoTaskRunner(final EchoTask<VALUE> t, final SimplePromise<VALUE> p) {
            task = t;
            promise = p;
        }

        void cancel() {
            Log.i("ETR", "Cancelling Echo task");
            cancelled.set(true);
        }

        @Override
        public void run() {
            try {
                Log.i("ETR", "Echo task starting...");
                final VALUE result = task.call();

                if (cancelled.get()) {
                    Log.i("ETR", "Echo task finished, but it was cancelled");
                    promise.cancelled();
                    return;
                }

                Log.i("ETR", "Echo task finished: " + result);
                promise.success(result);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("ETR", "Echo task failed");
                promise.failed(e);
            }
        }
    }
}