/*
 * $Id: JXImageView.java,v 1.15 2007/05/04 00:30:02 joshy Exp $
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

package org.jdesktop.swingx;

import org.jdesktop.swingx.color.ColorUtil;
import org.jdesktop.swingx.error.ErrorListener;
import org.jdesktop.swingx.error.ErrorSupport;
import org.jdesktop.swingx.util.PaintUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>A panel which shows an image centered. The user can drag an image into the 
 * panel from other applications and move the image around within the view.
 * The JXImageView has built in actions for scaling, rotating, opening a new 
 * image, and saving. These actions can be obtained using the relevant get*Action()
 * methods.
 *</p> 
 * 
 * <p>TODO: has dashed rect and text indicating you should drag there.</p>
 * 
 * 
 * <p>If the user drags more than one photo at a time into the JXImageView only
 * the first photo will be loaded and shown. Any errors generated internally, 
 * such as dragging in a list of files which are not images, will be reported 
 * to any attached {@link org.jdesktop.swingx.error.ErrorListener} added by the
 * <CODE>{@link #addErrorListener}()</CODE> method.</p>
 *
 * @author Joshua Marinacci joshua.marinacci@sun.com
 */
public class JXImageView extends JXPanel {
    
    /* ======= instance variables ========= */
    // the image this view will show
    private Image image;
    // the url of the image, if available
    private URL imageURL;
    
    // support for error listeners
    private ErrorSupport errorSupport = new ErrorSupport(this);
    
    // location to draw image. if null then draw in the center
    private Point2D imageLocation;
    // the background paint 
    private Paint checkerPaint;
    // the scale for drawing the image
    private double scale = 1.0;
    // controls whether the user can move images around
    private boolean editable = true;
    // the handler for moving the image around within the panel
    private MoveHandler moveHandler = new MoveHandler(this);
    // controls the drag part of drag and drop
    private boolean dragEnabled = false;
    // controls the filename of the dropped file
    private String exportName = "UntitledImage";
    // controls the format and filename extension of the dropped file
    private String exportFormat = "png";
    
    /** Creates a new instance of JXImageView */
    public JXImageView() {
        checkerPaint = ColorUtil.getCheckerPaint(Color.white,new Color(250,250,250),50);
        setEditable(true);
    }
    
   

    /* ========= properties ========= */
    /**
     * Gets the current image location. This location can be changed programmatically 
     * or by the user dragging the image within the JXImageView.
     * @return the current image location
     */
    public Point2D getImageLocation() {
        return imageLocation;
    }

    /**
     * Set the current image location.
     * @param imageLocation The new image location.
     */
    public void setImageLocation(Point2D imageLocation) {
        this.imageLocation = imageLocation;
        repaint();
    }
    
    /**
     * Gets the currently set image, or null if no image is set.
     * @return the currently set image, or null if no image is set.
     */
    public Image getImage() {
        return image;
    }

    /**
     * Sets the current image. Can set null if there should be no image show.
     * @param image the new image to set, or null.
     */
    public void setImage(Image image) {
        Image oldImage = getImage();
        this.image = image;
        setImageLocation(null);
        setScale(1.0);
        firePropertyChange("image",oldImage,image);
        repaint();
    }
    
    /**
     * Set the current image to an image pointed to by this URL.
     * @param url a URL pointing to an image, or null
     * @throws java.io.IOException thrown if the image cannot be loaded
     */
    public void setImage(URL url) throws IOException {
        setImageURL(url);
        //setImage(ImageIO.read(url));
    }
    
    /**
     * Set the current image to an image pointed to by this File.
     * @param file a File pointing to an image
     * @throws java.io.IOException thrown if the image cannot be loaded
     */
    public void setImage(File file) throws IOException {
        setImageURL(file.toURL());
    }
    
    /**
     * Gets the current image scale . When the scale is set to 1.0 
     * then one image pixel = one screen pixel. When scale < 1.0 the draw image
     * will be smaller than it's real size. When scale > 1.0 the drawn image will
     * be larger than it's real size. 1.0 is the default value.
     * @return the current image scale
     */
    public double getScale() {
        return scale;
    }

