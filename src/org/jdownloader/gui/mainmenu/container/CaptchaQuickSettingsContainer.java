package org.jdownloader.gui.mainmenu.container;

import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.translate._JDT;

public class CaptchaQuickSettingsContainer extends MenuContainer {
    public CaptchaQuickSettingsContainer(boolean visible) {
        this();
        setVisible(visible);
    }

    public CaptchaQuickSettingsContainer(/* STorable */) {

        setName(_JDT._.CaptchaQuickSettingsContainer_CaptchaQuickSettingsContainer());
        setIconKey("ocr");

    }

}
