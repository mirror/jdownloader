package org.jdownloader.gui.mainmenu.container;

import java.util.HashSet;

import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.controlling.contextmenu.MenuItemProperty;
import org.jdownloader.translate._JDT;

public class CaptchaQuickSettingsContainer extends MenuContainer {
    public CaptchaQuickSettingsContainer(/* STorable */) {
        this((MenuItemProperty[]) null);
    }

    public CaptchaQuickSettingsContainer(MenuItemProperty... properties) {

        setName(_JDT._.CaptchaQuickSettingsContainer_CaptchaQuickSettingsContainer());
        setIconKey("ocr");
        if (properties != null && properties.length > 0) {
            HashSet<MenuItemProperty> props = new HashSet<MenuItemProperty>();
            for (MenuItemProperty mip : properties) {
                props.add(mip);
            }
            setProperties(props);
        }

    }

}
