package org.jdownloader.gui.donate;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.http.Browser;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.StorageException;
import org.appwork.storage.TypeRef;
import org.appwork.swing.MigPanel;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.Exceptions;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.logging.Log;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.jdownloader.donate.DonationManager;
import org.jdownloader.gui.mainmenu.DonateAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.logging.LogController;
import org.jdownloader.statistics.StatsManager;

public class DonationDialog extends AbstractDialog<Object> {

    private LogSource         logger;
    // private String symbol;
    // private String currencyCode;
    // private JFormattedTextField input;
    // private Border defaultBorder;
    // private JLabel recurringLabel;
    // private JCheckBox recurring;
    // private PseudoMultiCombo<CategoryPriority> catSel;
    // private ExtTextArea note;
    // protected long prioritySum;
    protected DonationDetails details;
    private Browser           br;
    protected String          transactionID;
    private JTabbedPane       tabbed;
    protected ProviderPanel   currentPanel;

    public DonationDialog(DonationDetails details2) {
        super(0, _GUI._.DonationDialog_DonationDialog_title_(), null, _GUI._.DonationDialog_ok(), null);
        logger = LogController.getInstance().getLogger("DonationDialog");
        this.details = details2;
        br = new Browser();
    }

    @Override
    protected Object createReturnValue() {
        return null;
    }

    @Override
    protected int getPreferredHeight() {
        return 420;
    }

    @Override
    protected int getPreferredWidth() {
        return 740;
    }

    public void actionPerformed(final ActionEvent e) {

        if (e.getSource() == this.okButton) {
            Log.L.fine("Answer: Button<OK:" + this.okButton.getText() + ">");
            this.setReturnmask(true);
            final double amt = getProviderPanel().getAmount();
            if (amt <= 0) {
                return;
            }

            final String[] list = getProviderPanel().getSelectedCategories();
            final boolean recurringValue = getProviderPanel().isRecurring();
            final String noteText = getProviderPanel().getNote();
            final String cCode = getProviderPanel().getCurrencyCode();
            final String provider = getProviderPanel().getProvider();
            final HashMap<String, String> custom = new HashMap<String, String>();
            custom.put("source", "button");
            custom.put("os", CrossSystem.getOS().name());
            final AtomicBoolean close = new AtomicBoolean(true);
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

                    try {
                        // createDonation&String1&String2&double1&boolean1&HashMap1&String[]1&String3
                        String json = br.getPage(DonateAction.SERVER + "payment/createDonation?" + toQuery(provider, cCode, amt, recurringValue, custom, list, noteText));
                        transactionID = JSonStorage.restoreFromString(json, TypeRef.STRING);

                        StatsManager.I().track("/donation/button/redirect");
                        CrossSystem.openURL(DonateAction.SERVER + "payment/donationRedirect?" + toQuery(transactionID));

                        while (true) {
                            try {
                                String url = DonateAction.SERVER + "payment/getStatus?" + toQuery(transactionID);

                                final String jsonStatus = br.getPage(url);

                                TransactionStatus enu = JSonStorage.restoreFromString(jsonStatus, new TypeRef<TransactionStatus>() {
                                });

                                switch (enu) {
                                case DONE:
                                    StatsManager.I().track("/donation/button/success");
                                    DonationManager.getInstance().autoHide();
                                    writeTransactionFile(amt, list, noteText, cCode, provider);
                                    Dialog.getInstance().showMessageDialog(_GUI._.DonationDialog_run_thanks_());
                                    close.set(true);
                                    return;
                                case FAILED:
                                    StatsManager.I().track("/donation/button/failed");
                                    Dialog.getInstance().showMessageDialog(_GUI._.DonationDialog_run_failed());
                                    close.set(false);
                                    return;
                                case CANCELED:
                                    StatsManager.I().track("/donation/button/canceled");
                                    Dialog.getInstance().showMessageDialog(_GUI._.DonationDialog_run_cancel());
                                    close.set(false);
                                    return;

                                case PENDING:
                                case UNKNOWN:

                                }

                            } catch (Throwable e) {
                                logger.log(e);
                            }
                            Thread.sleep(1000);
                        }
                    } catch (InterruptedException e) {
                        StatsManager.I().track("/donation/button/exception/InterruptedException");
                        startFallbackWaiter(list, noteText, amt, provider, cCode);
                        close.set(true);
                    } catch (Throwable e) {
                        try {
                            if (e.getCause() != null) {
                                e = e.getCause();
                            }
                            final StringBuilder sb = new StringBuilder();
                            final String[] lines = Regex.getLines(Exceptions.getStackTrace(e));

                            for (String line : lines) {
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

                        logger.log(e);
                        custom.put("source", "buttonFallback");

                        new EDTRunner() {

                            @Override
                            protected void runInEDT() {

                                try {
                                    CrossSystem.openURL(DonateAction.SERVER + "payment/fallbackDonation?" + toQuery(provider, cCode, amt, recurringValue, custom, list, noteText));
                                } catch (Throwable e1) {
                                    logger.log(e1);

                                }
                                // Util.showErrorMessage(getInstallerContext().getMessage("donate_failed"));

                            }
                        };
                    } finally {

                    }

                }
            }, UIOManager.BUTTONS_HIDE_OK, _GUI._.DonationDialog_layoutDialogContent_please_wait_title(), _GUI._.DonationDialog_layoutDialogContent_please_wait_progress_msg(), null, null, null);

            try {
                Dialog.getInstance().showDialog(d);
            } catch (DialogCanceledException e1) {

                close.set(false);

            } catch (final Throwable e1) {

            }
            if (!close.get()) {
                return;
            }

        } else if (e.getSource() == this.cancelButton) {
            Log.L.fine("Answer: Button<CANCEL:" + this.cancelButton.getText() + ">");
            this.setReturnmask(false);
        }

