package org.jdownloader.gui.views.downloads.contextmenumanager;

import org.appwork.storage.config.ConfigInterface;

public interface ContextMenuConfigInterface extends ConfigInterface {

    MenuContainerRoot getMenuStructure();

    public void setMenuStructure(MenuContainerRoot root);

}
