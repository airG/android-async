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

package com.airg.android.async.sample;

import android.content.Context;
import android.graphics.Bitmap;

import com.airg.android.async.ThreadPool;
import com.airg.android.async.future.FuturePromise;
import com.airg.android.async.future.Promise;
import com.airg.android.async.future.SimplePromise;
import com.airg.android.logging.Logger;
import com.airg.android.logging.TaggedLogger;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;

import java.util.concurrent.Callable;

/**
 * Created by mahramf.
 */

final class ImageLoader {
    private static TaggedLogger LOG = Logger.tag("LOADR");

    static Bitmap load(final Context context, final String imageUri) throws Exception {
        return new BitmapCallable(context, imageUri).call();
    }

    static Promise<Bitmap> loadSimplePromise(final Context context, final String imageUri) {
        final SimplePromise<Bitmap> promise = new SimplePromise<>(ThreadPool.foreground());

        ThreadPool.bg(new Runnable() {
            @Override
            public void run() {
                try {
                    promise.success(load(context, imageUri));
                    LOG.d("Simple: Loaded image");
                } catch (Exception e) {
                    promise.failed(e);
                    LOG.e(e, "Simple: Load failed");
                }
            }
        });

        return promise;
    }

    static Promise<Bitmap> loadFuturePromise(final Context context, final String uri) {
        final FuturePromise<Bitmap> promise = new FuturePromise<>(new BitmapCallable(context, uri), ThreadPool.foreground());

        ThreadPool.bg(promise);

        return promise;
    }

    private static class BitmapCallable implements Callable<Bitmap> {

        private final Context context;
        private final String imageUri;

        BitmapCallable(final Context c, final String imgUri) {
            context = c;
            imageUri = imgUri;
        }

        @Override
        public Bitmap call() throws Exception {
            return Glide.with(context).load(imageUri).asBitmap().into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL).get();
        }
    }
}
