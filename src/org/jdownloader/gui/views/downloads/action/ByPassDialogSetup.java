package org.jdownloader.gui.views.downloads.action;

import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.KeyObserver;

public class ByPassDialogSetup implements ActionContext {

    private boolean  bypassDialog               = false;
    private Modifier byPassDialogToggleModifier = null;

    @Customizer(name = "Key Modifier to toggle 'Bypass Rly? Dialog'")
    public Modifier getByPassDialogToggleModifier() {
        return byPassDialogToggleModifier;
    }

    public void setByPassDialogToggleModifier(Modifier byPassDialogToggleModifier) {
        this.byPassDialogToggleModifier = byPassDialogToggleModifier;
    }

    @Customizer(name = "Bypass the 'Really?' Dialog")
    public boolean isBypassDialog() {
        Modifier byPassDialog = getByPassDialogToggleModifier();

        if (byPassDialog != null && KeyObserver.getInstance().isModifierPressed(byPassDialog.getModifier(), false)) { return !bypassDialog; }

        return bypassDialog;
    }

    public void setBypassDialog(boolean bypassDialog) {
        this.bypassDialog = bypassDialog;
    }
}
