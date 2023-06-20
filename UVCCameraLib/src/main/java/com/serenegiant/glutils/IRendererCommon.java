//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.serenegiant.glutils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface IRendererCommon {
    int MIRROR_NORMAL = 0;
    int MIRROR_HORIZONTAL = 1;
    int MIRROR_VERTICAL = 2;
    int MIRROR_BOTH = 3;
    int MIRROR_NUM = 4;

    void setMirror(int var1);

    int getMirror();

    @Retention(RetentionPolicy.SOURCE)
    public @interface MirrorMode {
    }
}
