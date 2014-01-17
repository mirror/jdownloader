package org.jdownloader.updatev2;

import javax.swing.Icon;

public class UpdateStatusUpdateEvent extends UpdaterEvent {
    private String label;
    private Icon   icon;
    private double progress;

    public String getLabel() {
        return label;
    }

    public Icon getIcon() {
        return icon;
    }

    public double getProgress() {
        return progress;
    }

    public UpdateStatusUpdateEvent(UpdateController updateController, String statusLabel, Icon statusIcon, double statusProgress) {
        super(updateController, Type.UPDATE_STATUS);

        this.label = statusLabel;
        this.icon = statusIcon;
        this.progress = statusProgress;

    }

}
