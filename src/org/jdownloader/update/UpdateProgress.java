package org.jdownloader.update;

import jd.gui.swing.jdgui.components.IconedProcessIndicator;

import org.jdownloader.images.NewTheme;

public class UpdateProgress extends IconedProcessIndicator {

    protected UpdateProgress() {
        super(NewTheme.I().getIcon("update", 16));
    }

}
