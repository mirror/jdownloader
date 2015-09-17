package org.jdownloader.gui.mainmenu;

import java.awt.event.ActionEvent;
import java.io.File;

import jd.http.Browser;
import jd.http.URLConnectionAdapter;

import org.appwork.storage.JSonStorage;
import org.appwork.txtresource.TranslationFactory;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.Exceptions;
import org.appwork.utils.Regex;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.gui.donate.DonateFeedback;
import org.jdownloader.gui.donate.DonationDetails;
import org.jdownloader.gui.donate.DonationDialog;
import org.jdownloader.gui.donate.PaymentProvider;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.statistics.StatsManager;

public class DonateAction extends CustomizableAppAction {
    protected static final long A_WEEK = 1 * 7 * 24 * 60 * 60 * 1000l;
    public static String        SERVER = "https://payments.appwork.org/";
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
                    File iconFolder = Application.getResource("tmp/donateicons/");

                    String json = br.getPage(getUrl("payment", "getDonationScreenDetails", TranslationFactory.getDesiredLanguage(), "button"));
                    details = JSonStorage.restoreFromString(json, DonationDetails.TYPEREF);

                    for (PaymentProvider p : details.getPaymentProvider()) {
                        try {

                            File file = new File(iconFolder, p.getLocaleName() + ".png");
                            if (!file.exists() || (System.currentTimeMillis() - file.lastModified()) > A_WEEK) {
                                URLConnectionAdapter con = br.openGetConnection(getUrl(p.getApi(), "getIcon", TranslationFactory.getDesiredLanguage(), "button"));
                                try {
                                    if (con.isOK()) {
                                        file.delete();
                                        Browser.download(file, con);
                                    }
                                } finally {
                                    try {
                                        con.disconnect();
                                    } catch (Throwable e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }

                } catch (Throwable e) {
                    final Throwable oe = e;
                    try {
                        //
                        if (e.getCause() != null) {
                            e = e.getCause();
                        }
                        final StringBuilder sb = new StringBuilder();
                        final String[] lines = Regex.getLines(Exceptions.getStackTrace(e));

                        for (String line : lines) {
                            if (line.contains("org.jdownloader.gui")) {
                                break;
                            }
                            if (sb.length() > 0) {
                                sb.insert(0, "/");
                            }
                            if (line.startsWith("at")) {
                                sb.insert(0, URLEncode.encodeRFC2396(line.replaceAll("(^.*)(\\.[a-zA-Z0-9]+\\()", "$2").replaceAll("(at | |\\(|\\)|\\$|\\.|:)", "")));
                            } else {
                                sb.insert(0, URLEncode.encodeRFC2396(line.replaceAll("(at | |\\(|\\)|\\$|\\.|:)", "")));
                            }
                        }
                        StatsManager.I().track("/donation/button/exception/" + sb.toString());
                        StatsManager.I().track("/donation/button/exception/" + URLEncode.encodeRFC2396(e.getClass() + "/" + e.getMessage()));
                    } catch (Throwable e2) {
                        StatsManager.I().track("/donation/button/exception/" + URLEncode.encodeRFC2396(e2.getClass() + "/" + e2.getMessage()));
                    }
                    DonateFeedback.reportFailed(oe);
                } finally {

                }
                if (details == null || !details.isEnabled() || details.getCategories() == null) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    Dialog.getInstance().showErrorDialog(_GUI._.DonationDialog_layoutDialogContent_donation_disabled());
                    return;
                }
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                DonationDialog d = new DonationDialog(details);
                UIOManager.I().show(null, d);

            }
        }, UIOManager.BUTTONS_HIDE_OK, _GUI._.DonationDialog_layoutDialogContent_please_wait_title(), _GUI._.DonationDialog_layoutDialogContent_please_wait_msg(), null, null, null);
        d.setWaitForTermination(0);
        try {
            Dialog.getInstance().showDialog(d);
        } catch (final Throwable e1) {

        }

    }

    /**
     * @param method
     * @param params
     * @return
     */
    private static String getUrl(String namespace, String method, Object... params) {
        StringBuilder url = new StringBuilder();
        url.append(SERVER).append(namespace).append("/").append(method);
        if (params != null && params.length > 0) {

            for (int i = 0; i < params.length; i++) {
                url.append(i > 0 ? "&" : "?");
                url.append(JSonStorage.serializeToJson(params[i]));
            }
        }
        String u = url.toString();
        return u;
    }
}
