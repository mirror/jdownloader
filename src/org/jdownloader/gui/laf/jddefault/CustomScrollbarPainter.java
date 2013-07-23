package org.jdownloader.gui.laf.jddefault;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;

import javax.swing.plaf.synth.SynthContext;

import jd.gui.swing.laf.LAFOptions;

import de.javasoft.plaf.synthetica.SyntheticaSimple2DLookAndFeel;
import de.javasoft.plaf.synthetica.simple2D.ScrollBarPainter;

public class CustomScrollbarPainter extends ScrollBarPainter {

    private Color color;
    private Color colorMouseOver;

    private Shape createThumbShape(float f, float f1, float f2, float f3, float f4) {
        return new java.awt.geom.RoundRectangle2D.Float(f, f1, f2, f3, f4, f4);
    }

    public CustomScrollbarPainter() {
        super();

    }

    // public void paintScrollBarThumbBorder(SynthContext synthcontext, Graphics g, int i, int j, int k, int l, int i1) {
    // if ((float) l <= getScale() * 2.0F + 1.0F || (float) k <= getScale() * 2.0F + 1.0F) {
    // return;
    // } else {
    // Graphics2D graphics2d = prepareGraphics2D(synthcontext, g, i, j, true);
    // Shape shape = createThumbShape(0.0F, 0.0F, calcRelativeLength(graphics2d, k, 0.0F), calcRelativeLength(graphics2d, l, 0.0F),
    // scaleArc(4F));
    // float f = i1 != 0 ? calcRelativeGradientPos(graphics2d, 0.0F, 0.0F) : 0.0F;
    // float f1 = i1 != 0 ? 0.0F : calcRelativeGradientPos(graphics2d, 0.0F, 0.0F);
    // float f2 = i1 != 0 ? calcRelativeGradientPos(graphics2d, k - 1, 0.0F) : 0.0F;
    // float f3 = i1 != 0 ? 0.0F : calcRelativeGradientPos(graphics2d, l - 1, 0.0F);
    // graphics2d.setPaint(createLinearGradientPaint(f, f1, f2, f3, new float[] { 0.0F, 1.0F }, new Color[] { new Color(0xD7E7F0), new
    // Color(0xD7E7F0) }));
    // graphics2d.draw(shape);
    // restoreGraphics2D(graphics2d);
    // return;
    // }
    // }

    public void paintScrollBarThumbBackground(SynthContext synthcontext, Graphics g, int i, int j, int k, int l, int i1) {
        if ((float) l <= getScale() * 2.0F + 1.0F || (float) k <= getScale() * 2.0F + 1.0F) return;
        Graphics2D graphics2d = prepareGraphics2D(synthcontext, g, i, j, true);
        Shape shape = createThumbShape(0.0F, 0.0F, calcRelativeLength(graphics2d, k, 0.0F), calcRelativeLength(graphics2d, l, 0.0F), scaleArc(4F));
        int j1 = 1;
        float f = i1 != 0 ? calcRelativeGradientPos(graphics2d, 0.0F, j1) : 0.0F;
        float f1 = i1 != 0 ? 0.0F : calcRelativeGradientPos(graphics2d, 0.0F, j1);
        float f2 = i1 != 0 ? calcRelativeGradientPos(graphics2d, k - 1, -j1) : 0.0F;
        float f3 = i1 != 0 ? 0.0F : calcRelativeGradientPos(graphics2d, l - 1, -j1);
        graphics2d.setPaint(createLinearGradientPaint(f, f1, f2, f3, new float[] { 0.0F, 1.0F }, new Color[] { getColor(), getColor() }));
        graphics2d.fill(subtractStroke(graphics2d, shape));
        if ((synthcontext.getComponentState() & 2) > 0) {
            graphics2d.setPaint(SyntheticaSimple2DLookAndFeel.getHoverColor());
            graphics2d.setPaint(createLinearGradientPaint(f, f1, f2, f3, new float[] { 0.0F, 1.0F }, new Color[] { getColorMouseOver(), getColorMouseOver() }));
            graphics2d.fill(subtractStroke(graphics2d, shape));
        }

        restoreGraphics2D(graphics2d);
    }

    private Color getColorMouseOver() {
        // no synth because we are in the EDT anyway
        if (colorMouseOver != null) return colorMouseOver;

        colorMouseOver = LAFOptions.getInstance().getColorForScrollbarsMouseOverState();
        return colorMouseOver;
    }

    private Color getColor() {
        // no synth because we are in the EDT anyway
        if (color != null) return color;

        color = LAFOptions.getInstance().getColorForScrollbarsNormalState();

        return color;
    }
}
