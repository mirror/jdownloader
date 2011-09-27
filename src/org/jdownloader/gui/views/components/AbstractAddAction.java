package org.jdownloader.gui.views.components;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public abstract class AbstractAddAction extends AppAction {

    public AbstractAddAction() {
        super();
        setName(_GUI._.literally_add());
        setIconKey("add");

    }

}
