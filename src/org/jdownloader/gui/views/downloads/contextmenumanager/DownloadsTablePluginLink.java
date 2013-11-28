package org.jdownloader.gui.views.downloads.contextmenumanager;

import java.lang.reflect.InvocationTargetException;

import javax.swing.JComponent;

import jd.plugins.DownloadLink;

import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.MenuLink;
import org.jdownloader.extensions.ExtensionNotLoadedException;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo.PluginView;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;

public class DownloadsTablePluginLink extends MenuItemData implements MenuLink {

    @Override
    public String getName() {
        return _GUI._.DownloadsTablePluginLink_getName_object_();
    }

    @Override
    public String getIconKey() {
        return IconKey.ICON_PLUGIN;
    }

    @Override
    public JComponent addTo(JComponent root) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, SecurityException, ExtensionNotLoadedException {
        for (PluginView<DownloadLink> pv : DownloadsTable.getInstance().getSelectionInfo().getPluginViews()) {
            pv.getPlugin().extendDownloadsTableContextMenu(root, pv);
        }
        return null;
    }
}
