/*
 * $Id: BlendComposite.java,v 1.12 2007/11/20 21:36:13 rah003 Exp $
 *
 * Dual-licensed under LGPL (Sun and Romain Guy) and BSD (Romain Guy).
 *
 * Copyright 2005 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * Copyright (c) 2006 Romain Guy <romain.guy@mac.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jdesktop.swingx.graphics;

import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.RasterFormatException;

/**
 * <p>A blend composite defines the rule according to which a drawing primitive
 * (known as the source) is mixed with existing graphics (know as the
 * destination.)</p>
 * <p><code>BlendComposite</code> is an implementation of the
 * {@link java.awt.Composite} interface and must therefore be set as a state on
 * a {@link java.awt.Graphics2D} surface.</p>
 * <p>Please refer to {@link java.awt.Graphics2D#setComposite(java.awt.Composite)}
 * for more information on how to use this class with a graphics surface.</p>
 * <h2>Blending Modes</h2>
 * <p>This class offers a certain number of blending modes, or compositing
 * rules. These rules are inspired from graphics editing software packages,
 * like <em>Adobe Photoshop</em> or <em>The GIMP</em>.</p>
 * <p>Given the wide variety of implemented blending modes and the difficulty
 * to describe them with words, please refer to those tools to visually see
 * the result of these blending modes.</p>
 * <h2>Opacity</h2>
 * <p>Each blending mode has an associated opacity, defined as a float value
 * between 0.0 and 1.0. Changing the opacity controls the force with which the
 * compositing operation is applied. For instance, a composite with an opacity
 * of 0.0 will not draw the source onto the destination. With an opacity of
 * 1.0, the source will be fully drawn onto the destination, according to the
 * selected blending mode rule.</p>
 * <p>The opacity, or alpha value, is used by the composite instance to mutiply
 * the alpha value of each pixel of the source when being composited over the
 * destination.</p>
 * <h2>Creating a Blend Composite</h2>
 * <p>Blend composites can be created in various manners:</p>
 * <ul>
 *   <li>Use one of the pre-defined instance. Example:
 *     <code>BlendComposite.Average</code>.</li>
 *   <li>Derive one of the pre-defined instances by calling
 *     {@link #derive(float)} or {@link #derive(BlendingMode)}. Deriving allows
 *     you to change either the opacity or the blending mode. Example:
 *     <code>BlendComposite.Average.derive(0.5f)</code>.</li>
 *   <li>Use a factory method: {@link #getInstance(BlendingMode)} or
 *     {@link #getInstance(BlendingMode, float)}.</li>
 * </ul>
 * <h2>Implementation Caveat</h2>
 * <p>TThe blending mode <em>SoftLight</em> has not been implemented yet.</p>
 *
 * @see org.jdesktop.swingx.graphics.BlendComposite.BlendingMode
 * @see java.awt.Graphics2D
 * @see java.awt.Composite
 * @see java.awt.AlphaComposite
 * @author Romain Guy <romain.guy@mac.com>
 */
public final class BlendComposite implements Composite {
    /**
     * <p>A blending mode defines the compositing rule of a
     * {@link org.jdesktop.swingx.graphics.BlendComposite}.</p>
     *
     * @author Romain Guy <romain.guy@mac.com>
     */
    public enum BlendingMode {
        AVERAGE,
        MULTIPLY,
        SCREEN,
        DARKEN,
        LIGHTEN,
        OVERLAY,
        HARD_LIGHT,
        SOFT_LIGHT,
        DIFFERENCE,
        NEGATION,
        EXCLUSION,
        COLOR_DODGE,
        INVERSE_COLOR_DODGE,
        SOFT_DODGE,
        COLOR_BURN,
        INVERSE_COLOR_BURN,
        SOFT_BURN,
        REFLECT,
        GLOW,
        FREEZE,
        HEAT,
        ADD,
        SUBTRACT,
        STAMP,
        RED,
        GREEN,
        BLUE,
        HUE,
        SATURATION,
        COLOR,
        LUMINOSITY
    }