        this.dispose();
    }

    protected String toQuery(Object... objects) throws UnsupportedEncodingException, StorageException {
        StringBuilder sb = new StringBuilder();
        for (Object o : objects) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(URLEncode.encodeRFC2396(JSonStorage.serializeToJson(o)));

        }
        return sb.toString();
    }

    public ProviderPanel getProviderPanel() {
        return (ProviderPanel) tabbed.getSelectedComponent();
    }

    @Override
    public JComponent layoutDialogContent() {

        MigPanel p = new MigPanel("ins 5", "0[]0", "[][grow,fill]");

        JLabel top = new JLabel("<html><b>" + _GUI._.DonationDialog_layoutDialogContent_top_text() + "</b></html>");
        p.add(top, "spanx,pushx,growx");
        tabbed = new JTabbedPane();

        for (PaymentProvider provider : details.getPaymentProvider()) {
            Icon icon = null;
            if ("paypal.com".equalsIgnoreCase(provider.getId())) {
                icon = new AbstractIcon("paypal", 20);
            } else if ("coinbase.com".equalsIgnoreCase(provider.getId())) {
                icon = new AbstractIcon("bitcoin", 20);
            }
            StringBuilder label = new StringBuilder();
            if ("EUR".equalsIgnoreCase(provider.getcCode())) {
                label.append("Euro (").append(provider.getcSymbol() + ") via " + provider.getId());
            } else if ("USD".equalsIgnoreCase(provider.getcCode())) {
                label.append("US Dollar (").append(provider.getcSymbol() + ") via " + provider.getId());
            } else if ("XBT".equalsIgnoreCase(provider.getcCode())) {
                label.append("Bitcoin via " + provider.getId());
            } else {

                label.append(provider.getcCode() + " (").append(provider.getcSymbol() + ") via " + provider.getId());

            }
            tabbed.addTab(label.toString(), icon, new ProviderPanel(details, provider));
        }

        p.add(tabbed, "pushx,growx,spanx,pushy,growy,spany");
        tabbed.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                ProviderPanel newPanel = getProviderPanel();
                if (currentPanel != null && newPanel != currentPanel && newPanel != null) {
                    newPanel.setSelectedCategories(currentPanel.getSelectedCategories());
                    newPanel.setNote(currentPanel.getNote());
                    newPanel.setRecurring(currentPanel.isRecurring());
                }
                currentPanel = newPanel;
            }
        });
        tabbed.setSelectedIndex(details.getDefaultProvider());

        return p;
    }

    public void startFallbackWaiter(final String[] list, final String noteText, final double amt, final String provider, final String cCode) {
        StatsManager.I().track("/donation/button/startfallbackwait");
        new Thread("Wait for Donation") {
            public void run() {
                long start = System.currentTimeMillis();

                while (System.currentTimeMillis() - start < 30 * 60 * 60 * 1000l) {
                    try {
                        String url;

                        url = DonateAction.SERVER + "payment/getStatus?" + toQuery(transactionID);

                        final String jsonStatus = br.getPage(url);

                        TransactionStatus enu = JSonStorage.restoreFromString(jsonStatus, new TypeRef<TransactionStatus>() {
                        });

                        switch (enu) {
                        case DONE:
                            StatsManager.I().track("/donation/button/success/fallbackwait");
                            writeTransactionFile(amt, list, noteText, cCode, provider);
                            DonationManager.getInstance().autoHide();
                            Dialog.getInstance().showMessageDialog(_GUI._.DonationDialog_run_thanks_());

                            return;
                        case FAILED:
                            StatsManager.I().track("/donation/button/failed/fallbackwait");
                            Dialog.getInstance().showMessageDialog(_GUI._.DonationDialog_run_failed());

                            return;
                        case CANCELED:
                            StatsManager.I().track("/donation/button/canceled/fallbackwait");
                            Dialog.getInstance().showMessageDialog(_GUI._.DonationDialog_run_cancel());

                            return;

                        case PENDING:
                        case UNKNOWN:

                        }
                    } catch (Throwable e) {
                        logger.log(e);
                    }

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            };
        }.start();
    }

    /**
     * @param amt
     * @return
     * @throws UnsupportedEncodingException
     */
    public String enc(final Object amt) throws UnsupportedEncodingException {
        return URLEncode.encodeRFC2396(amt + "");
    }

    /**
     * @param amt
     * @param list
     * @param noteText
     * @param cCode
     * @param provider
     * @param cfg
     * @throws IOException
     */
    public void writeTransactionFile(final double amt, final String[] list, final String noteText, final String cCode, final String provider) throws IOException {
        File cfg = Application.getResource("cfg");
        cfg.mkdirs();
        File donation = null;
        int i = 0;
        do {
            donation = new File(cfg, "donation_" + (i++) + ".json");
        } while (donation.exists());
        Transaction transaction = new Transaction();
        transaction.setProvider(provider);
        transaction.setAmount(amt);
        transaction.setCategories(list);
        transaction.setCurrency(cCode);
        transaction.setNote(noteText);
        transaction.setTid(transactionID);
        transaction.setTime(System.currentTimeMillis());
        IO.writeStringToFile(donation, JSonStorage.serializeToJson(transaction));
    }

}
