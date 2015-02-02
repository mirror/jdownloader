package org.jdownloader.gui.mainmenu;

import java.awt.event.ActionEvent;

import jd.http.Browser;

import org.appwork.storage.JSonStorage;
import org.appwork.txtresource.TranslationFactory;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.gui.donate.DonationDetails;
import org.jdownloader.gui.donate.DonationDialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.statistics.StatsManager;

public class DonateAction extends CustomizableAppAction {
    public static String SERVER = "https://payments.appwork.org/";
    static {
        if (!Application.isJared(null)) {
            SERVER = "https://payments.appwork.org/test/";
        }
    }

    public DonateAction() {
        setIconKey("heart");
        setName(_GUI._.DonateAction());
        setTooltipText(_GUI._.DonateAction_tt());
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        final ProgressDialog d = new ProgressDialog(new ProgressGetter() {

            @Override
            public String getLabelString() {
                return null;
            }

            @Override
            public int getProgress() {
                return -1;

            }

            @Override
            public String getString() {
                return _GUI._.DonationDialog_layoutDialogContent_please_wait();
            }

            @Override
            public void run() throws Exception {
                DonationDetails details = null;
                try {
                    Browser br = new Browser();

                    String json = br.getPage(SERVER + "payment/getDonationScreenDetails?" + TranslationFactory.getDesiredLanguage() + "&button");
                    details = JSonStorage.restoreFromString(json, DonationDetails.TYPEREF);
                } catch (Throwable e) {
                    StatsManager.I().track("/donation/button/exception/" + e.getMessage());
                } finally {
                    StatsManager.I().track("/donation/button/details/" + (details != null && details.isEnabled()));
                }
                if (details == null || !details.isEnabled() || details.getCategories() == null) {
                    Dialog.getInstance().showErrorDialog(_GUI._.DonationDialog_layoutDialogContent_donation_disabled());

                    return;
                }

                DonationDialog d = new DonationDialog(details);
                UIOManager.I().show(null, d);

            }
        }, UIOManager.BUTTONS_HIDE_OK, _GUI._.DonationDialog_layoutDialogContent_please_wait_title(), _GUI._.DonationDialog_layoutDialogContent_please_wait_msg(), null, null, null);

        try {
            Dialog.getInstance().showDialog(d);
        } catch (final Throwable e1) {

        }

    }

}
