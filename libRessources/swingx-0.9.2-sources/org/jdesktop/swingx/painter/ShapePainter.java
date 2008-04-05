/*
 * $Id: ShapePainter.java,v 1.16 2007/03/26 21:25:20 rbair Exp $
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
import java.awt.*;
import java.awt.geom.Ellipse2D;


/**
 * <p>A Painter that paints java.awt.Shapes. It uses a stroke and a fillPaint to do so. The
 * shape is painted as is, at a specific location. If no Shape is specified, nothing
 * will be painted. If no stroke is specified, the default for the Graphics2D
 * will be used. If no fillPaint is specified, the component background color
 * will be used. The shape can be positioned using the insets, horizontal, and
 * vertical properties.</p>
 * 
 * <p>Here is an example that draws a rectangle aligned on the center right:
 * <pre><code>
 *  Rectangle2D.Double rect = new Rectangle2D.Double(0, 0, 50, 50);
 *  ShapePainter p = new ShapePainter(rect);
 * p.setHorizontal(HorizontalAlignment.RIGHT);
 * p.setVertical(VerticalAlignment.CENTER);
 * </code></pre>
 * @author rbair
 */
public class ShapePainter<T> extends AbstractAreaPainter<T> {
    /**
     * The Shape to fillPaint. If null, nothing is painted.
     */
    private Shape shape;
    
    /**
     * Create a new ShapePainter
     */
    public ShapePainter() {
        super();
        this.shape = new Ellipse2D.Double(0,0,100,100);
        this.setBorderWidth(3);
        this.setFillPaint(Color.RED);
        this.setBorderPaint(Color.BLACK);
    }
    
    /**
     * Create a new ShapePainter with the specified shape.
     *
     *
     * @param shape the shape to fillPaint
     */
    public ShapePainter(Shape shape) {
        super();
        this.shape = shape;
    }
    
    /**
     * Create a new ShapePainter with the specified shape and fillPaint.
     *
     *
     * @param shape the shape to fillPaint
     * @param paint the fillPaint to be used to fillPaint the shape
     */
    public ShapePainter(Shape shape, Paint paint) {
        super();
        this.shape = shape;
        this.setFillPaint(paint);
    }
    
    /**
     * Create a new ShapePainter with the specified shape and fillPaint. The shape
     * can be filled or stroked (only the ouline is painted).
     *
     *
     * @param shape the shape to fillPaint
     * @param paint the fillPaint to be used to fillPaint the shape
     * @param style specifies the ShapePainter.Style to use for painting this shape.
     *        If null, then Style.BOTH is used
     */
    public ShapePainter(Shape shape, Paint paint, Style style) {
        super();
        this.shape = shape;
        this.setFillPaint(paint);
        this.setStyle(style == null ? Style.BOTH : style);
    }
    
    /**
     * Sets the shape to fillPaint. This shape is not resized when the component
     * bounds are. To do that, create a custom shape that is bound to the
     * component width/height
     *
     *
     * @param s the Shape to fillPaint. May be null
     */
    public void setShape(Shape s) {
        Shape old = getShape();
        this.shape = s;
        setDirty(true);
        firePropertyChange("shape", old, getShape());
    }
    
    /**
     * Gets the current shape
     * @return the Shape to fillPaint. May be null
     */
    public Shape getShape() {
        return shape;
    }
    
    /**
     * @inheritDoc
     */
    public void doPaint(Graphics2D g, T component, int w, int h) {
        //set the stroke if it is not null
        Stroke s = new BasicStroke(this.getBorderWidth());
        g.setStroke(s);
        
        if(getShape() != null) {
            Shape shape = provideShape(g,component, w, h);
            Rectangle bounds = shape.getBounds();
            Rectangle rect = calculateLayout(bounds.width, bounds.height, w, h);
            //u.p("rect = " + rect);
            g = (Graphics2D)g.create();
            g.translate(rect.x, rect.y);
            //draw/fill the shape
            drawPathEffects(g, shape, rect.width, rect.height);
            switch (getStyle()) {
                case BOTH:
                    drawShape(g, shape, component, rect.width, rect.height);
                    fillShape(g, shape, component, rect.width, rect.height);
                    break;
                case FILLED:
                    fillShape(g, shape, component, rect.width, rect.height);
                    break;
                case OUTLINE:
                    drawShape(g, shape, component, rect.width, rect.height);
                    break;
            }
            
            
            g.dispose();
        }
    }
    
    private void drawShape(Graphics2D g, Shape shape, T component, int w, int h) {
        g.setPaint(calculateStrokePaint(component, w, h));
        g.draw(shape);
    }
    
    private void fillShape(Graphics2D g, Shape shape, T component, int w, int h) {
        g.setPaint(calculateFillPaint(component, w, h));
        g.fill(shape);
    }
    
    // shape effect stuff
    public Shape provideShape(Graphics2D g, T comp, int width, int height) {
        return getShape();
    }
    
    private Paint calculateStrokePaint(T component, int width, int height) {
        Paint p = getBorderPaint();
        if (p == null) {
            if(component instanceof JComponent) {
                p = ((JComponent)component).getForeground();
            }
        }
        if(isPaintStretched()) {
            p = calculateSnappedPaint(p, width, height);
        }
        return p;
    }
    
    private Paint calculateFillPaint(T component, int width, int height) {
        //set the fillPaint
        Paint p = getFillPaint();
        if(isPaintStretched()) {
            p = calculateSnappedPaint(p, width, height);
        } else {
        }
        if (p == null) {
            if(component instanceof JComponent) {
                p = ((JComponent)component).getBackground();
            }
        }
        return p;
    }

    private void drawPathEffects(Graphics2D g, Shape shape, int w, int h) {
        if(getAreaEffects() != null) {
            //Paint pt = calculateFillPaint(component, w, h);
            for(AreaEffect ef : getAreaEffects()) {
                ef.apply(g, shape, w, h);
            }
        }
    }
}
