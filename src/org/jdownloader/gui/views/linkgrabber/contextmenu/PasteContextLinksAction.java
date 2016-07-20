package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.bottombar.PasteLinksAction;
import org.jdownloader.translate._JDT;

public class PasteContextLinksAction extends CustomizableTableContextAppAction {

    public static final String DEEP_DECRYPT_ENABLED = "deepDecryptEnabled";
    private boolean            deepDecryptEnabled   = false;

    public static String getTranslationForDeepDecryptEnabled() {
        return _JDT.T.PasteContextLinksAction_getTranslationForDeepDecryptEnabled();
    }

    @Customizer(link = "#getTranslationForDeepDecryptEnabled")
    public boolean isDeepDecryptEnabled() {

        return deepDecryptEnabled;
    }

    public void setDeepDecryptEnabled(boolean deepDecryptEnabled) {
        this.deepDecryptEnabled = deepDecryptEnabled;
        update();
    }

    @Override
    public void initContextDefaults() {
        super.initContextDefaults();
    }

    @Override
    public void requestUpdate(Object requestor) {
        super.requestUpdate(requestor);
        update();
    }

    private void update() {
        if (isDeepDecryptEnabled()) {
            setName(_GUI.T.PasteContextLinksAction_deep());
            setIconKey(IconKey.ICON_CLIPBOARD);
        } else {
            setName(_GUI.T.PasteContextLinksAction());
            setIconKey(IconKey.ICON_CLIPBOARD);
        }
    }

    public PasteContextLinksAction() {
        super(true, false);
        setIconKey(IconKey.ICON_CLIPBOARD);
        setAccelerator(KeyEvent.VK_V);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        PasteLinksAction.processPaste(isDeepDecryptEnabled());
    }

}
