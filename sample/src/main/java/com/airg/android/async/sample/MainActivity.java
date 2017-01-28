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

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.airg.android.async.future.Promise;
import com.airg.android.logging.Logger;
import com.airg.android.logging.TaggedLogger;
import com.airg.android.util.Toaster;

import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {
    private final TaggedLogger LOG = Logger.tag("MAIN");

    @BindView(R.id.futurepromise)
    ImageView futurePromiseImageView;

    @BindView(R.id.futurepromise_progress)
    ProgressBar futurePromiseProgress;

    @BindView(R.id.simplepromise)
    ImageView simplePromiseImageView;

    @BindView(R.id.simplepromise_progress)
    ProgressBar simplePromiseProgress;

    private String[] imageUris;

    private final Random rnd = new Random(System.currentTimeMillis());
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        imageUris = getResources().getStringArray(R.array.imageUris);
        reloadFuturePromiseImage();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_reload:
                reloadFuturePromiseImage();
                reloadSimplePromiseImage();
                break;
            case R.id.action_reload_future:
                reloadFuturePromiseImage();
                break;
            case R.id.action_reload_simple:
                reloadSimplePromiseImage();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    private void reloadSimplePromiseImage() {
        simplePromiseProgress.setVisibility(View.VISIBLE);

        ImageLoader.loadSimplePromise(this, randomImageUri()).onComplete(new Promise.OnCompleteListener<Bitmap>() {
            @Override
            public void onComplete(final Bitmap bitmap) {
                simplePromiseImageView.setImageBitmap(bitmap);
                simplePromiseProgress.setVisibility(View.GONE);
            }
        }).onFail(new Promise.OnFailListener() {
            @Override
            public void onFailed(Throwable error) {
                LOG.e(error);
                Toaster.light(MainActivity.this, R.string.image_load_failed);
                simplePromiseProgress.setVisibility(View.GONE);
            }
        });
    }

    private void reloadFuturePromiseImage() {
        futurePromiseProgress.setVisibility(View.VISIBLE);

        ImageLoader.loadFuturePromise(this, randomImageUri())
                .onComplete(new Promise.OnCompleteListener<Bitmap>() {
                    @Override
                    public void onComplete(final Bitmap bitmap) {
                        futurePromiseImageView.setImageBitmap(bitmap);
                        futurePromiseProgress.setVisibility(View.GONE);
                    }
                }).onFail(new Promise.OnFailListener() {
            @Override
            public void onFailed(Throwable error) {
                LOG.e(error);
                Toaster.light(MainActivity.this, R.string.image_load_failed);
                futurePromiseProgress.setVisibility(View.GONE);
            }
        });
    }

    private String randomImageUri() {
        if (null == imageUris || imageUris.length == 0)
            throw new IllegalStateException("Y U SO EMPTY?");

        return imageUris[rnd.nextInt(imageUris.length)];
    }
}
