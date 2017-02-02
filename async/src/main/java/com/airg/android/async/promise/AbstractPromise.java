package com.airg.android.async.promise;

import com.airg.android.async.ThreadPool;
import com.airg.android.logging.Logger;
import com.airg.android.logging.TaggedLogger;

import java.util.concurrent.Executor;

/**
 * Another implementation for {@link Promise}. Override the bits that do work and this will do the rest.
 *
 * @author Mahram Z. Foadi
 */
@SuppressWarnings({"UnusedDeclaration", "WeakerAccess"})
public abstract class AbstractPromise<RESULT>
        implements Promise<RESULT>, Runnable {
    protected static final TaggedLogger LOG = Logger.tag("ASYNC:AP");

    private final SimplePromise<RESULT> promise;

    private AbstractPromise(SimplePromise<RESULT> p) {
        promise = p;
    }

    /**
     * Default constructor. Callbacks occur on the executing thread (not ideal for UI).
     */
    protected AbstractPromise() {
        this(new SimplePromise<RESULT>());
    }

    /**
     * Alternate constructor. Callbacks are executed on the provided {@link Executor}. For UI use, pass in {@link ThreadPool#foreground()} and save yourself the trouble of <code>View.post(Runnable)</code> or <code>Activity.runOnUIThread(Runnable)</code>
     *
     * @param executor executor to execute callbacks on.
     */
    protected AbstractPromise(final Executor executor) {
        this(new SimplePromise<RESULT>(executor));
    }

    /**
     * Final method. Obtains the result and takes care of the success, failure, and cancel reporting.
     */
    @Override
    public final void run() {
        try {
            final RESULT result = result();

            synchronized (promise) {
                if (promise.isCancelled())
                    return;

                promise.success(result);
            }
        } catch (Exception e) {
            LOG.e(e);
            synchronized (promise) {
                promise.failed(e);
            }
        }
    }

    /**
     * Final method. Marks the task as cancelled. To actually
     */
    public final void cancel() {
        synchronized (promise) {
            if (promise.isCancelled()) // already cancelled once
                return;

            try {
                abort();
            } catch (Exception e) {
                LOG.e(e, "abort() failed.");
            } finally {
                promise.cancelled();
            }
        }
    }

    /**
     * Obtain the result for this task.
     *
     * @return your result
     */
    protected abstract RESULT result() throws Exception;

    /**
     * Called when a cancel request is received and before the {@link OnCancelListener#onCancelled()} is called. If you
     * are able to abort the execution, this is where you do it. Does nothing by default.
     */
    protected void abort() {
        // override to actually cancel
    }

    /**
     * See {@link Promise#onComplete(OnCompleteListener)}
     */
    @Override
    public AbstractPromise<RESULT> onComplete(final OnCompleteListener<RESULT> listener) {
        promise.onComplete(listener);
        return this;
    }

    /**
     * See {@link Promise#onFail(OnFailListener)}
     */
    @Override
    public AbstractPromise<RESULT> onFail(final OnFailListener listener) {
        promise.onFail(listener);
        return this;
    }

    /**
     * See {@link Promise#onCancel(OnCancelListener)}
     */
    @Override
    public AbstractPromise<RESULT> onCancel(final OnCancelListener listener) {
        promise.onCancel(listener);
        return this;
    }

    /**
     * See {@link Promise#isDone()}
     */
    @Override
    public boolean isDone() {
        synchronized (promise) {
            return promise.isDone();
        }
    }

    /**
     * See {@link Promise#succeeded()}
     */
    @Override
    public boolean succeeded() {
        synchronized (promise) {
            return promise.succeeded();
        }
    }

    /**
     * See {@link Promise#isFailed()}
     */
    @Override
    public boolean isFailed() {
        synchronized (promise) {
            return promise.isFailed();
        }
    }

    /**
     * See {@link Promise#isCancelled()}
     */
    @Override
    public boolean isCancelled() {
        synchronized (promise) {
            return promise.isCancelled();
        }
    }
}
