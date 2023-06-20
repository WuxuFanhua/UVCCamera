//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.serenegiant.glutils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class RendererHolder extends AbstractRendererHolder {
    private static final String TAG = RendererHolder.class.getSimpleName();

    public RendererHolder(int width, int height, @Nullable RenderHolderCallback callback) {
        this(width, height, 3, (EGLBase.IContext)null, 2, callback);
    }

    public RendererHolder(int width, int height, int maxClientVersion, EGLBase.IContext sharedContext, int flags, @Nullable RenderHolderCallback callback) {
        super(width, height, maxClientVersion, sharedContext, flags, callback);
    }

    @NonNull
    protected AbstractRendererHolder.RendererTask createRendererTask(int width, int height, int maxClientVersion, EGLBase.IContext sharedContext, int flags) {
        return new MyRendererTask(this, width, height, maxClientVersion, sharedContext, flags);
    }

    protected static final class MyRendererTask extends AbstractRendererHolder.RendererTask {
        public MyRendererTask(RendererHolder parent, int width, int height) {
            super(parent, width, height);
        }

        public MyRendererTask(@NonNull AbstractRendererHolder parent, int width, int height, int maxClientVersion, EGLBase.IContext sharedContext, int flags) {
            super(parent, width, height, maxClientVersion, sharedContext, flags);
        }
    }
}
