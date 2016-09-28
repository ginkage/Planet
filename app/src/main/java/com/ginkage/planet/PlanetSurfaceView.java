package com.ginkage.planet;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.VelocityTracker;

class PlanetSurfaceView extends GLSurfaceView
{
	public PlanetRenderer mRenderer = null;
	private ScaleGestureDetector scaleDetector = null;
	private VelocityTracker velocityTracker = null;
	private boolean isScale = false;
	private float mPreviousX = 0;
	private float mPreviousY = 0;

	public PlanetSurfaceView(Context context)
	{
		super(context);

		setEGLContextClientVersion(2);
		setEGLConfigChooser(8, 8, 8, 8, 16, 0);

		mRenderer = new PlanetRenderer(context);
		setRenderer(mRenderer);

		scaleDetector = new ScaleGestureDetector(context, new MyGestureListener());
		velocityTracker = VelocityTracker.obtain();
	}

	private class MyGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener
	{
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			mRenderer.scaleFactor *= detector.getScaleFactor();
			return true;
		}

		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			return true;
		}

		@Override
		public void onScaleEnd(ScaleGestureDetector detector) {
			isScale = true;
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent e)
	{
		velocityTracker.addMovement(e);

		float x = e.getX(0);
		float y = e.getY(0);

		switch (e.getAction()) {
		case MotionEvent.ACTION_DOWN:
			isScale = false;
			mRenderer.rotateSpeed = 0;
			mRenderer.tiltSpeed = 0;
			break;

		case MotionEvent.ACTION_UP:
			velocityTracker.computeCurrentVelocity(1000);
			mRenderer.rotateSpeed = -velocityTracker.getXVelocity() / (mRenderer.screenWidth * mRenderer.ratioX * mRenderer.scaleFactor * Math.PI);
			mRenderer.tiltSpeed = velocityTracker.getYVelocity() * 0.5f / (mRenderer.screenHeight * mRenderer.ratioY * mRenderer.scaleFactor);
			break;

		case MotionEvent.ACTION_MOVE:
			if (!isScale) {
				float dx = x - mPreviousX;
				float dy = y - mPreviousY;
				mRenderer.rotateAngle -= dx / (mRenderer.screenWidth * mRenderer.ratioX * mRenderer.scaleFactor * Math.PI);
				mRenderer.tiltAngle += dy * 0.5f / (mRenderer.screenHeight * mRenderer.ratioY * mRenderer.scaleFactor);
			}
			break;
		}

		mPreviousX = x;
		mPreviousY = y;

		return scaleDetector.onTouchEvent(e);
	}
}
