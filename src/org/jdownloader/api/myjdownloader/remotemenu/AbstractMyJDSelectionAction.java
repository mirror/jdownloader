package org.jdownloader.api.myjdownloader.remotemenu;

import java.awt.event.ActionEvent;

import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.myjdownloader.client.bindings.interfaces.UIInterface.Context;

public abstract class AbstractMyJDSelectionAction extends CustomizableAppAction {

    private SelectionInfo<?, ?> selection;

    @SuppressWarnings("unchecked")
    protected SelectionInfo<?, ?> getSelection() {
        return selection;
    }

    public abstract String getID();

    public void setSelection(SelectionInfo<?, ?> selection) {
        this.selection = selection;
    }

    public Object performAction(Object src, Context context) {
        actionPerformed(new ActionEvent(src, 0, null));
        return null;
    }
}