    public static final BlendComposite Average = new BlendComposite(BlendingMode.AVERAGE);
    public static final BlendComposite Multiply = new BlendComposite(BlendingMode.MULTIPLY);
    public static final BlendComposite Screen = new BlendComposite(BlendingMode.SCREEN);
    public static final BlendComposite Darken = new BlendComposite(BlendingMode.DARKEN);
    public static final BlendComposite Lighten = new BlendComposite(BlendingMode.LIGHTEN);
    public static final BlendComposite Overlay = new BlendComposite(BlendingMode.OVERLAY);
    public static final BlendComposite HardLight = new BlendComposite(BlendingMode.HARD_LIGHT);
    public static final BlendComposite SoftLight = new BlendComposite(BlendingMode.SOFT_LIGHT);
    public static final BlendComposite Difference = new BlendComposite(BlendingMode.DIFFERENCE);
    public static final BlendComposite Negation = new BlendComposite(BlendingMode.NEGATION);
    public static final BlendComposite Exclusion = new BlendComposite(BlendingMode.EXCLUSION);
    public static final BlendComposite ColorDodge = new BlendComposite(BlendingMode.COLOR_DODGE);
    public static final BlendComposite InverseColorDodge = new BlendComposite(BlendingMode.INVERSE_COLOR_DODGE);
    public static final BlendComposite SoftDodge = new BlendComposite(BlendingMode.SOFT_DODGE);
    public static final BlendComposite ColorBurn = new BlendComposite(BlendingMode.COLOR_BURN);
    public static final BlendComposite InverseColorBurn = new BlendComposite(BlendingMode.INVERSE_COLOR_BURN);
    public static final BlendComposite SoftBurn = new BlendComposite(BlendingMode.SOFT_BURN);
    public static final BlendComposite Reflect = new BlendComposite(BlendingMode.REFLECT);
    public static final BlendComposite Glow = new BlendComposite(BlendingMode.GLOW);
    public static final BlendComposite Freeze = new BlendComposite(BlendingMode.FREEZE);
    public static final BlendComposite Heat = new BlendComposite(BlendingMode.HEAT);
    public static final BlendComposite Add = new BlendComposite(BlendingMode.ADD);
    public static final BlendComposite Subtract = new BlendComposite(BlendingMode.SUBTRACT);
    public static final BlendComposite Stamp = new BlendComposite(BlendingMode.STAMP);
    public static final BlendComposite Red = new BlendComposite(BlendingMode.RED);
    public static final BlendComposite Green = new BlendComposite(BlendingMode.GREEN);
    public static final BlendComposite Blue = new BlendComposite(BlendingMode.BLUE);
    public static final BlendComposite Hue = new BlendComposite(BlendingMode.HUE);
    public static final BlendComposite Saturation = new BlendComposite(BlendingMode.SATURATION);
    public static final BlendComposite Color = new BlendComposite(BlendingMode.COLOR);
    public static final BlendComposite Luminosity = new BlendComposite(BlendingMode.LUMINOSITY);

    private final float alpha;
    private final BlendingMode mode;

    private BlendComposite(BlendingMode mode) {
        this(mode, 1.0f);
    }

    private BlendComposite(BlendingMode mode, float alpha) {
        this.mode = mode;

        if (alpha < 0.0f || alpha > 1.0f) {
            throw new IllegalArgumentException(
                    "alpha must be comprised between 0.0f and 1.0f");
        }
        this.alpha = alpha;
    }

    /**
     * <p>Creates a new composite based on the blending mode passed
     * as a parameter. A default opacity of 1.0 is applied.</p>
     *
     * @param mode the blending mode defining the compositing rule
     * @return a new <code>BlendComposite</code> based on the selected blending
     *   mode, with an opacity of 1.0
     */
    public static BlendComposite getInstance(BlendingMode mode) {
        return new BlendComposite(mode);
    }

    /**
     * <p>Creates a new composite based on the blending mode and opacity passed
     * as parameters. The opacity must be a value between 0.0 and 1.0.</p>
     *
     * @param mode the blending mode defining the compositing rule
     * @param alpha the constant alpha to be multiplied with the alpha of the
     *   source. <code>alpha</code> must be a floating point between 0.0 and 1.0.
     * @throws IllegalArgumentException if the opacity is less than 0.0 or
     *   greater than 1.0
     * @return a new <code>BlendComposite</code> based on the selected blending
     *   mode and opacity
     */
    public static BlendComposite getInstance(BlendingMode mode, float alpha) {
        return new BlendComposite(mode, alpha);
    }

    /**
     * <p>Returns a <code>BlendComposite</code> object that uses the specified
     * blending mode and this object's alpha value. If the newly specified
     * blending mode is the same as this object's, this object is returned.</p>
     *
     * @param mode the blending mode defining the compositing rule
     * @return a <code>BlendComposite</code> object derived from this object,
     *   that uses the specified blending mode
     */
    public BlendComposite derive(BlendingMode mode) {
        return this.mode == mode ? this : new BlendComposite(mode, getAlpha());
    }

