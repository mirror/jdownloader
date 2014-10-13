package org.jdownloader.gui.views.linkgrabber.bottombar;

import org.jdownloader.controlling.contextmenu.ActionData;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.gui.views.downloads.bottombar.SelfLayoutInterface;
import org.jdownloader.gui.views.linkgrabber.actions.ConfirmSelectionBarAction;

public class ConfirmButtonMenuItem extends MenuItemData implements SelfLayoutInterface {
    public ConfirmButtonMenuItem() {
        super(new ActionData(ConfirmSelectionBarAction.class).putSetup(ConfirmSelectionBarAction.SELECTION_ONLY, false));
    }

    @Override
    public String createConstraints() {
        return "height 24!,aligny top,growx,pushx";
    }

}
