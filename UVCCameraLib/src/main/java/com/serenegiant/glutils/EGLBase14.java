package com.serenegiant.glutils;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.opengl.GLES10;
import android.opengl.GLES20;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import androidx.annotation.NonNull;

import com.serenegiant.utils.BuildCheck;

@TargetApi(18)
class EGLBase14 extends EGLBase {
    private static final String TAG = "EGLBase14";
    private static final Context EGL_NO_CONTEXT;
    private Config mEglConfig = null;
    @NonNull
    private Context mContext;
    private EGLDisplay mEglDisplay;
    private EGLContext mDefaultContext;
    private int mGlVersion;
    private final int[] mSurfaceDimension;

    public EGLBase14(int maxClientVersion, Context sharedContext, boolean withDepthBuffer, int stencilBits, boolean isRecordable) {
        this.mContext = EGL_NO_CONTEXT;
        this.mEglDisplay = EGL14.EGL_NO_DISPLAY;
        this.mDefaultContext = EGL14.EGL_NO_CONTEXT;
        this.mGlVersion = 2;
        this.mSurfaceDimension = new int[2];
        this.init(maxClientVersion, sharedContext, withDepthBuffer, stencilBits, isRecordable);
    }

    public void release() {
        if (this.mEglDisplay != EGL14.EGL_NO_DISPLAY) {
            this.destroyContext();
            EGL14.eglTerminate(this.mEglDisplay);
            EGL14.eglReleaseThread();
        }

        this.mEglDisplay = EGL14.EGL_NO_DISPLAY;
        this.mContext = EGL_NO_CONTEXT;
    }

    public EglSurface createFromSurface(Object nativeWindow) {
        EglSurface eglSurface = new EglSurface(this, nativeWindow);
        eglSurface.makeCurrent();
        return eglSurface;
    }

    public EglSurface createOffscreen(int width, int height) {
        EglSurface eglSurface = new EglSurface(this, width, height);
        eglSurface.makeCurrent();
        return eglSurface;
    }

    public String queryString(int what) {
        return EGL14.eglQueryString(this.mEglDisplay, what);
    }

    public int getGlVersion() {
        return this.mGlVersion;
    }

    public Context getContext() {
        return this.mContext;
    }

    public Config getConfig() {
        return this.mEglConfig;
    }

    public void makeDefault() {
        if (!EGL14.eglMakeCurrent(this.mEglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)) {
            Log.w("TAG", "makeDefault" + EGL14.eglGetError());
        }

    }

    public void sync() {
        EGL14.eglWaitGL();
        EGL14.eglWaitNative(12379);
    }

