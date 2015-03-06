package org.jdownloader.gui.donate;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.border.Border;
import javax.swing.text.MaskFormatter;

import org.appwork.swing.MigPanel;
import org.appwork.swing.action.BasicAction;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.components.ExtTextArea;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.ExtRealCheckBoxMenuItem;
import org.jdownloader.gui.views.components.PseudoMultiCombo;
import org.jdownloader.images.AbstractIcon;

public class ProviderPanel extends MigPanel {

    private DonationDetails                    details;
    private PaymentProvider                    provider;
    private JTextField                         input;
    private Border                             defaultBorder;
    private JLabel                             recurringLabel;
    private JCheckBox                          recurring;
    private PseudoMultiCombo<CategoryPriority> catSel;
    private long                               prioritySum;
    private ExtTextArea                        note;
    private ArrayList<PaymentProvider>         providerList;
    private PaymentProvider                    defaultProvider;
    private JComboBox<PaymentProvider>         curCombo;

    public ProviderPanel(DonationDetails details, ArrayList<PaymentProvider> list) {
        super("ins 5", "[grow,fill]15[]0", "[]");
        defaultProvider = details.getPaymentProvider()[details.getDefaultProvider()];
        ;
        this.details = details;
        this.providerList = list;

        provider = providerList.contains(defaultProvider) ? defaultProvider : providerList.get(0);
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
            Dialog.getInstance().showErrorDialog(_GUI._.DonationDialog_setReturnmask_range2(provider.getAmtMin(), provider.getAmtMax()));

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

        curCombo = new JComboBox<PaymentProvider>(providerList.toArray(new PaymentProvider[] {}));
        final ListCellRenderer org = curCombo.getRenderer();
        curCombo.setRenderer(new ListCellRenderer<PaymentProvider>() {

            @Override
            public Component getListCellRendererComponent(JList<? extends PaymentProvider> list, PaymentProvider value, int index, boolean isSelected, boolean cellHasFocus) {
                try {
                    if (StringUtils.equals(value.getcSymbol(), "BTC")) {
                        return org.getListCellRendererComponent(list, "Bitcoin" + " (" + value.getcSymbol() + ")", index, isSelected, cellHasFocus);
                    }
                    return org.getListCellRendererComponent(list, Currency.getInstance(value.getcCode()).getDisplayName() + " (" + value.getcSymbol() + ")", index, isSelected, cellHasFocus);
                } catch (Throwable e) {
                    return org.getListCellRendererComponent(list, value.getcSymbol(), index, isSelected, cellHasFocus);
                }
            }
        });
        curCombo.setSelectedItem(provider);
        curCombo.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                setProvider((PaymentProvider) curCombo.getSelectedItem());
            }
        });
        left.add(curCombo, "growx");
        if (providerList.size() <= 1) {
            curCombo.setEditable(false);
            curCombo.setEnabled(false);
        }

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

    protected void setProvider(PaymentProvider selectedItem) {
        String[] cats = getSelectedCategories();
        String amt = getAmount() == provider.getAmt() ? null : input.getText();
        String noteText = note.getText();
        this.removeAll();
        this.provider = selectedItem;
        init();
        setSelectedCategories(cats);
        if (amt != null) {
            input.setText(amt);
        }
        note.setText(noteText);
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