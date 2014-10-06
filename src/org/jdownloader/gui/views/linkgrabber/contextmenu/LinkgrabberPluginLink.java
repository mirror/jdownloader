package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.swing.JComponent;

import jd.controlling.linkcrawler.CrawledLink;

import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.MenuLink;
import org.jdownloader.extensions.ExtensionNotLoadedException;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo.PluginView;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;

public class LinkgrabberPluginLink extends MenuItemData implements MenuLink {

    @Override
    public String getName() {
        return _GUI._.LinkgrabberPluginLink_getName_object_();
    }

    @Override
    public String getIconKey() {
        return IconKey.ICON_PLUGIN;
    }

    @Override
    public List<AppAction> createActionsToLink() {
        return null;
    }

    @Override
    public JComponent createSettingsPanel() {
        return null;
    }

    @Override
    public JComponent addTo(JComponent root) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, SecurityException, ExtensionNotLoadedException {
        List<PluginView<CrawledLink>> views = LinkGrabberTable.getInstance().getSelectionInfo().getPluginViews();
        for (PluginView<CrawledLink> pv : views) {
            pv.getPlugin().extendLinkgrabberContextMenu(root, pv, views);
        }
        return null;
    }
}
