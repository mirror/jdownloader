package org.jdownloader.gui.donate;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.border.Border;
import javax.swing.text.MaskFormatter;

import jd.http.Browser;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.swing.MigPanel;
import org.appwork.swing.action.BasicAction;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.components.ExtTextArea;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
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
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.mainmenu.DonateAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.ExtRealCheckBoxMenuItem;
import org.jdownloader.gui.views.components.PseudoMultiCombo;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.logging.LogController;
import org.jdownloader.statistics.StatsManager;

public class DonationDialog extends AbstractDialog<Object> {

    private LogSource                          logger;
    private String                             symbol;
    private String                             currencyCode;
    private JFormattedTextField                input;
    private Border                             defaultBorder;
    private JLabel                             recurringLabel;
    private JCheckBox                          recurring;
    private PseudoMultiCombo<CategoryPriority> catSel;
    private ExtTextArea                        note;
    protected long                             prioritySum;
    protected DonationDetails                  details;
    private Browser                            br;
    protected String                           transactionID;

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
        return 400;
    }

    @Override
    protected int getPreferredWidth() {
        return 700;
    }

    public void actionPerformed(final ActionEvent e) {

        if (e.getSource() == this.okButton) {
            Log.L.fine("Answer: Button<OK:" + this.okButton.getText() + ">");
            this.setReturnmask(true);

            String amountString = input.getText().replaceAll("[^0-9\\.\\;\\-]+", "");
            int amount = -1;
            try {
                amount = Integer.parseInt(amountString);
            } catch (Throwable e1) {
            }
            ;
            if (amount < 1 || amount > 1000) {
                input.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.RED));
                Dialog.getInstance().showErrorDialog(_GUI._.DonationDialog_setReturnmask_range());

                return;
            }
            final List<CategoryPriority> sel = catSel.getSelectedItems();
            final String[] list = new String[sel.size()];
            for (int i = 0; i < sel.size(); i++) {
                list[i] = sel.get(i).getCategory();
            }
            final boolean recurringValue = recurring.isSelected();
            final String noteText = note.getText();
            final int amountValue = amount;

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

                        String json = br.getPage(DonateAction.SERVER + "payment/createDonation?" + URLEncode.encodeRFC2396(amountValue + "") + "&" + URLEncode.encodeRFC2396(currencyCode) + "&" + URLEncode.encodeRFC2396(recurringValue + "") + "&" + URLEncode.encodeRFC2396(JSonStorage.serializeToJson(custom)) + "&" + URLEncode.encodeRFC2396(JSonStorage.serializeToJson(sel)) + "&" + URLEncode.encodeRFC2396(noteText));
                        transactionID = JSonStorage.restoreFromString(json, TypeRef.STRING);

                        StatsManager.I().track("/donation/button/redirect");
                        CrossSystem.openURL(DonateAction.SERVER + "payment/donationRedirect?" + URLEncode.encodeRFC2396(JSonStorage.serializeToJson(transactionID)));

                        while (true) {
                            try {
                                String url = DonateAction.SERVER + "payment/getStatus?" + URLEncode.encodeRFC2396(JSonStorage.serializeToJson(transactionID));

                                final String jsonStatus = br.getPage(url);

                                TransactionStatus enu = JSonStorage.restoreFromString(jsonStatus, new TypeRef<TransactionStatus>() {
                                });

                                switch (enu) {
                                case DONE:
                                    StatsManager.I().track("/donation/button/success");
                                    File cfg = Application.getResource("cfg");
                                    cfg.mkdirs();

                                    File donation = null;
                                    int i = 0;
                                    do {
                                        donation = new File(cfg, "donation_" + (i++) + ".json");
                                    } while (donation.exists());
                                    Transaction transaction = new Transaction();
                                    transaction.setAmount(amountValue);
                                    transaction.setCategories(list);
                                    transaction.setCurrency(currencyCode);
                                    transaction.setNote(noteText);
                                    transaction.setTid(transactionID);
                                    transaction.setTime(System.currentTimeMillis());
                                    IO.writeStringToFile(donation, JSonStorage.serializeToJson(transaction));
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

                        startFallbackWaiter(list, noteText, amountValue);
                        close.set(true);
                    } catch (Throwable e) {
                        try {
                            StatsManager.I().track("/donation/button/exception/" + URLEncode.encodeRFC2396(e.getMessage()));
                        } catch (UnsupportedEncodingException e2) {
                            e2.printStackTrace();

                        }

                        logger.log(e);
                        custom.put("source", "buttonFallback");

                        new EDTRunner() {

                            @Override
                            protected void runInEDT() {

                                try {
                                    CrossSystem.openURL(DonateAction.SERVER + "payment/fallbackDonation?" + URLEncode.encodeRFC2396(JSonStorage.serializeToJson(amountValue)) + "&" + URLEncode.encodeRFC2396(JSonStorage.serializeToJson(currencyCode)) + "&" + URLEncode.encodeRFC2396(JSonStorage.serializeToJson(recurringValue)) + "&" + URLEncode.encodeRFC2396(JSonStorage.serializeToJson(custom)) + "&" + URLEncode.encodeRFC2396(JSonStorage.serializeToJson(list)) + "&" + URLEncode.encodeRFC2396(JSonStorage.serializeToJson(note)));
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

    @Override
    public JComponent layoutDialogContent() {

        MigPanel p = new MigPanel("ins 5", "[grow,fill]15[]0", "[]");

        JLabel top = new JLabel("<html><b>" + _GUI._.DonationDialog_layoutDialogContent_top_text() + "</b></html>");
        p.add(top, "spanx,pushx,growx,gapbottom 20");
        MigPanel left = new MigPanel("ins 0,wrap 3", "[][][grow,fill]", "[]");
        p.add(left, "pushy,growy");

        JLabel icon = new JLabel(new AbstractIcon("botty_heart", -1));
        icon.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Color.gray));
        p.add(icon, "aligny bottom");

        left.add(new JLabel(_GUI._.DonationDialog_layoutDialogContent_donate_amount()));

        String lng = System.getProperty("user.language");
        String country = System.getProperty("user.country");
        if (lng == null) {
            lng = "en";
        }
        if (country == null || country.length() != 2) {
            country = "US";
        }
        Locale loc = new Locale(lng, country);

        Currency currency = Currency.getInstance(loc);
        symbol = "€";
        currencyCode = "EUR";

        NumberFormat format = null;
        if ("EUR".equals(currency.getCurrencyCode())) {

        } else {
            symbol = "$";
            currencyCode = "USD";

        }

        MigPanel pre = new MigPanel("ins 0", "[grow,fill][grow,fill][grow,fill][grow,fill][grow,fill][grow,fill][grow,fill]", "[]");
        pre.add(createPreButton(1));
        pre.add(createPreButton(2));
        pre.add(createPreButton(5));
        pre.add(createPreButton(10));
        pre.add(createPreButton(20));
        pre.add(createPreButton(50));
        pre.add(createPreButton(100));

        left.add(pre, "skip 1,spanx");

        MaskFormatter formatter;

        NumberFormat f = NumberFormat.getInstance();
        f.setMaximumFractionDigits(0);
        f.setMinimumFractionDigits(0);
        f.setMaximumIntegerDigits(4);
        f.setMinimumIntegerDigits(1);
        f.setGroupingUsed(false);
        if (f instanceof DecimalFormat) {
            f.setCurrency(currency);

        }
        input = new JFormattedTextField(f);
        input.setValue(5.00);
        defaultBorder = input.getBorder();
        // input.setColumns(20);

        left.add(new JLabel(symbol), "skip 1");
        left.add(input, "pushx,growx");
        input.addFocusListener(new FocusListener() {

            @Override
            public void focusLost(FocusEvent e) {
            }

            @Override
            public void focusGained(FocusEvent e) {
                input.setBorder(defaultBorder);
            }
        });
        recurringLabel = new JLabel(_GUI._.DonationDialog_layoutDialogContent_donate_recurring());
        // left.add(recurringLabel, "hidemode 2");
        recurring = new JCheckBox();
        // left.add(recurring, "skip 1,hidemode 2");
        recurring.setVisible(false);
        recurringLabel.setVisible(false);
        left.add(new JLabel(_GUI._.DonationDialog_layoutDialogContent_donate_category()));
        catSel = new PseudoMultiCombo<CategoryPriority>(new CategoryPriority[] {}) {
            protected String getNothingText() {
                return _GUI._.DonationDialog_layoutDialogContent_donate_for_generel();

            };

            @Override
            public ExtRealCheckBoxMenuItem createMenuItem(final CategoryPriority sc, BasicAction action) {
                return new ExtRealCheckBoxMenuItem(action) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        g.setColor(new Color(54, 114, 119, 128));
                        g.fillRect(0, 0, (getWidth() * sc.getPriority()) / 10000, getHeight());
                        super.paintComponent(g);

                    }
                };
            }

            @Override
            public void setToolTipText(String text) {

            }

            @Override
            protected String getLabel(CategoryPriority sc) {

                return sc.getLabel() + " (" + (prioritySum == 0 ? 0 : ((sc.getPriority() * 100) / +prioritySum)) + "%)";
            }
        };
        left.add(catSel, "skip 1");

        note = new ExtTextArea();
        note.setHelpText(_GUI._.DonationDialog_layoutDialogContent_donate_help_note());
        this.note.setLineWrap(true);
        this.note.setWrapStyleWord(false);
        JScrollPane sp = new JScrollPane(note);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        left.add(sp, "spanx,wmin 10,height 60:600:n,pushx,growx,pushy,growy");

        this.note.setEnabled(false);
        this.input.setEnabled(false);
        this.recurring.setEnabled(false);

        catSel.setValues(details.getCategories().toArray(new CategoryPriority[] {}));
        note.setEnabled(true);
        input.setEnabled(true);
        recurring.setEnabled(true);
        input.setText(details.getDefaultAmount() + "");
        recurringLabel.setVisible(details.isRecurringEnabled());
        recurring.setVisible(details.isRecurringEnabled());
        recurring.setSelected(false);
        long sum = 0;
        for (CategoryPriority c : details.getCategories()) {
            sum += c.getPriority();
        }
        prioritySum = sum;

        return p;
    }

    private Component createPreButton(final int i) {
        ExtButton ret = new ExtButton(new AppAction() {
            {
                setName(i + "" + symbol);
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                StatsManager.I().track("/donation/button/preselect/" + i);
                input.setText(i + "");
            }
        });
        // ret.setRolloverEffectEnabled(true);
        return ret;
    }

    public void startFallbackWaiter(final String[] list, final String noteText, final int amountValue) {
        StatsManager.I().track("/donation/button/startfallbackwait");
        new Thread("Wait for Donation") {
            public void run() {
                long start = System.currentTimeMillis();

                while (System.currentTimeMillis() - start < 30 * 60 * 60 * 1000l) {
                    try {
                        String url;

                        url = DonateAction.SERVER + "payment/getStatus?" + URLEncode.encodeRFC2396(JSonStorage.serializeToJson(transactionID));

                        final String jsonStatus = br.getPage(url);

                        TransactionStatus enu = JSonStorage.restoreFromString(jsonStatus, new TypeRef<TransactionStatus>() {
                        });

                        switch (enu) {
                        case DONE:
                            StatsManager.I().track("/donation/button/success/fallbackwait");
                            File cfg = Application.getResource("cfg");
                            cfg.mkdirs();

                            File donation = null;
                            int i = 0;
                            do {
                                donation = new File(cfg, "donation_" + (i++) + ".json");
                            } while (donation.exists());
                            Transaction transaction = new Transaction();
                            transaction.setAmount(amountValue);
                            transaction.setCategories(list);
                            transaction.setCurrency(currencyCode);
                            transaction.setNote(noteText);
                            transaction.setTid(transactionID);
                            transaction.setTime(System.currentTimeMillis());
                            IO.writeStringToFile(donation, JSonStorage.serializeToJson(transaction));
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

}
