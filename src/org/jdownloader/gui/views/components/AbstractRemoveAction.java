package org.jdownloader.gui.views.components;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public abstract class AbstractRemoveAction extends AppAction {

    public AbstractRemoveAction() {
        super();
        setName(_GUI._.literally_remove());
        setIconKey("remove");

    }

}
