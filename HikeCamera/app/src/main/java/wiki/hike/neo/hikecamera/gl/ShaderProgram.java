/**************************************************************************************************
 BINARYVR, INC. PROPRIETARY INFORMATION
 This software is supplied under the terms of a license agreement or nondisclosure
 agreement with BinaryVR, Inc. and may not be copied or disclosed except in
 accordance with the terms of that agreement
 Copyright(c) 2016 BinaryVR, Inc. All Rights Reserved.
 **************************************************************************************************/
package wiki.hike.neo.hikecamera.gl;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ShaderProgram {
    static final String TAG = "ShaderProgram";

    int mProgramHandle;

    public ShaderProgram(String vsh_code, String fsh_code, String[] uniforms, String[] attributes) {
        int vsh = createShader(GLES20.GL_VERTEX_SHADER, vsh_code);
        int fsh = createShader(GLES20.GL_FRAGMENT_SHADER, fsh_code);

        mProgramHandle = GLES20.glCreateProgram();

        GLES20.glAttachShader(mProgramHandle, vsh);
        GLES20.glAttachShader(mProgramHandle, fsh);

        GLES20.glLinkProgram(mProgramHandle);
        int[] status = new int[1];

        GLES20.glGetProgramiv(mProgramHandle, GLES20.GL_LINK_STATUS, status, 0);

        if (status[0] != GLES20.GL_TRUE) {
            String message = "Link Error:" + GLES20.glGetProgramInfoLog(mProgramHandle);
            Log.e(TAG, message);
            throw new RuntimeException(message);
        }

        GLES20.glDetachShader(mProgramHandle, vsh);
        GLES20.glDetachShader(mProgramHandle, fsh);

        GLES20.glDeleteShader(vsh);
        GLES20.glDeleteShader(fsh);

        mUniformLocations = new HashMap<>();
        mAttributeLocations = new HashMap<>();

        for (String uniform : uniforms) {
            mUniformLocations.put(uniform, GLES20.glGetUniformLocation(mProgramHandle, uniform));
        }

        for (String attrib : attributes) {
            mAttributeLocations.put(attrib, GLES20.glGetAttribLocation(mProgramHandle, attrib));
        }
    }

    public void release() {
        if (mProgramHandle != GLES20.GL_NONE) {
            GLES20.glDeleteProgram(mProgramHandle);
            mProgramHandle = GLES20.GL_NONE;
        }
    }

    static int createShader(int shaderType, String code) {
        int shader = GLES20.glCreateShader(shaderType);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);
        int[] status = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] == GLES20.GL_FALSE) {
            String message = String.format(
                    Locale.US,
                    "Shader(%d) compile fails: %s",
                    shaderType,
                    GLES20.glGetShaderInfoLog(shader));
            Log.e(TAG, message);
            throw new RuntimeException(message);
        }
        return shader;
    }

    public int getUniformLocation(String name) {
        return mUniformLocations.get(name);
    }

    public int getAttributeLocation(String name) {
        return mAttributeLocations.get(name);
    }

    /**
     * Accessors
     */
    public void use() {
        GLES20.glUseProgram(mProgramHandle);
    }

    /**
     * Helpers
     */
    public static FloatBuffer createFloatBuffer(int items) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * items);
        byteBuffer.order(ByteOrder.nativeOrder());

        return byteBuffer.asFloatBuffer();
    }

    public static FloatBuffer createFloatBuffer(float[] coords) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * coords.length);
        byteBuffer.order(ByteOrder.nativeOrder());

        FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
        floatBuffer.put(coords);
        floatBuffer.position(0);

        return floatBuffer;
    }

    public static ShortBuffer createShortBuffer(int items) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(2 * items);
        byteBuffer.order(ByteOrder.nativeOrder());
        return byteBuffer.asShortBuffer();
    }

    public static IntBuffer createIntBuffer(int items) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * items);
        byteBuffer.order(ByteOrder.nativeOrder());
        return byteBuffer.asIntBuffer();
    }

    public static ShortBuffer createShortBuffer(short[] indices) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(2 * indices.length);
        byteBuffer.order(ByteOrder.nativeOrder());

        ShortBuffer shortBuffer = byteBuffer.asShortBuffer();
        shortBuffer.put(indices);
        shortBuffer.position(0);

        return shortBuffer;
    }

    Map<String, Integer> mUniformLocations;
    Map<String, Integer> mAttributeLocations;
}
