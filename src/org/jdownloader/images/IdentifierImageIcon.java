package org.jdownloader.images;

import java.awt.Image;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.appwork.swing.components.IdentifierInterface;

public class IdentifierImageIcon extends ImageIcon implements Icon, IdentifierInterface {

    private String id;

    public IdentifierImageIcon(Image image, String relativePath) {
        super(image);
        this.id = relativePath;
    }

    @Override
    public Object toIdentifier() {
        return id;
    }

}