    private void init(int maxClientVersion, Context sharedContext, boolean withDepthBuffer, int stencilBits, boolean isRecordable) {
        if (this.mEglDisplay != EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("EGL already set up");
        } else {
            this.mEglDisplay = EGL14.eglGetDisplay(0);
            if (this.mEglDisplay == EGL14.EGL_NO_DISPLAY) {
                throw new RuntimeException("eglGetDisplay failed");
            } else {
                int[] version = new int[2];
                if (!EGL14.eglInitialize(this.mEglDisplay, version, 0, version, 1)) {
                    this.mEglDisplay = null;
                    throw new RuntimeException("eglInitialize failed");
                } else {
                    sharedContext = sharedContext != null ? sharedContext : EGL_NO_CONTEXT;
                    EGLConfig config;
                    EGLContext context;
                    if (maxClientVersion >= 3) {
                        config = this.getConfig(3, withDepthBuffer, stencilBits, isRecordable);
                        if (config != null) {
                            context = this.createContext(sharedContext, config, 3);
                            if (EGL14.eglGetError() == 12288) {
                                this.mEglConfig = new Config(config);
                                this.mContext = new Context(context);
                                this.mGlVersion = 3;
                            }
                        }
                    }

                    if (maxClientVersion >= 2 && (this.mContext == null || this.mContext.eglContext == EGL14.EGL_NO_CONTEXT)) {
                        config = this.getConfig(2, withDepthBuffer, stencilBits, isRecordable);
                        if (config == null) {
                            throw new RuntimeException("chooseConfig failed");
                        }

                        try {
                            context = this.createContext(sharedContext, config, 2);
                            this.checkEglError("eglCreateContext");
                            this.mEglConfig = new Config(config);
                            this.mContext = new Context(context);
                            this.mGlVersion = 2;
                        } catch (Exception var10) {
                            if (isRecordable) {
                                config = this.getConfig(2, withDepthBuffer, stencilBits, false);
                                if (config == null) {
                                    throw new RuntimeException("chooseConfig failed");
                                }

                                EGLContext context1 = this.createContext(sharedContext, config, 2);
                                this.checkEglError("eglCreateContext");
                                this.mEglConfig = new Config(config);
                                this.mContext = new Context(context1);
                                this.mGlVersion = 2;
                            }
                        }
                    }

                    if (this.mContext == null || this.mContext.eglContext == EGL14.EGL_NO_CONTEXT) {
                        config = this.getConfig(1, withDepthBuffer, stencilBits, isRecordable);
                        if (config == null) {
                            throw new RuntimeException("chooseConfig failed");
                        }

                        context = this.createContext(sharedContext, config, 1);
                        this.checkEglError("eglCreateContext");
                        this.mEglConfig = new Config(config);
                        this.mContext = new Context(context);
                        this.mGlVersion = 1;
                    }

                    int[] values = new int[1];
                    EGL14.eglQueryContext(this.mEglDisplay, this.mContext.eglContext, 12440, values, 0);
                    Log.d("EGLBase14", "EGLContext created, client version " + values[0]);
                    this.makeDefault();
                }
            }
        }
    }

    private boolean makeCurrent(EGLSurface surface) {
        if (surface != null && surface != EGL14.EGL_NO_SURFACE) {
            if (!EGL14.eglMakeCurrent(this.mEglDisplay, surface, surface, this.mContext.eglContext)) {
                Log.w("TAG", "eglMakeCurrent" + EGL14.eglGetError());
                return false;
            } else {
                return true;
            }
        } else {
            int error = EGL14.eglGetError();
            if (error == 12299) {
                Log.e("EGLBase14", "makeCurrent:returned EGL_BAD_NATIVE_WINDOW.");
            }

            return false;
        }
    }

    private int swap(EGLSurface surface) {
        if (!EGL14.eglSwapBuffers(this.mEglDisplay, surface)) {
            int err = EGL14.eglGetError();
            return err;
        } else {
            return 12288;
        }
    }

    private int swap(EGLSurface surface, long presentationTimeNs) {
        EGLExt.eglPresentationTimeANDROID(this.mEglDisplay, surface, presentationTimeNs);
        if (!EGL14.eglSwapBuffers(this.mEglDisplay, surface)) {
            int err = EGL14.eglGetError();
            return err;
        } else {
            return 12288;
        }
    }

    private EGLContext createContext(Context sharedContext, EGLConfig config, int version) {
        int[] attrib_list = new int[]{12440, version, 12344};
        EGLContext context = EGL14.eglCreateContext(this.mEglDisplay, config, sharedContext.eglContext, attrib_list, 0);
        return context;
    }

    private void destroyContext() {
        if (!EGL14.eglDestroyContext(this.mEglDisplay, this.mContext.eglContext)) {
            Log.e("destroyContext", "display:" + this.mEglDisplay + " context: " + this.mContext.eglContext);
            Log.e("EGLBase14", "eglDestroyContext:" + EGL14.eglGetError());
        }

        this.mContext = EGL_NO_CONTEXT;
        if (this.mDefaultContext != EGL14.EGL_NO_CONTEXT) {
            if (!EGL14.eglDestroyContext(this.mEglDisplay, this.mDefaultContext)) {
                Log.e("destroyContext", "display:" + this.mEglDisplay + " context: " + this.mDefaultContext);
                Log.e("EGLBase14", "eglDestroyContext:" + EGL14.eglGetError());
            }

            this.mDefaultContext = EGL14.EGL_NO_CONTEXT;
        }

    }

