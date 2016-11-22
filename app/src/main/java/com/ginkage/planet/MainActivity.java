/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ginkage.planet;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v8.renderscript.*;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class MainActivity extends Activity {
    /* Number of bitmaps that is used for renderScript thread and UI thread synchronization.
       Ideally, this can be reduced to 2, however in some devices, 2 buffers still showing tierings on UI.
       Investigating a root cause.
     */
    private final int NUM_BITMAPS = 3;
    private int mCurrentBitmap = 0;
    private Bitmap[] mBitmapsOut;
    private ImageView mImageView;

    private RenderScript mRS;
    private Allocation[] mOutAllocations;
    private ScriptC_rotation mScript;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_layout);

        /*
         * Initialize UI
         */
        mBitmapsOut = new Bitmap[NUM_BITMAPS];
        for (int i = 0; i < NUM_BITMAPS; ++i) {
            mBitmapsOut[i] = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888);
        }

        mImageView = (ImageView) findViewById(R.id.imageView);
        mImageView.setImageBitmap(mBitmapsOut[mCurrentBitmap]);
        mCurrentBitmap += (mCurrentBitmap + 1) % NUM_BITMAPS;

        SeekBar seekbar = (SeekBar) findViewById(R.id.seekBar1);
        seekbar.setMax(360);
        seekbar.setProgress(180);
        seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float max = 2.0f;
                float min = 0.0f;
                float f = (float) ((max - min) * (progress / 360.0) + min);
                updateImage(f);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        /*
         * Create renderScript
         */
        createScript();

        /*
         * Invoke renderScript kernel and update imageView
         */
        updateImage(1.0f);
    }

    /*
     * Initialize RenderScript
     * In the sample, it creates RenderScript kernel that performs saturation manipulation.
     */
    private void createScript() {
        //Initialize RS
        mRS = RenderScript.create(this);

        //Allocate buffers
        mOutAllocations = new Allocation[NUM_BITMAPS];
        for (int i = 0; i < NUM_BITMAPS; ++i) {
            mOutAllocations[i] = Allocation.createFromBitmap(mRS, mBitmapsOut[i]);
        }

        //Load script
        mScript = new ScriptC_rotation(mRS);
        mScript.set_gLinear(Sampler.WRAP_LINEAR(mRS));
        mScript.set_gPlanet(Allocation.createFromBitmap(mRS, loadBitmap(R.drawable.moon)));
        mScript.set_gNormalMap(Allocation.createFromBitmap(mRS, loadBitmap(R.drawable.moon_normal)));
    }

    /*
     * In the AsyncTask, it invokes RenderScript intrinsics to do a filtering.
     * After the filtering is done, an operation blocks at Application.copyTo() in AsyncTask thread.
     * Once all operation is finished at onPostExecute() in UI thread, it can invalidate and update ImageView UI.
     */
    private class RenderScriptTask extends AsyncTask<Float, Integer, Integer> {
        Boolean issued = false;

        protected Integer doInBackground(Float... values) {
            int index = -1;
            if (!isCancelled()) {
                issued = true;
                index = mCurrentBitmap;

                /*
                 * Set global variable in RS
                 */
                mScript.set_rotateX(1 - values[0]);

                /*
                 * Invoke saturation filter kernel
                 */
                mScript.forEach_rotation(mOutAllocations[index], mOutAllocations[index]);

                /*
                 * Copy to bitmap and invalidate image view
                 */
                mOutAllocations[index].copyTo(mBitmapsOut[index]);
                mCurrentBitmap = (mCurrentBitmap + 1) % NUM_BITMAPS;
            }
            return index;
        }

        void updateView(Integer result) {
            if (result != -1) {
                // Request UI update
                mImageView.setImageBitmap(mBitmapsOut[result]);
                mImageView.invalidate();
            }
        }

        protected void onPostExecute(Integer result) {
            updateView(result);
        }

        protected void onCancelled(Integer result) {
            if (issued) {
                updateView(result);
            }
        }
    }

    RenderScriptTask currentTask = null;

    /*
    Invoke AsynchTask and cancel previous task.
    When AsyncTasks are piled up (typically in slow device with heavy kernel),
    Only the latest (and already started) task invokes RenderScript operation.
     */
    private void updateImage(final float f) {
        if (currentTask != null)
            currentTask.cancel(false);
        currentTask = new RenderScriptTask();
        currentTask.execute(f);
    }

    /*
    Helper to load Bitmap from resource
     */
    private Bitmap loadBitmap(int resource) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inScaled = false;
        return BitmapFactory.decodeResource(getResources(), resource, options);
    }

}