    /**
     * Sets the current image scale . When the scale is set to 1.0 
     * then one image pixel = one screen pixel. When scale < 1.0 the draw image
     * will be smaller than it's real size. When scale > 1.0 the drawn image will
     * be larger than it's real size. 1.0 is the default value.
     * @param scale the new image scale
     */
    public void setScale(double scale) {
        double oldScale = this.scale;
        this.scale = scale;
        this.firePropertyChange("scale",oldScale,scale);
        repaint();
    }

    /**
     * Returns whether or not the user can drag images.
     * @return whether or not the user can drag images
     */
    public boolean isEditable() {
        return editable;
    }

    /**
     * Sets whether or not the user can drag images. When set to true the user can
     * drag the photo around with their mouse. Also the cursor will be set to the
     * 'hand' cursor. When set to false the user cannot drag photos around
     * and the cursor will be set to the default.
     * @param editable whether or not the user can drag images
     */
    public void setEditable(boolean editable) {
        this.editable = editable;
        if(editable) {
            addMouseMotionListener(moveHandler);
            addMouseListener(moveHandler);
            this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            try {
                this.setTransferHandler(new DnDHandler());
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
                fireError(ex);
            }
        } else {
            removeMouseMotionListener(moveHandler);
            removeMouseListener(moveHandler);
            this.setCursor(Cursor.getDefaultCursor());
            setTransferHandler(null);
        }
    }
    
    /**
     * Sets the <CODE>dragEnabled</CODE> property, which determines whether or not 
     * the user can drag images out of the image view and into other components or 
     * application. Note: <B>setting
     * this to true will disable the ability to move the image around within the
     * well.</B>, though it will not change the <b>editable</b> property directly.
     * @param dragEnabled the value to set the dragEnabled property to.
     */
    public void setDragEnabled(boolean dragEnabled) {
        this.dragEnabled = dragEnabled;
    }

    /**
     * Gets the current value of the <CODE>dragEnabled</CODE> property.
     * @return the current value of the <CODE>dragEnabled</CODE> property
     */
    public boolean isDragEnabled() {
        return dragEnabled;
    }
    
    /**
     * Adds an ErrorListener to the list of listeners to be notified
     * of ErrorEvents
     * @param el an ErrorListener to add
     */
    public void addErrorListener(ErrorListener el) {
        errorSupport.addErrorListener(el);
    }
    
    /**
     * Remove an ErrorListener from the list of listeners to be notified of ErrorEvents.
     * @param el an ErrorListener to remove
     */
    public void removeErrorListener(ErrorListener el) {
        errorSupport.removeErrorListener(el);
    }
    
    /**
     * Send a new ErrorEvent to all registered ErrorListeners
     * @param throwable the Error or Exception which was thrown
     */
    protected void fireError(Throwable throwable) {
        errorSupport.fireErrorEvent(throwable);
    }

    
    private static FileDialog getSafeFileDialog(Component comp) {
        Window win = SwingUtilities.windowForComponent(comp);
        if(win instanceof Dialog) {
            return new FileDialog((Dialog)win);
        }
        if(win instanceof Frame) {
            return new FileDialog((Frame)win);
        }
        return null;
    }
            
