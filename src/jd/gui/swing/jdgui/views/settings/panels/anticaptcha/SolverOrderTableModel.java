package jd.gui.swing.jdgui.views.settings.panels.anticaptcha;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.List;

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
import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.renderer.RenderLabel;
import org.appwork.utils.swing.renderer.RendererMigPanel;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.captcha.v2.SolverService;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;

public class SolverOrderTableModel extends ExtTableModel<SolverService> {

    public SolverOrderTableModel() {
        super("SolverOrderTableModel");

        update();
    }

    private void update() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                // make sure that this class is loaded. it contains the logic to restore old settings.

                List<SolverService> lst = ChallengeResponseController.getInstance().listServices();

                _fireTableStructureChanged(lst, true);
            }
        };

    }

    @Override
    protected void autoColumnWidth() {
        super.autoColumnWidth();
    }

    @Override
    protected void initColumns() {

        this.addColumn(new ExtCheckColumn<SolverService>(_GUI.T.premiumaccounttablemodel_column_enabled()) {

            private final JComponent empty = new RendererMigPanel("ins 0", "[]", "[]");

            public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {
                    private final Icon ok = NewTheme.I().getIcon(IconKey.ICON_OK, 14);

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        setIcon(ok);
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
            public JComponent getRendererComponent(SolverService value, boolean isSelected, boolean hasFocus, int row, int column) {

                JComponent ret = super.getRendererComponent(value, isSelected, hasFocus, row, column);

                return ret;
            }

            @Override
            public boolean isSortable(final SolverService obj) {
                return false;
            }

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            protected boolean getBooleanValue(SolverService value) {
                return value.isEnabled();
            }

            @Override
            public boolean isEditable(SolverService obj) {
                return true;
            }

            @Override
            protected void setBooleanValue(boolean value, final SolverService object) {
                object.setEnabled(!object.isEnabled());

            }
        });
        addColumn(new ExtTextColumn<SolverService>(_GUI.T.SolverOrderTableModel_initColumns_service()) {

            @Override
            public boolean isSortable(final SolverService obj) {
                return false;
            }

            @Override
            protected Icon getIcon(SolverService value) {
                return value.getIcon(18);
            }

            @Override
            public int getDefaultWidth() {
                return 100;
            }

            @Override
            public boolean isEnabled(SolverService obj) {
                return obj.isEnabled();
            }

            @Override
            public String getStringValue(SolverService value) {
                return value.getName();
            }
        });

        addColumn(new ExtTextColumn<SolverService>(_GUI.T.SolverOrderTableModel_initColumns_type_()) {

            @Override
            public boolean isSortable(final SolverService obj) {
                return false;
            }

            @Override
            public boolean isEnabled(SolverService obj) {
                return obj.isEnabled();
            }

            @Override
            public int getDefaultWidth() {
                return 300;
            }

            @Override
            public String getStringValue(SolverService value) {
                return value.getType();
            }
        });

        // addColumn(new ExtSpinnerColumn<SolverService>(_GUI.T.SolverOrderTableModel_initColumns_startafter()) {
        // @Override
        // public boolean isHidable() {
        // return false;
        // }
        //
        // @Override
        // public boolean isSortable(final SolverService obj) {
        // return false;
        // }
        //
        // @Override
        // public boolean isEditable(SolverService obj) {
        // return true;
        // }
        //
        // @Override
        // protected String getTooltipText(SolverService obj) {
        // return _GUI.T.SolverOrderTableModel_getTooltipText_object_(obj.getServiceName(),
        // TimeFormatter.formatMilliSeconds(obj.getWaittime(), 0));
        // }
        //
        // @Override
        // public boolean isEnabled(SolverService obj) {
        // return true;
        // }
        //
        // @Override
        // public int getDefaultWidth() {
        // return 120;
        // }
        //
        // @Override
        // protected Number getNumber(SolverService value) {
        // return (int) (value.getWaittime() / 1000);
        // }
        //
        // @Override
        // protected void setNumberValue(Number value, SolverService object) {
        // object.setWaittime(value.intValue() * 1000);
        // ChallengeResponseController.getInstance().setSolverOrder(getTableData());
        // update();
        // }
        //
        // @Override
        // public String getStringValue(SolverService value) {
        // return (value.getWaittime() / 1000) + " seconds";
        // }
        // });
        this.addColumn(new ExtComponentColumn<SolverService>(_GUI.T.SolverOrderTableModel_initColumns_timeout()) {
            private JButton            editorBtn;
            private JButton            rendererBtn;
            private SolverService      editing;
            protected MigPanel         editor;
            protected RendererMigPanel renderer;
            private RenderLabel        label;

            {
                editorBtn = new JButton("");

                editorBtn.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (editing != null) {
                            SolverTimingDialog d = new SolverTimingDialog(editing);
                            UIOManager.I().show(null, d);

                        }
                    }
                });
                label = new RenderLabel();
                rendererBtn = new JButton("");

                this.editor = new MigPanel("ins 1", "[grow,fill]", "[grow]") {

                    @Override
                    public void requestFocus() {
                    }

                };
                editor.add(editorBtn);
                this.renderer = new RendererMigPanel("ins 1", "[grow,fill]", "[grow]");
                renderer.add(rendererBtn);
                setClickcount(1);
            }

            public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        setIcon(new AbstractIcon(IconKey.ICON_WAIT, 14));
                        setHorizontalAlignment(CENTER);
                        setText(_GUI.T.SolverOrderTableModel_initColumns_timeout());
                        return this;
                    }

                };

                return ret;
            }

            @Override
            public boolean isAutoWidthEnabled() {
                return true;
            }

            @Override
            protected boolean isDefaultResizable() {
                return true;
            }

            protected String generateID() {
                return "CaptchaOrderTable.timeoutbutton3";
            }

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            public int getDefaultWidth() {
                return 3;
            }

            public Dimension getCellSizeEstimation(SolverService element, int row) {

                Component c = getTableCellRendererComponent(getModel().getTable(), element, false, false, row, 1);
                rendererBtn.setMaximumSize(null);
                return c.getPreferredSize();

            }

            @Override
            public boolean isSortable(final SolverService obj) {
                return false;
            }

            @Override
            protected JComponent getInternalEditorComponent(SolverService value, boolean isSelected, int row, int column) {
                return editor;
            }

            @Override
            public boolean onSingleClick(MouseEvent e, SolverService obj) {
                return super.onSingleClick(e, obj);
            }

            @Override
            protected JComponent getInternalRendererComponent(SolverService value, boolean isSelected, boolean hasFocus, int row, int column) {
                return renderer;
            }

            @Override
            public boolean isEnabled(SolverService obj) {
                return true;
            }

            @Override
            public void configureRendererComponent(SolverService value, boolean isSelected, boolean hasFocus, int row, int column) {

                // rendererBtn.setIcon(new AbstractIcon(IconKey.ICON_WAIT, 16));
                rendererBtn.setText(_GUI.T.lit_edit());

                rendererBtn.setMaximumSize(new Dimension(getWidth(), getTable().getRowHeight(row) - 2));
            }

            @Override
            public void configureEditorComponent(SolverService value, boolean isSelected, int row, int column) {
                editing = value;
                editorBtn.setText(_GUI.T.lit_edit());
                editorBtn.setMaximumSize(new Dimension(getWidth(), getTable().getRowHeight(row) - 2));

            }

            @Override
            public void resetEditor() {
            }

            @Override
            public void resetRenderer() {
            }

        });
        this.addColumn(new ExtComponentColumn<SolverService>(_GUI.T.lit_settings()) {
            private JButton            editorBtn;
            private JButton            rendererBtn;
            private SolverService      editing;
            protected MigPanel         editor;
            protected RendererMigPanel renderer;
            private RenderLabel        label;

            {
                editorBtn = new JButton("");

                editorBtn.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (editing != null) {
                            SolverPropertiesDialog d = new SolverPropertiesDialog(editing, editing.getConfigPanel());
                            UIOManager.I().show(null, d);

                        }
                    }
                });
                label = new RenderLabel();
                rendererBtn = new JButton("");
                this.editor = new MigPanel("ins 0", "[grow,fill]", "[grow]") {

                    @Override
                    public void requestFocus() {
                    }

                };
                editor.add(editorBtn);
                this.renderer = new RendererMigPanel("ins 0", "[grow,fill]", "[grow]");
                renderer.add(rendererBtn);
                setClickcount(1);
            }

            @Override
            protected boolean isDefaultResizable() {
                return true;
            }

            @Override
            public boolean isAutoWidthEnabled() {
                return true;
            }

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            public int getDefaultWidth() {
                return 100;
            }

            @Override
            public boolean isSortable(final SolverService obj) {
                return false;
            }

            @Override
            protected JComponent getInternalEditorComponent(SolverService value, boolean isSelected, int row, int column) {
                return editor;
            }

            @Override
            public boolean onSingleClick(MouseEvent e, SolverService obj) {
                return super.onSingleClick(e, obj);
            }

            @Override
            protected JComponent getInternalRendererComponent(SolverService value, boolean isSelected, boolean hasFocus, int row, int column) {
                return renderer;
            }

            @Override
            public boolean isEnabled(SolverService obj) {
                return obj.hasConfigPanel();
            }

            @Override
            public void configureRendererComponent(SolverService value, boolean isSelected, boolean hasFocus, int row, int column) {

                // rendererBtn.setIcon(new AbstractIcon(IconKey.ICON_THUMBS_DOWN, 16));
                rendererBtn.setText(_GUI.T.lit_edit());
                rendererBtn.setMaximumSize(new Dimension(getWidth(), getTable().getRowHeight(row) - 2));

            }

            protected String generateID() {
                return "CaptchaOrderTable.editButton2";
            }

            public Dimension getCellSizeEstimation(SolverService element, int row) {

                Component c = getTableCellRendererComponent(getModel().getTable(), element, false, false, row, 1);
                rendererBtn.setMaximumSize(null);
                return c.getPreferredSize();

            }

            @Override
            public void configureEditorComponent(SolverService value, boolean isSelected, int row, int column) {
                editing = value;
                editorBtn.setText(_GUI.T.lit_edit());
                editorBtn.setMaximumSize(new Dimension(getWidth(), getTable().getRowHeight(row) - 2));
            }

            public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        setIcon(new AbstractIcon(IconKey.ICON_SETTINGS, 14));
                        setHorizontalAlignment(CENTER);
                        setText(_GUI.T.lit_settings());
                        return this;
                    }

                };

                return ret;
            }

            @Override
            public void resetEditor() {
            }

            @Override
            public void resetRenderer() {
            }

        });

    }

}
