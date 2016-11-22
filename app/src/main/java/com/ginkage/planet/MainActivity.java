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

    private Bitmap mBitmapPlanet;
    private Bitmap mBitmapNormal;
    private Bitmap mBitmapLight;
    private ImageView mImageView;

    private float mRotation = 0.25f;
    private float mLight = 1.0f;
    private RenderScriptTask mCurrentTask = null;

    private RenderScript mRS;
    private Allocation mAllocPlanet;
    private Allocation mAllocNormal;
    private Allocation mAllocLight;
    private ScriptC_rotation mScript;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_layout);

        mBitmapPlanet = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888);
        mBitmapNormal = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888);
        mBitmapLight =  Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888);

        mImageView = (ImageView) findViewById(R.id.imageView);
        mImageView.setImageBitmap(mBitmapLight);

        SeekBar rotate = (SeekBar) findViewById(R.id.seekBarRotate);
        rotate.setMax(360);
        rotate.setProgress(90);

        SeekBar light = (SeekBar) findViewById(R.id.seekBarLight);
        light.setMax(360);
        light.setProgress(180);

        rotate.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float min = 0.0f, max = 1.0f;
                mRotation = (float) ((max - min) * (progress / 360.0) + min);
                updateImage(true);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        light.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float min = 0.0f, max = 2.0f;
                mLight = (float) ((max - min) * (progress / 360.0) + min);
                updateImage(false);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        createScript();
        updateImage(true);
    }

    private void createScript() {
        mRS = RenderScript.create(this);

        mAllocPlanet = Allocation.createFromBitmap(mRS, mBitmapPlanet);
        mAllocNormal = Allocation.createFromBitmap(mRS, mBitmapNormal);
        mAllocLight = Allocation.createFromBitmap(mRS, mBitmapLight);

        mScript = new ScriptC_rotation(mRS);
        mScript.set_gLinear(Sampler.WRAP_LINEAR(mRS));
        mScript.set_gPlanetMap(Allocation.createFromBitmap(mRS, loadBitmap(R.drawable.moon)));
        mScript.set_gNormalMap(Allocation.createFromBitmap(mRS, loadBitmap(R.drawable.moon_normal)));
        mScript.set_gPlanet(mAllocPlanet);
        mScript.set_gNormal(mAllocNormal);
    }

    private class RenderScriptTask extends AsyncTask<Boolean, Void, Bitmap> {
        Boolean issued = false;

        protected Bitmap doInBackground(Boolean... values) {
            if (isCancelled()) {
                return null;
            }

            issued = true;

            if (values[0]) {
                mScript.set_rotateY(1 - mRotation);
                mScript.forEach_rotation(mAllocPlanet, mAllocPlanet);
                mAllocPlanet.copyTo(mBitmapPlanet);
                mAllocNormal.copyTo(mBitmapNormal);
            }

            mScript.set_lightY((float)(Math.PI * (1 - mLight)));
            mScript.forEach_lighting(mAllocPlanet, mAllocLight);
            mAllocLight.copyTo(mBitmapLight);

            return mBitmapLight;
        }

        void updateView(Bitmap result) {
            if (result != null) {
                mImageView.setImageBitmap(result);
                mImageView.invalidate();
            }
        }

        protected void onPostExecute(Bitmap result) {
            updateView(result);
        }

        protected void onCancelled(Bitmap result) {
            if (issued) {
                updateView(result);
            }
        }
    }

    private void updateImage(boolean rotate) {
        if (mCurrentTask != null) {
            mCurrentTask.cancel(false);
        }
        mCurrentTask = new RenderScriptTask();
        mCurrentTask.execute(rotate);
    }

    private Bitmap loadBitmap(int resource) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inScaled = false;
        return BitmapFactory.decodeResource(getResources(), resource, options);
    }

}
