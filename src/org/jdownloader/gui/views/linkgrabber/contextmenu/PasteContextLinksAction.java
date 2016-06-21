package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import jd.controlling.ClipboardMonitoring;
import jd.controlling.ClipboardMonitoring.ClipboardContent;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkOrigin;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.actions.AddLinksProgress;
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
        // tableContext.setItemVisibleForEmptySelection(true);
        // tableContext.setItemVisibleForSelections(false);
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
        new Thread("Add Links Thread") {
            @Override
            public void run() {
                final ClipboardContent content = ClipboardMonitoring.getINSTANCE().getCurrentContent();
                final LinkCollectingJob crawljob = new LinkCollectingJob(LinkOrigin.PASTE_LINKS_ACTION.getLinkOriginDetails(), content != null ? content.getContent() : null);
                if (content != null) {
                    crawljob.setCustomSourceUrl(content.getBrowserURL());
                }
                crawljob.setDeepAnalyse(isDeepDecryptEnabled());

                final AddLinksProgress d = new AddLinksProgress(crawljob);
                if (d.isHiddenByDontShowAgain()) {
                    d.getAddLinksDialogThread(crawljob, null).start();
                } else {
                    try {
                        Dialog.getInstance().showDialog(d);
                    } catch (DialogClosedException e1) {
                        e1.printStackTrace();
                    } catch (DialogCanceledException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }.start();

    }

}
