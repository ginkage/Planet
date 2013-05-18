package com.ginkage.planet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLUtils;
import android.os.SystemClock;
import android.util.Log;

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
			"uniform vec2 uTilt;\n" +
			"varying vec4 TexCoord0;\n" +
			"void main() {\n" +
			"	vec4 vTex = texture2D(uTexture0, TexCoord0.xy);\n" +
			"	vec3 vOff = vTex.xyz * 255.0;\n" +

			"	float hiY = floor(vOff.y / 16.0);\n" +
			"	float loY = vOff.y - 16.0 * hiY;\n" +

			"	vec2 vCoord = vec2(\n" +
			"		(vOff.z * 16.0 + hiY) * 0.5 / 4095.0 + uOffset,\n" +
			"		(256.0 * loY + vOff.x) / 4095.0);\n" +

			"  	gl_FragColor = vec4(texture2D(uTexture1, vCoord).rgb * vTex.w, (vTex.w > 0.0 ? 1.0 : 0.0));\n" +
			"}\n";
*/
	private final String quadFS =
		"precision mediump float;\n" +
		"uniform sampler2D uTexture0;\n" +
		"uniform sampler2D uTexture1;\n" +
		"uniform float uOffset;\n" +
		"uniform vec2 uTilt;\n" +
		"varying vec4 TexCoord0;\n" +
//		"const float PI = 3.14159265358;\n" +
		"void main() {\n" +
		"	float sx = TexCoord0.x;\n" +
		"	float sy = TexCoord0.y;\n" +
		"	float z2 = 1.0 - sx * sx - sy * sy;\n" +

		"	if (z2 > 0.0) {;\n" +
		"		float sz = sqrt(z2);\n" +
		"		float x = (sx - 1.0) * 0.5;\n" +
		"		float y = (1.0 + sy * uTilt.y - sz * uTilt.x) * 0.5;\n" +
		"		float z = (sy * uTilt.x + sz * uTilt.y);\n" +

		"		vec4 vTex = texture2D(uTexture0, vec2(x, y));\n" +
		"		vec3 vOff = vTex.xyz * 255.0;\n" +

		"		float hiY = floor(vOff.y / 16.0);\n" +
		"		float loY = vOff.y - 16.0 * hiY;\n" +

		"		vec2 vCoord = vec2(\n" +
		"			(vOff.z * 16.0 + hiY) * 0.5 / 4095.0,\n" +
		"			(256.0 * loY + vOff.x) / 4095.0);\n" +

