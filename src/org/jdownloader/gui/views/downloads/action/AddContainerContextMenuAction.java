package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import jd.gui.swing.jdgui.menu.actions.AddContainerAction;

import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.controlling.contextmenu.TableContext;
import org.jdownloader.gui.translate._GUI;

public class AddContainerContextMenuAction extends CustomizableAppAction {

    private static final long serialVersionUID = 1901008532686173167L;

    public AddContainerContextMenuAction() {
        super();
        addContextSetup(new TableContext(true, false));

        setName(_GUI._.action_addcontainer());
        setTooltipText(_GUI._.action_addcontainer_tooltip());
        setIconKey("load");

    }

    public void actionPerformed(ActionEvent e) {
        new AddContainerAction().actionPerformed(e);
    }

}