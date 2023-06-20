//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.serenegiant.glutils;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;
import android.util.SparseArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.serenegiant.utils.BuildCheck;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public abstract class AbstractRendererHolder implements IRendererHolder {
    private static final boolean DEBUG = false;
    private static final String TAG = AbstractRendererHolder.class.getSimpleName();
    private static final String RENDERER_THREAD_NAME = "RendererHolder";
    private static final String CAPTURE_THREAD_NAME = "CaptureTask";
    protected static final int REQUEST_DRAW = 1;
    protected static final int REQUEST_UPDATE_SIZE = 2;
    protected static final int REQUEST_ADD_SURFACE = 3;
    protected static final int REQUEST_REMOVE_SURFACE = 4;
    protected static final int REQUEST_REMOVE_SURFACE_ALL = 12;
    protected static final int REQUEST_RECREATE_MASTER_SURFACE = 5;
    protected static final int REQUEST_MIRROR = 6;
    protected static final int REQUEST_ROTATE = 7;
    protected static final int REQUEST_CLEAR = 8;
    protected static final int REQUEST_CLEAR_ALL = 9;
    protected static final int REQUEST_SET_MVP = 10;
    protected final Object mSync;
    @Nullable
    private final RenderHolderCallback mCallback;
    protected volatile boolean isRunning;
    private File mCaptureFile;
    private int mCaptureCompression;
    protected final RendererTask mRendererTask;
    private final Runnable mCaptureTask;

    protected AbstractRendererHolder(int width, int height, @Nullable RenderHolderCallback callback) {
        this(width, height, 3, (EGLBase.IContext)null, 2, callback);
    }

    protected AbstractRendererHolder(int width, int height, int maxClientVersion, EGLBase.IContext sharedContext, int flags, @Nullable RenderHolderCallback callback) {
        this.mSync = new Object();
        this.mCaptureTask = new Runnable() {
            EGLBase eglBase;
            EGLBase.IEglSurface captureSurface;
            GLDrawer2D drawer;
            final float[] mMvpMatrix = new float[16];

            public void run() {
                synchronized(AbstractRendererHolder.this.mSync) {
                    while(!AbstractRendererHolder.this.isRunning && !AbstractRendererHolder.this.mRendererTask.isFinished()) {
                        try {
                            AbstractRendererHolder.this.mSync.wait(1000L);
                        } catch (InterruptedException var11) {
                        }
                    }
                }

                if (AbstractRendererHolder.this.mRendererTask.isRunning()) {
                    this.init();

                    try {
                        if (this.eglBase.getGlVersion() > 2) {
                            this.captureLoopGLES3();
                        } else {
                            this.captureLoopGLES2();
                        }
                    } catch (Exception var9) {
                        Log.w(AbstractRendererHolder.TAG, var9);
                    } finally {
                        this.release();
                    }
                }

            }

            private final void init() {
                this.eglBase = EGLBase.createFrom(3, AbstractRendererHolder.this.mRendererTask.getContext(), false, 0, false);
                this.captureSurface = this.eglBase.createOffscreen(AbstractRendererHolder.this.mRendererTask.width(), AbstractRendererHolder.this.mRendererTask.height());
                Matrix.setIdentityM(this.mMvpMatrix, 0);
                this.drawer = new GLDrawer2D(true);
                AbstractRendererHolder.this.setupCaptureDrawer(this.drawer);
            }

            private final void captureLoopGLES2() {
                int width = -1;
                int height = -1;
                ByteBuffer buf = null;
                int captureCompression = 90;

                while(AbstractRendererHolder.this.isRunning) {
                    synchronized(AbstractRendererHolder.this.mSync) {
                        if (AbstractRendererHolder.this.mCaptureFile == null) {
                            try {
                                AbstractRendererHolder.this.mSync.wait();
                            } catch (InterruptedException var19) {
                                break;
                            }

                            if (AbstractRendererHolder.this.mCaptureFile == null) {
                                continue;
                            }

                            captureCompression = AbstractRendererHolder.this.mCaptureCompression;
                            if (captureCompression <= 0 || captureCompression >= 100) {
                                captureCompression = 90;
                            }
                        }

                        if (buf == null || width != AbstractRendererHolder.this.mRendererTask.width() || height != AbstractRendererHolder.this.mRendererTask.height()) {
                            width = AbstractRendererHolder.this.mRendererTask.width();
                            height = AbstractRendererHolder.this.mRendererTask.height();
                            buf = ByteBuffer.allocateDirect(width * height * 4);
                            buf.order(ByteOrder.LITTLE_ENDIAN);
                            if (this.captureSurface != null) {
                                this.captureSurface.release();
                                this.captureSurface = null;
                            }

                            this.captureSurface = this.eglBase.createOffscreen(width, height);
                        }

                        if (AbstractRendererHolder.this.isRunning && width > 0 && height > 0) {
                            AbstractRendererHolder.setMirror(this.mMvpMatrix, AbstractRendererHolder.this.mRendererTask.mirror());
                            float[] var10000 = this.mMvpMatrix;
                            var10000[5] *= -1.0F;
                            this.drawer.setMvpMatrix(this.mMvpMatrix, 0);
                            this.captureSurface.makeCurrent();
                            this.drawer.draw(AbstractRendererHolder.this.mRendererTask.mTexId, AbstractRendererHolder.this.mRendererTask.mTexMatrix, 0);
                            this.captureSurface.swap();
                            buf.clear();
                            GLES20.glReadPixels(0, 0, width, height, 6408, 5121, buf);
                            Bitmap.CompressFormat compressFormat = CompressFormat.PNG;
                            if (AbstractRendererHolder.this.mCaptureFile.toString().endsWith(".jpg")) {
                                compressFormat = CompressFormat.JPEG;
                            }

                            BufferedOutputStream os = null;

                            try {
                                try {
                                    os = new BufferedOutputStream(new FileOutputStream(AbstractRendererHolder.this.mCaptureFile));
                                    Bitmap bmp = Bitmap.createBitmap(width, height, Config.ARGB_8888);
                                    buf.clear();
                                    bmp.copyPixelsFromBuffer(buf);
                                    bmp.compress(compressFormat, captureCompression, os);
                                    bmp.recycle();
                                    os.flush();
                                } finally {
                                    if (os != null) {
                                        os.close();
                                    }

                                }
                            } catch (FileNotFoundException var21) {
                                Log.w(AbstractRendererHolder.TAG, "failed to save file", var21);
                            } catch (IOException var22) {
                                Log.w(AbstractRendererHolder.TAG, "failed to save file", var22);
                            }
                        } else if (AbstractRendererHolder.this.isRunning) {
                            Log.w(AbstractRendererHolder.TAG, "#captureLoopGLES3:unexpectedly width/height is zero");
                        }

                        AbstractRendererHolder.this.mCaptureFile = null;
                        AbstractRendererHolder.this.mSync.notifyAll();
                    }
                }

                synchronized(AbstractRendererHolder.this.mSync) {
                    AbstractRendererHolder.this.mSync.notifyAll();
                }
            }

            private final void captureLoopGLES3() {
                int width = -1;
                int height = -1;
                ByteBuffer buf = null;
                int captureCompression = 90;

                while(AbstractRendererHolder.this.isRunning) {
                    synchronized(AbstractRendererHolder.this.mSync) {
                        if (AbstractRendererHolder.this.mCaptureFile == null) {
                            try {
                                AbstractRendererHolder.this.mSync.wait();
                            } catch (InterruptedException var19) {
                                break;
                            }

                            if (AbstractRendererHolder.this.mCaptureFile == null) {
                                continue;
                            }

                            captureCompression = AbstractRendererHolder.this.mCaptureCompression;
                            if (captureCompression <= 0 || captureCompression >= 100) {
                                captureCompression = 90;
                            }
                        }

                        if (buf == null || width != AbstractRendererHolder.this.mRendererTask.width() || height != AbstractRendererHolder.this.mRendererTask.height()) {
                            width = AbstractRendererHolder.this.mRendererTask.width();
                            height = AbstractRendererHolder.this.mRendererTask.height();
                            buf = ByteBuffer.allocateDirect(width * height * 4);
                            buf.order(ByteOrder.LITTLE_ENDIAN);
                            if (this.captureSurface != null) {
                                this.captureSurface.release();
                                this.captureSurface = null;
                            }

                            this.captureSurface = this.eglBase.createOffscreen(width, height);
                        }

                        if (AbstractRendererHolder.this.isRunning && width > 0 && height > 0) {
                            AbstractRendererHolder.setMirror(this.mMvpMatrix, AbstractRendererHolder.this.mRendererTask.mirror());
                            float[] var10000 = this.mMvpMatrix;
                            var10000[5] *= -1.0F;
                            this.drawer.setMvpMatrix(this.mMvpMatrix, 0);
                            this.captureSurface.makeCurrent();
                            this.drawer.draw(AbstractRendererHolder.this.mRendererTask.mTexId, AbstractRendererHolder.this.mRendererTask.mTexMatrix, 0);
                            this.captureSurface.swap();
                            buf.clear();
                            GLES20.glReadPixels(0, 0, width, height, 6408, 5121, buf);
                            Bitmap.CompressFormat compressFormat = CompressFormat.PNG;
                            if (AbstractRendererHolder.this.mCaptureFile.toString().endsWith(".jpg")) {
                                compressFormat = CompressFormat.JPEG;
                            }

                            BufferedOutputStream os = null;

                            try {
                                try {
                                    os = new BufferedOutputStream(new FileOutputStream(AbstractRendererHolder.this.mCaptureFile));
                                    Bitmap bmp = Bitmap.createBitmap(width, height, Config.ARGB_8888);
                                    buf.clear();
                                    bmp.copyPixelsFromBuffer(buf);
                                    bmp.compress(compressFormat, captureCompression, os);
                                    bmp.recycle();
                                    os.flush();
                                } finally {
                                    if (os != null) {
                                        os.close();
                                    }

                                }
                            } catch (FileNotFoundException var21) {
                                Log.w(AbstractRendererHolder.TAG, "failed to save file", var21);
                            } catch (IOException var22) {
                                Log.w(AbstractRendererHolder.TAG, "failed to save file", var22);
                            }
                        } else if (AbstractRendererHolder.this.isRunning) {
                            Log.w(AbstractRendererHolder.TAG, "#captureLoopGLES3:unexpectedly width/height is zero");
                        }

                        AbstractRendererHolder.this.mCaptureFile = null;
                        AbstractRendererHolder.this.mSync.notifyAll();
                    }
                }

                synchronized(AbstractRendererHolder.this.mSync) {
                    AbstractRendererHolder.this.mSync.notifyAll();
                }
            }

            private final void release() {
                if (this.captureSurface != null) {
                    this.captureSurface.makeCurrent();
                    this.captureSurface.release();
                    this.captureSurface = null;
                }

                if (this.drawer != null) {
                    this.drawer.release();
                    this.drawer = null;
                }

                if (this.eglBase != null) {
                    this.eglBase.release();
                    this.eglBase = null;
                }

            }
        };
        this.mCallback = callback;
        this.mRendererTask = this.createRendererTask(width, height, maxClientVersion, sharedContext, flags);
        (new Thread(this.mRendererTask, "RendererHolder")).start();
        if (!this.mRendererTask.waitReady()) {
            throw new RuntimeException("failed to start renderer thread");
        } else {
            this.startCaptureTask();
        }
    }

    public boolean isRunning() {
        return this.isRunning;
    }

    public void release() {
        this.mRendererTask.release();
        synchronized(this.mSync) {
            this.isRunning = false;
            this.mSync.notifyAll();
        }
    }

    @Nullable
    public EGLBase.IContext getContext() {
        return this.mRendererTask.getContext();
    }

    public Surface getSurface() {
        return this.mRendererTask.getSurface();
    }

    public SurfaceTexture getSurfaceTexture() {
        return this.mRendererTask.getSurfaceTexture();
    }

    public void reset() {
        this.mRendererTask.checkMasterSurface();
    }

    public void resize(int width, int height) throws IllegalStateException {
        this.mRendererTask.resize(width, height);
    }

    public void setMirror(int mirror) {
        this.mRendererTask.mirror(mirror % 4);
    }

    public int getMirror() {
        return this.mRendererTask.mirror();
    }

    public void addSurface(int id, Object surface, boolean isRecordable) throws IllegalStateException, IllegalArgumentException {
        this.mRendererTask.addSurface(id, surface);
    }

    public void addSurface(int id, Object surface, boolean isRecordable, int maxFps) throws IllegalStateException, IllegalArgumentException {
        this.mRendererTask.addSurface(id, surface, maxFps);
    }

    public void removeSurface(int id) {
        this.mRendererTask.removeSurface(id);
    }

    public void removeSurfaceAll() {
        this.mRendererTask.removeSurfaceAll();
    }

    public void clearSurface(int id, int color) {
        this.mRendererTask.clearSurface(id, color);
    }

    public void clearSurfaceAll(int color) {
        this.mRendererTask.clearSurfaceAll(color);
    }

    public void setMvpMatrix(int id, int offset, @NonNull float[] matrix) {
        this.mRendererTask.setMvpMatrix(id, offset, matrix);
    }

    public boolean isEnabled(int id) {
        return this.mRendererTask.isEnabled(id);
    }

    public void setEnabled(int id, boolean enable) {
        this.mRendererTask.setEnabled(id, enable);
    }

    public void requestFrame() {
        this.mRendererTask.removeRequest(1);
        this.mRendererTask.offer(1);
    }

    public int getCount() {
        return this.mRendererTask.getCount();
    }

    public void captureStillAsync(String path) {
        this.captureStillAsync(path, 90);
    }

    public void captureStillAsync(String path, int captureCompression) {
        File file = new File(path);
        synchronized(this.mSync) {
            this.mCaptureFile = file;
            this.mCaptureCompression = captureCompression;
            this.mSync.notifyAll();
        }
    }

    public void captureStill(String path) {
        this.captureStill(path, 90);
    }

    public void captureStill(String path, int captureCompression) {
        File file = new File(path);
        synchronized(this.mSync) {
            this.mCaptureFile = file;
            this.mCaptureCompression = captureCompression;
            this.mSync.notifyAll();

            while(this.isRunning && this.mCaptureFile != null) {
                try {
                    this.mSync.wait(1000L);
                } catch (InterruptedException var7) {
                }
            }

        }
    }

    @NonNull
    protected abstract RendererTask createRendererTask(int var1, int var2, int var3, EGLBase.IContext var4, int var5);

    protected void startCaptureTask() {
        (new Thread(this.mCaptureTask, "CaptureTask")).start();
        synchronized(this.mSync) {
            if (!this.isRunning) {
                try {
                    this.mSync.wait();
                } catch (InterruptedException var4) {
                }
            }

        }
    }

    protected void notifyCapture() {
        synchronized(this.mCaptureTask) {
            this.mCaptureTask.notify();
        }
    }

    protected void callOnCreate(Surface surface) {
        if (this.mCallback != null) {
            try {
                this.mCallback.onCreate(surface);
            } catch (Exception var3) {
                Log.w(TAG, var3);
            }
        }

    }

    protected void callOnFrameAvailable() {
        if (this.mCallback != null) {
            try {
                this.mCallback.onFrameAvailable();
            } catch (Exception var2) {
                Log.w(TAG, var2);
            }
        }

    }

    protected void callOnDestroy() {
        if (this.mCallback != null) {
            try {
                this.mCallback.onDestroy();
            } catch (Exception var2) {
                Log.w(TAG, var2);
            }
        }

    }

    protected void setupCaptureDrawer(GLDrawer2D drawer) {
    }

    protected static void setMirror(float[] mvp, int mirror) {
        switch (mirror) {
            case 0:
                mvp[0] = Math.abs(mvp[0]);
                mvp[5] = Math.abs(mvp[5]);
                break;
            case 1:
                mvp[0] = -Math.abs(mvp[0]);
                mvp[5] = Math.abs(mvp[5]);
                break;
            case 2:
                mvp[0] = Math.abs(mvp[0]);
                mvp[5] = -Math.abs(mvp[5]);
                break;
            case 3:
                mvp[0] = -Math.abs(mvp[0]);
                mvp[5] = -Math.abs(mvp[5]);
        }

    }

    protected static void rotate(float[] mvp, int degrees) {
        if (degrees % 180 != 0) {
            Matrix.rotateM(mvp, 0, (float)degrees, 0.0F, 0.0F, 1.0F);
        }

    }

    protected static void setRotation(float[] mvp, int degrees) {
        Matrix.setIdentityM(mvp, 0);
        if (degrees % 180 != 0) {
            Matrix.rotateM(mvp, 0, (float)degrees, 0.0F, 0.0F, 1.0F);
        }

    }

    protected abstract static class RendererTask extends BaseRendererTask {
        protected GLDrawer2D mDrawer;

        public RendererTask(@NonNull AbstractRendererHolder parent, int width, int height) {
            super(parent, width, height);
        }

        public RendererTask(@NonNull AbstractRendererHolder parent, int width, int height, int maxClientVersion, EGLBase.IContext sharedContext, int flags) {
            super(parent, width, height, maxClientVersion, sharedContext, flags);
        }

        protected void internalOnStart() {
            this.mDrawer = new GLDrawer2D(true);
        }

        protected void internalOnStop() {
            if (this.mDrawer != null) {
                this.mDrawer.release();
                this.mDrawer = null;
            }

        }

        protected void preprocess() {
        }

        protected void onDrawClient(@NonNull RendererSurfaceRec client, int texId, float[] texMatrix) {
            client.draw(this.mDrawer, texId, texMatrix);
        }
    }

    protected abstract static class BaseRendererTask extends EglTask {
        private final SparseArray<RendererSurfaceRec> mClients;
        private final AbstractRendererHolder mParent;
        private int mVideoWidth;
        private int mVideoHeight;
        final float[] mTexMatrix;
        int mTexId;
        private SurfaceTexture mMasterTexture;
        private Surface mMasterSurface;
        private int mMirror;
        private int mRotation;
        private volatile boolean mIsFirstFrameRendered;
        protected final SurfaceTexture.OnFrameAvailableListener mOnFrameAvailableListener;

        public BaseRendererTask(@NonNull AbstractRendererHolder parent, int width, int height) {
            this(parent, width, height, 3, (EGLBase.IContext)null, 2);
        }

        public BaseRendererTask(@NonNull AbstractRendererHolder parent, int width, int height, int maxClientVersion, EGLBase.IContext sharedContext, int flags) {
            super(maxClientVersion, sharedContext, flags);
            this.mClients = new SparseArray();
            this.mTexMatrix = new float[16];
            this.mMirror = 0;
            this.mRotation = 0;
            this.mOnFrameAvailableListener = new SurfaceTexture.OnFrameAvailableListener() {
                public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                    BaseRendererTask.this.removeRequest(1);
                    BaseRendererTask.this.mIsFirstFrameRendered = true;
                    BaseRendererTask.this.offer(1);
                }
            };
            this.mParent = parent;
            this.mVideoWidth = width > 0 ? width : 640;
            this.mVideoHeight = height > 0 ? height : 480;
        }

        protected final void onStart() {
            this.handleReCreateMasterSurface();
            this.internalOnStart();
            synchronized(this.mParent.mSync) {
                this.mParent.isRunning = true;
                this.mParent.mSync.notifyAll();
            }
        }

        protected void onStop() {
            synchronized(this.mParent.mSync) {
                this.mParent.isRunning = false;
                this.mParent.mSync.notifyAll();
            }

            this.makeCurrent();
            this.internalOnStop();
            this.handleReleaseMasterSurface();
            this.handleRemoveAll();
        }

        protected boolean onError(Exception e) {
            return false;
        }

        protected abstract void internalOnStart();

        protected abstract void internalOnStop();

        protected Object processRequest(int request, int arg1, int arg2, Object obj) {
            switch (request) {
                case 1:
                    this.handleDraw();
                    break;
                case 2:
                    this.handleResize(arg1, arg2);
                    break;
                case 3:
                    this.handleAddSurface(arg1, obj, arg2);
                    break;
                case 4:
                    this.handleRemoveSurface(arg1);
                    break;
                case 5:
                    this.handleReCreateMasterSurface();
                    break;
                case 6:
                    this.handleMirror(arg1);
                    break;
                case 7:
                    this.handleRotate(arg1, arg2);
                    break;
                case 8:
                    this.handleClear(arg1, arg2);
                    break;
                case 9:
                    this.handleClearAll(arg1);
                    break;
                case 10:
                    this.handleSetMvp(arg1, arg2, obj);
                case 11:
                default:
                    break;
                case 12:
                    this.handleRemoveAll();
            }

            return null;
        }

        public Surface getSurface() {
            this.checkMasterSurface();
            return this.mMasterSurface;
        }

        public SurfaceTexture getSurfaceTexture() {
            this.checkMasterSurface();
            return this.mMasterTexture;
        }

        public void addSurface(int id, Object surface) throws IllegalStateException, IllegalArgumentException {
            this.addSurface(id, surface, -1);
        }

        public void addSurface(int id, Object surface, int maxFps) throws IllegalStateException, IllegalArgumentException {
            this.checkFinished();
            if (!(surface instanceof SurfaceTexture) && !(surface instanceof Surface) && !(surface instanceof SurfaceHolder)) {
                throw new IllegalArgumentException("Surface should be one of Surface, SurfaceTexture or SurfaceHolder");
            } else {
                synchronized(this.mClients) {
                    if (this.mClients.get(id) == null) {
                        while(this.isRunning()) {
                            if (this.offer(3, id, maxFps, surface)) {
                                try {
                                    this.mClients.wait();
                                } catch (InterruptedException var7) {
                                }
                                break;
                            }

                            try {
                                this.mClients.wait(5L);
                            } catch (InterruptedException var8) {
                                break;
                            }
                        }
                    }

                }
            }
        }

        public void removeSurface(int id) {
            synchronized(this.mClients) {
                if (this.mClients.get(id) != null) {
                    while(this.isRunning()) {
                        if (this.offer(4, id)) {
                            try {
                                this.mClients.wait();
                            } catch (InterruptedException var5) {
                            }
                            break;
                        }

                        try {
                            this.mClients.wait(5L);
                        } catch (InterruptedException var6) {
                            break;
                        }
                    }
                }

            }
        }

        public void removeSurfaceAll() {
            synchronized(this.mClients) {
                while(this.isRunning()) {
                    if (this.offer(12)) {
                        try {
                            this.mClients.wait();
                        } catch (InterruptedException var4) {
                        }
                        break;
                    }

                    try {
                        this.mClients.wait(5L);
                    } catch (InterruptedException var5) {
                        break;
                    }
                }

            }
        }

        public void clearSurface(int id, int color) {
            this.checkFinished();
            this.offer(8, id, color);
        }

        public void clearSurfaceAll(int color) {
            this.checkFinished();
            this.offer(9, color);
        }

        public void setMvpMatrix(int id, int offset, @NonNull float[] matrix) {
            this.checkFinished();
            this.offer(10, id, offset, matrix);
        }

        public boolean isEnabled(int id) {
            synchronized(this.mClients) {
                RendererSurfaceRec rec = (RendererSurfaceRec)this.mClients.get(id);
                return rec != null && rec.isEnabled();
            }
        }

        public void setEnabled(int id, boolean enable) {
            synchronized(this.mClients) {
                RendererSurfaceRec rec = (RendererSurfaceRec)this.mClients.get(id);
                if (rec != null) {
                    rec.setEnabled(enable);
                }

            }
        }

        public int getCount() {
            synchronized(this.mClients) {
                return this.mClients.size();
            }
        }

        public void resize(int width, int height) throws IllegalStateException {
            this.checkFinished();
            if (width > 0 && height > 0 && (this.mVideoWidth != width || this.mVideoHeight != height)) {
                this.offer(2, width, height);
            }

        }

        protected int width() {
            return this.mVideoWidth;
        }

        protected int height() {
            return this.mVideoHeight;
        }

        public void mirror(int mirror) {
            this.checkFinished();
            if (this.mMirror != mirror) {
                this.offer(6, mirror);
            }

        }

        public int mirror() {
            return this.mMirror;
        }

        public void checkMasterSurface() {
            this.checkFinished();
            if (this.mMasterSurface == null || !this.mMasterSurface.isValid()) {
                Log.d(AbstractRendererHolder.TAG, "checkMasterSurface:invalid master surface");
                this.offerAndWait(5, 0, 0, (Object)null);
            }

        }

        protected void checkFinished() throws IllegalStateException {
            if (this.isFinished()) {
                throw new IllegalStateException("already finished");
            }
        }

        protected AbstractRendererHolder getParent() {
            return this.mParent;
        }

        protected void handleDraw() {
            if (this.mMasterSurface != null && this.mMasterSurface.isValid()) {
                if (this.mIsFirstFrameRendered) {
                    try {
                        this.makeCurrent();
                        this.handleUpdateTexture();
                    } catch (Exception var2) {
                        Log.e(AbstractRendererHolder.TAG, "draw:thread id =" + Thread.currentThread().getId(), var2);
                        this.offer(5);
                        return;
                    }

                    this.mParent.notifyCapture();
                    this.preprocess();
                    this.handleDrawClients();
                    this.mParent.callOnFrameAvailable();
                }

                this.makeCurrent();
                GLES20.glClear(16384);
                GLES20.glFlush();
            } else {
                Log.e(AbstractRendererHolder.TAG, "checkMasterSurface:invalid master surface");
                this.offer(5);
            }
        }

        protected void handleUpdateTexture() {
            this.mMasterTexture.updateTexImage();
            this.mMasterTexture.getTransformMatrix(this.mTexMatrix);
        }

        protected abstract void preprocess();

        protected void handleDrawClients() {
            synchronized(this.mClients) {
                int n = this.mClients.size();

                for(int i = n - 1; i >= 0; --i) {
                    RendererSurfaceRec client = (RendererSurfaceRec)this.mClients.valueAt(i);
                    if (client != null && client.canDraw()) {
                        try {
                            this.onDrawClient(client, this.mTexId, this.mTexMatrix);
                        } catch (Exception var7) {
                            this.mClients.removeAt(i);
                            client.release();
                        }
                    }
                }

            }
        }

        protected abstract void onDrawClient(@NonNull RendererSurfaceRec var1, int var2, float[] var3);

        protected void handleAddSurface(int id, Object surface, int maxFps) {
            this.checkSurface();
            synchronized(this.mClients) {
                RendererSurfaceRec client = (RendererSurfaceRec)this.mClients.get(id);
                if (client == null) {
                    try {
                        client = RendererSurfaceRec.newInstance(this.getEgl(), surface, maxFps);
                        this.setMirror(client, this.mMirror);
                        this.mClients.append(id, client);
                    } catch (Exception var8) {
                        Log.w(AbstractRendererHolder.TAG, "invalid surface: surface=" + surface, var8);
                    }
                } else {
                    Log.w(AbstractRendererHolder.TAG, "surface is already added: id=" + id);
                }

                this.mClients.notifyAll();
            }
        }

        protected void handleRemoveSurface(int id) {
            synchronized(this.mClients) {
                RendererSurfaceRec client = (RendererSurfaceRec)this.mClients.get(id);
                if (client != null) {
                    this.mClients.remove(id);
                    if (client.isValid()) {
                        client.clear(0);
                    }

                    client.release();
                }

                this.checkSurface();
                this.mClients.notifyAll();
            }
        }

        protected void handleRemoveAll() {
            synchronized(this.mClients) {
                int n = this.mClients.size();

                for(int i = 0; i < n; ++i) {
                    RendererSurfaceRec client = (RendererSurfaceRec)this.mClients.valueAt(i);
                    if (client != null) {
                        if (client.isValid()) {
                            client.clear(0);
                        }

                        client.release();
                    }
                }

                this.mClients.clear();
                this.mClients.notifyAll();
            }
        }

        protected void checkSurface() {
            synchronized(this.mClients) {
                int n = this.mClients.size();

                for(int i = 0; i < n; ++i) {
                    RendererSurfaceRec client = (RendererSurfaceRec)this.mClients.valueAt(i);
                    if (client != null && !client.isValid()) {
                        int id = this.mClients.keyAt(i);
                        ((RendererSurfaceRec)this.mClients.valueAt(i)).release();
                        this.mClients.remove(id);
                    }
                }

            }
        }

        protected void handleClear(int id, int color) {
            synchronized(this.mClients) {
                RendererSurfaceRec client = (RendererSurfaceRec)this.mClients.get(id);
                if (client != null && client.isValid()) {
                    client.clear(color);
                }

            }
        }

        protected void handleClearAll(int color) {
            synchronized(this.mClients) {
                int n = this.mClients.size();

                for(int i = 0; i < n; ++i) {
                    RendererSurfaceRec client = (RendererSurfaceRec)this.mClients.valueAt(i);
                    if (client != null && client.isValid()) {
                        client.clear(color);
                    }
                }

            }
        }

        protected void handleSetMvp(int id, int offset, Object mvp) {
            if (mvp instanceof float[] && ((float[])((float[])mvp)).length >= 16 + offset) {
                float[] array = (float[])((float[])mvp);
                synchronized(this.mClients) {
                    RendererSurfaceRec client = (RendererSurfaceRec)this.mClients.get(id);
                    if (client != null && client.isValid()) {
                        System.arraycopy(array, offset, client.mMvpMatrix, 0, 16);
                    }
                }
            }

        }

        @SuppressLint({"NewApi"})
        protected void handleReCreateMasterSurface() {
            this.makeCurrent();
            this.handleReleaseMasterSurface();
            this.makeCurrent();
            this.mTexId = GLHelper.initTex(36197, 9728);
            this.mMasterTexture = new SurfaceTexture(this.mTexId);
            this.mMasterSurface = new Surface(this.mMasterTexture);
            if (BuildCheck.isAndroid4_1()) {
                this.mMasterTexture.setDefaultBufferSize(this.mVideoWidth, this.mVideoHeight);
            }

            this.mMasterTexture.setOnFrameAvailableListener(this.mOnFrameAvailableListener);
            this.mParent.callOnCreate(this.mMasterSurface);
        }

        protected void handleReleaseMasterSurface() {
            if (this.mMasterSurface != null) {
                try {
                    this.mMasterSurface.release();
                } catch (Exception var3) {
                    Log.w(AbstractRendererHolder.TAG, var3);
                }

                this.mMasterSurface = null;
                this.mParent.callOnDestroy();
            }

            if (this.mMasterTexture != null) {
                try {
                    this.mMasterTexture.release();
                } catch (Exception var2) {
                    Log.w(AbstractRendererHolder.TAG, var2);
                }

                this.mMasterTexture = null;
            }

            if (this.mTexId != 0) {
                GLHelper.deleteTex(this.mTexId);
                this.mTexId = 0;
            }

        }

        @SuppressLint({"NewApi"})
        protected void handleResize(int width, int height) {
            this.mVideoWidth = width;
            this.mVideoHeight = height;
            if (BuildCheck.isAndroid4_1()) {
                this.mMasterTexture.setDefaultBufferSize(this.mVideoWidth, this.mVideoHeight);
            }

        }

        protected void handleMirror(int mirror) {
            this.mMirror = mirror;
            synchronized(this.mClients) {
                int n = this.mClients.size();

                for(int i = 0; i < n; ++i) {
                    RendererSurfaceRec client = (RendererSurfaceRec)this.mClients.valueAt(i);
                    if (client != null) {
                        this.setMirror(client, mirror);
                    }
                }

            }
        }

        protected void setMirror(RendererSurfaceRec client, int mirror) {
            RendererHolder.setMirror(client.mMvpMatrix, mirror);
        }

        protected void handleRotate(int id, int degree) {
        }
    }
}
