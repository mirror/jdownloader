package org.jdownloader.extensions.jdtrayicon;

import org.jdownloader.extensions.jdtrayicon.translate.T;

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