    private final int getSurfaceWidth(EGLSurface surface) {
        boolean ret = EGL14.eglQuerySurface(this.mEglDisplay, surface, 12375, this.mSurfaceDimension, 0);
        if (!ret) {
            this.mSurfaceDimension[0] = 0;
        }

        return this.mSurfaceDimension[0];
    }

    private final int getSurfaceHeight(EGLSurface surface) {
        boolean ret = EGL14.eglQuerySurface(this.mEglDisplay, surface, 12374, this.mSurfaceDimension, 1);
        if (!ret) {
            this.mSurfaceDimension[1] = 0;
        }

        return this.mSurfaceDimension[1];
    }

    private final EGLSurface createWindowSurface(Object nativeWindow) {
        int[] surfaceAttribs = new int[]{12344};
        EGLSurface result = null;

        try {
            result = EGL14.eglCreateWindowSurface(this.mEglDisplay, this.mEglConfig.eglConfig, nativeWindow, surfaceAttribs, 0);
            if (result != null && result != EGL14.EGL_NO_SURFACE) {
                this.makeCurrent(result);
                return result;
            } else {
                int error = EGL14.eglGetError();
                if (error == 12299) {
                    Log.e("EGLBase14", "createWindowSurface returned EGL_BAD_NATIVE_WINDOW.");
                }

                throw new RuntimeException("createWindowSurface failed error=" + error);
            }
        } catch (Exception var5) {
            Log.e("EGLBase14", "eglCreateWindowSurface", var5);
            throw new IllegalArgumentException(var5);
        }
    }

    private final EGLSurface createOffscreenSurface(int width, int height) {
        int[] surfaceAttribs = new int[]{12375, width, 12374, height, 12344};
        EGLSurface result = null;

        try {
            result = EGL14.eglCreatePbufferSurface(this.mEglDisplay, this.mEglConfig.eglConfig, surfaceAttribs, 0);
            this.checkEglError("eglCreatePbufferSurface");
            if (result == null) {
                throw new RuntimeException("surface was null");
            }
        } catch (IllegalArgumentException var6) {
            Log.e("EGLBase14", "createOffscreenSurface", var6);
        } catch (RuntimeException var7) {
            Log.e("EGLBase14", "createOffscreenSurface", var7);
        }

        return result;
    }

