package org.jdownloader.controlling.contextmenu;

import java.util.ArrayList;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DisableObjectCache;

public interface ContextMenuConfigInterface extends ConfigInterface {
    @DisableObjectCache
    MenuContainerRoot getMenu();

    public void setMenu(MenuContainerRoot root);

    void setUnusedItems(ArrayList<String> list);

    ArrayList<String> getUnusedItems();

}
