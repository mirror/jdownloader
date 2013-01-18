package org.jdownloader.updatev2;

import jd.gui.swing.jdgui.components.IconedProcessIndicator;

import org.jdownloader.images.NewTheme;

public class UpdateProgress extends IconedProcessIndicator {

    public UpdateProgress() {
        super(NewTheme.I().getIcon("update", 16));
    }

}
