package com.wenba100.ocr;

/**
 * Created by allen on 2017/2/6.
 */

public class Core {
    static {
        //System.loadLibrary("OCR");
    }
    public static native boolean initEngine(String modelDirPath);
    public static native void releaseEngine();
    public static native String recognizeChi(String strokes);
    public static native String recognizeEng(String strokes);
}
