package com.wenba100.ocr;

/**
 * Created by allen on 2017/4/11.
 */

public class Formula {
    static {
        System.loadLibrary("Formula");
    }
    public static native boolean initFormulaEngine(String modelDirPath);
    public static native void releaseFormulaEngine();
    //"10 10,20,20;10 10,20 20
    public static native String recognizeFormula(String strokes);
}
