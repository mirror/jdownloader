/*
 * $Id: RectanglePainter.java,v 1.7 2007/11/19 16:20:38 kschaefe Exp $
 *
 * Copyright 2006 Sun Microsystems, Inc., 4150 Network Circle,
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.awt.geom.RoundRectangle2D;

import org.jdesktop.swingx.painter.effects.AreaEffect;
import org.jdesktop.swingx.util.PaintUtils;



/**
 * A painter which paints square and rounded rectangles
 * @author joshua.marinacci@sun.com
 */

public class RectanglePainter<T> extends AbstractAreaPainter<T> {
    private boolean rounded = false;
    //private Insets insets = new Insets(0,0,0,0);
    private int roundWidth = 20;
    private int roundHeight = 20;
    private int width = -1;
    private int height = -1;
    //private double strokeWidth = 1;

    /** Creates a new instance of RectanglePainter */
    public RectanglePainter() {
        this(0,0,0,0, 0,0, false, Color.RED, 1f, Color.BLACK);
    }

    public RectanglePainter(Color fillPaint, Color borderPaint) {
        this(0,0,0,0,0,0,false,fillPaint,1f,borderPaint);
    }

    public RectanglePainter(Paint fillPaint, Paint borderPaint, float borderWidth, RectanglePainter.Style style) {
        this();
        setFillPaint(fillPaint);
        setBorderPaint(borderPaint);
        setBorderWidth(borderWidth);
        setStyle(style);
    }
    public RectanglePainter(int top, int left, int bottom, int right) {
        this(top, left, bottom, right, 0, 0, false, Color.RED, 1f, Color.BLACK);
    }
    public RectanglePainter(int top, int left, int bottom, int right,
            int roundWidth, int roundHeight) {
        this(top,left,bottom,right,roundWidth, roundHeight, true, Color.RED, 1f, Color.BLACK);
    }

    public RectanglePainter(int width, int height, int cornerRadius, Paint fillPaint) {
        this(new Insets(0,0,0,0), width,height,
                cornerRadius, cornerRadius, true,
                fillPaint, 1f, Color.BLACK);
    }

    public RectanglePainter(Insets insets,
            int width, int height,
            int roundWidth, int roundHeight, boolean rounded, Paint fillPaint,
            float strokeWidth, Paint borderPaint) {
        this.width = width;
        this.height = height;
        setFillHorizontal(false);
        setFillVertical(false);
        setInsets(insets);
        this.roundWidth = roundWidth;
        this.roundHeight = roundHeight;
        this.rounded = rounded;
        this.setFillPaint(fillPaint);
        this.setBorderWidth(strokeWidth);
        this.setBorderPaint(borderPaint);
    }

    public RectanglePainter(int top, int left, int bottom, int right,
            int roundWidth, int roundHeight, boolean rounded, Paint fillPaint,
            float strokeWidth, Paint borderPaint) {
        this.setInsets(new Insets(top,left,bottom,right));
        setFillVertical(true);
        setFillHorizontal(true);
        this.roundWidth = roundWidth;
        this.roundHeight = roundHeight;
        this.rounded = rounded;
        this.setFillPaint(fillPaint);
        this.setBorderWidth(strokeWidth);
        this.setBorderPaint(borderPaint);
    }




    /**
     * Indicates if the rectangle is rounded
     * @return if the rectangle is rounded
     */
    public boolean isRounded() {
        return rounded;
    }

    /**
     * sets if the rectangle should be rounded
     * @param rounded if the rectangle should be rounded
     */
    public void setRounded(boolean rounded) {
        boolean oldRounded = isRounded();
        this.rounded = rounded;
        setDirty(true);
        firePropertyChange("rounded",oldRounded,rounded);
    }

    /**
     * gets the round width of the rectangle
     * @return the current round width
     */
    public int getRoundWidth() {
        return roundWidth;
    }

