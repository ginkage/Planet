package com.ginkage.planet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLUtils;
import android.os.SystemClock;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class PlanetRenderer implements Renderer {
	private final String quadVS =
	   	"precision mediump float;\n" +
		"attribute vec4 vPosition;\n" +
		"attribute vec4 vTexCoord0;\n" +
		"uniform vec4 uRatio;\n" +
		"varying vec4 TexCoord0;\n" +
		"void main() {\n" +
		"	gl_Position = vPosition * uRatio;\n" +
		"	TexCoord0 = vTexCoord0;\n" +
		"}\n";
/*
	private final String quadFS =
		"precision mediump float;\n" +
		"uniform sampler2D uTexture0;\n" +
		"uniform sampler2D uTexture1;\n" +
		"uniform float uOffset;\n" +
		"varying vec4 TexCoord0;\n" +
		"void main() {\n" +
		"	vec4 vTex = texture2D(uTexture0, TexCoord0.xy);\n" +
		"	vTex.x += uOffset;\n" +
		"	vec3 vCol = texture2D(uTexture1, vTex.xy).rgb;\n" +
		"  	gl_FragColor = vec4(vCol * vTex.w, (vTex.w > 0.0 ? 1.0 : 0.0));\n" +
		"}\n";

	private final String quadFS =
		"precision mediump float;\n" +
		"uniform sampler2D uTexture0;\n" +
		"uniform sampler2D uTexture1;\n" +
		"uniform float uOffset;\n" +
		"varying vec4 TexCoord0;\n" +
		"void main() {\n" +
		"	vec4 vTex = texture2D(uTexture0, TexCoord0.xy);\n" +

		"	vec3 vOff = vTex.xyz * 255.0 + vec3(0.0, 0.5, 0.0);\n" +
		"	float hiY = floor(vOff.y / 16.0);\n" +
		"	float loY = vOff.y - 16.0 * hiY;\n" +
		"	vec2 vCoord = vec2(\n" +
		"		(vOff.x * 16.0 + loY) / 4095.0 + uOffset,\n" +
		"		(vOff.z * 16.0 + hiY) / 4095.0);\n" +

		"	vec3 vCol = texture2D(uTexture1, vCoord).rgb;\n" +
		"  	gl_FragColor = vec4(vCol * vTex.w, (vTex.w > 0.0 ? 1.0 : 0.0));\n" +
		"}\n";
*/
	private final String quadFS =
		"precision mediump float;\n" +
		"uniform sampler2D uTexture0;\n" +
		"uniform sampler2D uTexture1;\n" +
		"uniform float uOffset;\n" +
		"uniform vec2 uTilt;\n" +
		"varying vec4 TexCoord0;\n" +
		"void main() {\n" +
		"	float sx = 2.0 * TexCoord0.x - 1.0;\n" +
		"	float sy = 2.0 * TexCoord0.y - 1.0;\n" +
		"	float z2 = 1.0 - sx * sx - sy * sy;\n" +

		"	if (z2 > 0.0) {;\n" +
		"		float sz = sqrt(z2);\n" +
		"		float y = (sy * uTilt.y - sz * uTilt.x + 1.0) * 0.5;\n" +
		"		float z = (sy * uTilt.x + sz * uTilt.y);\n" +

		"		vec4 vTex = texture2D(uTexture0, vec2(TexCoord0.x, y));\n" +
		"		vec3 vOff = vTex.xyz * 255.0 + vec3(0.0, 0.5, 0.0);\n" +
		"		float hiY = floor(vOff.y / 16.0);\n" +
		"		float loY = vOff.y - 16.0 * hiY;\n" +
		"		vec2 vCoord = vec2(\n" +
		"			(vOff.x * 16.0 + loY) / 4095.0,\n" +
		"			(vOff.z * 16.0 + hiY) / 4095.0);\n" +

		"		if (z < 0.0) { vCoord.x = 1.0 - vCoord.x; }\n" +
		"		vCoord.x += uOffset;\n" +

		"		vec3 vCol = texture2D(uTexture1, vCoord).rgb;\n" +
		"   	gl_FragColor = vec4(vCol * sz, 1.0);\n" +
		"	} else {\n" +
		"   	gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);\n" +
		"	}\n" +
		"}\n";

	private int quadProgram;
	private int qvPosition;
	private int qvTexCoord0;
	private int quRatio;
	private int quTexture0;
	private int quTexture1;
	private int quOffset;
	private int quTilt;

	float ratioX, ratioY;

	private int quadVB;

	private int planetTex;
	private int offsetTex;

	private final int[] genbuf = new int[1];

	public float fps = 0;
	private long start_frame;
	private long frames_drawn;

	private long prevTime = -1;
	public float rotateAngle = 0;
	public float tiltAngle = 0;
	public int screenWidth = 0;
	public int screenHeight = 0;

	public float scaleFactor = 1;
	public double rotateSpeed = -0.125f;
	public double tiltSpeed = 0;

	private final Context mContext;

	public PlanetRenderer(Context context)
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
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, offsetTex);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, planetTex);

		GLES20.glUseProgram(quadProgram);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, quadVB);
		GLES20.glEnableVertexAttribArray(qvPosition);
		GLES20.glVertexAttribPointer(qvPosition, 3, GLES20.GL_FLOAT, false, 20, 0);
		GLES20.glEnableVertexAttribArray(qvTexCoord0);
		GLES20.glVertexAttribPointer(qvTexCoord0, 2, GLES20.GL_FLOAT, false, 20, 12);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

		GLES20.glUniform1i(quTexture0, 0);
		GLES20.glUniform1i(quTexture1, 1);
		GLES20.glUniform1f(quOffset, rotateAngle);
		
		double ta = tiltAngle * Math.PI;
		GLES20.glUniform2f(quTilt, (float) Math.sin(ta), (float) Math.cos(ta));

		float minScale = 0.5f, maxScale = 2.0f / (ratioX < ratioY ? ratioX : ratioY);
		if (scaleFactor < minScale) scaleFactor = minScale;
		if (scaleFactor > maxScale) scaleFactor = maxScale;

		GLES20.glUniform4f(quRatio, ratioX * scaleFactor, ratioY * scaleFactor, 1, 1);

		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

		GLES20.glDisableVertexAttribArray(qvPosition);
		GLES20.glDisableVertexAttribArray(qvTexCoord0);

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

	int loadTexture(final Context context, final int resourceId)
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
		int texSize = 1024;
		double r = texSize * 0.5;
		int[] pixels = new int[texSize * texSize];

		for (int row = 0, idx = 0; row < texSize; row++) {
			double y = (r - row) / r;
			double sin_theta = Math.sqrt(1 - y*y);
			double theta = Math.acos(y);
			long v = Math.round(4095 * theta / Math.PI);
//			long v = Math.round(255 * theta / Math.PI);

			for (int col = 0; col < texSize; col++) {
				double x = (r - col) / r;
				long u = 0, a = 0;

				if (x >= -sin_theta && x <= sin_theta) {
					double z = Math.sqrt(1 - y*y - x*x);
					double phi = Math.atan2(z, x);
					u = Math.round(4095 * phi / (2 * Math.PI));
//					u = Math.round(255 * phi / (2 * Math.PI));
					a = Math.round(255 * z);
				}

				pixels[idx++] = (int) ((a << 24) + (v << 12) + ((u & 15) << 8) + (u >> 4));
//				pixels[idx++] = (int) ((a << 24) + (v << 8) + u);
			}
		}

		GLES20.glGenTextures(1, genbuf, 0);
		offsetTex = genbuf[0];
		if (offsetTex != 0) {
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, offsetTex);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_NONE);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_NONE);
			GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, texSize, texSize, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, IntBuffer.wrap(pixels));
		}

		planetTex = loadTexture(mContext, R.drawable.planet);
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
		GLES20.glClearColor(0.25f, 0.25f, 0.25f, 1);

		quadProgram = Compile(quadVS, quadFS);
		qvPosition = GLES20.glGetAttribLocation(quadProgram, "vPosition");
		qvTexCoord0 = GLES20.glGetAttribLocation(quadProgram, "vTexCoord0");
		quRatio = GLES20.glGetUniformLocation(quadProgram, "uRatio");
		quTexture0 = GLES20.glGetUniformLocation(quadProgram, "uTexture0");
		quTexture1 = GLES20.glGetUniformLocation(quadProgram, "uTexture1");
		quOffset = GLES20.glGetUniformLocation(quadProgram, "uOffset");
		quTilt = GLES20.glGetUniformLocation(quadProgram, "uTilt");

		final float quad[] = {
			-1,  1, 0, 0, 0,
			-1, -1, 0, 0, 1,
			 1,  1, 0, 1, 0,
			 1, -1, 0, 1, 1
		};

		quadVB = createBuffer(quad);
	}
}
