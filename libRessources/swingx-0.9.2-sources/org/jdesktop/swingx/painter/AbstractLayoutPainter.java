/*
 * AbstractLayoutPainter.java
 *
 * Created on July 31, 2006, 3:10 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jdesktop.swingx.painter;

import java.awt.Insets;
import java.awt.Rectangle;

/**
 * An abstract base class for any painter which can be positioned. This means
 * the painter has some intrinsic size to what it is drawing and
 * can be stretched or aligned both horizontally and vertically.
 * 
 * 
 * The AbstractLayoutPainter class provides the following configuraable properties:
 * 
 * <ul>
 * <li>horizonalAlignment - the horizonal alignment (left, center, and right)</li>
 * <li>verticalAlignment - the verticalAlignment alignment (top, center, and bottom)</li>
 * <li>fillHorizontal - indicates if the painter should stretch to fill the available space horizontally</li>
 * <li>fillVertical - indicates if the painter should stretch to fill the available space vertically</li>
 * <li>insets - whitespace on the top, bottom, left, and right.
 * </ul>
 * 
 * By combining these five properties any AbstractLayoutPainter subclass can position it's content
 * within the paintable area.  For example, an ImagePainter has an intrinsic size based on the image
 * it is painting. If you wanted to paint the image in the lower right hand corner of the paintable
 * area, but inset by 5 pixels, you could do the following:
 * 
 * <pre><code>
 *     ImagePainter p = new ImagePainter(null);
 *     p.setVerticalAlignment(AbstractLayoutPainter.VerticalAlignment.BOTTOM);
 *     p.setHorizontalAlignment(AbstractLayoutPainter.HorizontalAlignment.RIGHT);
 *     p.setInsets(new Insets(0,0,5,5));
 * </code></pre>
 * 
 * 
 * For something which is resizable, like a RectanglePainter, you can use the fill properties
 * to make it resize along with the paintable area. For example, to make a rectangle with 20 px
 * rounded corners, and which resizes with the paintable area but is inset 
 * by 10 pixels on all sides, you could do
 * the following:
 * 
 * <pre><code>
 *     RectanglePainter p = new RectanglePainter();
 *     p.setRoundHeight(20);
 *     p.setRoundWidth(20);
 *     p.setInsets(new Insets(10,10,10,10));
 *     p.setFillHorizontal(true);
 *     p.setFillVertical(true);
 * </code></pre>
 * 
 * 
 * @author joshua@marinacci.org
 */
public abstract class AbstractLayoutPainter<T> extends AbstractPainter<T> {
   
    /**
     * Specifies how to draw the image, i.e. what kind of Style to use
     * when drawing
     */
    private VerticalAlignment verticalAlignment = VerticalAlignment.CENTER;
    private HorizontalAlignment horizontalAlignment = HorizontalAlignment.CENTER;
    private Insets insets = new Insets(0,0,0,0);
    private boolean fillVertical = false;
    private boolean fillHorizontal = false;

    /**
     * Creates a new instance of AbstractLayoutPainter
     */
    public AbstractLayoutPainter() {
    }

