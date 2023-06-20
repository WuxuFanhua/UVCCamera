package com.serenegiant.glutils;


public interface IDrawer2dES2 extends IDrawer2D {
    int glGetAttribLocation(String var1);

    int glGetUniformLocation(String var1);

    void glUseProgram();
}