package org.jdownloader.extensions.eventscripter;

import javax.swing.Icon;

import org.jdownloader.gui.settings.AbstractConfigPanel;

public abstract class TriggerSetupPanel extends AbstractConfigPanel {
    public TriggerSetupPanel(int insets) {
        super(insets);
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public String getLeftGap() {
        return "0";
    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public void updateContents() {
    }
}