    /**
     * sets the round width of the rectangle
     * @param roundWidth a new round width
     */
    public void setRoundWidth(int roundWidth) {
        int oldRoundWidth = getRoundWidth();
        this.roundWidth = roundWidth;
        setDirty(true);
        firePropertyChange("roundWidth",oldRoundWidth,roundWidth);
    }

    /**
     * gets the round height of the rectangle
     * @return the current round height
     */
    public int getRoundHeight() {
        return roundHeight;
    }

    /**
     * sets the round height of the rectangle
     * @param roundHeight a new round height
     */
    public void setRoundHeight(int roundHeight) {
        int oldRoundHeight = getRoundHeight();
        this.roundHeight = roundHeight;
        setDirty(true);
        firePropertyChange("roundHeight",oldRoundHeight,roundHeight);
    }


    /* ======== drawing code ============ */
    protected RectangularShape calculateShape(int width, int height) {
        Insets insets = getInsets();
        int x = insets.left;
        int y = insets.top;

        // use the position calcs from the super class
        Rectangle bounds = calculateLayout(this.width, this.height, width, height);
        if(this.width != -1 && !isFillHorizontal()) {
            width = this.width;
            x = bounds.x;
        }
        if(this.height != -1 && !isFillVertical()) {
            height = this.height;
            y = bounds.y;
        }

        if(isFillHorizontal()) {
            width = width - insets.left - insets.right;
        }
        if(isFillVertical()) {
            height = height - insets.top - insets.bottom;
        }


        RectangularShape shape = new Rectangle2D.Double(x, y, width, height);
        if(rounded) {
            shape = new RoundRectangle2D.Double(x, y, width, height, roundWidth, roundHeight);
        }
        return shape;
    }



    public void doPaint(Graphics2D g, T component, int width, int height) {
        RectangularShape shape = calculateShape(width, height);
        switch (getStyle()) {
        case BOTH:
            drawBackground(g,shape,width,height);
            drawBorder(g,shape,width,height);
            break;
        case FILLED:
            drawBackground(g,shape,width,height);
            break;
        case OUTLINE:
            drawBorder(g,shape,width,height);
            break;
        case NONE:
            break;
        }

        // background
        // border
        // leave the clip to support masking other painters
        PaintUtils.setMergedClip(g,shape);
        /*
        Area area = new Area(g.getClip());
        area.intersect(new Area(shape));//new Rectangle(0,0,width,height)));
        g.setClip(area);*/
        //g.setClip(shape);
    }

    private void drawBorder(Graphics2D g, RectangularShape shape, int width, int height) {
        Paint p = getBorderPaint();
        if(isPaintStretched()) {
            p = calculateSnappedPaint(p, width, height);
        }

        g.setPaint(p);

        g.setStroke(new BasicStroke(getBorderWidth()));
        // shrink the border by 1 px
        if(shape instanceof Rectangle2D) {
            g.draw(new Rectangle2D.Double(shape.getX(), shape.getY(),
                    shape.getWidth()-1, shape.getHeight()-1));
        } else if(shape instanceof RoundRectangle2D) {
            g.draw(new RoundRectangle2D.Double(shape.getX(), shape.getY(),
                    shape.getWidth()-1, shape.getHeight()-1,
                    ((RoundRectangle2D)shape).getArcWidth(),
                    ((RoundRectangle2D)shape).getArcHeight()));

        } else {
            g.draw(shape);
        }


    }

    private void drawBackground(Graphics2D g, Shape shape, int width, int height) {
        Paint p = getFillPaint();
        if(isPaintStretched()) {
            p = calculateSnappedPaint(p, width, height);
        }

        g.setPaint(p);

        g.fill(shape);
        if(getAreaEffects() != null) {
            for(AreaEffect ef : getAreaEffects()) {
                ef.apply(g, shape, width, height);
            }
        }
    }

    public Shape provideShape(Graphics2D g, T comp, int width, int height) {
        return calculateShape(width,height);
    }
    
}

