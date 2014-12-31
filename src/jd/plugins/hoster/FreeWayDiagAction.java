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
                    MessageDialogImpl dialog = new MessageDialogImpl(UIOManager.LOGIC_COUNTDOWN, getPhrase("FREEWAYDIAGNOSE_SETTINGS_OK"));
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

                    MessageDialogImpl dialog = new MessageDialogImpl(UIOManager.LOGIC_COUNTDOWN, getPhrase("FREEWAYDIAGNOSE_PROBLEMS") + probs);
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
        int numberofFreeWayAccounts = 0;
        List<Account> accs = AccountController.getInstance().getMultiHostAccounts("uploaded.to");
        for (Account acc : accs) {
            if (acc.getHoster().equals("free-way.me")) {
                numberofFreeWayAccounts++;
                if (!acc.isEnabled()) {
                    diagnose.add(String.format(getPhrase("FREEWAYDIAGNOSE_PROBLEMS_ACCOUNT_DEACTIVATED"), acc.getUser()));
                } else if (acc.getAccountInfo().getBooleanProperty(jd.plugins.hoster.FreeWayMe.ACC_PROPERTY_DROSSEL_ACTIVE, false)) {
                    diagnose.add(String.format(getPhrase("FREEWAYDIAGNOSE_PROBLEMS_ACCOUNT_LIMITED"), acc.getUser()));
                }
            }
        }
        if (numberofFreeWayAccounts == 0) {
            /*
             * No free-way.me accounts available --> No 'problems' present! --> Should also already be covered inside the free-way.me
             * plugin.
             */
            diagnose.clear();
            return diagnose;
        }

        if (DownloadWatchDog.getInstance().isPaused()) {
            diagnose.add(getPhrase("FREEWAYDIAGNOSE_PROBLEMS_PAUSE"));
        }

        if (org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT_ENABLED.getValue()) {
            diagnose.add(getPhrase("FREEWAYDIAGNOSE_PROBLEMS_SPEEDLIMIT"));
        }

        if (org.jdownloader.settings.staticreferences.CFG_GENERAL.MAX_SIMULTANE_DOWNLOADS.getValue() < 2) {
            diagnose.add(getPhrase("FREEWAYDIAGNOSE_PROBLEMS_SIMULTANDLS"));
        }

        if (!org.jdownloader.settings.staticreferences.CFG_GENERAL.USE_AVAILABLE_ACCOUNTS.getValue()) {
            diagnose.add(getPhrase("FREEWAYDIAGNOSE_PROBLEMS_ACCOUNTS_DEACTIVATED"));
        }
        return diagnose;
    }

    private String getPhrase(final String phrasekey) {
        return jd.plugins.hoster.FreeWayMe.getPhrase(phrasekey);
    }
}
