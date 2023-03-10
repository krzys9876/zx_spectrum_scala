package org.kr.scala.z80.zxscreen;

import java.awt.*;

/**
 * Mutable video data arrays used for translation of ZX Spectrum raw screen memory to RGB image
 */
public class VideoData {
    /**
     * Raw replica of ZX Spectrum screen memory
     */
    public byte[] raw;
    /**
     * Pixels memory with standard (X,Y) layout
     */
    public boolean[] mask;
    /**
     * Colorized RGB pixels with standard (X,Y) layout
     */
    public int[] colorized;
    /**
     * Background color
     */
    public Color border= Color.lightGray;

    /**
     * Create blank memory data
     */
    VideoData(int rawSize, int pixelsSize) {
        raw=new byte[rawSize];
        mask=new boolean[pixelsSize];
        colorized=new int[pixelsSize*3];
    }
}
