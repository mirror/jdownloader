package org.jdownloader.images;

import java.awt.Image;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.appwork.swing.components.IDIcon;
import org.appwork.swing.components.IconIdentifier;

public class IdentifierImageIcon extends ImageIcon implements Icon, IDIcon {

    private String id;

    public IdentifierImageIcon(Image image, String relativePath) {
        super(image);
        this.id = relativePath;
    }

    @Override
    public IconIdentifier getIdentifier() {
        return new IconIdentifier(null, id);
    }

}
