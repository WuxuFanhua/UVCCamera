package com.serenegiant.glutils;

import android.os.Build;

public abstract class EGLBase {
    public static final Object EGL_LOCK = new Object();
    public static final int EGL_RECORDABLE_ANDROID = 12610;
    public static final int EGL_CONTEXT_CLIENT_VERSION = 12440;
    public static final int EGL_OPENGL_ES2_BIT = 4;
    public static final int EGL_OPENGL_ES3_BIT_KHR = 64;

    public EGLBase() {
    }

    public static EGLBase createFrom(IContext sharedContext, boolean withDepthBuffer, boolean isRecordable) {
        return createFrom(3, sharedContext, withDepthBuffer, 0, isRecordable);
    }

    public static EGLBase createFrom(IContext sharedContext, boolean withDepthBuffer, int stencilBits, boolean isRecordable) {
        return createFrom(3, sharedContext, withDepthBuffer, stencilBits, isRecordable);
    }

    public static EGLBase createFrom(int maxClientVersion, IContext sharedContext, boolean withDepthBuffer, int stencilBits, boolean isRecordable) {
        return (EGLBase)(!isEGL14Supported() || sharedContext != null && !(sharedContext instanceof EGLBase14.Context) ? new EGLBase10(maxClientVersion, (EGLBase10.Context)sharedContext, withDepthBuffer, stencilBits, isRecordable) : new EGLBase14(maxClientVersion, (EGLBase14.Context)sharedContext, withDepthBuffer, stencilBits, isRecordable));
    }

    public static boolean isEGL14Supported() {
        return Build.VERSION.SDK_INT >= 18;
    }

    public abstract void release();

    public abstract String queryString(int var1);

    public abstract int getGlVersion();

    public abstract IContext getContext();

    public abstract IConfig getConfig();

    public abstract IEglSurface createFromSurface(Object var1);

    public abstract IEglSurface createOffscreen(int var1, int var2);

    public abstract void makeDefault();

    public abstract void sync();

    public interface IEglSurface {
        void makeCurrent();

        void swap();

        IContext getContext();

        void swap(long var1);

        void release();

        boolean isValid();
    }

    public abstract static class IConfig {
        public IConfig() {
        }
    }

    public abstract static class IContext {
        public IContext() {
        }

        public abstract long getNativeHandle();

        public abstract Object getEGLContext();
    }
}
