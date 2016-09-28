package com.ginkage.planet;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;

public class PlanetActivity extends Activity
{
	private PlanetSurfaceView mGLView = null;
	private final Handler mHandler = new Handler();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mGLView = new PlanetSurfaceView(this);
		setContentView(mGLView);

		mHandler.removeCallbacks(mUpdateTimeTask);
		mHandler.postDelayed(mUpdateTimeTask, 1000);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mGLView != null)
			mGLView.onPause();
		mHandler.removeCallbacks(mUpdateTimeTask);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mGLView != null)
			mGLView.onResume();
		mHandler.removeCallbacks(mUpdateTimeTask);
		mHandler.postDelayed(mUpdateTimeTask, 1000);
	}

	private final Runnable mUpdateTimeTask = new Runnable() {
		public void run() {
			if (mGLView != null) {
				if (mGLView.mRenderer != null) {
					float fps = mGLView.mRenderer.fps;
					setTitle(String.format("%.1f FPS", fps));
				}
			}

			mHandler.postDelayed(this, 1000);
		}
	};
}

