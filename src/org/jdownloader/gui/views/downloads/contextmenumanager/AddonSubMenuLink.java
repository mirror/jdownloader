package org.jdownloader.gui.views.downloads.contextmenumanager;

import java.lang.reflect.InvocationTargetException;

import javax.swing.JComponent;
import javax.swing.JSeparator;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.exceptions.WTFException;
import org.jdownloader.gui.menu.eventsender.MenuFactoryEvent;
import org.jdownloader.gui.menu.eventsender.MenuFactoryEventSender;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.table.DownloadTableContext;

public class AddonSubMenuLink extends MenuItemData implements MenuLink {
    public AddonSubMenuLink() {
        setName(_GUI._.AddonSubMenuLink_AddonSubMenuLink_());
        setIconKey("extension");
    }

    public JComponent addTo(JComponent root, SelectionInfo<?, ?> selection) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        if (!showItem(selection)) return null;
        if (selection.getContextPackage() instanceof FilePackage) {
            int count = root.getComponentCount();
            MenuFactoryEventSender.getInstance().fireEvent(new MenuFactoryEvent(MenuFactoryEvent.Type.EXTEND, new DownloadTableContext(root, (SelectionInfo<FilePackage, DownloadLink>) selection, selection.getContextColumn())));
            if (root.getComponentCount() > count) {
                root.add(new JSeparator());
            }

            return null;
        } else {
            throw new WTFException("TODO");
        }

    }
}
