package org.jdownloader.gui.jdtrayicon;

import org.jdownloader.gui.jdtrayicon.translate._TRAY;

public enum OnCloseAction {
    TO_TRAY(_TRAY._.OnCloseAction_totray()),
    TO_TASKBAR(_TRAY._.OnCloseAction_totaskbar()),
    EXIT(_TRAY._.OnCloseAction_exit()),
    ASK(_TRAY._.OnMinimizeAction_ask());
    private String translation;

    public String getTranslation() {
        return translation;
    }

    private OnCloseAction(String translation) {
        this.translation = translation;
    }
}