    /**
     * An enum which controls horizontalAlignment alignment
     */
    public static enum HorizontalAlignment { LEFT, CENTER, RIGHT }

    
    /**
     * An enum which controls verticalAlignment alignment
     */
    public static enum VerticalAlignment { TOP, CENTER, BOTTOM }

    
    /**
     * Gets the current horizontalAlignment alignment.
     * 
     * @return the current horizontalAlignment alignment
     */
    public HorizontalAlignment getHorizontalAlignment() {
        return horizontalAlignment;
    }

    
    /**
     * Gets the current whitespace insets.
     * @return the current insets
     */
    public Insets getInsets() {
        return insets;
    }

    
    /**
     * gets the current verticalAlignment alignment
     * 
     * @return current verticalAlignment alignment
     */
    public VerticalAlignment getVerticalAlignment() {
        return verticalAlignment;
    }

    
    /**
     * indicates if the painter content is stretched horizontally
     * 
     * @return the current horizontalAlignment stretch value
     */
    public boolean isFillHorizontal() {
        return fillHorizontal;
    }

    
    /**
     * indicates if the painter content is stretched vertically
     * 
     * @return the current verticalAlignment stretch value
     */
    public boolean isFillVertical() {
        return fillVertical;
    }

    
    /**
     * Sets a new horizontalAlignment alignment. Used to position the content at the left, right, or center.
     * 
     * @param horizontal new horizontalAlignment alignment
     */
    public void setHorizontalAlignment(HorizontalAlignment horizontal) {
        HorizontalAlignment old = this.getHorizontalAlignment();
        this.horizontalAlignment = horizontal;
        setDirty(true);
        firePropertyChange("horizontal",old,this.horizontalAlignment);
    }

    
    /**
     * Sets if the content should be stretched horizontally to fill all available horizontalAlignment
     * space (minus the left and right insets).
     * 
     * 
     * @param fillHorizontal new horizonal stretch value
     */
    public void setFillHorizontal(boolean fillHorizontal) {
        boolean old = this.isFillHorizontal();
        this.fillHorizontal = fillHorizontal;
        setDirty(true);
        firePropertyChange("horizontalStretch",old,this.fillHorizontal);
    }

    
    /**
     * Sets the current whitespace insets.
     * @param insets new insets
     */
    public void setInsets(Insets insets) {
        Insets old = this.getInsets();
        this.insets = insets;
        setDirty(true);
        firePropertyChange("insets",old,this.insets);
    }

    
    
    /**
     * Sets a new verticalAlignment alignment. Used to position the content at the top, bottom, or center.
     * 
     * @param vertical new verticalAlignment alignment
     */
    public void setVerticalAlignment(VerticalAlignment vertical) {
        VerticalAlignment old = this.getVerticalAlignment();
        this.verticalAlignment = vertical;
        setDirty(true);
        firePropertyChange("vertical",old,this.verticalAlignment);
    }

    
    /**
     * Sets if the content should be stretched vertically to fill all available verticalAlignment
     * space (minus the top and bottom insets).
     * 
     * 
     * @param verticalStretch new verticalAlignment stretch value
     */
    public void setFillVertical(boolean verticalStretch) {
        boolean old = this.isFillVertical();
        this.fillVertical = verticalStretch;
        setDirty(true);
        firePropertyChange("verticalStretch",old,this.fillVertical);
    }
    
    /**
     * A protected method used by subclasses to calculate the final position of the
     * content. This will position the content using the fillHorizontal, fillVertical
     * horizontalAlignment, and verticalAlignment properties. This method
     * is typically called by subclasses in their doPaint() methods.
     * 
     * @param contentWidth The width of the content to be painted
     * @param contentHeight The height of the content to be painted
     * @param width the width of the area that the content will be positioned in
     * @param height the height of the area that the content will be positioned in
     * @return the rectangle for the content to be painted in
     */
    protected final Rectangle calculateLayout(final int contentWidth, final int contentHeight, 
            final int width, final int height) {
        
        Rectangle rect = new Rectangle();
        rect.width = contentWidth;
        rect.height = contentHeight;
        
        if(isFillHorizontal()) {
            rect.width = width - insets.left - insets.right;
        }
        
        if(isFillVertical()) {
            rect.height = height - insets.top - insets.bottom;
        }
        rect.x = calculateX(rect.width, width);
        rect.y = calculateY(rect.height, height);
        return rect;
    }

    private int calculateY(final int imgHeight, final int height) {
        int y = 0;
        if(getVerticalAlignment() == VerticalAlignment.TOP) {
            y = 0;
            y+= insets.top;
        }
        if(getVerticalAlignment() == VerticalAlignment.CENTER) {
            y = (height-imgHeight)/2;
            y += insets.top;
        }
        if(getVerticalAlignment() == VerticalAlignment.BOTTOM) {
            y = height-imgHeight;
            y-= insets.bottom;
        }
        return y;
    }

    private int calculateX(final int imgWidth, final int width) {
        int x = 0;
        if(getHorizontalAlignment() == HorizontalAlignment.LEFT) {
            x = 0;
            x+= insets.left;
        }
        if(getHorizontalAlignment() == HorizontalAlignment.CENTER) {
            x = (width-imgWidth)/2;
            x += insets.left;
        }
        if(getHorizontalAlignment() == HorizontalAlignment.RIGHT) {
            x = width-imgWidth;
            x-= insets.right;
        }
        return x;
    }
}
