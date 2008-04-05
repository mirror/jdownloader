/*
 * ImageEditor.java
 *
 * Created on July 21, 2006, 1:35 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jdesktop.swingx.editors;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditorSupport;
import javax.swing.JButton;

/**
 *
 * @author joshy
 */
public class ImageEditor extends PropertyEditorSupport {
    Image image = null;
    ImagePicker picker = new ImagePicker();
    /** Creates a new instance of ImageEditor */
    public ImageEditor() {
        picker.imageView.addPropertyChangeListener("image",new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                image = picker.imageView.getImage();
                firePropertyChange();
            }
        });
    }
    
    public Image getValue() {
        return image;
    }
    
    public void setValue(Object object) {
        image = (Image)object;
        super.setValue(image);
        picker.imageView.setImage(image);
    }

    public void setAsText(String text) throws IllegalArgumentException {
        // do nothing right now
    }
    
    public String getAsText() {
        return "an Image";
    }

    public void paintValue(Graphics graphics, Rectangle r) {
        graphics.drawImage(image, (int)r.getX(), (int)r.getY(),
                (int)r.getWidth(), (int)r.getHeight(), null);   
    }
    

    public boolean isPaintable() {
        return true;
    }

    public boolean supportsCustomEditor() {
        return true;
    }

    public Component getCustomEditor() {
        return picker;
    }
    
}
