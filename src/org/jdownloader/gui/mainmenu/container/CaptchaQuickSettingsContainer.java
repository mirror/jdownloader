package org.jdownloader.gui.mainmenu.container;

import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.gui.IconKey;
import org.jdownloader.translate._JDT;

public class CaptchaQuickSettingsContainer extends MenuContainer {
    public CaptchaQuickSettingsContainer(boolean visible) {
        this();
        setVisible(visible);
    }

    public CaptchaQuickSettingsContainer(/* STorable */) {

        setName(_JDT.T.CaptchaQuickSettingsContainer_CaptchaQuickSettingsContainer());
        setIconKey(IconKey.ICON_OCR);

    }

}
