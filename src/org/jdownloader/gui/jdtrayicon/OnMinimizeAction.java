package org.jdownloader.gui.jdtrayicon;

import org.jdownloader.gui.jdtrayicon.translate.T;

public enum OnMinimizeAction {
    TO_TRAY(T._.OnMinimizeAction_totray()),
    TO_TASKBAR(T._.OnMinimizeAction_totaskbar());

    private String translation;

    public String getTranslation() {
        return translation;
    }

    private OnMinimizeAction(String translation) {
        this.translation = translation;
    }
}
