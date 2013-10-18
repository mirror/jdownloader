package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import jd.gui.swing.jdgui.menu.actions.AddContainerAction;

import org.jdownloader.actions.AbstractContextMenuAction;
import org.jdownloader.gui.translate._GUI;

public class AddContainerContextMenuAction extends AbstractContextMenuAction {

    private static final long serialVersionUID = 1901008532686173167L;

    public AddContainerContextMenuAction() {
        super();
        setItemVisibleForEmptySelection(true);
        setItemVisibleForSelections(false);
        setName(_GUI._.action_addcontainer());
        setTooltipText(_GUI._.action_addcontainer_tooltip());
        setIconKey("load");

    }

    public void actionPerformed(ActionEvent e) {
        new AddContainerAction().actionPerformed(e);
    }

}