//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.serenegiant.glutils;

import android.graphics.SurfaceTexture;
import android.view.Surface;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface IRendererHolder extends IRendererCommon {
    boolean isRunning();

    void release();

    @Nullable
    EGLBase.IContext getContext();

    Surface getSurface();

    SurfaceTexture getSurfaceTexture();

    void reset();

    void resize(int var1, int var2) throws IllegalStateException;

    void addSurface(int var1, Object var2, boolean var3) throws IllegalStateException, IllegalArgumentException;

    void addSurface(int var1, Object var2, boolean var3, int var4) throws IllegalStateException, IllegalArgumentException;

    void removeSurface(int var1);

    void removeSurfaceAll();

    void clearSurface(int var1, int var2);

    void clearSurfaceAll(int var1);

    void setMvpMatrix(int var1, int var2, @NonNull float[] var3);

    boolean isEnabled(int var1);

    void setEnabled(int var1, boolean var2);

    void requestFrame();

    int getCount();

    void captureStillAsync(String var1);

    void captureStillAsync(String var1, int var2);

    void captureStill(String var1);

    void captureStill(String var1, int var2);
}
