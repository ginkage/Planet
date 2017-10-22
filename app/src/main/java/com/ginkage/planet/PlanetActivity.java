package com.ginkage.planet;

import android.app.Activity;
import android.os.Bundle;

public class PlanetActivity extends Activity
{
	private PlanetSurfaceView mGLView = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mGLView = new PlanetSurfaceView(this);
		setContentView(mGLView);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mGLView != null) {
			mGLView.onPause();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mGLView != null) {
			mGLView.onResume();
		}
	}
}