    private void destroyWindowSurface(EGLSurface surface) {
        if (surface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglMakeCurrent(this.mEglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroySurface(this.mEglDisplay, surface);
        }

        surface = EGL14.EGL_NO_SURFACE;
    }

    private void checkEglError(String msg) {
        int error;
        if ((error = EGL14.eglGetError()) != 12288) {
            throw new RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error));
        }
    }

    private EGLConfig getConfig(int version, boolean hasDepthBuffer, int stencilBits, boolean isRecordable) {
        int renderableType = 4;
        if (version >= 3) {
            renderableType |= 64;
        }

        int[] attribList = new int[]{12352, renderableType, 12324, 8, 12323, 8, 12322, 8, 12321, 8, 12344, 12344, 12344, 12344, 12344, 12344, 12344};
        int offset = 10;
        if (stencilBits > 0) {
            attribList[offset++] = 12326;
            attribList[offset++] = stencilBits;
        }

        if (hasDepthBuffer) {
            attribList[offset++] = 12325;
            attribList[offset++] = 16;
        }

        if (isRecordable && BuildCheck.isAndroid4_3()) {
            attribList[offset++] = 12610;
            attribList[offset++] = 1;
        }

        for(int i = attribList.length - 1; i >= offset; --i) {
            attribList[i] = 12344;
        }

        EGLConfig config = this.internalGetConfig(attribList);
        if (config == null && version == 2 && isRecordable) {
            int n = attribList.length;

            label51:
            for(int i = 10; i < n - 1; i += 2) {
                if (attribList[i] == 12610) {
                    int j = i;

                    while(true) {
                        if (j >= n) {
                            break label51;
                        }

                        attribList[j] = 12344;
                        ++j;
                    }
                }
            }

            config = this.internalGetConfig(attribList);
        }

        if (config == null) {
            Log.w("EGLBase14", "try to fallback to RGB565");
            attribList[3] = 5;
            attribList[5] = 6;
            attribList[7] = 5;
            config = this.internalGetConfig(attribList);
        }

        return config;
    }

    private EGLConfig internalGetConfig(int[] attribList) {
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        return !EGL14.eglChooseConfig(this.mEglDisplay, attribList, 0, configs, 0, configs.length, numConfigs, 0) ? null : configs[0];
    }

    static {
        EGL_NO_CONTEXT = new Context(EGL14.EGL_NO_CONTEXT);
    }

    public static class EglSurface implements EGLBase.IEglSurface {
        private final EGLBase14 mEglBase;
        private EGLSurface mEglSurface;

        private EglSurface(EGLBase14 eglBase, Object surface) throws IllegalArgumentException {
            this.mEglSurface = EGL14.EGL_NO_SURFACE;
            this.mEglBase = eglBase;
            if (!(surface instanceof Surface) && !(surface instanceof SurfaceHolder) && !(surface instanceof SurfaceTexture) && !(surface instanceof SurfaceView)) {
                throw new IllegalArgumentException("unsupported surface");
            } else {
                this.mEglSurface = this.mEglBase.createWindowSurface(surface);
            }
        }

        private EglSurface(EGLBase14 eglBase, int width, int height) {
            this.mEglSurface = EGL14.EGL_NO_SURFACE;
            this.mEglBase = eglBase;
            if (width > 0 && height > 0) {
                this.mEglSurface = this.mEglBase.createOffscreenSurface(width, height);
            } else {
                this.mEglSurface = this.mEglBase.createOffscreenSurface(1, 1);
            }

        }

        public void makeCurrent() {
            this.mEglBase.makeCurrent(this.mEglSurface);
            if (this.mEglBase.getGlVersion() >= 2) {
                GLES20.glViewport(0, 0, this.mEglBase.getSurfaceWidth(this.mEglSurface), this.mEglBase.getSurfaceHeight(this.mEglSurface));
            } else {
                GLES10.glViewport(0, 0, this.mEglBase.getSurfaceWidth(this.mEglSurface), this.mEglBase.getSurfaceHeight(this.mEglSurface));
            }

        }

        public void swap() {
            this.mEglBase.swap(this.mEglSurface);
        }

        public void swap(long presentationTimeNs) {
            this.mEglBase.swap(this.mEglSurface, presentationTimeNs);
        }

        public void setPresentationTime(long presentationTimeNs) {
            EGLExt.eglPresentationTimeANDROID(this.mEglBase.mEglDisplay, this.mEglSurface, presentationTimeNs);
        }

        public EGLBase.IContext getContext() {
            return this.mEglBase.getContext();
        }

        public boolean isValid() {
            return this.mEglSurface != null && this.mEglSurface != EGL14.EGL_NO_SURFACE && this.mEglBase.getSurfaceWidth(this.mEglSurface) > 0 && this.mEglBase.getSurfaceHeight(this.mEglSurface) > 0;
        }

        public void release() {
            this.mEglBase.makeDefault();
            this.mEglBase.destroyWindowSurface(this.mEglSurface);
            this.mEglSurface = EGL14.EGL_NO_SURFACE;
        }
    }

    public static class Config extends EGLBase.IConfig {
        public final EGLConfig eglConfig;

        private Config(EGLConfig eglConfig) {
            this.eglConfig = eglConfig;
        }
    }

    public static class Context extends EGLBase.IContext {
        public final EGLContext eglContext;

        private Context(EGLContext context) {
            this.eglContext = context;
        }

        @SuppressLint({"NewApi"})
        public long getNativeHandle() {
            return this.eglContext != null ? (BuildCheck.isLollipop() ? this.eglContext.getNativeHandle() : (long)this.eglContext.getHandle()) : 0L;
        }

        public Object getEGLContext() {
            return this.eglContext;
        }
    }
}

