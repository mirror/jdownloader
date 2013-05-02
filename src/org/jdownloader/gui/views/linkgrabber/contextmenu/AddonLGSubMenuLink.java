package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.lang.reflect.InvocationTargetException;

import javax.swing.JComponent;

import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.MenuLink;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class AddonLGSubMenuLink extends MenuItemData implements MenuLink {
    public AddonLGSubMenuLink() {
        setName(_GUI._.AddonSubMenuLink_AddonSubMenuLink_());
        setIconKey("extension");
    }

    public JComponent addTo(JComponent root, SelectionInfo<?, ?> selection) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        // if (!showItem(selection)) return null;
        // if (selection.getContextPackage() instanceof CrawledPackage) {
        // int count = root.getComponentCount();
        // MenuFactoryEventSender.getInstance().fireEvent(new MenuFactoryEvent(MenuFactoryEvent.Type.EXTEND, new
        // LinkgrabberTableContext(root, (SelectionInfo<CrawledPackage, CrawledLink>) selection, selection.getContextColumn())));
        //
        // if (root.getComponentCount() > count) {
        // root.add(new JSeparator());
        // }
        //
        // return null;
        // } else {
        // throw new WTFException("");
        // }
        return null;
    }
}
