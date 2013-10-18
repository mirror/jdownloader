package org.jdownloader.gui.views.linkgrabber.bottombar;

import org.jdownloader.controlling.contextmenu.ActionData;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.gui.views.downloads.bottombar.SelfLayoutInterface;
import org.jdownloader.gui.views.linkgrabber.actions.LinkgrabberBarConfirmAllButton;

public class ConfirmMenuItem extends MenuItemData implements SelfLayoutInterface {

    public ConfirmMenuItem() {
        super(new ActionData(LinkgrabberBarConfirmAllButton.class));
    }

    @Override
    public String getConstraints() {
        return "height 24!,pushx,growx";
    }
}
