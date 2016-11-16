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

package com.airg.android.async.future;

import lombok.Synchronized;

/**
 * An implementation of {@link Promise} that can be retrofitted into any asynchronous flow:
 * <ol>
 *     <li>Instantiate a {@link SimplePromise}</li>
 *     <li>Interact with the <code>SimplePromise</code> instance in your runnable running in the background</li>
 *     <li>Return the instantiated <code>SimplePromise</code> in the calling thread</li>
 * </ol>
 * <code>
 *     public Promise&lt;MyResult&gt; getResultAsync () {
 *         final Promise&lt;MyResult&gt; promise = new SimplePromise&lt;MyResult&gt; ()
 *
 *         executor.execute (new Runnable () {
 *             @Override public void run () {
 *                 try {
 *                     promise.done (getResultFromNetworkOrDiskOrWhatever ());
 *                 } catch (Exception e) {
 *                     promise.failed (e);
 *                 }
 *             }
 *         });
 *
 *         return promise;
 *     }
 * </code>
 * @author Mahram Z. Foadi
 */
@SuppressWarnings({"UnusedDeclaration", "WeakerAccess"})
public final class SimplePromise<RESULT> implements Promise<RESULT> {
    private OnCompleteListener<RESULT> onCompleteListener;
    private OnFailListener onFailListener;
    private OnCancelListener onCancelListener;

    private Throwable error = null;
    private RESULT result = null;

    private boolean done = false;
    private boolean failed = false;
    private boolean cancelled = false;

    /**
     * Report task result and mark task as done
     * @param r obtained result
     */
    @Synchronized @Override
    public void done(final RESULT r) {
        assertNotComplete ();

        if (cancelled) return;

        result = r;
        done = true;
        notifyDoneMaybe();
    }

    /**
     * Mark task as failed and provide a cause
     * @param t cause of the failure
     */
    @Synchronized @Override
    public void failed(Throwable t) {
        assertNotComplete();

        if (cancelled) // if the task is already cancelled, don't report a failure
            return;

        error = t;
        failed = true;
        notifyFailedMaybe();
    }

    /**
     * Mark task as cancelled
     */
    @Synchronized @Override
    public void cancelled() {
        if (done || failed) return;

        cancelled = true;
        notifyCancelledMaybe();
    }

    /**
     * Set completion callback
     * @param listener listener to notify on completion
     */
    @Synchronized @Override
    public void onComplete(OnCompleteListener<RESULT> listener) {
        onCompleteListener = listener;
        notifyDoneMaybe();
    }

    /**
     * Set failure callback
     * @param listener listener to notify on failure.
     */
    @Synchronized @Override
    public void onFail(OnFailListener listener) {
        onFailListener = listener;
        notifyFailedMaybe();
    }

    /**
     * Set cancellation callback
     * @param listener listener to notify on cancellation
     */
    @Synchronized @Override
    public void onCancel(OnCancelListener listener) {
        onCancelListener = listener;
        notifyCancelledMaybe();
    }

    /**
     * Are we there yet?
     * @return <code>true</code> if task is complete and <code>false</code> otherwise
     */
    @Synchronized @Override
    public boolean isDone() {
        return done;
    }

    /**
     * Did the task fail?
     * @return <code>true</code> if failed and <code>false</code> otherwise
     */
    @Synchronized @Override
    public boolean isFailed() {
        return failed;
    }

    /**
     * Was the task cancelled?
     * @return <code>true</code> if cancelled and <code>false</code> otherwise
     */
    @Synchronized @Override
    public boolean isCancelled() {
        return cancelled;
    }

    // ---------- Private helper bits ----------

    private void assertNotComplete() {
        if (done || failed)
            throw new IllegalStateException("Already marked as " + (done ? "done" : "failed"));
    }

    @Synchronized
    private void notifyDoneMaybe() {
        if (!done || null == onCompleteListener)
            return;

        onCompleteListener.onComplete(result);
    }

    @Synchronized
    private void notifyFailedMaybe() {
        if (!failed || null == onFailListener)
            return;

        onFailListener.onFailed(error);
    }

    @Synchronized
    private void notifyCancelledMaybe() {
        if (!cancelled || null == onCancelListener)
            return;

        onCancelListener.onCancelled();
    }
}
