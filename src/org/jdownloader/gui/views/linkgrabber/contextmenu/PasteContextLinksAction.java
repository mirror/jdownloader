package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import jd.gui.swing.jdgui.interfaces.View;

import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.KeyObserver;
import org.jdownloader.gui.event.GUIEventSender;
import org.jdownloader.gui.event.GUIListener;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.bottombar.PasteLinksAction;
import org.jdownloader.translate._JDT;

public class PasteContextLinksAction extends CustomizableTableContextAppAction implements GUIListener, ActionContext {
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

    private boolean metaCtrl = false;

    @Override
    public void onKeyModifier(int parameter) {
        if (KeyObserver.getInstance().isControlDown(false) || KeyObserver.getInstance().isMetaDown(false)) {
            metaCtrl = true;
        } else {
            metaCtrl = false;
        }
        update();
    }

    @Override
    public void requestUpdate(Object requestor) {
        super.requestUpdate(requestor);
        onKeyModifier(-1);
    }

    private void update() {
        if (isDeepDecrypt()) {
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
        GUIEventSender.getInstance().addListener(this, true);
        metaCtrl = KeyObserver.getInstance().isMetaDown(true) || KeyObserver.getInstance().isControlDown(true);
    }

    private boolean isDeepDecrypt() {
        boolean ret = isDeepDecryptEnabled();
        if (ret == false) {
            return metaCtrl;
        } else {
            return !metaCtrl;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        PasteLinksAction.processPaste(isDeepDecrypt());
    }

    @Override
    public void onGuiMainTabSwitch(View oldView, View newView) {
    }
}
