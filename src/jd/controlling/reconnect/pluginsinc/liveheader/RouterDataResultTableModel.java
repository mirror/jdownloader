package jd.controlling.reconnect.pluginsinc.liveheader;

import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;

import jd.controlling.reconnect.pluginsinc.liveheader.remotecall.RouterData;
import jd.controlling.reconnect.pluginsinc.liveheader.translate.T;

import org.appwork.swing.MigPanel;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtComponentColumn;
import org.appwork.swing.exttable.columns.ExtSpinnerColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.uio.UIOManager;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.renderer.RenderLabel;
import org.appwork.utils.swing.renderer.RendererMigPanel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public class RouterDataResultTableModel extends ExtTableModel<RouterData> {

    private ExtSpinnerColumn<RouterData> sorton;

    public RouterDataResultTableModel() {
        super("RouterDataResultTableModel");
    }

    @Override
    protected void initColumns() {
        addColumn(new ExtTextColumn<RouterData>(T._.routername()) {

            @Override
            public String getStringValue(RouterData value) {
                return value.getRouterName();
            }
        });

        addColumn(new ExtTextColumn<RouterData>(T._.isp()) {

            @Override
            public String getStringValue(RouterData value) {
                return value.getIsp().replaceAll("^\\w+\\d+ ", "");
            }
        });

        addColumn(new ExtTextColumn<RouterData>(T._.manufactor()) {
            @Override
            public int getDefaultWidth() {
                return 80;
            }

            @Override
            public String getStringValue(RouterData value) {
                return value.getManufactor();
            }
        });

        addColumn(sorton = new ExtSpinnerColumn<RouterData>(T._.success_rate()) {
            {

            }

            @Override
            public boolean isEditable(RouterData obj) {
                return false;
            }

            @Override
            public int getDefaultWidth() {
                return 60;
            }

            @Override
            public int getMaxWidth() {
                return super.getDefaultWidth();
            }

            @Override
            public int getMinWidth() {
                return super.getDefaultWidth();
            }

            @Override
            protected Number getNumber(RouterData value) {
                return value.getPriorityIndicator();
            }

            @Override
            protected void setNumberValue(Number value, RouterData object) {
            }

            @Override
            public String getStringValue(RouterData value) {
                return value.getPriorityIndicator() + "";
            }

        });
        addColumn(new ExtTextColumn<RouterData>(T._.avg_time()) {
            {

            }

            @Override
            public int getDefaultWidth() {
                return 80;
            }

            @Override
            public int getMaxWidth() {
                return super.getDefaultWidth();
            }

            @Override
            public int getMinWidth() {
                return super.getDefaultWidth();
            }

            @Override
            public String getStringValue(RouterData value) {
                return TimeFormatter.formatMilliSeconds(value.getAvgScD(), 0);
            }
        });

        this.addColumn(new ExtComponentColumn<RouterData>(T._.details()) {
            private JButton            editorBtn;
            private JButton            rendererBtn;
            private RouterData         editing;
            protected MigPanel         editor;
            protected RendererMigPanel renderer;
            private RenderLabel        label;

            {
                editorBtn = new JButton("");

                editorBtn.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (editing != null) {
                            setSelectedObject(editing);
                            final LiveHeaderScriptConfirmDialog d = new LiveHeaderScriptConfirmDialog(Dialog.STYLE_HIDE_ICON | UIOManager.BUTTONS_HIDE_CANCEL, T._.script(editing.getRouterName()), new AbstractIcon("reconnect", 32), _GUI._.lit_close(), null, editing, null, editing.getRouterName()) {
                                @Override
                                public String getMessage() {
                                    return T._.edit_script();
                                }

                                @Override
                                public ModalityType getModalityType() {
                                    return ModalityType.MODELESS;
                                }
                            };
                            new Thread() {
                                {
                                    setDaemon(true);
                                }

                                @Override
                                public void run() {
                                    UIOManager.I().show(null, d);
                                }
                            }.start();

                        }
                    }
                });
                label = new RenderLabel();
                rendererBtn = new JButton("");
                this.editor = new MigPanel("ins 1", "[grow,fill]", "[18!]") {

                    @Override
                    public void requestFocus() {
                    }

                };
                editor.add(editorBtn);
                this.renderer = new RendererMigPanel("ins 1", "[grow,fill]", "[18!]");
                renderer.add(rendererBtn);
                setClickcount(1);
            }

            @Override
            protected boolean isDefaultResizable() {
                return false;
            }

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            public int getDefaultWidth() {
                return 80;
            }

            @Override
            public boolean isSortable(final RouterData obj) {
                return false;
            }

            @Override
            protected JComponent getInternalEditorComponent(RouterData value, boolean isSelected, int row, int column) {
                return editor;
            }

            @Override
            public boolean onSingleClick(MouseEvent e, RouterData obj) {
                return super.onSingleClick(e, obj);
            }

            @Override
            protected JComponent getInternalRendererComponent(RouterData value, boolean isSelected, boolean hasFocus, int row, int column) {
                return renderer;
            }

            @Override
            public boolean isEnabled(RouterData obj) {
                return true;
            }

            @Override
            public void configureRendererComponent(RouterData value, boolean isSelected, boolean hasFocus, int row, int column) {

                // rendererBtn.setIcon(new AbstractIcon(IconKey.ICON_WAIT, 16));
                rendererBtn.setText(T._.details());
                // }
            }

            @Override
            public void configureEditorComponent(RouterData value, boolean isSelected, int row, int column) {
                editing = value;
                // editorBtn.setIcon(new AbstractIcon(IconKey.ICON_WAIT, 16));
                editorBtn.setText(T._.details());

            }

            @Override
            public void resetEditor() {
            }

            @Override
            public void resetRenderer() {
            }

        });

        this.sortColumn = sorton;
    }

    @Override
    public ExtColumn<RouterData> getSortColumn() {
        return super.getSortColumn();
    }

    public void update(List<RouterData> list) {
        this.sortColumn = sorton;
        _fireTableStructureChanged(list, true);

        getTable().getTableHeader().repaint();
        // refreshSort();
    }

}
