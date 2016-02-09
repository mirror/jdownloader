package org.jdownloader.gui.jdtrayicon;

import org.jdownloader.gui.jdtrayicon.translate._TRAY;

public enum OnCloseAction {
    TO_TRAY(_TRAY.T.OnCloseAction_totray()),
    TO_TASKBAR(_TRAY.T.OnCloseAction_totaskbar()),
    EXIT(_TRAY.T.OnCloseAction_exit()),
    ASK(_TRAY.T.OnMinimizeAction_ask());
    private String translation;

    public String getTranslation() {
        return translation;
    }

    private OnCloseAction(String translation) {
        this.translation = translation;
    }
}
