package org.jdownloader.gui.views.linkgrabber.bottombar;

import java.lang.reflect.InvocationTargetException;

import javax.swing.JComponent;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.MenuLink;
import org.jdownloader.extensions.ExtensionNotLoadedException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.bottombar.SelfLayoutInterface;

public class AutoConfirmMenuLink extends MenuItemData implements MenuLink, SelfLayoutInterface {

    protected static AutoConfirmButton INSTANCE;

    @Override
    public JComponent createItem() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, SecurityException, ExtensionNotLoadedException {
        if (INSTANCE != null) return INSTANCE;
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                INSTANCE = new AutoConfirmButton();
            }
        }.waitForEDT();

        return INSTANCE;

    }

    @Override
    public String getName() {
        return _GUI._.AutoConfirmMenuLink_getName();
    }

    @Override
    public String getIconKey() {
        return "paralell";
    }

    @Override
    public String createConstraints() {
        return "height 24!,width 24!,hidemode 3,gapright 3";
    }

}
