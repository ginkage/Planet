package com.ginkage.planet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLUtils;
import android.os.SystemClock;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

class PlanetRenderer implements Renderer {
	private static final String quadVS =
		"precision mediump float;\n" +
		"attribute vec4 vPosition;\n" +
		"uniform vec4 uRatio;\n" +
		"varying vec4 Position;\n" +

		"void main() {\n" +
		"	gl_Position = vPosition * uRatio;\n" +
		"	Position = vPosition;\n" +
		"}\n";

	private static final String quadFS =
		"#define PI 3.141592653589793\n" +
		"precision mediump float;\n" +
		"uniform sampler2D uTexture0;\n" +
		"uniform sampler2D uTexture1;\n" +
		"uniform vec3 uRotate;\n" +
		"varying vec4 Position;\n" +

		"void main() {\n" +
		"	float sx = Position.x * 1.1;\n" +
		"	float sy = Position.y * 1.1;\n" +
		"	float z2 = 1.0 - sx * sx - sy * sy;\n" +

		"	if (z2 > 0.0) {\n" +
		"		float sz = sqrt(z2);\n" +
		"		float y = (sz * uRotate.y + sy * uRotate.z);\n" +
		"		float z = (-sy * uRotate.y + sz * uRotate.z);\n" +
		"		vec2 vCoord = vec2(0.0, 0.0);\n" +

		"		if (abs(z) > abs(y)) {\n" +
		"			vCoord.x = atan(sqrt(1.0 - y*y - sx*sx), -sx) / (2.0 * PI);\n" +
		"			vCoord.y = acos(y) / PI;\n" +
		"			if (z < 0.0) { vCoord.x = 1.0 - vCoord.x; }\n" +
		"		}\n" +
		"		else {\n" +
		"			vCoord.x = atan(z, -sx) / (2.0 * PI);\n" +
		"			vCoord.y = acos(sqrt(1.0 - z*z - sx*sx)) / PI;\n" +
		"			if (z < 0.0) { vCoord.x = 1.0 + vCoord.x; }\n" +
		"			if (y < 0.0) { vCoord.y = 1.0 - vCoord.y; }\n" +
		"		}\n" +

		"		vCoord.x += uRotate.x;\n" +

		"		vec3 vCol = texture2D(uTexture0, vCoord).rgb;\n" +
		"		vec3 vNorm = normalize(texture2D(uTexture1, vCoord).rgb - 0.5);\n" +

		"		float sin_theta = -sy;\n" +
		"		float cos_theta = sqrt(1.0 - sy * sy);\n" +
		"		float sin_phi = sx / cos_theta;\n" +
		"		float cos_phi = sz / cos_theta;\n" +
		"		float light = (vNorm.z * cos_theta - vNorm.y * sin_theta) * cos_phi - vNorm.x * sin_phi;\n" +

		"		gl_FragColor = vec4(vCol * light, 1.0);\n" +
		"	} else {\n" +
		"		gl_FragColor = vec4(0.25, 0.5, 1.0, (z2 + 0.21) * 1.5);\n" +
		"	}\n" +
		"}\n";

	private int quadProgram;
	private int qvPosition;
	private int quRatio;
	private int quTexture0;
	private int quTexture1;
	private int quRotate;

	float ratioX, ratioY;

	private int quadVB;

	private int planetTex;
	private int normalTex;

	private final int[] genbuf = new int[1];

	float fps = 0;
	private long start_frame;
	private long frames_drawn;

	private long prevTime = -1;
	float rotateAngle = 0;
	float tiltAngle = 0;
	int screenWidth = 0;
	int screenHeight = 0;

	float scaleFactor = 1;
	double rotateSpeed = -0.125f;
	double tiltSpeed = 0;

	private final Context mContext;

	PlanetRenderer(Context context)
	{
		super();
		mContext = context;
	}

