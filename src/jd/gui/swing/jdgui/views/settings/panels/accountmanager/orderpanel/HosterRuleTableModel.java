package jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import org.appwork.swing.MigPanel;
import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtCheckColumn;
import org.appwork.swing.exttable.columns.ExtComponentColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.swing.renderer.RendererMigPanel;
import org.jdownloader.DomainInfo;
import org.jdownloader.controlling.hosterrule.AccountUsageRule;
import org.jdownloader.controlling.hosterrule.HosterRuleController;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class HosterRuleTableModel extends ExtTableModel<AccountUsageRule> {

    public HosterRuleTableModel() {
        super("HosterRuleTableModel");
    }

    @Override
    protected void initColumns() {

        this.addColumn(new ExtCheckColumn<AccountUsageRule>(_GUI._.premiumaccounttablemodel_column_enabled()) {

            private final JComponent empty = new RendererMigPanel("ins 0", "[]", "[]");

            public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        setIcon(NewTheme.I().getIcon("ok", 14));
                        setHorizontalAlignment(CENTER);
                        setText(null);
                        return this;
                    }

                };

                return ret;
            }

            @Override
            public int getMaxWidth() {

                return 30;
            }

            @Override
            public JComponent getRendererComponent(AccountUsageRule value, boolean isSelected, boolean hasFocus, int row, int column) {

                JComponent ret = super.getRendererComponent(value, isSelected, hasFocus, row, column);

                return ret;
            }

            @Override
            public boolean isSortable(final AccountUsageRule obj) {
                return true;
            }

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            protected boolean getBooleanValue(AccountUsageRule value) {
                return value.isEnabled();
            }

            @Override
            public boolean isEditable(AccountUsageRule obj) {
                return true;
            }

            @Override
            protected void setBooleanValue(boolean value, final AccountUsageRule object) {
                object.setEnabled(value);

            }
        });

        this.addColumn(new ExtTextColumn<AccountUsageRule>(_GUI._.HosterRuleTableModel_initColumns_hoster_()) {

            private static final long serialVersionUID = -8070328156326837828L;

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            public int getDefaultWidth() {
                return getMinWidth();
            }

            @Override
            public boolean isSortable(final AccountUsageRule obj) {

                return true;
            }

            @Override
            public boolean isEnabled(final AccountUsageRule obj) {
                return obj.isEnabled();
            }

            @Override
            public int getMinWidth() {
                return 100;
            }

            @Override
            public boolean isEditable(AccountUsageRule obj) {
                return false;
            }

            @Override
            protected void setStringValue(String value, AccountUsageRule object) {

            }

            @Override
            protected Icon getIcon(AccountUsageRule value) {
                return DomainInfo.getInstance(value.getHoster()).getFavIcon();
            }

            @Override
            public String getStringValue(AccountUsageRule value) {

                return value.getHoster();

            }
        });

        this.addColumn(new ExtComponentColumn<AccountUsageRule>(_GUI._.HosterRuleTableModel_initColumns_edit_()) {
            private JButton          button;
            private MigPanel         panel;
            private JButton          rbutton;
            private MigPanel         rpanel;
            private AccountUsageRule editing;

            {
                button = new JButton(_GUI._.HosterRuleTableModel_initColumns_edit_());

                panel = new RendererMigPanel("ins 2", "[]", "[16!]");
                panel.add(button);
                button.setOpaque(false);

                rbutton = new JButton(_GUI._.HosterRuleTableModel_initColumns_edit_());

                rbutton.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (editing != null) {
                            HosterRuleController.getInstance().showEditPanel(editing);

                        }
                    }
                });

                rpanel = new MigPanel("ins 2", "[]", "[16!]");
                rpanel.add(rbutton);
                rbutton.setOpaque(false);
            }

            public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        setIcon(NewTheme.I().getIcon("edit", 14));
                        setHorizontalAlignment(CENTER);
                        setText(null);
                        return this;
                    }

                };

                return ret;
            }

            @Override
            public int getMaxWidth() {
                return panel.getPreferredSize().width;
            }

            @Override
            public boolean isEnabled(AccountUsageRule obj) {
                return true;
            }

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            public int getDefaultWidth() {
                return panel.getPreferredSize().width;
            }

            @Override
            public int getMinWidth() {
                return panel.getPreferredSize().width;
            }

            @Override
            public boolean isEditable(AccountUsageRule obj) {
                return super.isEditable(obj);
            }

            @Override
            public boolean onSingleClick(MouseEvent e, AccountUsageRule obj) {
                return super.onSingleClick(e, obj);
            }

            @Override
            protected JComponent getInternalEditorComponent(AccountUsageRule value, boolean isSelected, int row, int column) {
                return rpanel;
            }

            @Override
            protected JComponent getInternalRendererComponent(AccountUsageRule value, boolean isSelected, boolean hasFocus, int row, int column) {

                return panel;
            }

            @Override
            public void configureEditorComponent(AccountUsageRule value, boolean isSelected, int row, int column) {
                editing = value;
                // rbutton.setEnabled(isEnabled(value));
            }

            @Override
            public void configureRendererComponent(AccountUsageRule value, boolean isSelected, boolean hasFocus, int row, int column) {
                // button.setEnabled(isEnabled(value));
                ;
            }

            @Override
            public void resetEditor() {
                rpanel.setBackground(null);
                rpanel.setOpaque(false);
            }

            @Override
            public void resetRenderer() {
                panel.setBackground(null);
                panel.setOpaque(false);
            }

        });

    }

}