//		"		vec2 vCoord;\n" +
//		"		float sina = 2.0 * y - 1.0;\n" +
//		"		vCoord.y = asin(sina) / PI + 0.5;\n" +
//		"		vCoord.x = (1.0 - acos(sx / sqrt(1.0 - sina * sina)) / PI) * 0.5;\n" +

		"		if (z < 0.0) { vCoord.x = 1.0 - vCoord.x; }\n" +
		"		vCoord.x += uOffset;\n" +

		"   	gl_FragColor = vec4(texture2D(uTexture1, vCoord).rgb * sz, 1.0);\n" +
		"	} else {\n" +
		"   	gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);\n" +
		"	}\n" +
		"}\n";

	private int mQProgram;
	private int maQPosition;
	private int maQTexCoord;
	private int muQRatio;
	private int muQTexture0;
	private int muQTexture1;
	private int muQOffset;
	private int muQTilt;

	float ratioX, ratioY;

	private int glQuadVB;

	private int planetTex;
	private int offsetTex;

	private final int texSize = 1024;

	int[] genbuf = new int[1];

	public float fps = 0;
	private long start_frame;
	private long frames_drawn;

	private long prevTime = -1;
	public float rotateAngle = 0;
	public float tiltAngle = 0;
	public int screenWidth = 0;
	public int screenHeight = 0;

	public float scaleFactor = 1;
	public double rotateSpeed = 0.125f;
	public double tiltSpeed = 0;

	Context mContext;

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
		rotateAngle += (curTime - prevTime) * rotateSpeed / 1000.0f;
		rotateAngle -= Math.floor(rotateAngle);
		tiltAngle += (curTime - prevTime) * tiltSpeed / 1000.0f;
		while (tiltAngle > 2) tiltAngle -= 2;
		while (tiltAngle < 0) tiltAngle += 2;
		prevTime = curTime;

		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		
		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, offsetTex);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, planetTex);

		GLES20.glUseProgram(mQProgram);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, glQuadVB);
		GLES20.glEnableVertexAttribArray(maQPosition);
		GLES20.glVertexAttribPointer(maQPosition, 3, GLES20.GL_FLOAT, false, 20, 0);
		GLES20.glEnableVertexAttribArray(maQTexCoord);
		GLES20.glVertexAttribPointer(maQTexCoord, 2, GLES20.GL_FLOAT, false, 20, 12);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

		GLES20.glUniform1i(muQTexture0, 0);
		GLES20.glUniform1i(muQTexture1, 1);
		GLES20.glUniform1f(muQOffset, rotateAngle);
		
		double ta = tiltAngle * Math.PI;
		GLES20.glUniform2f(muQTilt, (float) Math.sin(ta), (float) Math.cos(ta));

		float minScale = 0.5f, maxScale = 2.0f / (ratioX < ratioY ? ratioX : ratioY);
		if (scaleFactor < minScale) scaleFactor = minScale;
		if (scaleFactor > maxScale) scaleFactor = maxScale;

		GLES20.glUniform4f(muQRatio, ratioX * scaleFactor, ratioY * scaleFactor, 1, 1);

		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

		GLES20.glDisableVertexAttribArray(maQPosition);
		GLES20.glDisableVertexAttribArray(maQTexCoord);

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

		int prog = GLES20.glCreateProgram();			 // create empty OpenGL Program
		GLES20.glAttachShader(prog, vertexShader);   // add the vertex shader to program
		GLES20.glAttachShader(prog, fragmentShader); // add the fragment shader to program
		GLES20.glLinkProgram(prog);				  // creates OpenGL program executables

		return prog;
	}

	public int loadTexture(final Context context, final int resourceId)
	{
		GLES20.glGenTextures(1, genbuf, 0);
		if (genbuf[0] != 0)
		{
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inScaled = false;   // No pre-scaling
			// Read in the resource
			final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);

			// Bind to the texture in OpenGL
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, genbuf[0]);
			// Set filtering
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

			// Load the bitmap into the bound texture.
			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

			// Recycle the bitmap, since its data has been loaded into OpenGL.
			bitmap.recycle();
		}

		return genbuf[0];
	}

	private void initPlanet()
	{
		int width = texSize, height = texSize;
		double d = texSize, r = d * 0.5;
		double u, v;
		long aa, uu, vv;

		int[] pixels = new int[width * height];
		for (int y = 0, idx = 0; y < height; y++) {
			double sina = (y - r) / r;
			double w = Math.sqrt(1 - sina*sina);

			v = (0.5 + Math.asin(sina) / Math.PI);
			vv = Math.round(v * 4095);

			for (int x = 0; x < width; x++) {
				double cosa = (x - r) / r;

				if (cosa >= -w && cosa <= w) {
					u = (1 - Math.acos(cosa / w) / Math.PI);
					aa = Math.round(Math.sqrt(1 - sina*sina - cosa*cosa) * 255);
				}
				else {
					u = 0;
					aa = 0;
				}
				uu = Math.round(u * 4095);

				pixels[idx++] = (int) ((aa << 24) + (uu << 12) + vv);
			}
		}

		GLES20.glGenTextures(1, genbuf, 0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, genbuf[0]);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_NONE);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_NONE);
		GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, IntBuffer.wrap(pixels));
		offsetTex = genbuf[0];

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

		mQProgram = Compile(quadVS, quadFS);
		maQPosition = GLES20.glGetAttribLocation(mQProgram, "vPosition");
		maQTexCoord = GLES20.glGetAttribLocation(mQProgram, "vTexCoord0");
		muQRatio = GLES20.glGetUniformLocation(mQProgram, "uRatio");
		muQTexture0 = GLES20.glGetUniformLocation(mQProgram, "uTexture0");
		muQTexture1 = GLES20.glGetUniformLocation(mQProgram, "uTexture1");
		muQOffset = GLES20.glGetUniformLocation(mQProgram, "uOffset");
		muQTilt = GLES20.glGetUniformLocation(mQProgram, "uTilt");

		final float quadv[] = {
			-1,  1, 0, -1, -1,
			-1, -1, 0, -1, 1,
			 1,  1, 0, 1, -1,
			 1, -1, 0, 1, 1
		};

		glQuadVB = createBuffer(quadv);
	}
}