	@Override
	public void onDrawFrame(GL10 arg0)
	{
		long curTime = SystemClock.uptimeMillis();

		if (curTime > start_frame + 1000) {
			fps = frames_drawn * 1000.0f / (curTime - start_frame);
			start_frame = curTime;
			frames_drawn = 0;
		}

		if (prevTime < 0) prevTime = curTime;
		double delta = (curTime - prevTime) / 1000.0f;
		prevTime = curTime;

		rotateAngle += delta * rotateSpeed;
		rotateAngle -= Math.floor(rotateAngle);

		tiltAngle += delta * tiltSpeed;
		while (tiltAngle > 2) tiltAngle -= 2;
		while (tiltAngle < 0) tiltAngle += 2;

		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, planetTex);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, normalTex);

		GLES20.glUseProgram(quadProgram);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, quadVB);
		GLES20.glEnableVertexAttribArray(qvPosition);
		GLES20.glVertexAttribPointer(qvPosition, 2, GLES20.GL_FLOAT, false, 8, 0);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

		GLES20.glUniform1i(quTexture0, 0);
		GLES20.glUniform1i(quTexture1, 1);

		double ta = tiltAngle * Math.PI;
		GLES20.glUniform3f(quRotate, rotateAngle, (float) Math.sin(ta), (float) Math.cos(ta));

		float minScale = 0.5f, maxScale = 2.0f / (ratioX < ratioY ? ratioX : ratioY);
		if (scaleFactor < minScale) scaleFactor = minScale;
		if (scaleFactor > maxScale) scaleFactor = maxScale;

		GLES20.glUniform4f(quRatio, ratioX * scaleFactor, ratioY * scaleFactor, 1, 1);

		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

		GLES20.glDisableVertexAttribArray(qvPosition);
		GLES20.glDisable(GLES20.GL_BLEND);

		frames_drawn++;
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height)
	{
		GLES20.glViewport(0, 0, width, height);

		screenWidth = width;
		screenHeight = height;

		if (width < height) {
			ratioX = 1;
			ratioY = width / (float)height;
		}
		else {
			ratioX = height / (float)width;
			ratioY = 1;
		}

		initPlanet();

		start_frame = SystemClock.uptimeMillis();
		frames_drawn = 0;
		fps = 0;
	}

	private int loadShader(int type, String shaderCode)
	{
		int shader = GLES20.glCreateShader(type);
		GLES20.glShaderSource(shader, shaderCode);
		GLES20.glCompileShader(shader);
		Log.e("Shader", GLES20.glGetShaderInfoLog(shader));
		return shader;
	}

	private int Compile(String vs, String fs)
	{
		int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vs);
		int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fs);

		int prog = GLES20.glCreateProgram();
		GLES20.glAttachShader(prog, vertexShader);
		GLES20.glAttachShader(prog, fragmentShader);
		GLES20.glLinkProgram(prog);

		return prog;
	}

	private int loadTexture(final Context context, final int resourceId)
	{
		GLES20.glGenTextures(1, genbuf, 0);
		int tex = genbuf[0];

		if (tex != 0) {
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inScaled = false;
			final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);
			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
			bitmap.recycle();
		}

		return tex;
	}

	private void initPlanet()
	{
		planetTex = loadTexture(mContext, R.drawable.planet);
		normalTex = loadTexture(mContext, R.drawable.normalmap);
	}

	private int createBuffer(float[] buffer)
	{
		FloatBuffer floatBuf = ByteBuffer.allocateDirect(buffer.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		floatBuf.put(buffer);
		floatBuf.position(0);

		GLES20.glGenBuffers(1, genbuf, 0);
		int glBuf = genbuf[0];
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, glBuf);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, buffer.length * 4, floatBuf, GLES20.GL_STATIC_DRAW);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

		return glBuf;
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config)
	{
		GLES20.glClearColor(0, 0, 0, 1);

		quadProgram = Compile(quadVS, quadFS);
		qvPosition = GLES20.glGetAttribLocation(quadProgram, "vPosition");
		quRatio = GLES20.glGetUniformLocation(quadProgram, "uRatio");
		quTexture0 = GLES20.glGetUniformLocation(quadProgram, "uTexture0");
		quTexture1 = GLES20.glGetUniformLocation(quadProgram, "uTexture1");
		quRotate = GLES20.glGetUniformLocation(quadProgram, "uRotate");

		final float quad[] = {
				-1,  1,
				-1, -1,
				1,  1,
				1, -1,
		};

		quadVB = createBuffer(quad);
	}
}
