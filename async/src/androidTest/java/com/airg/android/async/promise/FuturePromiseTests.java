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
 * Created by mahramf.
 */
@RunWith(AndroidJUnit4.class)
public final class FuturePromiseTests extends BaseExecutorTest {
    @Test
    public void normalExecution() throws InterruptedException, ExecutionException {
        final AtomicBoolean onCompleteCalled = new AtomicBoolean(false);

        final FuturePromise<Boolean> promise = new FuturePromise<>(new EchoTask<>(true, 200));
        promise.onComplete(new Promise.OnCompleteListener<Boolean>() {
            @Override
            public void onComplete(Boolean aBoolean) {
                synchronized (onCompleteCalled) {
                    onCompleteCalled.set(true);
                    onCompleteCalled.notifyAll();
                }
            }
        });

        assertFalse(promise.isDone());
        ThreadPool.bg(promise);

        // wait for task to finish
        synchronized (onCompleteCalled) {
            if (!onCompleteCalled.get())
                onCompleteCalled.wait(1000);
        }

        // We gave it enough time, didn't we? If not called by now, it never will
        assertTrue("Task should have completed", onCompleteCalled.get());
        assertTrue("Task should be done", promise.isDone());
        assertTrue("Task should have succeeded", promise.succeeded());
        assertTrue("Task should have returned true", promise.get());
    }

    @Test
    public void normalExecutionResultHolder() throws InterruptedException, ExecutionException {
        final AtomicBoolean onCompleteCalled = new AtomicBoolean(false);
        final AtomicReference<String> result = new AtomicReference<>(null);
        final String expected = "bob";

        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (!result.compareAndSet(null, new EchoTask<>(expected, 200).call()))
                        throw new IllegalStateException();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        final FuturePromise<AtomicReference<String>> promise = new FuturePromise<>(runnable, result);
        promise.onComplete(new Promise.OnCompleteListener<AtomicReference<String>>() {
            @Override
            public void onComplete(final AtomicReference<String> value) {
                synchronized (onCompleteCalled) {
                    onCompleteCalled.set(true);
                    onCompleteCalled.notifyAll();
                }
            }
        });

        assertFalse(promise.isDone());
        ThreadPool.bg(promise);

        // wait for task to finish
        synchronized (onCompleteCalled) {
            if (!onCompleteCalled.get())
                onCompleteCalled.wait(1000);
        }

        // We gave it enough time, didn't we? If not called by now, it never will
        assertTrue("Task should have completed", onCompleteCalled.get());
        assertTrue("Task should be done", promise.isDone());
        assertTrue("Task should have succeeded", promise.succeeded());

        final String resultStr = result.get();
        assertTrue("results don't match", result == promise.get());
        assertEquals("Task should have returned " + expected, expected, resultStr);
    }

    @Test
    public void cancelMidExecution() throws InterruptedException, ExecutionException {
        final AtomicBoolean lock = new AtomicBoolean();
        final AtomicBoolean onCompleteCalled = new AtomicBoolean(false);
        final AtomicBoolean onCancelledCalled = new AtomicBoolean(false);

        final FuturePromise<Boolean> promise = new FuturePromise<>(new EchoTask<>(true, 1000));
        promise.onComplete(new Promise.OnCompleteListener<Boolean>() {
            @Override
            public void onComplete(Boolean aBoolean) {
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
        ThreadPool.bg(promise);
        assertFalse(promise.isDone());

        Thread.sleep(100);
        promise.cancel(true);

        // wait for task to finish
        synchronized (lock) {
            if (!lock.get())
                lock.wait(2000);
        }

        // We gave it enough time, didn't we? If not called by now, it never will
        assertFalse("Task should NOT have completed", onCompleteCalled.get());
        assertTrue("Task should have cancelled", onCancelledCalled.get());
        assertTrue("Task should be done", promise.isDone());
        assertFalse("Task should NOT have succeeded", promise.succeeded());
        assertTrue("Task should have been cancelled", promise.isCancelled());
    }

    @Test(expected = RuntimeException.class)
    public void failedExecution() throws InterruptedException {
        final AtomicBoolean lock = new AtomicBoolean();
        final AtomicBoolean onCompleteCalled = new AtomicBoolean(false);
        final AtomicBoolean onFailedCalled = new AtomicBoolean(false);
        final AtomicReference<Throwable> error = new AtomicReference<>(null);

        final String failMsg = "Failz";

        final FuturePromise<Boolean> promise = new FuturePromise<>(new EchoTask<>(true, 200, new RuntimeException(failMsg)));
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
        ThreadPool.bg(promise);

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
}
