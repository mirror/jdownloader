package org.jdownloader.gui.mainmenu.action;

import org.jdownloader.actions.AppAction;
import org.jdownloader.actions.CachableInterface;

public abstract class AbstractMainMenuAction extends AppAction implements CachableInterface {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public AbstractMainMenuAction() {
        super();
    }

    @Override
    public void setData(String data) {
    }

}
