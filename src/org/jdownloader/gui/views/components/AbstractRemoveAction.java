package org.jdownloader.gui.views.components;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

public abstract class AbstractRemoveAction extends AppAction {

    public AbstractRemoveAction() {
        super();
        setName(_GUI.T.literally_remove());
        setIconKey(IconKey.ICON_REMOVE);
    }

}
