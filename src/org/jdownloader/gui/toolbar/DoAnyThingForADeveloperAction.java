package org.jdownloader.gui.toolbar;

import java.awt.event.ActionEvent;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.notify.BasicNotify;
import org.jdownloader.gui.notify.BubbleNotify;
import org.jdownloader.gui.toolbar.action.ToolBarAction;
import org.jdownloader.images.NewTheme;

public class DoAnyThingForADeveloperAction extends ToolBarAction {
    private static int ID = 0;

    public DoAnyThingForADeveloperAction() {
        setName("DoAnyThingForADeveloperAction");
        setIconKey(IconKey.ICON_BATCH);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        final int id = ID++;
        BubbleNotify.getInstance().show(new BasicNotify("BlaBla....", "Even more Bla ...............", NewTheme.I().getIcon(IconKey.ICON_DESKTOP, 32)) {
            public String toString() {
                return "bubble_" + id;
            }
        });
    }

    @Override
    protected String createTooltip() {
        return null;
    }

}
