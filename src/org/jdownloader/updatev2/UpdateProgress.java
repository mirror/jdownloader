package org.jdownloader.updatev2;

import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;

import jd.gui.swing.jdgui.components.IconedProcessIndicator;

public class UpdateProgress extends IconedProcessIndicator {

    public UpdateProgress() {
        super(new AbstractIcon(IconKey.ICON_UPDATE, 16));
    }

}
