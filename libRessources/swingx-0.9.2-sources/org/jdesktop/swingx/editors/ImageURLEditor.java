package org.jdesktop.swingx.editors;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditorSupport;

/**
 *
 * @author joshy
 */
public class ImageURLEditor extends PropertyEditorSupport {
    Image image = null;
    String imageURL = null;
    ImagePicker picker = new ImagePicker();
    /** Creates a new instance of ImageEditor */
    public ImageURLEditor() {
        picker.imageView.addPropertyChangeListener("image",new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                image = picker.imageView.getImage();
                if(picker.imageView.getImageURL() != null) {
                    imageURL = picker.imageView.getImageURL().toString();
                }
                firePropertyChange();
            }
        });
    }
    
    public String getValue() {
        return imageURL;
    }
    
    public void setValue(Object object) {
        imageURL = (String)object;
        super.setValue(imageURL);
        picker.imageView.setImage(image);
    }
    
    public void setAsText(String text) throws IllegalArgumentException {
        // do nothing right now
    }
    
    public String getAsText() {
        return imageURL;
    }
    
    public void paintValue(Graphics graphics, Rectangle r) {
        //graphics.drawImage(image, (int)r.getX(), (int)r.getY(),
        //        (int)r.getWidth(), (int)r.getHeight(), null);
    }
    
    
    public boolean isPaintable() {
        return false;
    }
    
    public boolean supportsCustomEditor() {
        return true;
    }
    
    public Component getCustomEditor() {
        return picker;
    }
    
}
