/*
 * $Id: CompoundPainter.java,v 1.15 2007/06/19 10:53:37 rah003 Exp $
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

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

/**
 * <p>A {@link Painter} implemention composed of an array of <code>Painter</code>s.
 * <code>CompoundPainter</code> provides a means for combining several individual
 * <code>Painter</code>s, or groups of them, into one logical unit. Each of the
 * <code>Painter</code>s are executed in order. BufferedImageOp filter effects can
 * be applied to them together as a whole. The entire set of painting operations
 * may be cached together.</p>
 *
 * <p></p>
 *
 * <p>For example, if I want to create a CompoundPainter that started with a blue
 * background, had pinstripes on it running at a 45 degree angle, and those
 * pinstripes appeared to "fade in" from left to right, I would write the following:
 * <pre><code>
 *  Color blue = new Color(0x417DDD);
 *  Color translucent = new Color(blue.getRed(), blue.getGreen(), blue.getBlue(), 0);
 *  panel.setBackground(blue);
 *  panel.setForeground(Color.LIGHT_GRAY);
 *  GradientPaint blueToTranslucent = new GradientPaint(
 *    new Point2D.Double(.4, 0),
 *    blue,
 *    new Point2D.Double(1, 0),
 *    translucent);
 *  MattePainter veil = new MattePainter(blueToTranslucent);
 *  veil.setPaintStretched(true);
 *  Painter pinstripes = new PinstripePainter(45);
 *  Painter backgroundPainter = new RectanglePainter(this.getBackground(), null);
 *  Painter p = new CompoundPainter(backgroundPainter, pinstripes, veil);
 *  panel.setBackgroundPainter(p);
 * </code></pre></p>
 *
 * @author rbair
 */
public class CompoundPainter<T> extends AbstractPainter<T> {
    private Painter[] painters = new Painter[0];
    private AffineTransform transform;
    private boolean clipPreserved = false;

    /** Creates a new instance of CompoundPainter */
    public CompoundPainter() {
    }
    
    /**
     * Convenience constructor for creating a CompoundPainter for an array
     * of painters. A defensive copy of the given array is made, so that future
     * modification to the array does not result in changes to the CompoundPainter.
     *
     * @param painters array of painters, which will be painted in order
     */
    public CompoundPainter(Painter... painters) {
        this.painters = new Painter[painters == null ? 0 : painters.length];
        if (painters != null) {
            System.arraycopy(painters, 0, this.painters, 0, painters.length);
        }
    }
    
    /**
     * Sets the array of Painters to use. These painters will be executed in
     * order. A null value will be treated as an empty array.
     *
     * @param painters array of painters, which will be painted in order
     */
    public void setPainters(Painter... painters) {
        Painter[] old = getPainters();
        this.painters = new Painter[painters == null ? 0 : painters.length];
        if (painters != null) {
            System.arraycopy(painters, 0, this.painters, 0, this.painters.length);
        }
        setDirty(true);
        firePropertyChange("painters", old, getPainters());
    }
    
    /**
     * Gets the array of painters used by this CompoundPainter
     * @return a defensive copy of the painters used by this CompoundPainter.
     *         This will never be null.
     */
    public final Painter[] getPainters() {
        Painter[] results = new Painter[painters.length];
        System.arraycopy(painters, 0, results, 0, results.length);
        return results;
    }
    
    
    /**
     * Indicates if the clip produced by any painter is left set once it finishes painting. 
     * Normally the clip will be reset between each painter. Setting clipPreserved to
     * true can be used to let one painter mask other painters that come after it.
     * @return if the clip should be preserved
     * @see #setClipPreserved(boolean)
     */
    public boolean isClipPreserved() {
        return clipPreserved;
    }
    
