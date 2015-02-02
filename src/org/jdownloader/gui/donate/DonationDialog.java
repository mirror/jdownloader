package org.jdownloader.gui.donate;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
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

    protected String toQuery(Object... objects) {
        StringBuilder sb = new StringBuilder();
        for (Object o : objects) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(JSonStorage.serializeToJson(o));

        }
        return sb.toString();
    }

    public static class ProviderPanel extends MigPanel {

        private DonationDetails                    details;
        private PaymentProvider                    provider;
        private JTextField                         input;
        private Border                             defaultBorder;
        private JLabel                             recurringLabel;
        private JCheckBox                          recurring;
        private PseudoMultiCombo<CategoryPriority> catSel;
        private long                               prioritySum;
        private ExtTextArea                        note;

        public ProviderPanel(DonationDetails details, PaymentProvider provider) {
            super("ins 5", "[grow,fill]15[]0", "[]");
            this.details = details;
            this.provider = provider;
            init();
        }

        public String getCurrencyCode() {
            return provider.getcCode();
        }

        public String getProvider() {
            return provider.getId();
        }

        public String getNote() {
            return note.getText();
        }

        public boolean isRecurring() {
            return provider.isRecurring() && recurring.isVisible() && recurring.isSelected();
        }

        public String[] getSelectedCategories() {
            final List<CategoryPriority> sel = catSel.getSelectedItems();
            final String[] list = new String[sel.size()];
            for (int i = 0; i < sel.size(); i++) {
                list[i] = sel.get(i).getCategory();
            }
            return list;
        }

        public double getAmount() {
            String amountString = input.getText().replaceAll("[^0-9\\.\\,\\-]+", "").replace(",", ".");
            double amount = -1;
            try {
                amount = Double.parseDouble(amountString);
            } catch (Throwable e1) {
            }
            ;
            if (amount < provider.getAmtMin() || amount > provider.getAmtMax()) {
                input.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.RED));
                Dialog.getInstance().showErrorDialog(_GUI._.DonationDialog_setReturnmask_range());

                return -1;
            }
            return amount;
        }

        private Component createPreButton(final double d) {
            ExtButton ret = new ExtButton(new AppAction() {
                {
                    setName(d + "" + provider.getcSymbol());
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    StatsManager.I().track("/donation/button/preselect/" + d);
                    input.setText(d + "");
                }
            });
            // ret.setRolloverEffectEnabled(true);
            return ret;
        }

        private void init() {
            MigPanel left = new MigPanel("ins 0,wrap 2", "[][grow,fill]", "[]");
            add(left, "pushy,growy");

            JLabel icon = new JLabel(new AbstractIcon("botty_heart", -1));
            icon.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Color.gray));
            add(icon, "aligny bottom");

            left.add(new JLabel(_GUI._.DonationDialog_layoutDialogContent_donate_amount()), "alignx right");

            if (provider.getAmtSuggest() != null && provider.getAmtSuggest().length > 0) {
                MigPanel pre = new MigPanel("ins 0", "[]", "[]");
                for (double d : provider.getAmtSuggest()) {
                    pre.add(createPreButton(d), "pushx,growx");
                }

                left.add(pre, "spanx");
            }

            MaskFormatter formatter;

            DecimalFormat df = new DecimalFormat("#.", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
            df.setMaximumFractionDigits(340); // 340 = DecimalFormat.DOUBLE_FRACTION_DIGITS
            // f.setMaximumFractionDigits(0);
            // f.setMinimumFractionDigits(0);
            // f.setMaximumIntegerDigits(4);
            // f.setMinimumIntegerDigits(1);
            // f.setGroupingUsed(false);
            // if (f instanceof DecimalFormat) {
            // try {
            // f.setCurrency(Currency.getInstance(provider.getcCode()));
            // } catch (IllegalArgumentException e) {
            // f.setCurrency(Currency.getInstance("USD"));
            // }
            //
            // }
            input = new JFormattedTextField(df);
            input.setText("5.00");
            defaultBorder = input.getBorder();
            // input.setColumns(20);

            left.add(new JLabel(provider.getcSymbol()), "alignx right");
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
            recurring.setVisible(provider.isRecurring());
            recurringLabel.setVisible(provider.isRecurring());
            left.add(new JLabel(_GUI._.DonationDialog_layoutDialogContent_donate_category()), "alignx right");
            long sum = 0;
            for (CategoryPriority c : details.getCategories()) {
                sum += c.getPriority();
            }
            prioritySum = sum;
            catSel = new PseudoMultiCombo<CategoryPriority>(new CategoryPriority[] {}) {
                protected String getNothingText() {
                    return _GUI._.DonationDialog_layoutDialogContent_donate_for_generel();

                };

                @Override
                public void onChanged() {
                    super.onChanged();

                }

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
            left.add(catSel, "");

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
            input.setText(provider.getAmt() + "");
            recurringLabel.setVisible(provider.isRecurring());
            recurring.setVisible(provider.isRecurring());
            recurring.setSelected(false);

        }

        public void setSelectedCategories(String[] selectedCategories) {

            ArrayList<CategoryPriority> selected = new ArrayList<CategoryPriority>();
            HashSet<String> selectedIds = new HashSet<String>();
            for (String s : selectedCategories) {
                selectedIds.add(s);
            }

            for (CategoryPriority c : details.getCategories()) {
                if (selectedIds.contains(c.getCategory())) {
                    selected.add(c);
                }
            }
            catSel.setSelectedItems(selected);
        }

        public void setNote(String note2) {
            note.setText(note2);
        }

        public void setRecurring(boolean recurring2) {
            recurring.setSelected(recurring2);
        }
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