    /**
     * <p>Returns a <code>BlendComposite</code> object that uses the specified
     * opacity, or alpha, and this object's blending mode. If the newly specified
     * opacity is the same as this object's, this object is returned.</p>
     *
     * @param alpha the constant alpha to be multiplied with the alpha of the
     *   source. <code>alpha</code> must be a floating point between 0.0 and 1.0.
     * @throws IllegalArgumentException if the opacity is less than 0.0 or
     *   greater than 1.0
     * @return a <code>BlendComposite</code> object derived from this object,
     *   that uses the specified blending mode
     */
    public BlendComposite derive(float alpha) {
        return this.alpha == alpha ? this : new BlendComposite(getMode(), alpha);
    }

    /**
     * <p>Returns the opacity of this composite. If no opacity has been defined,
     * 1.0 is returned.</p>
     *
     * @return the alpha value, or opacity, of this object
     */
    public float getAlpha() {
        return alpha;
    }

    /**
     * <p>Returns the blending mode of this composite.</p>
     *
     * @return the blending mode used by this object
     */
    public BlendingMode getMode() {
        return mode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Float.floatToIntBits(alpha) * 31 + mode.ordinal();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BlendComposite)) {
            return false;
        }

        BlendComposite bc = (BlendComposite) obj;
        return mode == bc.mode && alpha == bc.alpha;
    }

    private static boolean isRgbColorModel(ColorModel cm) {
        if (cm instanceof DirectColorModel &&
                cm.getTransferType() == DataBuffer.TYPE_INT) {
            DirectColorModel directCM = (DirectColorModel) cm;

            return directCM.getRedMask() == 0x00FF0000 &&
                   directCM.getGreenMask() == 0x0000FF00 &&
                   directCM.getBlueMask() == 0x000000FF &&
                   (directCM.getNumComponents() == 3 ||
                    directCM.getAlphaMask() == 0xFF000000);
        }

        return false;
    }

    private static boolean isBgrColorModel(ColorModel cm) {
        if (cm instanceof DirectColorModel &&
                cm.getTransferType() == DataBuffer.TYPE_INT) {
            DirectColorModel directCM = (DirectColorModel) cm;

            return directCM.getRedMask() == 0x000000FF &&
                   directCM.getGreenMask() == 0x0000FF00 &&
                   directCM.getBlueMask() == 0x00FF0000 &&
                   (directCM.getNumComponents() == 3 ||
                    directCM.getAlphaMask() == 0xFF000000);
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    public CompositeContext createContext(ColorModel srcColorModel,
                                          ColorModel dstColorModel,
                                          RenderingHints hints) {
        if (isRgbColorModel(srcColorModel) && isRgbColorModel(dstColorModel)) {
            return new BlendingRgbContext(this);
        } else if (isBgrColorModel(srcColorModel) && isBgrColorModel(dstColorModel)) {
            return new BlendingBgrContext(this);
        }

        throw new RasterFormatException("Incompatible color models");
    }

    private static abstract class BlendingContext implements CompositeContext {
        protected final Blender blender;
        protected final BlendComposite composite;

        private BlendingContext(BlendComposite composite) {
            this.composite = composite;
            this.blender = Blender.getBlenderFor(composite);
        }

        public void dispose() {
        }
    }

    private static class BlendingRgbContext extends BlendingContext {
        private BlendingRgbContext(BlendComposite composite) {
            super(composite);
        }

        public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
            int width = Math.min(src.getWidth(), dstIn.getWidth());
            int height = Math.min(src.getHeight(), dstIn.getHeight());

            float alpha = composite.getAlpha();

            int[] result = new int[4];
            int[] srcPixel = new int[4];
            int[] dstPixel = new int[4];
            int[] srcPixels = new int[width];
            int[] dstPixels = new int[width];

            for (int y = 0; y < height; y++) {
                src.getDataElements(0, y, width, 1, srcPixels);
                dstIn.getDataElements(0, y, width, 1, dstPixels);
                for (int x = 0; x < width; x++) {
                    // pixels are stored as INT_ARGB
                    // our arrays are [R, G, B, A]
                    int pixel = srcPixels[x];
                    srcPixel[0] = (pixel >> 16) & 0xFF;
                    srcPixel[1] = (pixel >>  8) & 0xFF;
                    srcPixel[2] = (pixel      ) & 0xFF;
                    srcPixel[3] = (pixel >> 24) & 0xFF;

                    pixel = dstPixels[x];
                    dstPixel[0] = (pixel >> 16) & 0xFF;
                    dstPixel[1] = (pixel >>  8) & 0xFF;
                    dstPixel[2] = (pixel      ) & 0xFF;
                    dstPixel[3] = (pixel >> 24) & 0xFF;

                    blender.blend(srcPixel, dstPixel, result);

                    // mixes the result with the opacity
                    dstPixels[x] = ((int) (dstPixel[3] + (result[3] - dstPixel[3]) * alpha) & 0xFF) << 24 |
                                   ((int) (dstPixel[0] + (result[0] - dstPixel[0]) * alpha) & 0xFF) << 16 |
                                   ((int) (dstPixel[1] + (result[1] - dstPixel[1]) * alpha) & 0xFF) <<  8 |
                                    (int) (dstPixel[2] + (result[2] - dstPixel[2]) * alpha) & 0xFF;
                }
                dstOut.setDataElements(0, y, width, 1, dstPixels);
            }
        }
    }

    private static class BlendingBgrContext extends BlendingContext {
        private BlendingBgrContext(BlendComposite composite) {
            super(composite);
        }

        public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
            int width = Math.min(src.getWidth(), dstIn.getWidth());
            int height = Math.min(src.getHeight(), dstIn.getHeight());

            float alpha = composite.getAlpha();

            int[] result = new int[4];
            int[] srcPixel = new int[4];
            int[] dstPixel = new int[4];
            int[] srcPixels = new int[width];
            int[] dstPixels = new int[width];

            for (int y = 0; y < height; y++) {
                src.getDataElements(0, y, width, 1, srcPixels);
                dstIn.getDataElements(0, y, width, 1, dstPixels);
                for (int x = 0; x < width; x++) {
                    // pixels are stored as INT_ABGR
                    // our arrays are [R, G, B, A]
                    int pixel = srcPixels[x];
                    srcPixel[0] = (pixel      ) & 0xFF;
                    srcPixel[1] = (pixel >>  8) & 0xFF;
                    srcPixel[2] = (pixel >> 16) & 0xFF;
                    srcPixel[3] = (pixel >> 24) & 0xFF;

                    pixel = dstPixels[x];
                    dstPixel[0] = (pixel      ) & 0xFF;
                    dstPixel[1] = (pixel >>  8) & 0xFF;
                    dstPixel[2] = (pixel >> 16) & 0xFF;
                    dstPixel[3] = (pixel >> 24) & 0xFF;

                    blender.blend(srcPixel, dstPixel, result);

                    // mixes the result with the opacity
                    dstPixels[x] = ((int) (dstPixel[3] + (result[3] - dstPixel[3]) * alpha) & 0xFF) << 24 |
                                   ((int) (dstPixel[0] + (result[0] - dstPixel[0]) * alpha) & 0xFF)       |
                                   ((int) (dstPixel[1] + (result[1] - dstPixel[1]) * alpha) & 0xFF) <<  8 |
                                   ((int) (dstPixel[2] + (result[2] - dstPixel[2]) * alpha) & 0xFF) << 16;
                }
                dstOut.setDataElements(0, y, width, 1, dstPixels);
            }
        }
    }

    private static abstract class Blender {
        public abstract void blend(int[] src, int[] dst, int[] result);

        public static Blender getBlenderFor(BlendComposite composite) {
            switch (composite.getMode()) {
                case ADD:
                    return new Blender() {
                        @Override
                        public void blend(int[] src, int[] dst, int[] result) {
                            result[0] = Math.min(255, src[0] + dst[0]);
                            result[1] = Math.min(255, src[1] + dst[1]);
                            result[2] = Math.min(255, src[2] + dst[2]);
                            result[3] = Math.min(255, src[3] + dst[3]);
                        }
                    };
                case AVERAGE:
                    return new Blender() {
                        @Override
                        public void blend(int[] src, int[] dst, int[] result) {
                            result[0] = (src[0] + dst[0]) >> 1;
                            result[1] = (src[1] + dst[1]) >> 1;
                            result[2] = (src[2] + dst[2]) >> 1;
                            result[3] = Math.min(255, src[3] + dst[3] - (src[3] * dst[3]) / 255);
                        }
                    };
                case BLUE:
                    return new Blender() {
                        @Override
                        public void blend(int[] src, int[] dst, int[] result) {
                            result[0] = dst[0];
                            result[1] = src[1];
                            result[2] = dst[2];
                            result[3] = Math.min(255, src[3] + dst[3] - (src[3] * dst[3]) / 255);
                        }
                    };
                case COLOR:
                    return new Blender() {
                        @Override
                        public void blend(int[] src, int[] dst, int[] result) {
                            float[] srcHSL = new float[3];
                            ColorUtilities.RGBtoHSL(src[0], src[1], src[2], srcHSL);
                            float[] dstHSL = new float[3];
                            ColorUtilities.RGBtoHSL(dst[0], dst[1], dst[2], dstHSL);

                            ColorUtilities.HSLtoRGB(srcHSL[0], srcHSL[1], dstHSL[2], result);
                            result[3] = Math.min(255, src[3] + dst[3] - (src[3] * dst[3]) / 255);
                        }
                    };
                case COLOR_BURN:
                    return new Blender() {
                        @Override
                        public void blend(int[] src, int[] dst, int[] result) {
                            result[0] = src[0] == 0 ? 0 :
                                Math.max(0, 255 - (((255 - dst[0]) << 8) / src[0]));
                            result[1] = src[1] == 0 ? 0 :
                                Math.max(0, 255 - (((255 - dst[1]) << 8) / src[1]));
                            result[2] = src[2] == 0 ? 0 :
                                Math.max(0, 255 - (((255 - dst[2]) << 8) / src[2]));
                            result[3] = Math.min(255, src[3] + dst[3] - (src[3] * dst[3]) / 255);
                        }
                    };
                case COLOR_DODGE:
                    return new Blender() {
                        @Override
                        public void blend(int[] src, int[] dst, int[] result) {
                            result[0] = src[0] == 255 ? 255 :
                                Math.min((dst[0] << 8) / (255 - src[0]), 255);
                            result[1] = src[1] == 255 ? 255 :
                                Math.min((dst[1] << 8) / (255 - src[1]), 255);
                            result[2] = src[2] == 255 ? 255 :
                                Math.min((dst[2] << 8) / (255 - src[2]), 255);
                            result[3] = Math.min(255, src[3] + dst[3] - (src[3] * dst[3]) / 255);
                        }
                    };
                case DARKEN:
                    return new Blender() {
                        @Override
                        public void blend(int[] src, int[] dst, int[] result) {
                            result[0] = Math.min(src[0], dst[0]);
                            result[1] = Math.min(src[1], dst[1]);
                            result[2] = Math.min(src[2], dst[2]);
                            result[3] = Math.min(255, src[3] + dst[3] - (src[3] * dst[3]) / 255);
                        }
                    };
                case DIFFERENCE:
                    return new Blender() {
                        @Override
                        public void blend(int[] src, int[] dst, int[] result) {
                            result[0] = Math.abs(dst[0] - src[0]);
                            result[1] = Math.abs(dst[1] - src[1]);
                            result[2] = Math.abs(dst[2] - src[2]);
                            result[3] = Math.min(255, src[3] + dst[3] - (src[3] * dst[3]) / 255);
                        }
                    };
                case EXCLUSION:
                    return new Blender() {
                        @Override
                        public void blend(int[] src, int[] dst, int[] result) {
                            result[0] = dst[0] + src[0] - (dst[0] * src[0] >> 7);
                            result[1] = dst[1] + src[1] - (dst[1] * src[1] >> 7);
                            result[2] = dst[2] + src[2] - (dst[2] * src[2] >> 7);
                            result[3] = Math.min(255, src[3] + dst[3] - (src[3] * dst[3]) / 255);
                        }
                    };
                case FREEZE:
                    return new Blender() {
                        @Override
                        public void blend(int[] src, int[] dst, int[] result) {
                            result[0] = src[0] == 0 ? 0 :
                                Math.max(0, 255 - (255 - dst[0]) * (255 - dst[0]) / src[0]);
                            result[1] = src[1] == 0 ? 0 :
                                Math.max(0, 255 - (255 - dst[1]) * (255 - dst[1]) / src[1]);
                            result[2] = src[2] == 0 ? 0 :
                                Math.max(0, 255 - (255 - dst[2]) * (255 - dst[2]) / src[2]);
                            result[3] = Math.min(255, src[3] + dst[3] - (src[3] * dst[3]) / 255);
                        }
                    };
                case GLOW:
                    return new Blender() {
                        @Override
                        public void blend(int[] src, int[] dst, int[] result) {
                            result[0] = dst[0] == 255 ? 255 :
                                Math.min(255, src[0] * src[0] / (255 - dst[0]));
                            result[1] = dst[1] == 255 ? 255 :
                                Math.min(255, src[1] * src[1] / (255 - dst[1]));
                            result[2] = dst[2] == 255 ? 255 :
                                Math.min(255, src[2] * src[2] / (255 - dst[2]));
                            result[3] = Math.min(255, src[3] + dst[3] - (src[3] * dst[3]) / 255);
                        }
                    };
                case GREEN:
                    return new Blender() {
                        @Override
                        public void blend(int[] src, int[] dst, int[] result) {
                            result[0] = dst[0];
                            result[1] = dst[1];
                            result[2] = src[2];
                            result[3] = Math.min(255, src[3] + dst[3] - (src[3] * dst[3]) / 255);
                        }
                    };
                case HARD_LIGHT:
                    return new Blender() {
                        @Override
                        public void blend(int[] src, int[] dst, int[] result) {
                            result[0] = src[0] < 128 ? dst[0] * src[0] >> 7 :
                                255 - ((255 - src[0]) * (255 - dst[0]) >> 7);
                            result[1] = src[1] < 128 ? dst[1] * src[1] >> 7 :
                                255 - ((255 - src[1]) * (255 - dst[1]) >> 7);
                            result[2] = src[2] < 128 ? dst[2] * src[2] >> 7 :
                                255 - ((255 - src[2]) * (255 - dst[2]) >> 7);
                            result[3] = Math.min(255, src[3] + dst[3] - (src[3] * dst[3]) / 255);
                        }
                    };
                case HEAT:
                    return new Blender() {
                        @Override
                        public void blend(int[] src, int[] dst, int[] result) {
                            result[0] = dst[0] == 0 ? 0 :
                                Math.max(0, 255 - (255 - src[0]) * (255 - src[0]) / dst[0]);
                            result[1] = dst[1] == 0 ? 0 :
                                Math.max(0, 255 - (255 - src[1]) * (255 - src[1]) / dst[1]);
                            result[2] = dst[2] == 0 ? 0 :
                                Math.max(0, 255 - (255 - src[2]) * (255 - src[2]) / dst[2]);
                            result[3] = Math.min(255, src[3] + dst[3] - (src[3] * dst[3]) / 255);
                        }
                    };
                case HUE:
                    return new Blender() {
                        @Override
                        public void blend(int[] src, int[] dst, int[] result) {
                            float[] srcHSL = new float[3];
                            ColorUtilities.RGBtoHSL(src[0], src[1], src[2], srcHSL);
                            float[] dstHSL = new float[3];
                            ColorUtilities.RGBtoHSL(dst[0], dst[1], dst[2], dstHSL);

                            ColorUtilities.HSLtoRGB(srcHSL[0], dstHSL[1], dstHSL[2], result);
                            result[3] = Math.min(255, src[3] + dst[3] - (src[3] * dst[3]) / 255);
                        }
                    };
                case INVERSE_COLOR_BURN:
                    return new Blender() {
                        @Override
                        public void blend(int[] src, int[] dst, int[] result) {
                            result[0] = dst[0] == 0 ? 0 :
                                Math.max(0, 255 - (((255 - src[0]) << 8) / dst[0]));
                            result[1] = dst[1] == 0 ? 0 :
                                Math.max(0, 255 - (((255 - src[1]) << 8) / dst[1]));
                            result[2] = dst[2] == 0 ? 0 :
                                Math.max(0, 255 - (((255 - src[2]) << 8) / dst[2]));
                            result[3] = Math.min(255, src[3] + dst[3] - (src[3] * dst[3]) / 255);
                        }
                    };
                case INVERSE_COLOR_DODGE:
                    return new Blender() {
                        @Override
                        public void blend(int[] src, int[] dst, int[] result) {
                            result[0] = dst[0] == 255 ? 255 :
                                Math.min((src[0] << 8) / (255 - dst[0]), 255);
                            result[1] = dst[1] == 255 ? 255 :
                                Math.min((src[1] << 8) / (255 - dst[1]), 255);
                            result[2] = dst[2] == 255 ? 255 :
                                Math.min((src[2] << 8) / (255 - dst[2]), 255);
                            result[3] = Math.min(255, src[3] + dst[3] - (src[3] * dst[3]) / 255);
                        }
                    };
                case LIGHTEN:
                    return new Blender() {
                        @Override
                        public void blend(int[] src, int[] dst, int[] result) {
                            result[0] = Math.max(src[0], dst[0]);
                            result[1] = Math.max(src[1], dst[1]);
                            result[2] = Math.max(src[2], dst[2]);
                            result[3] = Math.min(255, src[3] + dst[3] - (src[3] * dst[3]) / 255);
                        }
                    };
                case LUMINOSITY:
                    return new Blender() {
                        @Override
                        public void blend(int[] src, int[] dst, int[] result) {
                            float[] srcHSL = new float[3];
                            ColorUtilities.RGBtoHSL(src[0], src[1], src[2], srcHSL);
                            float[] dstHSL = new float[3];
                            ColorUtilities.RGBtoHSL(dst[0], dst[1], dst[2], dstHSL);

                            ColorUtilities.HSLtoRGB(dstHSL[0], dstHSL[1], srcHSL[2], result);
                            result[3] = Math.min(255, src[3] + dst[3] - (src[3] * dst[3]) / 255);
                        }
                    };
                case MULTIPLY:
                    return new Blender() {
                        @Override
                        public void blend(int[] src, int[] dst, int[] result) {
                            result[0] = (src[0] * dst[0]) >> 8;
                            result[1] = (src[1] * dst[1]) >> 8;
                            result[2] = (src[2] * dst[2]) >> 8;
                            result[3] = Math.min(255, src[3] + dst[3] - (src[3] * dst[3]) / 255);
                        }
                    };
                case NEGATION:
                    return new Blender() {
                        @Override
                        public void blend(int[] src, int[] dst, int[] result) {
                            result[0] = 255 - Math.abs(255 - dst[0] - src[0]);
                            result[1] = 255 - Math.abs(255 - dst[1] - src[1]);
                            result[2] = 255 - Math.abs(255 - dst[2] - src[2]);
                            result[3] = Math.min(255, src[3] + dst[3] - (src[3] * dst[3]) / 255);
                        }
                    };
                case OVERLAY:
                    return new Blender() {
                        @Override
                        public void blend(int[] src, int[] dst, int[] result) {
                            result[0] = dst[0] < 128 ? dst[0] * src[0] >> 7 :
                                255 - ((255 - dst[0]) * (255 - src[0]) >> 7);
                            result[1] = dst[1] < 128 ? dst[1] * src[1] >> 7 :
                                255 - ((255 - dst[1]) * (255 - src[1]) >> 7);
                            result[2] = dst[2] < 128 ? dst[2] * src[2] >> 7 :
                                255 - ((255 - dst[2]) * (255 - src[2]) >> 7);
                            result[3] = Math.min(255, src[3] + dst[3] - (src[3] * dst[3]) / 255);
                        }
                    };
                case RED:
                    return new Blender() {
                        @Override
                        public void blend(int[] src, int[] dst, int[] result) {
                            result[0] = src[0];
                            result[1] = dst[1];
                            result[2] = dst[2];
                            result[3] = Math.min(255, src[3] + dst[3] - (src[3] * dst[3]) / 255);
                        }
                    };
                case REFLECT:
                    return new Blender() {
                        @Override
                        public void blend(int[] src, int[] dst, int[] result) {
                            result[0] = src[0] == 255 ? 255 :
                                Math.min(255, dst[0] * dst[0] / (255 - src[0]));
                            result[1] = src[1] == 255 ? 255 :
                                Math.min(255, dst[1] * dst[1] / (255 - src[1]));
                            result[2] = src[2] == 255 ? 255 :
                                Math.min(255, dst[2] * dst[2] / (255 - src[2]));
                            result[3] = Math.min(255, src[3] + dst[3] - (src[3] * dst[3]) / 255);
                        }
                    };
                case SATURATION:
                    return new Blender() {
                        @Override
                        public void blend(int[] src, int[] dst, int[] result) {
                            float[] srcHSL = new float[3];
                            ColorUtilities.RGBtoHSL(src[0], src[1], src[2], srcHSL);
                            float[] dstHSL = new float[3];
                            ColorUtilities.RGBtoHSL(dst[0], dst[1], dst[2], dstHSL);

                            ColorUtilities.HSLtoRGB(dstHSL[0], srcHSL[1], dstHSL[2], result);
                            result[3] = Math.min(255, src[3] + dst[3] - (src[3] * dst[3]) / 255);
                        }
                    };
                case SCREEN:
                    return new Blender() {
                        @Override
                        public void blend(int[] src, int[] dst, int[] result) {
                            result[0] = 255 - ((255 - src[0]) * (255 - dst[0]) >> 8);
                            result[1] = 255 - ((255 - src[1]) * (255 - dst[1]) >> 8);
                            result[2] = 255 - ((255 - src[2]) * (255 - dst[2]) >> 8);
                            result[3] = Math.min(255, src[3] + dst[3] - (src[3] * dst[3]) / 255);
                        }
                    };
                case SOFT_BURN:
                    return new Blender() {
                        @Override
                        public void blend(int[] src, int[] dst, int[] result) {
                            result[0] = dst[0] + src[0] < 256 ?
                                (dst[0] == 255 ? 255 :
                                 Math.min(255, (src[0] << 7) / (255 - dst[0]))) :
                                                                                Math.max(0, 255 - (((255 - dst[0]) << 7) / src[0]));
                            result[1] = dst[1] + src[1] < 256 ?
                                (dst[1] == 255 ? 255 :
                                 Math.min(255, (src[1] << 7) / (255 - dst[1]))) :
                                                                                Math.max(0, 255 - (((255 - dst[1]) << 7) / src[1]));
                            result[2] = dst[2] + src[2] < 256 ?
                                (dst[2] == 255 ? 255 :
                                 Math.min(255, (src[2] << 7) / (255 - dst[2]))) :
                                                                                Math.max(0, 255 - (((255 - dst[2]) << 7) / src[2]));
                            result[3] = Math.min(255, src[3] + dst[3] - (src[3] * dst[3]) / 255);
                        }
                    };
                case SOFT_DODGE:
                    return new Blender() {
                        @Override
                        public void blend(int[] src, int[] dst, int[] result) {
                            result[0] = dst[0] + src[0] < 256 ?
                                (src[0] == 255 ? 255 :
                                 Math.min(255, (dst[0] << 7) / (255 - src[0]))) :
                                    Math.max(0, 255 - (((255 - src[0]) << 7) / dst[0]));
                            result[1] = dst[1] + src[1] < 256 ?
                                (src[1] == 255 ? 255 :
                                 Math.min(255, (dst[1] << 7) / (255 - src[1]))) :
                                    Math.max(0, 255 - (((255 - src[1]) << 7) / dst[1]));
                            result[2] = dst[2] + src[2] < 256 ?
                                (src[2] == 255 ? 255 :
                                 Math.min(255, (dst[2] << 7) / (255 - src[2]))) :
                                    Math.max(0, 255 - (((255 - src[2]) << 7) / dst[2]));
                            result[3] = Math.min(255, src[3] + dst[3] - (src[3] * dst[3]) / 255);
                        }
                    };
                case SOFT_LIGHT:
                    return new Blender() {
                        @Override
                        public void blend(int[] src, int[] dst, int[] result) {
                            int mRed = src[0] * dst[0] / 255;
                            int mGreen = src[1] * dst[1] / 255;
                            int mBlue = src[2] * dst[2] / 255;
                            result[0] = mRed + src[0] * (255 - ((255 - src[0]) * (255 - dst[0]) / 255) - mRed) / 255;
                            result[1] = mGreen + src[1] * (255 - ((255 - src[1]) * (255 - dst[1]) / 255) - mGreen) / 255;
                            result[2] = mBlue + src[2] * (255 - ((255 - src[2]) * (255 - dst[2]) / 255) - mBlue) / 255;
                            result[3] = Math.min(255, src[3] + dst[3] - (src[3] * dst[3]) / 255);
                        }
                    };
                case STAMP:
                    return new Blender() {
                        @Override
                        public void blend(int[] src, int[] dst, int[] result) {
                            result[0] = Math.max(0, Math.min(255, dst[0] + 2 * src[0] - 256));
                            result[1] = Math.max(0, Math.min(255, dst[1] + 2 * src[1] - 256));
                            result[2] = Math.max(0, Math.min(255, dst[2] + 2 * src[2] - 256));
                            result[3] = Math.min(255, src[3] + dst[3] - (src[3] * dst[3]) / 255);
                        }
                    };
                case SUBTRACT:
                    return new Blender() {
                        @Override
                        public void blend(int[] src, int[] dst, int[] result) {
                            result[0] = Math.max(0, src[0] + dst[0] - 256);
                            result[1] = Math.max(0, src[1] + dst[1] - 256);
                            result[2] = Math.max(0, src[2] + dst[2] - 256);
                            result[3] = Math.min(255, src[3] + dst[3] - (src[3] * dst[3]) / 255);
                        }
                    };
            }
            throw new IllegalArgumentException("Blender not implemented for " +
                                               composite.getMode().name());
        }
    }
}
