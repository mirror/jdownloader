package org.jdownloader.extensions.translator.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jd.gui.swing.jdgui.JDGui;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.components.ExtTextArea;
import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.translator.TranslateEntry;

public class QuickEdit extends MigPanel implements ListSelectionListener {

    private TranslateTable table;
    private ExtTextArea    org;
    private ExtTextArea    translation;
    private ExtButton      next;
    private int            index;
    private ExtTextArea    desc;
    private TranslateEntry entry;
    private JLabel         problems;

    public QuickEdit(TranslateTable table) {
        super("ins 0,wrap 2", "[grow,fill,50%][grow,fill,50%]", "[][][grow,fill, 24:60:n][]");
        this.table = table;
        table.getSelectionModel().addListSelectionListener(this);

        org = new ExtTextArea();
        org.setLabelMode(true);
        add(desc = new ExtTextArea(), "spanx,pushx,growx");
        desc.setLabelMode(true);
        // desc.setPreferredSize(new Dimension(20, 20));
        add(new JLabel("Default"));
        add(new JLabel("Translation"));
        add(new JScrollPane(org));
        translation = new ExtTextArea() {

            @Override
            public void onChanged() {
                super.onChanged();
                updateProblems();
                if (!this.isHelpTextVisible()) {
                    setForeground(defaultColor);
                }
            }

        };
        translation.addFocusListener(new FocusListener() {

            @Override
            public void focusLost(FocusEvent e) {
            }

            @Override
            public void focusGained(FocusEvent e) {

                JDGui.help("Confirm", "\r\nCTRL + ENTER = Save & move to next untranslated Entry\r\nCTRL + SHIFT + ENTER = Save & move to next Entry", null);
            }
        });
        InputMap input = translation.getInputMap();

        input.put(KeyStroke.getKeyStroke("ctrl ENTER"), "TEXT_SUBMIT");
        input.put(KeyStroke.getKeyStroke("ctrl shift ENTER"), "TEXT_SUBMIT");
        ActionMap actions = translation.getActionMap();
        actions.put("TEXT_SUBMIT", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                next.doClick();
            }
        });

        // translation.setClearHelpTextOnFocus(false);
        // ttx.addKeyListener(this);
        // ttx.addMouseListener(this);

        add(new JScrollPane(translation));

        // problems.setForeground(Color.ORANGE);
        add(createButtonPanel(), "spanx,pushx,growx");
    }

    private Component createButtonPanel() {
        MigPanel p = new MigPanel("ins 0", "[grow,fill][]", "[]");

        p.add(problems = new JLabel());
        problems.setHorizontalAlignment(SwingConstants.RIGHT);
        p.add(next = new ExtButton(new AppAction() {
            {
                setName("Save & Next");
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (entry != null) {

                    entry.setTranslation(translation.getText());

                }
                JDGui.help("Save & Next", "\r\nClick = Save & Move to next untranslated Entry\r\nSHIFT+Click = Save & Move to next Entry", null);
                if (((e.getModifiers() & ActionEvent.SHIFT_MASK) != 0)) {

                    // next value
                    int nextIndex = index == table.getExtTableModel().getTableData().size() - 1 ? 0 : index + 1;
                    table.getSelectionModel().setSelectionInterval(nextIndex, nextIndex);
                    table.scrollToRow(nextIndex);
                } else {
                    // next untranslated

                    int nextIndex = index == table.getExtTableModel().getTableData().size() - 1 ? 0 : index + 1;
                    while (!table.getExtTableModel().getObjectbyRow(nextIndex).isMissing() && !table.getExtTableModel().getObjectbyRow(nextIndex).isDefault() && !table.getExtTableModel().getObjectbyRow(nextIndex).isParameterInvalid()) {
                        nextIndex = nextIndex == table.getExtTableModel().getTableData().size() - 1 ? 0 : nextIndex + 1;
                        if (nextIndex == index) {
                            nextIndex = index == table.getExtTableModel().getTableData().size() - 1 ? 0 : index + 1;
                            break;
                        }
                    }
                    table.getSelectionModel().setSelectionInterval(nextIndex, nextIndex);
                    table.scrollToRow(nextIndex);
                }
            }
        }));
        return p;
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        index = table.getSelectionModel().getLeadSelectionIndex();
        // System.out.println(index);
        TranslateEntry obj = table.getExtTableModel().getObjectbyRow(index);
        if (table.getSelectionModel().isSelectedIndex(index)) {
            set(table.getExtTableModel().getObjectbyRow(index));
        } else {
            set(null);
        }

    }

    private void set(TranslateEntry e) {
        if (e == null) {

            setVisible(false);
            return;
        }
        if (e.equals(entry)) {
            setVisible(true);
            entry = e;
            return;
        }
        this.entry = e;
        System.out.println(index);
        desc.setText(e.getDescription());
        org.setText(e.getDefault());
        System.out.println("Please translate: \r\n" + e.getDirect());
        translation.setHelpText("Please translate: \r\n" + e.getDirect());
        translation.setText(e.isMissing() ? "" : e.getTranslation().replace("\\r", "\r").replace("\\n", "\n"));

        if (translation.getText().length() == 0) {
            translation.selectAll();
        }
        updateProblems();
    }

    private void updateProblems() {
        if (entry == null) {
            problems.setText("");
            return;
        }
        if (entry.isMissing() && translation.getText().length() == 0) {
            problems.setText("<html><font color='#ff0000' >Error:</font><font color='#ff0000' >Not translated yet</font></html>");
        } else if (!entry.validateParameterCount(translation.getText())) {
            problems.setText("<html><font color='#ff0000' >Error:</font><font color='#ff0000' >Parameter Wildcards (%s*) do not match.</font></html>");

        } else if (entry.getDefault() != null && entry.getDefault().equals(translation.getText())) {
            problems.setText("<html><font color='#339900' >Warning:</font><font color='#339900' >The translation equals the english default language.</font></html>");
        } else {
            problems.setText("");
        }
    }

    public void setTable(TranslateTable table2) {
        this.table = table2;
        table.getSelectionModel().addListSelectionListener(this);
    }

}
