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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
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
    private final SimplePromise<RESULT> delegate;

    /**
     * Wrap a {@link Callable}
     *
     * @param callable callable to get the result from
     */
    public FuturePromise(Callable<RESULT> callable) {
        this(callable, null);
    }

    /**
     * Wrap a {@link Callable}
     *
     * @param callable callable to get the result from
     */
    public FuturePromise(Callable<RESULT> callable, final Executor executor) {
        super(callable);
        delegate = new SimplePromise<>(executor);
    }

    /**
     * Wrap a {@link Runnable}
     *
     * @param runnable     Runnable to get the result from
     * @param resultHolder The result placeholder
     */
    public FuturePromise(Runnable runnable, RESULT resultHolder) {
        this(runnable, resultHolder, null);
    }

    /**
     * Wrap a {@link Runnable}
     *
     * @param runnable     Runnable to get the result from
     * @param resultHolder The result placeholder
     */
    public FuturePromise(Runnable runnable, RESULT resultHolder, final Executor executor) {
        super(runnable, resultHolder);
        delegate = new SimplePromise<>(executor);
    }

    /**
     * Was the promise successfully completed?
     *
     * @return <code>true</code> if task was able to successfully obtain a result, <code>false</code> otherwise
     */
    @Override
    public boolean succeeded() {
        return isDone() && !(isFailed() || isCancelled());
    }

    // ---------- Promise bits ----------

    /**
     * DO NOT CALL. This isn't yours to call.
     */
    @Synchronized
    @Override
    public void success(final RESULT ignored) {
        throw new UnsupportedOperationException("Only this task can set the result");
    }

    /**
     * DO NOT CALL. This isn't yours to call.
     */
    @Synchronized
    @Override
    public void failed(final Throwable t) {
        throw new UnsupportedOperationException("Only this task can set the exception");
    }

    /**
     * DO NOT CALL. This isn't yours to call.
     */
    @Synchronized
    @Override
    public void cancelled() {
        throw new UnsupportedOperationException("Only this task can set the cancelled flag");
    }

    /**
     * See {@link Promise#onComplete(OnCompleteListener)}
     */
    @Synchronized
    @Override
    public final FuturePromise<RESULT> onComplete(final OnCompleteListener<RESULT> listener) {
        delegate.onComplete(listener);
        return this;
    }

    /**
     * See {@link Promise#onFail(OnFailListener)}
     */
    @Synchronized
    @Override
    public final FuturePromise<RESULT> onFail(final OnFailListener listener) {
        delegate.onFail(listener);
        return this;
    }

    /**
     * See {@link Promise#onCancel(OnCancelListener)}
     */
    @Synchronized
    @Override
    public final FuturePromise<RESULT> onCancel(final OnCancelListener listener) {
        delegate.onCancel(listener);
        return this;
    }

    /**
     * See {@link Promise#isFailed()}
     */
    @Synchronized
    @Override
    public boolean isFailed() {
        return delegate.isFailed();
    }

    // ---------- FutureTask bits ----------

    @Synchronized
    @Override
    protected void done() {
        super.done();

        if (isCancelled())
            delegate.cancelled();
        else
            try {
                delegate.success(get());
            } catch (ExecutionException ee) {
                delegate.failed(ee.getCause());
            } catch (Exception e) {
                throw new RuntimeException("Unable to get result", e);
            }
    }
}
