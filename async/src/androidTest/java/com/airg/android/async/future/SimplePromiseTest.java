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

package com.airg.android.async.future;

import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by mahramf.
 */
public class SimplePromiseTest extends BaseExecutorTest {
    @Test
    public void normalExecution() throws InterruptedException, ExecutionException {
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
        executor.execute(new EchoTaskRunner<>(new EchoTask<>(secret, 200), promise));

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
    public void cancelMidExecution() throws InterruptedException, ExecutionException {
        final AtomicBoolean lock = new AtomicBoolean();
        final AtomicBoolean onCompleteCalled = new AtomicBoolean(false);
        final AtomicBoolean onCancelledCalled = new AtomicBoolean(false);

        final SimplePromise<Void> promise = new SimplePromise<>();
        final long taskRuntime = 1000;
        final EchoTaskRunner<Void> task = new EchoTaskRunner<>(new EchoTask<Void>(null, taskRuntime), promise);

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
        executor.execute(task);
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

    @Test(expected = RuntimeException.class)
    public void failedExecution() throws InterruptedException {
        final AtomicBoolean lock = new AtomicBoolean();
        final AtomicBoolean onCompleteCalled = new AtomicBoolean(false);
        final AtomicBoolean onFailedCalled = new AtomicBoolean(false);
        final AtomicReference<Throwable> error = new AtomicReference<>(null);

        final String failMsg = "Failz";
        final SimplePromise<Boolean> promise = new SimplePromise<>();

        final EchoTaskRunner<Boolean> task = new EchoTaskRunner<>(new EchoTask<>(true, 200, new RuntimeException(failMsg)), promise);
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
        executor.execute(task);

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

    private static class EchoTaskRunner<VALUE> implements Runnable {

        private final EchoTask<VALUE> task;
        private final Promise<VALUE> promise;

        EchoTaskRunner(final EchoTask<VALUE> t, final Promise<VALUE> p) {
            task = t;
            promise = p;
        }

        void cancel () {
            promise.cancelled();
        }

        @Override
        public void run() {
            try {
                promise.success(task.call());
            } catch (Exception e) {
                promise.failed(e);
            }
        }
    }
}