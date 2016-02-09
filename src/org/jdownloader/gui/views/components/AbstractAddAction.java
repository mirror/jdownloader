package org.jdownloader.gui.views.components;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

public abstract class AbstractAddAction extends AppAction {

    public AbstractAddAction() {
        super();
        setName(_GUI.T.literally_add());
        setIconKey(IconKey.ICON_ADD);
    }

}
