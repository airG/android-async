/*
 * ****************************************************************************
 *   Copyright  2016 airG Inc.                                                 *
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

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 Created by mahra on 2016-09-22.
 */
@RunWith (AndroidJUnit4.class)
@SmallTest
public class AsyncHelperTests {
    @Test
    public void isMainThreadFromMainThread () throws Exception {
        final boolean ismain = runIsMainThreadOnExecutor (ThreadPool.foreground());
        assertTrue (ismain);
    }

    @Test
    public void isMainThreadFromBackgroundThread () throws Exception {
        final boolean ismain = runIsMainThreadOnExecutor (ThreadPool.background ());
        assertFalse (ismain);
    }

    @Test
    public void assertNotMainThreadOnMainThread () throws Exception {
        final Exception e = runAssertNotMainThreadOnExecutor (ThreadPool.foreground());
        assertNotNull (e);
        assertTrue (e instanceof IllegalStateException);
    }

    @Test
    public void assertNotMainThreadOnBackgroundThread () throws Exception {
        final Exception e = runAssertNotMainThreadOnExecutor (ThreadPool.background ());
        assertNull (e);
    }

    @Test
    public void assertMainThreadOnMainThread () throws Exception {
        final Exception e = runAssertMainThreadOnExecutor (ThreadPool.foreground());
        assertNull (e);
    }

    @Test
    public void assertMainThreadOnBackgroundThread () throws Exception {
        final Exception e = runAssertMainThreadOnExecutor (ThreadPool.background ());
        assertNotNull (e);
        assertTrue (e instanceof IllegalStateException);
    }

    private boolean runIsMainThreadOnExecutor (final Executor executor) throws InterruptedException {
        final AtomicBoolean result = new AtomicBoolean (true);

        synchronized (result) {
            executor.execute (new Runnable () {
                @Override public void run () {
                    synchronized (result) {
                        result.set (AsyncHelper.isMainThread ());
                        result.notifyAll ();
                    }
                }
            });

            result.wait ();
            return result.get ();
        }
    }

    private Exception runAssertMainThreadOnExecutor (final Executor executor) throws InterruptedException {
        final AtomicReference<Exception> exception = new AtomicReference<> ();

        synchronized (exception) {
            executor.execute (new Runnable () {
                @Override public void run () {
                    synchronized (exception) {
                        try {
                            AsyncHelper.assertMainThread ();
                            exception.set (null);
                        } catch (IllegalStateException e) {
                            exception.set(e);
                        }

                        exception.notifyAll ();
                    }
                }
            });

            exception.wait ();
            return exception.get ();
        }
    }

    private Exception runAssertNotMainThreadOnExecutor (final Executor executor) throws InterruptedException {
        final AtomicReference<Exception> exception = new AtomicReference<> ();

        synchronized (exception) {
            executor.execute (new Runnable () {
                @Override public void run () {
                    synchronized (exception) {
                        try {
                            AsyncHelper.assertNotMainThread ();
                            exception.set (null);
                        } catch (IllegalStateException e) {
                            exception.set(e);
                        }

                        exception.notifyAll ();
                    }
                }
            });

            exception.wait ();
            return exception.get ();
        }
    }
}