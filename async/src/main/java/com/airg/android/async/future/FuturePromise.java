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

    private Throwable error = null;

    /**
     * Execution state of this task
     */
    public enum ExecutionState {
        /**
         * Task hasn't started running yet
         */
        Pending,
        /**
         * Task is running
         */
        Running,
        /**
         * Task is completed
         */
        Completed,
        /**
         * Task has failed
         */
        Failed,
        /**
         * Task has been cancelled
         */
        Cancelled
    }

    private ExecutionState executionState = ExecutionState.Pending;

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

    /**
     * Get the execution state of this task
     * @return current {@link ExecutionState}
     */
    public ExecutionState getExecutionState() {
        return executionState;
    }

    // ---------- Promise bits ----------

    /**
     * See {@link Promise#done(Object)}
     */
    @Synchronized
    @Override
    public void done(final RESULT result) {
        executionState = ExecutionState.Completed;
        notifyDoneMaybe(result);
    }

    /**
     * See {@link Promise#failed(Throwable)}
     */
    @Synchronized
    @Override
    public void failed(Throwable t) {
        executionState = ExecutionState.Failed;
        error = t;
        notifyFailedMaybe();
    }

    /**
     * See {@link Promise#cancelled()}
     */
    @Synchronized
    @Override
    public void cancelled() {
        executionState = ExecutionState.Cancelled;
        notifyCancelledMaybe();
    }

    /**
     * See {@link Promise#onComplete(OnCompleteListener)}
     */
    @Synchronized @Override
    public final void onComplete(final OnCompleteListener<RESULT> listener) {
        onCompleteListener = listener;

        try {
            notifyDoneMaybe(get());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * See {@link Promise#onFail(OnFailListener)}
     */
    @Synchronized @Override
    public final void onFail(final OnFailListener listener) {
        onFailListener = listener;
        notifyFailedMaybe();
    }

    /**
     * See {@link Promise#onCancel(OnCancelListener)}
     */
    @Synchronized @Override
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
        return executionState == ExecutionState.Failed;
    }

    // ---------- FutureTask bits ----------

    /**
     * See {@link FutureTask#set(Object)}
     */
    @Synchronized
    @Override
    protected void set(RESULT result) {
        super.set(result);
        done(result);
    }

    @Synchronized
    @Override
    protected void setException(Throwable t) {
        super.setException(t);
        failed(t);
    }

    @Synchronized
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        final boolean superCancel = super.cancel(mayInterruptIfRunning);
        cancelled();
        return superCancel;
    }

    @Synchronized
    @Override
    public void run() {
        executionState = ExecutionState.Running;
        super.run();
    }

    // ---------- Private helper bits ----------
    @Synchronized
    private void notifyDoneMaybe(final RESULT result) {
        if (executionState != ExecutionState.Completed || null == onCompleteListener)
            return;

        onCompleteListener.onComplete(result);
    }

    @Synchronized
    private void notifyCancelledMaybe() {
        if (executionState != ExecutionState.Cancelled || null == onCancelListener) return;

        onCancelListener.onCancelled();
    }

    @Synchronized
    private void notifyFailedMaybe() {
        if (executionState != ExecutionState.Failed || null == onFailListener)
            return;

        onFailListener.onFailed(error);
    }
}
