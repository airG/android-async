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

package com.airg.android.async.promise;

import android.support.annotation.Nullable;

import com.airg.android.logging.Logger;
import com.airg.android.logging.TaggedLogger;

import java.util.concurrent.Executor;

import lombok.Synchronized;

/**
 An implementation of {@link Promise} that can be retrofitted into any asynchronous flow:
 <ol>
 <li>Instantiate a {@link SimplePromise}</li>
 <li>Interact with the <code>SimplePromise</code> instance in your runnable running in the background</li>
 <li>Return the instantiated <code>SimplePromise</code> in the calling thread</li>
 </ol>
 <pre>
 {@code
 public Promise<MyResult> getResultAsync () {
 final Promise<MyResult> promise = new SimplePromise<MyResult> ()

 executor.execute (new Runnable () {
 public void run () {
 try {
 promise.success (getResultFromNetworkOrDiskOrWhatever ());
 } catch (Exception e) {
 promise.failed (e);
 }
 }
 });

 return promise;
 }
 }
 </pre>

 @author Mahram Z. Foadi */
@SuppressWarnings ( {"UnusedDeclaration", "WeakerAccess"})
public class SimplePromise<RESULT>
  implements Promise<RESULT> {
    private static final TaggedLogger LOG = Logger.tag ("ASYNC:SP");

    private OnCompleteListener<RESULT> onCompleteListener;
    private OnFailListener             onFailListener;
    private OnCancelListener           onCancelListener;

    private volatile Throwable error  = null;
    private volatile RESULT    result = null;

    private volatile boolean done      = false;
    private volatile boolean cancelled = false;

    private final Executor callbackExecutor;

    public SimplePromise () {
        this (null);
    }

    public SimplePromise (@Nullable final Executor executor) {
        callbackExecutor = executor;
    }

    /**
     Report task result and mark task as done

     @param r
     obtained result
     */
    @Synchronized
    public void success (final RESULT r) {
        assertNotComplete ();

        if (cancelled) {
            LOG.d ("Promise has been cancelled. Ignoring result.");
            return;
        }

        LOG.d ("Promise kept: %s", r);
        done = true;
        result = r;
        notifyDoneMaybe ();
    }

    /**
     Mark task as failed and provide a cause

     @param t
     cause of the failure
     */
    @Synchronized
    public void failed (Throwable t) {
        assertNotComplete ();

        if (cancelled) { // if the task is already cancelled, don't report a failure
            LOG.d ("Promise has been cancelled. Ignoring failure.");
            return;
        }

        LOG.d (t, "Promise broken");
        error = t;
        done = true;
        notifyFailedMaybe ();
    }

    /**
     Mark task as cancelled
     */
    @Synchronized
    public void cancelled () {
        if (done || isFailed ()) {
            LOG.d ("Ignoring cancel request (already %s)", done ? "done" : "failed");
            return;
        }

        LOG.d ("Promise cancelled.");
        cancelled = true;
        done = true;
        notifyCancelledMaybe ();
    }

    /**
     Set completion callback

     @param listener
     listener to notify on completion

     @return this {@link Promise} to chain more callbacks
     */
    @Synchronized
    @Override
    public SimplePromise<RESULT> onComplete (OnCompleteListener<RESULT> listener) {
        onCompleteListener = listener;
        notifyDoneMaybe ();
        return this;
    }

    /**
     Set failure callback

     @param listener
     listener to notify on failure.

     @return this {@link Promise} to chain more callbacks
     */
    @Synchronized
    @Override
    public SimplePromise<RESULT> onFail (OnFailListener listener) {
        onFailListener = listener;
        notifyFailedMaybe ();
        return this;
    }

    /**
     Set cancellation callback

     @param listener
     listener to notify on cancellation

     @return this {@link Promise} to chain more callbacks
     */
    @Synchronized
    @Override
    public SimplePromise<RESULT> onCancel (OnCancelListener listener) {
        onCancelListener = listener;
        notifyCancelledMaybe ();
        return this;
    }

    /**
     Are we there yet?

     @return <code>true</code> if task is complete and <code>false</code> otherwise
     */
    @Synchronized
    @Override
    public boolean isDone () {
        return done;
    }

    /**
     Was the promise successfully completed?

     @return <code>true</code> if task was able to successfully obtain a result, <code>false</code> otherwise
     */
    @Synchronized
    @Override
    public boolean succeeded () {
        return done && null != result;
    }

    /**
     Did the task fail?

     @return <code>true</code> if failed and <code>false</code> otherwise
     */
    @Synchronized
    @Override
    public boolean isFailed () {
        return done && null != error;
    }

    /**
     Was the task cancelled?

     @return <code>true</code> if cancelled and <code>false</code> otherwise
     */
    @Synchronized
    @Override
    public boolean isCancelled () {
        return cancelled;
    }

    // ---------- Private helper bits ----------

    private void assertNotComplete () {
        if (isDone ())
            throw new IllegalStateException ("Already marked as " + (null == error ? "done" : "failed"));
    }

    @Synchronized
    private void notifyDoneMaybe () {
        if (!done || null == onCompleteListener || cancelled)
            return;

        LOG.d ("Notifying promise completion");
        runOnExecutor (new Runnable () {
            @Override
            public void run () {
                onCompleteListener.onComplete (result);
            }
        }, callbackExecutor);
    }

    @Synchronized
    private void notifyFailedMaybe () {
        if (!isFailed () || null == onFailListener || cancelled)
            return;
        LOG.d ("Notifying promise failure");
        runOnExecutor (new Runnable () {
            @Override
            public void run () {
                onFailListener.onFailed (error);
            }
        }, callbackExecutor);
    }

    @Synchronized
    private void notifyCancelledMaybe () {
        if (!cancelled || null == onCancelListener)
            return;
        LOG.d ("Notifying promise cancellation");
        runOnExecutor (new Runnable () {
            @Override
            public void run () {
                onCancelListener.onCancelled ();
            }
        }, callbackExecutor);
    }

    private static void runOnExecutor (final Runnable task, final Executor executor) {
        if (null == executor)
            task.run ();
        else
            executor.execute (task);
    }
}
