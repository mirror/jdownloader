package org.jdownloader.extensions.streaming.dlna.profiles.video;

import java.util.HashMap;

import org.appwork.exceptions.WTFException;
import org.jdownloader.extensions.streaming.dlna.profiles.IntRange;

public enum Resolution {
    _1_16_VGA_4_3_160x112("1/16_VGA_4:3", 160, 112),
    _1_7_VGA_16_9_240x135("1/7_VGA_16:9", 240, 135),
    _1_7_VGA_4_3_240x180("1/7_VGA_4:3", 240, 180),
    _1_9_VGA_4_3_208x160("1/9_VGA_4:3", 208, 160),
    _1280x720("720p", 1280, 720),
    _1920x1080("1080p", 1920, 1080),
    _525_1_2D1_352x480("525_1/2D1", 352, 480),
    _525_2_3D1_480x480("525_2/3D1", 480, 480),
    _525_3_4D1_544x480("525_3/4D1", 544, 480),

    _525_4SIF_704x480("525_4SIF", 704, 480),
    _525_D1_720x480("525_D1", 720, 480),
    _525QSIF8_176x120("525QSIF", 176, 120),
    _525SIF_352x240("525SIF", 352, 240),
    _625_1_2D1_352x576("625_1/2D1", 352, 576),
    _625_2_3D1_480x576("625_2/3D1", 480, 576),
    _625_3_4D1_544x576("625_3/4D1", 544, 576),
    _625_4SIF_704x576("625_4SIF", 704, 576),
    _625_D1_720x576("625_D1", 720, 576),
    _9_16_VGA_16_9_480x270("9/16_VGA_16:9", 480, 270),
    _9_16_VGA_4_3_480x360("9/16_VGA_4:3", 480, 360),
    _CIF_352x288("CIF", 352, 288),
    _QCIF_176x144("QCIF", 176, 144),
    _QVGA_16_9_320x180("QVGA_16:9", 320, 180),
    _QVGA_4_3_320x240("QVGA_4:3", 320, 240),
    _SQCIF_128x96("SQCIF", 128, 96),
    _SQVGA_16_9_160x90("SQVGA_16:9", 160, 90),
    _SQVGA_4_3_160x120("SQVGA_4:3", 160, 120),
    _UPTO_1920x1152("upto1920x1152", 1, 1920, 1, 1152),
    _upto1280x720("upto1280x720", 1, 1280, 1, 720),
    _upto1920x1080("upto1920x1080", 1, 1920, 1, 1080),
    _VGA_640x480("VGA", 640, 480),
    _VGA_16_9_640x360("VGA_16:9", 640, 360),
    _1440x1080("1440x1080", 1440, 1080),
    _1280x1080("1280x1080", 1280, 1080),
    _SIF_352x240("SIF", 352, 240),
    _2048x1536("2048x1536", 2048, 1536),
    _240x176("240x176", 240, 176),
    _1920x1152("1920x1152", 1920, 1152),
    _1920x540("1080i", 1920, 540);

    ;
    private static final HashMap<String, Resolution> MAP = new HashMap<String, Resolution>();
    static {
        for (Resolution sr : Resolution.values()) {
            StringBuilder key = new StringBuilder();
            key.append(sr.getWidth().getMin()).append("_").append(sr.getWidth().getMax()).append("_").append(sr.getHeight().getMin()).append("_").append(sr.getHeight().getMax());

            MAP.put(key.toString(), sr);
        }
    }

    public static Resolution get(int width, int height) {
        StringBuilder key = new StringBuilder();
        key.append(width).append("_").append(width).append("_").append(height).append("_").append(height);
        Resolution ret = MAP.get(key.toString());
        if (ret == null) throw new WTFException("Resolution missing: " + width + "x" + height);
        return ret;
    }

    private IntRange height;

    private String   id;

    private IntRange width;

    private Resolution(String id, int width, int height) {
        this.id = id;
        this.width = new IntRange(width, width);
        this.height = new IntRange(height, height);
    }

    private Resolution(String id, int minWidth, int maxWidth, int minHeight, int maxHeight) {
        this.id = id;
        this.width = new IntRange(minWidth, maxWidth);
        this.height = new IntRange(minHeight, maxHeight);

    }

    public IntRange getHeight() {
        return height;
    }

    public IntRange getWidth() {
        return width;
    }
}
