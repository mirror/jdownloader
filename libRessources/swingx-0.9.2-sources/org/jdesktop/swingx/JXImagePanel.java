/*
 * $Id: JXImagePanel.java,v 1.18 2007/11/10 00:42:12 kschaefe Exp $
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

package org.jdesktop.swingx;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import javax.imageio.ImageIO;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;


/**
 * <p>A panel that draws an image. The standard (and currently only supported)
 * mode is to draw the specified image starting at position 0,0 in the
 * panel. The component&amp;s preferred size is based on the image, unless
 * explicitly set by the user.</p>
 *
 * <p>In the future, the JXImagePanel will also support tiling of images,
 * scaling, resizing, cropping, segways etc.</p>
 *
 * <p>This component also supports allowing the user to set the image. If the
 * <code>JXImagePanel</code> is editable, then when the user clicks on the
 * <code>JXImagePanel</code> a FileChooser is shown allowing the user to pick
 * some other image to use within the <code>JXImagePanel</code>.</p>
 *
 * <p>Images to be displayed can be set based on URL, Image, etc.
 *
 * @author rbair
 */
public class JXImagePanel extends JXPanel {
    public static enum Style {CENTERED, TILED, SCALED, SCALED_KEEP_ASPECT_RATIO}
    private static final Logger LOG = Logger.getLogger(JXImagePanel.class
            .getName());
    /**
     * Text informing the user that clicking on this component will allow them to set the image
     */
    private static final String TEXT = "<html><i><b>Click here<br>to set the image</b></i></html>";
    /**
     * The image to draw
     */
    private Image img;
    /**
     * If true, then the image can be changed. Perhaps a better name is
     * &quot;readOnly&quot;, but editable was chosen to be more consistent
     * with other Swing components.
     */
    private boolean editable = false;
    /**
     * The mouse handler that is used if the component is editable
     */
    private MouseHandler mhandler = new MouseHandler();
    /**
     * If not null, then the user has explicitly set the preferred size of
     * this component, and this should be honored
     */
    private Dimension preferredSize;
    /**
     * Specifies how to draw the image, i.e. what kind of Style to use
     * when drawing
     */
    private Style style = Style.CENTERED;
    
    public JXImagePanel() {
    }
    
    public JXImagePanel(URL imageUrl) {
        try {
            setImage(ImageIO.read(imageUrl));
        } catch (Exception e) {
            //TODO need convert to something meaningful
            LOG.log(Level.WARNING, "", e);
        }
    }
    
    /**
     * Sets the image to use for the background of this panel. This image is
     * painted whether the panel is opaque or translucent.
     * @param image if null, clears the image. Otherwise, this will set the
     * image to be painted. If the preferred size has not been explicitly set,
     * then the image dimensions will alter the preferred size of the panel.
     */
    public void setImage(Image image) {
        if (image != img) {
            Image oldImage = img;
            img = image;
            firePropertyChange("image", oldImage, img);
            invalidate();
            repaint();
        }
    }
    
    /**
     * @return the image used for painting the background of this panel
     */
    public Image getImage() {
        return img;
    }
    
    /**
     * @param editable
     */
    public void setEditable(boolean editable) {
        if (editable != this.editable) {
            //if it was editable, remove the mouse handler
            if (this.editable) {
                removeMouseListener(mhandler);
            }
            this.editable = editable;
            //if it is now editable, add the mouse handler
            if (this.editable) {
                addMouseListener(mhandler);
            }
            setToolTipText(editable ? TEXT : "");
            firePropertyChange("editable", !editable, editable);
            repaint();
        }
    }
    
    /**
     * @return whether the image for this panel can be changed or not via
     * the UI. setImage may still be called, even if <code>isEditable</code>
     * returns false.
     */
    public boolean isEditable() {
        return editable;
    }
    
    /**
     * Sets what style to use when painting the image
     *
     * @param s
     */
    public void setStyle(Style s) {
        if (style != s) {
            Style oldStyle = style;
            style = s;
            firePropertyChange("style", oldStyle, s);
            repaint();
        }
    }
    
    /**
     * @return the Style used for drawing the image (CENTERED, TILED, etc).
     */
    public Style getStyle() {
        return style;
    }
    
    public void setPreferredSize(Dimension pref) {
        preferredSize = pref;
        super.setPreferredSize(pref);
    }
    
