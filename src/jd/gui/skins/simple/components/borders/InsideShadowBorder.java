package jd.gui.skins.simple.components.borders;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;

import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;

import com.jtattoo.plaf.AbstractLookAndFeel;
import com.jtattoo.plaf.ColorHelper;

public class InsideShadowBorder extends AbstractBorder {


    private Insets insets;

    public InsideShadowBorder(int top, int left, int bottom, int right) {
        this.insets = new Insets(top, left, bottom, right);
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
        Graphics2D g2D = (Graphics2D) g;
        Composite composite = g2D.getComposite();

        Color hiFrameColor = null;
        Color loFrameColor = null;
        Color hiBackColor = null;
        Color loBackColor = null;
     
        Color[] colors;
        if (UIManager.getLookAndFeel() instanceof AbstractLookAndFeel) {
            colors = AbstractLookAndFeel.getTheme().getColHeaderColors();           
            hiFrameColor = AbstractLookAndFeel.getControlHighlight();
            loFrameColor = AbstractLookAndFeel.getControlDarkShadow();

         
        } else {
            hiFrameColor = Color.white;
            loFrameColor = Color.gray;
            hiBackColor = ColorHelper.brighter(c.getBackground(), 30.0f);
            loBackColor = ColorHelper.darker(c.getBackground(), 10.0f);
        
            colors = ColorHelper.createColorArr(hiBackColor, loBackColor, 48);
        }

       
//        g.setColor(loFrameColor);
//        g.drawRect(x, y, w - shadowSize - 1, h - shadowSize - 1);
//        g.setColor(hiFrameColor);
//        g.drawRect(x + 1, y + 1, w - shadowSize - 3, h - shadowSize - 3);
//
//        g.setColor(loFrameColor);
//        g.drawLine(x + 2, y + getBorderInsets(c).top - innerSpace - 1, x + w - shadowSize - 1, y + getBorderInsets(c).top - innerSpace - 1);

        // paint the shadow
        
        
       
            g2D.setColor(new Color(0, 16, 0));
            float alphaValue = 0.4f;
            for (int i = 0; i < insets.top; i++) {
                AlphaComposite alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaValue);
                g2D.setComposite(alpha);
                g.drawLine(x+i, y + i, x + w - i*2, y + i );

                alphaValue -= (alphaValue / 2);
            }
        

        g2D.setComposite(composite);

        // JTattooUtilities.fillHorGradient(g, , 0, 0, getWidth(), getHeight());

//        JTattooUtilities.fillHorGradient(g, colors, x + 2, y + 2, w - shadowSize - 4, titleHeight);

//        paintText(c, g, x, y, w, h, textColor, null);
    }

   
    public Insets getBorderInsets(Component c) {
       
        return insets;
    }
}
