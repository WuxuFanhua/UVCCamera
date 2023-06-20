//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.serenegiant.glutils;

import android.opengl.GLES20;
import android.opengl.Matrix;
import com.serenegiant.utils.Time;

class RendererSurfaceRec {
    private Object mSurface;
    private EGLBase.IEglSurface mTargetSurface;
    final float[] mMvpMatrix;
    protected volatile boolean mEnable;

    static RendererSurfaceRec newInstance(EGLBase egl, Object surface, int maxFps) {
        return (RendererSurfaceRec)(maxFps > 0 ? new RendererSurfaceRecHasWait(egl, surface, maxFps) : new RendererSurfaceRec(egl, surface));
    }

    private RendererSurfaceRec(EGLBase egl, Object surface) {
        this.mMvpMatrix = new float[16];
        this.mEnable = true;
        this.mSurface = surface;
        this.mTargetSurface = egl.createFromSurface(surface);
        Matrix.setIdentityM(this.mMvpMatrix, 0);
    }

    public void release() {
        if (this.mTargetSurface != null) {
            this.mTargetSurface.release();
            this.mTargetSurface = null;
        }

        this.mSurface = null;
    }

    public boolean isValid() {
        return this.mTargetSurface != null && this.mTargetSurface.isValid();
    }

    private void check() throws IllegalStateException {
        if (this.mTargetSurface == null) {
            throw new IllegalStateException("already released");
        }
    }

    public boolean isEnabled() {
        return this.mEnable;
    }

    public void setEnabled(boolean enable) {
        this.mEnable = enable;
    }

    public boolean canDraw() {
        return this.mEnable;
    }

    public void draw(GLDrawer2D drawer, int textId, float[] texMatrix) {
        if (this.mTargetSurface != null) {
            this.mTargetSurface.makeCurrent();
            GLES20.glClear(16384);
            drawer.setMvpMatrix(this.mMvpMatrix, 0);
            drawer.draw(textId, texMatrix, 0);
            this.mTargetSurface.swap();
        }

    }

    public void clear(int color) {
        if (this.mTargetSurface != null) {
            this.mTargetSurface.makeCurrent();
            GLES20.glClearColor((float)((color & 16711680) >>> 16) / 255.0F, (float)((color & '\uff00') >>> 8) / 255.0F, (float)(color & 255) / 255.0F, (float)((color & -16777216) >>> 24) / 255.0F);
            GLES20.glClear(16384);
            this.mTargetSurface.swap();
        }

    }

    public void makeCurrent() throws IllegalStateException {
        this.check();
        this.mTargetSurface.makeCurrent();
    }

    public void swap() throws IllegalStateException {
        this.check();
        this.mTargetSurface.swap();
    }

    private static class RendererSurfaceRecHasWait extends RendererSurfaceRec {
        private long mNextDraw;
        private final long mIntervalsNs;

        private RendererSurfaceRecHasWait(EGLBase egl, Object surface, int maxFps) {
            super(egl, surface);
            this.mIntervalsNs = 1000000000L / (long)maxFps;
            this.mNextDraw = Time.nanoTime() + this.mIntervalsNs;
        }

        public boolean canDraw() {
            return this.mEnable && Time.nanoTime() - this.mNextDraw > 0L;
        }

        public void draw(GLDrawer2D drawer, int textId, float[] texMatrix) {
            this.mNextDraw = Time.nanoTime() + this.mIntervalsNs;
            super.draw(drawer, textId, texMatrix);
        }
    }
}
