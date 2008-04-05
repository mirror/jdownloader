/*
 * $Id: TextPainter.java,v 1.12 2007/03/26 21:25:20 rbair Exp $
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.jdesktop.swingx.painter;

import org.jdesktop.swingx.painter.effects.AreaEffect;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.font.GlyphVector;

/**
 * A painter which draws text. If the font, text, and paint are not provided they will be
 * obtained from the object being painted if it is a Swing text component.
 *
 * @author rbair
 */
public class TextPainter<T> extends AbstractAreaPainter<T> {
    private String text = "";
    private Font font = null;
    
    /** Creates a new instance of TextPainter */
    public TextPainter() {
        this("");
    }
    
    /**
     * Create a new TextPainter which will paint the specified text
     * @param text the text to paint
     */
    public TextPainter(String text) {
        this(text, null, null);
    }
    
    /**
     * Create a new TextPainter which will paint the specified text with the specified font.
     * @param text the text to paint
     * @param font the font to paint the text with
     */
    public TextPainter(String text, Font font) {
        this(text, font, null);
    }
    
    /**
     * Create a new TextPainter which will paint the specified text with the specified paint.
     * @param text the text to paint
     * @param paint the paint to paint with
     */
    public TextPainter(String text, Paint paint) {
        this(text, null, paint);
    }
    
    /**
     * Create a new TextPainter which will paint the specified text with the specified font and paint.
     * @param text the text to paint
     * @param font the font to paint the text with
     * @param paint the paint to paint with
     */
    public TextPainter(String text, Font font, Paint paint) {
        this.text = text;
        this.font = font;
        setFillPaint(paint);
    }
    
    /**
     * Set the font (and font size and style) to be used when drawing the text
     * @param f the new font
     */
    public void setFont(Font f) {
        Font old = getFont();
        this.font = f;
        setDirty(true);
        firePropertyChange("font", old, getFont());
    }
    
    /**
     * gets the font (and font size and style) to be used when drawing the text
     * @return the current font
     */
    public Font getFont() {
        return font;
    }
    
    /**
     * Sets the text to draw
     * @param text the text to draw
     */
    public void setText(String text) {
        String old = getText();
        this.text = text == null ? "" : text;
        setDirty(true);
        firePropertyChange("text", old, getText());
    }
    
    /**
     * gets the text currently used to draw
     * @return the text to be drawn
     */
    public String getText() {
        return text;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPaint(Graphics2D g, T component, int width, int height) {
        Font font = calculateFont(component);
        if (font != null) {
            g.setFont(font);
        }
        
        Paint paint = getFillPaint();
        if(paint == null) {
            if(component instanceof JComponent) {
                paint = ((JComponent)component).getForeground();
            }
        }
        
        String text = calculateText(component);
        
        // get the font metrics
        FontMetrics metrics = g.getFontMetrics(g.getFont());
        //Rectangle2D rect = metrics.getStringBounds(text,g);
        
        int tw = metrics.stringWidth(text);
        int th = metrics.getHeight();
        Rectangle res = calculateLayout(tw, th, width, height);
        
        g.translate(res.x, res.y);
        
        if(isPaintStretched()) {
            paint = calculateSnappedPaint(paint, res.width, res.height);
        }
        
        if (paint != null) {
            g.setPaint(paint);
        }
        
        g.drawString(text, 0, 0 + metrics.getAscent());
        if(getAreaEffects() != null) {
            Shape shape = provideShape(g, component, width, height);
            for(AreaEffect ef : getAreaEffects()) {
                ef.apply(g, shape, width, height);
            }
        }
        g.translate(-res.x,-res.y);
    }
    
    private String calculateText(final T component) {
        // prep the text
        String text = getText();
        //make components take priority if(text == null || text.trim().equals("")) {
        if(text != null && !text.trim().equals("")) {
            return text;
        }
        if(component instanceof JTextComponent) {
            text = ((JTextComponent)component).getText();
        }
        if(component instanceof JLabel) {
            text = ((JLabel)component).getText();
        }
        if(component instanceof AbstractButton) {
            text = ((AbstractButton)component).getText();
        }
        return text;
    }
    
    private Font calculateFont(final T component) {
        // prep the various text attributes
        Font font = getFont();
        if (font == null) {
            if(component instanceof JComponent) {
                font = ((JComponent)component).getFont();
            }
        }
        if (font == null) {
            font = new Font("Dialog", Font.PLAIN, 18);
        }
        return font;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Shape provideShape(Graphics2D g2, T comp, int width, int height) {
        Font font = calculateFont(comp);
        String text = calculateText(comp);
        FontMetrics metrics = g2.getFontMetrics(font);
        GlyphVector vect = font.createGlyphVector(g2.getFontRenderContext(),text);
        return vect.getOutline(0f,0f+ metrics.getAscent());
    }
}
