package org.jdownloader.gui.views.linkgrabber.bottombar;

import java.awt.event.ActionEvent;

import jd.controlling.ClipboardMonitoring;
import jd.controlling.ClipboardMonitoring.ClipboardContent;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkOrigin;
import jd.controlling.linkcollector.LinkOriginDetails;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.actions.AddLinksProgress;
import org.jdownloader.translate._JDT;

public class PasteLinksAction extends CustomizableAppAction implements ActionContext {

    public static final String DEEP_DECRYPT_ENABLED = "deepDecryptEnabled";
    private boolean            deepDecryptEnabled   = false;

    public static String getTranslationForDeepDecryptEnabled() {
        return _JDT._.PasteLinksAction_getTranslationForDeepDecryptEnabled();
    }

    @Customizer(link = "#getTranslationForDeepDecryptEnabled")
    public boolean isDeepDecryptEnabled() {

        return deepDecryptEnabled;
    }

    public void setDeepDecryptEnabled(boolean deepDecryptEnabled) {
        this.deepDecryptEnabled = deepDecryptEnabled;
        update();

    }

    private void update() {
        if (isDeepDecryptEnabled()) {
            setName(_GUI._.PasteLinksAction_PasteLinksAction_deep());

            setIconKey(IconKey.ICON_CLIPBOARD);
        } else {
            setName(_GUI._.PasteLinksAction_PasteLinksAction());

            setIconKey(IconKey.ICON_CLIPBOARD);
        }
    }

    public PasteLinksAction() {
        update();

    }

    @Override
    public void actionPerformed(ActionEvent e) {

        new Thread("Add Links Thread") {
            @Override
            public void run() {
                ClipboardContent content = ClipboardMonitoring.getINSTANCE().getCurrentContent();
                final LinkCollectingJob crawljob = new LinkCollectingJob(new LinkOriginDetails(LinkOrigin.PASTE_LINKS_ACTION, null), content != null ? content.getContent() : null);
                if (content != null) {
                    crawljob.setCustomSourceUrl(content.getBrowserURL());
                }
                crawljob.setDeepAnalyse(isDeepDecryptEnabled());

                AddLinksProgress d = new AddLinksProgress(crawljob);
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