    // an action which will open a file chooser and load the selected image
    // if any.
    /**
     * Returns an Action which will open a file chooser, ask the user for an image file
     * then load the image into the view. If the load fails an error will be fired
     * to all registered ErrorListeners
     * @return the action
     * @see ErrorListener
     */
    public Action getOpenAction() {
        Action action = new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                FileDialog fd = getSafeFileDialog(JXImageView.this);
                fd.setMode(FileDialog.LOAD);
                fd.setVisible(true);
                if(fd.getFile() != null) {
                    try {
                        setImage(new File(fd.getDirectory(),fd.getFile()));
                    } catch (IOException ex) {
                        fireError(ex);
                    }
                }
                /*
                JFileChooser chooser = new JFileChooser();
                chooser.showOpenDialog(JXImageView.this);
                File file = chooser.getSelectedFile();
                if(file != null) {
                    try {
                        setImage(file);
                    } catch (IOException ex) {
                        System.out.println(ex.getMessage());
                        ex.printStackTrace();
                        fireError(ex);
                    }
                }
                 */
            }
        };
        action.putValue(Action.NAME,"Open");
        return action;
    }
    
    // an action that will open a file chooser then save the current image to
    // the selected file, if any.
    /**
     * Returns an Action which will open a file chooser, ask the user for an image file
     * then save the image from the view. If the save fails an error will be fired
     * to all registered ErrorListeners
     * @return an Action
     */
    public Action getSaveAction() {
        Action action = new AbstractAction() {
            public void actionPerformed(ActionEvent evt) {
                Image img = getImage();
                BufferedImage dst = new BufferedImage(
                            img.getWidth(null),
                            img.getHeight(null), 
                            BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = (Graphics2D)dst.getGraphics();
                // smooth scaling
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                   RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.drawImage(img,0,0,null);
                g.dispose();
                FileDialog fd = new FileDialog((Frame)SwingUtilities.windowForComponent(JXImageView.this));
                fd.setMode(FileDialog.SAVE);
                fd.setVisible(true);
                if(fd.getFile() != null) {
                    try {
                        ImageIO.write(dst,"png",new File(fd.getDirectory(),fd.getFile()));
                    } catch (IOException ex) {
                        fireError(ex);
                    }
                }
                /*
                JFileChooser chooser = new JFileChooser();
                chooser.showSaveDialog(JXImageView.this);
                File file = chooser.getSelectedFile();
                if(file != null) {
                    try {
                        ImageIO.write(dst,"png",file);
                    } catch (IOException ex) {
                        System.out.println(ex.getMessage());
                        ex.printStackTrace();
                        fireError(ex);
                    }
                }
                 */
            }
        };

        action.putValue(Action.NAME,"Save");
        return action;
    }
    
    /**
     * Get an action which will rotate the currently selected image clockwise.
     * @return an action
     */
    public Action getRotateClockwiseAction() {
        Action action = new AbstractAction() {
            public void actionPerformed(ActionEvent evt) {
                Image img = getImage();
                BufferedImage src = new BufferedImage(
                            img.getWidth(null),
                            img.getHeight(null), 
                            BufferedImage.TYPE_INT_ARGB);
                BufferedImage dst = new BufferedImage(
                            img.getHeight(null), 
                            img.getWidth(null),
                            BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = (Graphics2D)src.getGraphics();
                // smooth scaling
                g.drawImage(img,0,0,null);
                g.dispose();
                AffineTransform trans = AffineTransform.getRotateInstance(Math.PI/2,0,0);
                trans.translate(0,-src.getHeight());
                BufferedImageOp op = new AffineTransformOp(trans, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
                op.filter(src,dst);
                setImage(dst);
            }
        };
        action.putValue(Action.NAME,"Rotate Clockwise");
        return action;        
    }
    
    /**
     * Gets an action which will rotate the current image counter clockwise.
     * @return an Action
     */
    public Action getRotateCounterClockwiseAction() {
        Action action = new AbstractAction() {
            public void actionPerformed(ActionEvent evt) {
                Image img = getImage();
                BufferedImage src = new BufferedImage(
                            img.getWidth(null),
                            img.getHeight(null), 
                            BufferedImage.TYPE_INT_ARGB);
                BufferedImage dst = new BufferedImage(
                            img.getHeight(null), 
                            img.getWidth(null),
                            BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = (Graphics2D)src.getGraphics();
                // smooth scaling
                g.drawImage(img,0,0,null);
                g.dispose();
                AffineTransform trans = AffineTransform.getRotateInstance(-Math.PI/2,0,0);
                trans.translate(-src.getWidth(),0);
                BufferedImageOp op = new AffineTransformOp(trans, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
                op.filter(src,dst);
                setImage(dst);
            }
        };
        action.putValue(Action.NAME, "Rotate CounterClockwise");
        return action;        
    }
       
    /**
     * Gets an action which will zoom the current image out by a factor of 2.
     * @return an action
     */
    public Action getZoomOutAction() {
        Action action = new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                setScale(getScale()*0.5);
            }
        };
        action.putValue(Action.NAME,"Zoom Out");
        return action;
    }
    
    /**
     * Gets an action which will zoom the current image in by a factor of 2
     * @return an action
     */
    public Action getZoomInAction() {
        Action action = new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                setScale(getScale()*2);
            }
        };
        action.putValue(Action.NAME,"Zoom In");
        return action;
    }
    /* === overriden methods === */
    
    /**
     * Implementation detail.
     * @param g 
     */
    protected void paintComponent(Graphics g) {
        ((Graphics2D)g).setPaint(checkerPaint);
        //g.setColor(getBackground());
        g.fillRect(0,0,getWidth(),getHeight());
        if(getImage() != null) {
            Point2D center = new Point2D.Double(getWidth()/2,getHeight()/2);
            if(getImageLocation() != null) {
                center = getImageLocation();
            }
            Point2D loc = new Point2D.Double();
            double width = getImage().getWidth(null)*getScale();
            double height = getImage().getHeight(null)*getScale();
            loc.setLocation(center.getX()-width/2, center.getY()-height/2);
            g.drawImage(getImage(), (int)loc.getX(), (int)loc.getY(),
                    (int)width,(int)height,
                    null);
        }
    }

    
    /* === Internal helper classes === */

    private class MoveHandler extends MouseInputAdapter {
        private JXImageView panel;
        private Point prev = null;
        private Point start = null;
        public MoveHandler(JXImageView panel) {
            this.panel = panel;
        }

        public void mousePressed(MouseEvent evt) {
            prev = evt.getPoint();
            start = prev;
        }

        public void mouseDragged(MouseEvent evt) {
            Point curr = evt.getPoint();
            
            if(isDragEnabled()) {
                //System.out.println("testing drag enabled: " + curr + " " + start);
                //System.out.println("distance = " + curr.distance(start));
                if(curr.distance(start) > 5) {
                    System.out.println("starting the drag: ");
                    panel.getTransferHandler().exportAsDrag((JComponent)evt.getSource(),evt,TransferHandler.COPY);
                    return;
                }
            }
            
            int offx = curr.x - prev.x;
            int offy = curr.y - prev.y;
            Point2D offset = getImageLocation();
            if (offset == null) {
                if (image != null) {
                    offset = new Point2D.Double(getWidth() / 2, getHeight() / 2);
                } else {
                    offset = new Point2D.Double(0, 0);
                }
            }
            offset = new Point2D.Double(offset.getX() + offx, offset.getY() + offy);
            setImageLocation(offset);
            prev = curr;
            repaint();
        }

        public void mouseReleased(MouseEvent evt) {
            prev = null;
        }
    }

    private class DnDHandler extends TransferHandler {
        DataFlavor urlFlavor;
        
        public DnDHandler() throws ClassNotFoundException {
             urlFlavor = new DataFlavor("application/x-java-url;class=java.net.URL");
        }
        
        public void exportAsDrag(JComponent c, InputEvent evt, int action) {
            //System.out.println("exportting as drag");
            super.exportAsDrag(c,evt,action);
        }
        public int getSourceActions(JComponent c) {
            //System.out.println("get source actions: " + c);
            return COPY;
        }
        protected void exportDone(JComponent source, Transferable data, int action) {
            //System.out.println("exportDone: " + source + " " + data + " " +action);
        }

        public boolean canImport(JComponent c, DataFlavor[] flavors) {
            //System.out.println("canImport:" + c);
            for (int i = 0; i < flavors.length; i++) {
                //System.out.println("testing: "+flavors[i]);
                if (DataFlavor.javaFileListFlavor.equals(flavors[i])) {
                    return true;
                }
                if (DataFlavor.imageFlavor.equals(flavors[i])) {
                    return true;
                }
                if (urlFlavor.match(flavors[i])) {
                    return true;
                }
                
            }
            return false;
        }

        protected Transferable createTransferable(JComponent c) {
            JXImageView view = (JXImageView)c;
            return new ImageTransferable(view.getImage(),
                    view.getExportName(), view.getExportFormat());
        }
        public boolean importData(JComponent comp, Transferable t) {
            if (canImport(comp, t.getTransferDataFlavors())) {
                try {
                    if(t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        List files = (List) t.getTransferData(DataFlavor.javaFileListFlavor);
                        //System.out.println("doing file list flavor");
                        if (files.size() > 0) {
                            File file = (File) files.get(0);
                            //System.out.println("readingt hte image: " + file.getCanonicalPath());
                            /*Iterator it = ImageIO.getImageReaders(new FileInputStream(file));
                            while(it.hasNext()) {
                                System.out.println("can read: " + it.next());
                            }*/
                            setImageString(file.toURI().toURL().toString());
                            //BufferedImage img = ImageIO.read(file.toURI().toURL());
                            //setImage(img);
                            return true;
                        }
                    }
                    //System.out.println("doing a uri list");
                    Object obj = t.getTransferData(urlFlavor);
                    //System.out.println("obj = " + obj + " " + obj.getClass().getPackage() + " "
                    //        + obj.getClass().getName());
                    if(obj instanceof URL) {
                        setImageString(((URL)obj).toString());
                    }
                    return true;
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                    ex.printStackTrace();
                    fireError(ex);
                }
            }
            return false;
        }

    }

    
    private class ImageTransferable implements Transferable {
        private Image img;
        private List files;
        private String exportName, exportFormat;
        public ImageTransferable(Image img, String exportName, String exportFormat) {
            this.img = img;
            this.exportName = exportName;
            this.exportFormat = exportFormat;
        }

        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] { DataFlavor.imageFlavor,
                DataFlavor.javaFileListFlavor };
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            if(flavor == DataFlavor.imageFlavor) {
                return true;
            }
            return flavor == DataFlavor.javaFileListFlavor;
        }

        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            //System.out.println("doing get trans data: " + flavor);
            if(flavor == DataFlavor.imageFlavor) {
                return img;
            }
            if(flavor == DataFlavor.javaFileListFlavor) {
                if(files == null) {
                    files = new ArrayList();
                    File file = File.createTempFile(exportName,"."+exportFormat);
                    //System.out.println("writing to: " + file);
                    ImageIO.write(PaintUtils.convertToBufferedImage(img),exportFormat,file);
                    files.add(file);
                }
                //System.out.println("returning: " + files);
                return files;
            }
            return null;
        }
    }

    public String getExportName() {
        return exportName;
    }

    public void setExportName(String exportName) {
        this.exportName = exportName;
    }

    public String getExportFormat() {
        return exportFormat;
    }

    public void setExportFormat(String exportFormat) {
        this.exportFormat = exportFormat;
    }

    public URL getImageURL() {
        return imageURL;
    }

    public void setImageURL(URL imageURL) throws IOException {
        this.imageURL = imageURL;
        setImage(ImageIO.read(getImageURL()));
    }
    
    /** Returns the current image's URL (if available) as a string.
     * If the image has no URL, or if there is no image, then this
     * method will return null.
     * @return the url of the image as a string
     */
    public String getImageString() {
        if(getImageURL() == null) {
            return null;
        }
        return getImageURL().toString();
    }
    
    /** Sets the current image using a string. This string <b>must</b>
     * contain a valid URL.
     * @param url string of a URL
     * @throws java.io.IOException thrown if the URL does not parse
     */
    public void setImageString(String url) throws IOException {
        String old = getImageString();
        setImageURL(new URL(url));
        firePropertyChange("imageString", old, url);
    }
    
}