    /**
     * Sets if the clip should be preserved.
     * Normally the clip will be reset between each painter. Setting clipPreserved to
     * true can be used to let one painter mask other painters that come after it.
     * 
     * @param shouldRestoreState new value of the clipPreserved property
     * @see #isClipPreserved()
     */
    public void setClipPreserved(boolean shouldRestoreState) {
        boolean oldShouldRestoreState = isClipPreserved();
        this.clipPreserved = shouldRestoreState;
        setDirty(true);
        firePropertyChange("shouldRestoreState",oldShouldRestoreState,shouldRestoreState);
    }

    /**
     * Gets the current transform applied to all painters in this CompoundPainter. May be null.
     * @return the current AffineTransform
     */
    public AffineTransform getTransform() {
        return transform;
    }

    /**
     * Set a transform to be applied to all painters contained in this CompoundPainter
     * @param transform a new AffineTransform
     */
    public void setTransform(AffineTransform transform) {
        AffineTransform old = getTransform();
        this.transform = transform;
        setDirty(true);
        firePropertyChange("transform",old,transform);
    }
    
    /**
     * <p>Iterates over all child <code>Painter</code>s and gives them a chance
     * to validate themselves. If any of the child painters are dirty, then
     * this <code>CompoundPainter</code> marks itself as dirty.</p>
     *
     * @inheritDoc
     */
    @Override
    protected void validate(T object) {
        boolean dirty = false;
        for (Painter p : painters) {
            if (p instanceof AbstractPainter) {
                AbstractPainter ap = (AbstractPainter)p;
                ap.validate(object);
                if (ap.isDirty()) {
                    dirty = true;
                    break;
                }
            }
        }
        clearLocalCacheOnly = true;
        setDirty(dirty); //super will call clear cache
        clearLocalCacheOnly = false;
    }

    //indicates whether the local cache should be cleared only, as opposed to the
    //cache's of all of the children. This is needed to optimize the caching strategy
    //when, during validate, the CompoundPainter is marked as dirty
    private boolean clearLocalCacheOnly = false;

    /**
     * <p>This <code>CompoundPainter</code> is dirty if it, or any of its children,
     * are dirty. Thus, we iterate
     * over all child <code>Painter</code>s and query them to see
     * if they are dirty. If so, then true is returned. Otherwise, we defer
     * to the super implementation.</p>
     *
     * @inheritDoc
     */
    @Override
    protected boolean isDirty() {
        boolean dirty = super.isDirty();
        if (dirty) {
            return true;
        } else {
            for (Painter p : painters) {
                if (p instanceof AbstractPainter) {
                    AbstractPainter ap = (AbstractPainter)p;
                    if (ap.isDirty()) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * <p>Clears the cache of this <code>Painter</code>, and all child
     * <code>Painters</code>. This is done to ensure that resources
     * are collected, even if clearCache is called by some framework
     * or other code that doesn't realize this is a CompoundPainter.</p>
     *
     * <p>Call #clearLocalCache if you only want to clear the cache of this
     * <code>CompoundPainter</code>
     *
     * @inheritDoc
     */
    @Override
    public void clearCache() {
        if (!clearLocalCacheOnly) {
            for (Painter p : painters) {
                if (p instanceof AbstractPainter) {
                    AbstractPainter ap = (AbstractPainter)p;
                    ap.clearCache();
                }
            }
        }
        super.clearCache();
    }

    /**
     * <p>Clears the cache of this painter only, and not of any of the children.</p>
     */
    public void clearLocalCache() {
        super.clearCache();
    }

    /**
     * @inheritDoc
     */
    @Override
    protected void doPaint(Graphics2D g, T component, int width, int height) {
        for (Painter p : getPainters()) {
            Graphics2D temp = (Graphics2D) g.create();
            p.paint(temp, component, width, height);
            if(isClipPreserved()) {
                g.setClip(temp.getClip());
            }
            temp.dispose();
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    protected void configureGraphics(Graphics2D g) {
        //applies the transform
        AffineTransform tx = getTransform();
        if (tx != null) {
            g.setTransform(tx);
        }
    }
    
    /**
     * @inheritDoc
     */
    @Override
    protected boolean shouldUseCache() {
        return (isCacheable() && painters != null && painters.length > 0) || super.shouldUseCache();
    }
}
