package jd.gui.swing.jdgui.views.myjd.panels;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import org.appwork.swing.MigPanel;
import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtComponentColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.renderer.RenderLabel;
import org.appwork.utils.swing.renderer.RendererMigPanel;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.api.useragent.ConnectedDevice;
import org.jdownloader.api.useragent.UserAgentListener;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public class ConnectedDevicesTableModel extends ExtTableModel<ConnectedDevice> {

    public ConnectedDevicesTableModel() {
        super("SolverOrderTableModel");

        update();

        RemoteAPIController.getInstance().getUaController().getEventSender().addListener(new UserAgentListener() {

            @Override
            public void onRemovedAPIUserAgent(ConnectedDevice ua) {
                update();

            }

            @Override
            public void onNewAPIUserAgent(ConnectedDevice ua) {
                update();

            }

            @Override
            public void onAPIUserAgentUpdate(ConnectedDevice fua) {
                update();
            }
        });
    }

    private void update() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                // make sure that this class is loaded. it contains the logic to restore old settings.

                List<ConnectedDevice> lst = RemoteAPIController.getInstance().getUaController().list();

                _fireTableStructureChanged(lst, true);
            }
        };

    }

    @Override
    protected void initColumns() {

        addColumn(new ExtTextColumn<ConnectedDevice>(_GUI._.ConnectedDevicesTableModel_frontend()) {

            @Override
            public String getStringValue(ConnectedDevice value) {
                return value.getFrontendName();
            }
        });
        addColumn(new ExtTextColumn<ConnectedDevice>(_GUI._.ConnectedDevicesTableModel_device()) {

            @Override
            public String getStringValue(ConnectedDevice value) {
                return value.getDeviceName();
            }
        });
        addColumn(new ExtTextColumn<ConnectedDevice>(_GUI._.ConnectedDevicesTableModel_connection()) {

            @Override
            public String getStringValue(ConnectedDevice value) {
                return value.getConnectionString();
            }
        });
        this.addColumn(new ExtComponentColumn<ConnectedDevice>(_GUI._.ConnectedDevicesTableModel_kill()) {
            private JButton            editorBtn;
            private JButton            rendererBtn;
            private ConnectedDevice    editing;
            protected MigPanel         editor;
            protected RendererMigPanel renderer;
            private RenderLabel        label;

            {
                editorBtn = new JButton("");

                editorBtn.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (editing != null) {
                            // SolverPropertiesDialog d = new SolverPropertiesDialog(editing, editing.getConfigPanel());
                            // UIOManager.I().show(null, d);

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
                return 100;
            }

            @Override
            public boolean isSortable(final ConnectedDevice obj) {
                return false;
            }

            @Override
            protected JComponent getInternalEditorComponent(ConnectedDevice value, boolean isSelected, int row, int column) {
                return editor;
            }

            @Override
            public boolean onSingleClick(MouseEvent e, ConnectedDevice obj) {
                return super.onSingleClick(e, obj);
            }

            @Override
            protected JComponent getInternalRendererComponent(ConnectedDevice value, boolean isSelected, boolean hasFocus, int row, int column) {
                return renderer;
            }

            @Override
            public boolean isEnabled(ConnectedDevice obj) {
                return true;
            }

            @Override
            public void configureRendererComponent(ConnectedDevice value, boolean isSelected, boolean hasFocus, int row, int column) {

                // rendererBtn.setIcon(new AbstractIcon(IconKey.ICON_THUMBS_DOWN, 16));
                rendererBtn.setText(_GUI._.lit_disconnect());
                // rendererBtn.setIcon(new AbstractIcon(IconKey.ICON_SETTINGS, 16));
            }

            @Override
            public void configureEditorComponent(ConnectedDevice value, boolean isSelected, int row, int column) {
                editing = value;
                editorBtn.setText(_GUI._.lit_disconnect());
                // editorBtn.setIcon(new AbstractIcon(IconKey.ICON_SETTINGS, 16));
            }

            public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        setIcon(new AbstractIcon(IconKey.ICON_CANCEL, 14));
                        setHorizontalAlignment(CENTER);
                        setText(_GUI._.ConnectedDevicesTableModel_kill());
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
