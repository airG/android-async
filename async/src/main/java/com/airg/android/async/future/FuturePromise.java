/*
 * ****************************************************************************
 *   Copyright 2016 airG Inc.                                                 *
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

import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import lombok.Synchronized;

/**
 * A {@link FutureTask} that implements the {@link Promise} interface to provide completion, failure, and cancellation
 * callbacks.
 *
 * @author Mahram Z. Foadi
 */
@SuppressWarnings({"UnusedDeclaration", "WeakerAccess"})
public final class FuturePromise<RESULT> extends FutureTask<RESULT> implements Promise<RESULT> {
    private OnCompleteListener<RESULT> onCompleteListener;
    private OnFailListener onFailListener;
    private OnCancelListener onCancelListener;
    private RESULT result;

    private Throwable error = null;

    /**
     * Wrap a {@link Callable}
     *
     * @param callable callable to get the result from
     */
    public FuturePromise(Callable<RESULT> callable) {
        super(callable);
    }

    /**
     * Wrap a {@link Runnable}
     *
     * @param runnable Runnable to get the result from
     * @param result   The result placeholder
     */
    public FuturePromise(Runnable runnable, RESULT result) {
        super(runnable, result);
    }

    /**
     * Set all callbacks at once
     *
     * @param callback An instance of a class that implements {@link com.airg.android.async.future.Promise.OnCompleteListener},
     *                 {@link com.airg.android.async.future.Promise.OnFailListener}, or
     *                 {@link com.airg.android.async.future.Promise.OnCancelListener}.
     * @throws IllegalArgumentException if the provided callback implements neither
     *                                  {@link com.airg.android.async.future.Promise.OnCompleteListener},
     *                                  {@link com.airg.android.async.future.Promise.OnFailListener}, or
     *                                  {@link com.airg.android.async.future.Promise.OnCancelListener}
     */
    @Synchronized
    public final void setCallback(final Object callback) {
        int count = 0;
        if (callback instanceof OnCompleteListener) {
            //noinspection unchecked
            onComplete((OnCompleteListener<RESULT>) callback);
            count++;
        }

        if (callback instanceof OnFailListener) {
            onFail((OnFailListener) callback);
            count++;
        }

        if (callback instanceof OnCancelListener) {
            onCancel((OnCancelListener) callback);
            count++;
        }

        if (0 == count) throw new IllegalArgumentException(String.format(Locale.ENGLISH,
                "'%s' does not implement '%s', '%s', or '%s'",
                callback.getClass().getSimpleName(),
                OnCompleteListener.class.getCanonicalName(),
                OnFailListener.class.getCanonicalName(),
                OnCancelListener.class.getCanonicalName()
        ));
    }

    public boolean succeeded () {
        return isDone() && !(isFailed() || isCancelled());
    }

    public FuturePromise<RESULT> completed (final OnCompleteListener<RESULT> listener) {
        onComplete(listener);
        return this;
    }

    public FuturePromise<RESULT> failed (final OnFailListener listener) {
        onFail(listener);
        return this;
    }

    public FuturePromise<RESULT> cancelled (final OnCancelListener listener) {
        onCancel(listener);
        return this;
    }

    // ---------- Promise bits ----------

    /**
     * See {@link Promise#success(Object)}
     */
    @Synchronized
    @Override
    public void success(final RESULT r) {
        result = r;
    }

    /**
     * See {@link Promise#failed(Throwable)}
     */
    @Synchronized
    @Override
    public void failed(Throwable t) {
        error = t;
        notifyFailedMaybe();
    }

    /**
     * See {@link Promise#cancelled()}
     */
    @Synchronized
    @Override
    public void cancelled() {
        notifyCancelledMaybe();
    }

    /**
     * See {@link Promise#onComplete(OnCompleteListener)}
     */
    @Synchronized
    @Override
    public final void onComplete(final OnCompleteListener<RESULT> listener) {
        onCompleteListener = listener;

        try {
            notifyDoneMaybe();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * See {@link Promise#onFail(OnFailListener)}
     */
    @Synchronized
    @Override
    public final void onFail(final OnFailListener listener) {
        onFailListener = listener;
        notifyFailedMaybe();
    }

    /**
     * See {@link Promise#onCancel(OnCancelListener)}
     */
    @Synchronized
    @Override
    public final void onCancel(final OnCancelListener listener) {
        onCancelListener = listener;
        notifyCancelledMaybe();
    }

    /**
     * See {@link Promise#isFailed()}
     */
    @Synchronized
    @Override
    public boolean isFailed() {
        return null != error;
    }

    // ---------- FutureTask bits ----------

    /**
     * See {@link FutureTask#set(Object)}
     */
    @Synchronized
    @Override
    protected void set(RESULT r) {
        success(r);
        super.set(result);
    }

    @Synchronized
    @Override
    protected void setException(Throwable t) {
        failed(t);
        super.setException(t);
    }

    @Override
    protected void done() {
        super.done();

        if (isCancelled())
            cancelled();
        else
            notifyDoneMaybe();
    }

    // ---------- Private helper bits ----------
    @Synchronized
    private void notifyDoneMaybe() {
        if (!isDone() || null == onCompleteListener)
            return;

        onCompleteListener.onComplete(result);
    }

    @Synchronized
    private void notifyCancelledMaybe() {
        if (!isCancelled() || null == onCancelListener) return;

        onCancelListener.onCancelled();
    }

    @Synchronized
    private void notifyFailedMaybe() {
        if (!isFailed() || null == onFailListener)
            return;

        onFailListener.onFailed(error);
    }
}
