package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.addlinksdialog.AddLinksDialog;

import jd.controlling.linkcollector.LinkCollectingJob;

public class AddLinksAction extends CustomizableAppAction implements ActionContext {
    /**
     *
     */
    private static final long serialVersionUID = -1824957567580275989L;

    public static enum EnableDisableUnchanged implements LabelInterface {
        ENABLED {
            @Override
            public String getLabel() {
                return _GUI.T.lit_enable();
            }
        },
        DISABLE {
            @Override
            public String getLabel() {
                return _GUI.T.lit_disable();
            }
        },
        UNCHANGED {
            @Override
            public String getLabel() {
                return _GUI.T.do_not_change_use_global_settings();
            }
        }
    }

    private EnableDisableUnchanged autoConfirmDownloads   = EnableDisableUnchanged.UNCHANGED;
    /**
     * @return the autoConfirmDownloads
     */
    public static final String     AUTO_CONFIRM_DOWNLOADS = "autoConfirmDownloads";

    @Customizer(link = "#getTranslationAutoConfirmDownloads")
    public EnableDisableUnchanged getAutoConfirmDownloads() {
        return autoConfirmDownloads;
    }

    /**
     * @param autoConfirmDownloads
     *            the autoConfirmDownloads to set
     */
    public void setAutoConfirmDownloads(EnableDisableUnchanged autoConfirmDownloads) {
        this.autoConfirmDownloads = autoConfirmDownloads;
    }

    private EnableDisableUnchanged autoStartDownloads = EnableDisableUnchanged.UNCHANGED;

    public static String getTranslationAutoStartDownloads() {
        return _GUI.T.PackagizerFilterRuleDialog_layoutDialogContent_autostart2();
    }

    public static String getTranslationAutoConfirmDownloads() {
        return _GUI.T.PackagizerFilterRuleDialog_layoutDialogContent_autoadd2();
    }

    /**
     * @return the autoStartDownloads
     */
    @Customizer(link = "#getTranslationAutoStartDownloads")
    public EnableDisableUnchanged getAutoStartDownloads() {
        return autoStartDownloads;
    }

    /**
     * @param autoStartDownloads
     *            the autoStartDownloads to set
     */
    public static final String AUTO_START_DOWNLOADS = "autoStartDownloads";

    public void setAutoStartDownloads(EnableDisableUnchanged autoStartDownloads) {
        this.autoStartDownloads = autoStartDownloads;
    }

    public AddLinksAction(String string) {
        super();
        setName(string);
        setIconKey(IconKey.ICON_ADD);
        setTooltipText(_GUI.T.AddLinksAction_AddLinksAction_tt());
        setAccelerator(KeyEvent.VK_L, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    }

    public AddLinksAction() {
        this(_GUI.T.AddLinksToLinkgrabberAction());
    }

    @Override
    public void setEnabled(boolean newValue) {
        super.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
        new Thread("AddLinksAction") {
            public void run() {
                try {
                    AddLinksDialog dialog = new AddLinksDialog();
                    dialog.setAutoConfirm(getAutoConfirmDownloads());
                    dialog.setAutoStart(getAutoStartDownloads());
                    UIOManager.I().show(null, dialog);
                    dialog.throwCloseExceptions();
                    final LinkCollectingJob crawljob = dialog.getReturnValue();
                    AddLinksProgress d = new AddLinksProgress(crawljob);
                    if (d.isHiddenByDontShowAgain()) {
                        d.getAddLinksDialogThread(crawljob, null).start();
                    } else {
                        Dialog.getInstance().showDialog(d);
                    }
                } catch (DialogNoAnswerException e1) {
                }
            }
        }.start();
    }
}
