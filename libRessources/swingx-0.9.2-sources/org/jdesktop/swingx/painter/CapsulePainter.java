/*
 * $Id: CapsulePainter.java,v 1.8 2007/03/26 21:25:20 rbair Exp $
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

import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Draws a capsule. This is a rectangle capped by two semi circles. You can draw only a
 * portion of a capsule using the portion property.
 * @author joshy
 */
public class CapsulePainter extends AbstractAreaPainter {
    public enum Portion { Top, Full, Bottom, Left, Right }
    private Portion portion;
    
    
    /**
     * Create a new CapsulePainter that draws a full capsule.
     */
    public CapsulePainter() {
        this.setPortion(Portion.Full);
    }
    /**
     * Create a new CapsulePainter that only draws the portion specified.
     * @param portion the portion to draw
     */
    public CapsulePainter(Portion portion) {
        this.setPortion(portion);
    }

    
    
    /**
     * Returns the current portion property. This property determines 
     * which part of the capsule will be drawn.
     * @return the current portion
     */
    public Portion getPortion() {
        return portion;
    }

    /**
     * Sets the current portion property. This property determines 
     * which part of the capsule will be drawn.
     * @param portion the new portion
     */
    public void setPortion(Portion portion) {
        Portion old = this.portion;
        this.portion = portion;
        setDirty(true);
        firePropertyChange("portion",old,getPortion());
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPaint(Graphics2D g, Object component, int width, int height) {
        Shape rect = provideShape(g,component,width,height);
        if(getStyle() == Style.BOTH || getStyle() == Style.FILLED) {
            g.setPaint(getFillPaint());
            g.fill(rect);
        }
        if(getStyle() == Style.BOTH || getStyle() == Style.OUTLINE) {
            g.setPaint(getBorderPaint());
            g.draw(rect);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Shape provideShape(Graphics2D g, Object comp, int width, int height) {
        int round = 10;
        int rheight = height;
        int ry = 0;
        if(getPortion() == Portion.Top) {
            round = height*2;
            rheight = height*2;
        }
        if(getPortion() == Portion.Bottom) {
            round = height*2;
            rheight = height*2;
            ry = -height;
        }
        // need to support left and right!
        
        return new RoundRectangle2D.Double(0, ry, width, rheight, round, round);
    }

}
