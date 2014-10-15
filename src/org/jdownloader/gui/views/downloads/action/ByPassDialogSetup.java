package org.jdownloader.gui.views.downloads.action;

import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.KeyObserver;
import org.jdownloader.translate._JDT;

public class ByPassDialogSetup implements ActionContext {

    private boolean  bypassDialog               = false;
    private Modifier byPassDialogToggleModifier = null;

    public static String getTranslationForByPassDialogToggleModifier() {
        return _JDT._.ByPassDialogSetup_getTranslationForByPassDialogToggleModifier();
    }

    public static String getTranslationForBypassDialog() {
        return _JDT._.ByPassDialogSetup_getTranslationForBypassDialog();
    }

    @Customizer(link = "#getTranslationForByPassDialogToggleModifier")
    public Modifier getByPassDialogToggleModifier() {
        return byPassDialogToggleModifier;
    }

    public void setByPassDialogToggleModifier(Modifier byPassDialogToggleModifier) {
        this.byPassDialogToggleModifier = byPassDialogToggleModifier;
    }

    @Customizer(link = "#getTranslationForBypassDialog")
    public boolean isBypassDialog() {
        Modifier byPassDialog = getByPassDialogToggleModifier();

        if (byPassDialog != null && KeyObserver.getInstance().isModifierPressed(byPassDialog.getModifier(), false)) {
            return !bypassDialog;
        }

        return bypassDialog;
    }

    public void setBypassDialog(boolean bypassDialog) {
        this.bypassDialog = bypassDialog;
    }
}
