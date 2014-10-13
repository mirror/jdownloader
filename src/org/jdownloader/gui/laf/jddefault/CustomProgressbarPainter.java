package org.jdownloader.gui.laf.jddefault;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JProgressBar;
import javax.swing.plaf.synth.SynthContext;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.jdownloader.updatev2.gui.LAFOptions;

import de.javasoft.plaf.synthetica.simple2D.ProgressBarPainter;

public class CustomProgressbarPainter extends ProgressBarPainter {

    private Color[] colorArray;

    public CustomProgressbarPainter() {

    }

    private Shape createShape(float paramFloat1, float paramFloat2, float paramFloat3, float paramFloat4, float paramFloat5) {
        return new RoundRectangle2D.Float(0.0F, 0.0F, paramFloat3, paramFloat4, paramFloat5, paramFloat5);
    }

    public void paintProgressBarForeground(SynthContext paramSynthContext, Graphics paramGraphics, int x, int y, int width, int height, int direction) {
        JProgressBar bar = (JProgressBar) paramSynthContext.getComponent();
        Insets barInsets = bar.getInsets();
        int paintableWidth = bar.getWidth() - barInsets.left - barInsets.right;
        int paintableHeight = bar.getHeight() - barInsets.top - barInsets.bottom;

        if (!bar.isIndeterminate()) {
            boolean l2r = bar.getComponentOrientation().isLeftToRight();
            x = l2r ? barInsets.left : paintableWidth + barInsets.left - width;
            y = direction == 0 ? barInsets.top : paintableHeight - height + barInsets.top;
            width = direction == 0 ? width : paintableWidth;
            height = direction == 0 ? paintableHeight : height;
        }

        if ((width <= 1) || (height <= 1)) {
            return;
        }
        Graphics2D localGraphics2D = prepareGraphics2D(paramSynthContext, paramGraphics, x, y, true);
        Shape localShape = createShape(0.0F, 0.0F, calcRelativeLength(localGraphics2D, width, 0.0F), calcRelativeLength(localGraphics2D, height, 0.0F), scaleArc(4.0F));
        if ((paramSynthContext.getComponentState() & 0x8) > 0) {
            localGraphics2D.setPaint(new Color(520093696, true));
        } else {
            float f = 1.0F;
            if (direction == 0) {
                localGraphics2D.setPaint(createLinearGradientPaint(0.0F, calcRelativeGradientPos(localGraphics2D, 0.0F, f), 0.0F, calcRelativeGradientPos(localGraphics2D, height - 1, -f), new float[] { 0.0F, 0.25F, 0.5F, 0.75F, 1.0F }, getColorArray()));

            } else {
                localGraphics2D.setPaint(createLinearGradientPaint(calcRelativeGradientPos(localGraphics2D, 0.0F, f), 0.0F, calcRelativeGradientPos(localGraphics2D, width - 1, -f), 0.0F, new float[] { 0.0F, 0.25F, 0.5F, 0.75F, 1.0F }, getColorArray()));
            }
        }

        localGraphics2D.fill(subtractStroke(localGraphics2D, localShape));
        restoreGraphics2D(localGraphics2D);
    }

    private Color[] getColorArray() {
        Color[] loc = colorArray;
        if (loc != null) {
            return loc;
        }
        colorArray = new Color[5];
        colorArray[0] = LAFOptions.getInstance().getColorForProgressbar1();
        colorArray[1] = LAFOptions.getInstance().getColorForProgressbar2();
        colorArray[2] = LAFOptions.getInstance().getColorForProgressbar3();
        colorArray[3] = LAFOptions.getInstance().getColorForProgressbar4();
        colorArray[4] = LAFOptions.getInstance().getColorForProgressbar5();
        LAFOptions.getInstance().getCfg()._getStorageHandler().getEventSender().addListener(new GenericConfigEventListener<Object>() {

            @Override
            public void onConfigValueModified(KeyHandler<Object> keyHandler, Object newValue) {
                colorArray = null;
            }

            @Override
            public void onConfigValidatorError(KeyHandler<Object> keyHandler, Object invalidValue, ValidationException validateException) {
            }
        });
        return colorArray;
    }

    // public void paintProgressBarBackground(SynthContext paramSynthContext, Graphics paramGraphics, int paramInt1, int paramInt2, int
    // paramInt3, int paramInt4) {
    // JProgressBar localJProgressBar = (JProgressBar) paramSynthContext.getComponent();
    // int i = localJProgressBar.getOrientation();
    //
    // Insets localInsets1 = paramSynthContext.getStyle().getInsets(paramSynthContext, null);
    //
    // Insets localInsets2 = localJProgressBar.getInsets();
    //
    // paramInt3 -= localInsets2.left + localInsets2.right - localInsets1.left - localInsets1.right;
    // paramInt4 -= localInsets2.top + localInsets2.bottom - localInsets1.top - localInsets1.bottom;
    // paramInt1 += localInsets2.left - localInsets1.left;
    // paramInt2 += localInsets2.top - localInsets1.top;
    //
    // Graphics2D localGraphics2D = prepareGraphics2D(paramSynthContext, paramGraphics, paramInt1, paramInt2, true);
    // Shape localShape = createShape(0.0F, 0.0F, calcRelativeLength(localGraphics2D, paramInt3, 0.0F), calcRelativeLength(localGraphics2D,
    // paramInt4, 0.0F), scaleArc(4.0F));
    // float f = 1.0F;
    // if (i == 0) {
    // localGraphics2D.setPaint(createLinearGradientPaint(0.0F, calcRelativeGradientPos(localGraphics2D, 0.0F, f), 0.0F,
    // calcRelativeGradientPos(localGraphics2D, paramInt4 - 1.0F, -f), new float[] { 0.0F, 0.25F, 0.5F, 0.75F, 1.0F }, new Color[] {
    // Color.WHITE, COLOR_F7F7F7, COLOR_ECECEC, COLOR_F7F7F7, Color.WHITE }));
    //
    // } else {
    // localGraphics2D.setPaint(createLinearGradientPaint(calcRelativeGradientPos(localGraphics2D, 0.0F, f), 0.0F,
    // calcRelativeGradientPos(localGraphics2D, paramInt3 - 1.0F, -f), 0.0F, new float[] { 0.0F, 0.25F, 0.5F, 0.75F, 1.0F }, new Color[] {
    // Color.WHITE, COLOR_F7F7F7, COLOR_ECECEC, COLOR_F7F7F7, Color.WHITE }));
    // }
    //
    // localGraphics2D.fill(subtractStroke(localGraphics2D, localShape));
    // restoreGraphics2D(localGraphics2D);
    //
    // if ((localJProgressBar.getBorder() != null) && (!localJProgressBar.getBorder().getClass().getName().endsWith(".synth.SynthBorder")))
    // {
    // paintProgressBarBorder(paramSynthContext, localGraphics2D, paramInt1, paramInt2, paramInt3, paramInt4);
    // }
    // }
}
