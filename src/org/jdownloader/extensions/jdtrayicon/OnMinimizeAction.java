package org.jdownloader.extensions.jdtrayicon;

import org.jdownloader.extensions.jdtrayicon.translate.T;

public enum OnMinimizeAction {
    TO_TRAY(T._.OnMinimizeAction_totray()),
    TO_TASKBAR(T._.OnMinimizeAction_totaskbar()),
    ASK(T._.OnMinimizeAction_ask());

    private String translation;

    public String getTranslation() {
        return translation;
    }

    private OnMinimizeAction(String translation) {
        this.translation = translation;
    }
}
