package jd.plugins.hoster;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import jd.controlling.AccountController;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.plugins.Account;

import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.appwork.utils.swing.dialog.MessageDialogImpl;
import org.jdownloader.extensions.AbstractExtensionAction;
import org.jdownloader.extensions.shutdown.ShutdownExtension;
import org.jdownloader.gui.toolbar.MenuManagerMainToolbar;

public class FreeWayDiagAction extends AbstractExtensionAction<ShutdownExtension> {

    /**
     *
     */
    private static final long serialVersionUID = 3534263713185754668L;

    public FreeWayDiagAction() {
        super();
        updateIcon();
        setTooltipText("free-way.me Diagnose");
    }

    private void updateIcon() {
        if (getDiagnose().size() == 0) {
            setIconKey("good");
        } else {
            setIconKey("bad");
        }
        MenuManagerMainToolbar.getInstance().refresh();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        updateIcon();
        final ArrayList<String> diagnose = getDiagnose();
        if (diagnose.size() == 0) {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    MessageDialogImpl dialog = new MessageDialogImpl(UIOManager.LOGIC_COUNTDOWN, "Ihre JDownloader Einstellungen sind für free-way in Ordnung!\r\nBei Problemen bitte an support@free-way.me wenden.");
                    try {
                        Dialog.getInstance().showDialog(dialog);
                    } catch (DialogNoAnswerException e) {
                    }
                }
            });
            t.start();
        } else {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    String probs = "";
                    for (String s : diagnose) {
                        probs += "- " + s + "\r\n";
                    }

                    MessageDialogImpl dialog = new MessageDialogImpl(UIOManager.LOGIC_COUNTDOWN, "Der Download über free-way.me könnte aufgrund folgender Probleme beschränkt sein:\r\n\r\n" + probs);
                    try {
                        Dialog.getInstance().showDialog(dialog);
                    } catch (DialogNoAnswerException e) {
                    }
                }
            });
            t.start();
        }
    }

    public ArrayList<String> getDiagnose() {
        ArrayList<String> diagnose = new ArrayList<String>();

        if (DownloadWatchDog.getInstance().isPaused()) {
            diagnose.add("Sie haben den Pausemodus aktiviert. Dadurch ist die maximale Downloadgeschwindigkeit begrenzt!");
        }
        List<Account> accs = AccountController.getInstance().getMultiHostAccounts("uploaded.to");
        for (Account acc : accs) {
            if (acc.getHoster().equals("free-way.me")) {
                if (!acc.isEnabled()) {
                    diagnose.add("Der free-way Account " + acc.getUser() + " ist nicht aktiviert!");
                } else if (acc.getAccountInfo().getBooleanProperty(jd.plugins.hoster.FreeWayMe.ACC_PROPERTY_DROSSEL_ACTIVE, false)) {
                    diagnose.add("Der free-way Account " + acc.getUser() + " ist gedrosselt!");
                }
            }
        }

        if (org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT_ENABLED.getValue()) {
            diagnose.add("Sie haben ein Geschwindigkeitslimit im JDownloader aktiviert!");
        }

        if (org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_SIMULTANE_DOWNLOADS.getValue() < 2) {
            diagnose.add("Es wird eine höhere Anzahl paralleler Downloads empfohlen!");
        }

        if (!org.jdownloader.settings.staticreferences.CFG_GENERAL.USE_AVAILABLE_ACCOUNTS.getValue()) {
            diagnose.add("Sie haben die Verwendung von Premiumaccounts im JDownloader deaktiviert!");
        }

        return diagnose;
    }
}
