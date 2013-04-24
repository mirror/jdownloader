package org.jdownloader.gui.views.downloads.contextmenumanager;

import java.util.ArrayList;

import org.appwork.storage.config.ConfigInterface;

public interface ContextMenuConfigInterface extends ConfigInterface {

    MenuContainerRoot getMenuStructure();

    public void setMenuStructure(MenuContainerRoot root);

    void setUnusedActions(ArrayList<ActionData> list);

    ArrayList<ActionData> getUnusedActions();

}