    public Dimension getPreferredSize() {
        if (preferredSize == null && img != null) {
            //it has not been explicitly set, so return the width/height of the image
            int width = img.getWidth(null);
            int height = img.getHeight(null);
            if (width == -1 || height == -1) {
                return super.getPreferredSize();
            }
            Insets insets = getInsets();
            width += insets.left + insets.right;
            height += insets.top + insets.bottom;
            return new Dimension(width, height);
        } else {
            return super.getPreferredSize();
        }
    }
    
    /**
     * Overriden to paint the image on the panel
     * @param g 
     */
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)g;
        if (img != null) {
            final int imgWidth = img.getWidth(null);
            final int imgHeight = img.getHeight(null);
            if (imgWidth == -1 || imgHeight == -1) {
                //image hasn't completed loading, return
                return;
            }
            
            Insets insets = getInsets();
            final int pw = getWidth() - insets.left - insets.right;
            final int ph = getHeight() - insets.top - insets.bottom;
            
            switch (style) {
                case CENTERED:
                    Rectangle clipRect = g2.getClipBounds();
                    int imageX = (pw - imgWidth) / 2 + insets.left;
                    int imageY = (ph - imgHeight) / 2 + insets.top;
                    Rectangle r = SwingUtilities.computeIntersection(imageX, imageY, imgWidth, imgHeight, clipRect);
                    if (r.x == 0 && r.y == 0 && (r.width == 0 || r.height == 0)) {
                        return;
                    }
                    //I have my new clipping rectangle "r" in clipRect space.
                    //It is therefore the new clipRect.
                    clipRect = r;
                    //since I have the intersection, all I need to do is adjust the
                    //x & y values for the image
                    int txClipX = clipRect.x - imageX;
                    int txClipY = clipRect.y - imageY;
                    int txClipW = clipRect.width;
                    int txClipH = clipRect.height;

                    g2.drawImage(img, clipRect.x, clipRect.y, clipRect.x + clipRect.width, clipRect.y + clipRect.height,
                            txClipX, txClipY, txClipX + txClipW, txClipY + txClipH, null);
                    break;
                case TILED:
                    g2.translate(insets.left, insets.top);
                    Rectangle clip = g2.getClipBounds();
                    g2.setClip(0, 0, pw, ph);
                    
                    int totalH = 0;
                    
                    while (totalH < ph) {
                        int totalW = 0;
                        
                        while (totalW < pw) {
                            g2.drawImage(img, totalW, totalH, null);
                            totalW += img.getWidth(null);
                        }
                        
                        totalH += img.getHeight(null);
                    }
                    
                    g2.setClip(clip);
                    g2.translate(-insets.left, -insets.top);
                    break;
                case SCALED:
                    g2.drawImage(img, insets.left, insets.top, pw, ph, null);
                    break;
                case SCALED_KEEP_ASPECT_RATIO:
                    int w;
                    int h;
                    if ((imgWidth - pw) > (imgHeight - ph)) {
                        w = pw;
                        final float ratio = ((float)w) / ((float)imgWidth);
                        h = (int)(imgHeight * ratio);
                    } else {
                        h = ph;
                        final float ratio = ((float)h) / ((float)imgHeight);
                        w = (int)(imgWidth * ratio);
                    }
                    final int x = (pw - w) / 2 + insets.left;
                    final int y = (ph - h) / 2 + insets.top;
                    g2.drawImage(img, x, y, w, h, null);
                    break;
                default:
                    LOG.fine("unimplemented");
                    g2.drawImage(img, insets.left, insets.top, this);
                    break;
            }
        }
    }
    
    /**
     * Handles click events on the component
     */
    private class MouseHandler extends MouseAdapter {
        private Cursor oldCursor;
        private JFileChooser chooser;
        
        public void mouseClicked(MouseEvent evt) {
            if (chooser == null) {
                chooser = new JFileChooser();
            }
            int retVal = chooser.showOpenDialog(JXImagePanel.this);
            if (retVal == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                try {
                    setImage(new ImageIcon(file.toURI().toURL()).getImage());
                } catch (Exception ex) {
                }
            }
        }
        
        public void mouseEntered(MouseEvent evt) {
            if (oldCursor == null) {
                oldCursor = getCursor();
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
        }
        
        public void mouseExited(MouseEvent evt) {
            if (oldCursor != null) {
                setCursor(oldCursor);
                oldCursor = null;
            }
        }
    }
}

