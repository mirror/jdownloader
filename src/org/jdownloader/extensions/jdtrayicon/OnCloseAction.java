package org.jdownloader.extensions.jdtrayicon;

import org.jdownloader.extensions.jdtrayicon.translate.T;

public enum OnCloseAction {
    TO_TRAY(T._.OnCloseAction_totray()),
    TO_TASKBAR(T._.OnCloseAction_totaskbar()),
    EXIT(T._.OnCloseAction_exit()),
    ASK(T._.OnMinimizeAction_ask());
    private String translation;

    public String getTranslation() {
        return translation;
    }

    private OnCloseAction(String translation) {
        this.translation = translation;
    }
}
