package jd.gui.swing.jdgui.views.settings.panels.proxy;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;

import jd.controlling.proxy.AbstractProxySelectorImpl;
import jd.controlling.proxy.ConnectionBan;
import jd.controlling.proxy.NoProxySelector;
import jd.controlling.proxy.PacProxySelectorImpl;
import jd.controlling.proxy.ProxyController;
import jd.controlling.proxy.SingleBasicProxySelectorImpl;
import jd.controlling.proxy.SingleDirectGatewaySelector;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.swing.components.tooltips.ToolTipController;
import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtCheckColumn;
import org.appwork.swing.exttable.columns.ExtComboColumn;
import org.appwork.swing.exttable.columns.ExtComponentColumn;
import org.appwork.swing.exttable.columns.ExtPasswordEditorColumn;
import org.appwork.swing.exttable.columns.ExtSpinnerColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.renderer.RenderLabel;
import org.appwork.utils.swing.renderer.RendererMigPanel;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.updatev2.FilterList;

public class ProxyTableModel extends ExtTableModel<AbstractProxySelectorImpl> {

    private static final long serialVersionUID = -5584463272737285033L;

    public ProxyTableModel() {
        super("proxyTable3");

    }

    @Override
    protected void initModel() {
        super.initModel();
    }

    protected boolean isSortStateSaverEnabled() {
        return false;
    }

    @Override
    public void _fireTableStructureChanged(List<jd.controlling.proxy.AbstractProxySelectorImpl> newtableData, boolean refreshSort) {
        super._fireTableStructureChanged(newtableData, false);
    }

    @Override
    public boolean move(java.util.List<AbstractProxySelectorImpl> transferData, int dropRow) {
        ProxyController.getInstance().move(transferData, dropRow);
        return true;
    }

    @Override
    protected void initColumns() {
        this.addColumn(new ExtCheckColumn<AbstractProxySelectorImpl>(_GUI._.gui_column_use(), this) {

            private static final long serialVersionUID = -4667150369226691276L;

            public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

                    private static final long serialVersionUID = 3938290423337000265L;

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        setIcon(NewTheme.I().getIcon(IconKey.ICON_OK, 14));
                        // defaultProxy
                        setHorizontalAlignment(CENTER);
                        setText(null);
                        setToolTipText(_GUI._.gui_column_use());
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
            public boolean isHidable() {
                return false;
            }

            @Override
            public boolean isSortable(final AbstractProxySelectorImpl obj) {
                return false;
            }

            @Override
            protected boolean getBooleanValue(AbstractProxySelectorImpl value) {
                return value.isEnabled();
            }

            @Override
            public boolean isEditable(AbstractProxySelectorImpl obj) {
                return true;
            }

            @Override
            protected void setBooleanValue(final boolean value, final AbstractProxySelectorImpl object) {
                ProxyController.getInstance().setEnabled(object, value);
            }
        });
        addColumn(new OrderColumn());
        DefaultComboBoxModel<AbstractProxySelectorImpl.Type> model = new DefaultComboBoxModel<AbstractProxySelectorImpl.Type>(new AbstractProxySelectorImpl.Type[] { AbstractProxySelectorImpl.Type.HTTP, AbstractProxySelectorImpl.Type.SOCKS5, AbstractProxySelectorImpl.Type.SOCKS4 });
        this.addColumn(new ExtComboColumn<AbstractProxySelectorImpl, AbstractProxySelectorImpl.Type>(_GUI._.gui_column_proxytype(), model) {

            @Override
            public boolean isEditable(AbstractProxySelectorImpl obj) {
                if (obj == null) {
                    return false;
                }
                switch (obj.getType()) {
                case NONE:
                case DIRECT:
                case PAC:
                    return false;
                default:
                    return true;
                }
            }

            protected boolean isDefaultResizable() {
                return false;
            }

            @Override
            public boolean isSortable(final AbstractProxySelectorImpl obj) {
                return false;
            }

            @Override
            public int getDefaultWidth() {
                return 130;
            }

            @Override
            public int getMinWidth() {
                return 100;
            }

            @Override
            protected String getTooltipText(AbstractProxySelectorImpl obj) {
                switch (obj.getType()) {
                case NONE:
                    return _GUI._.gui_column_proxytype_no_proxy_tt();
                case DIRECT:
                    return _GUI._.gui_column_proxytype_direct_tt();
                case HTTP:
                    return _GUI._.gui_column_proxytype_http_tt();
                case SOCKS5:
                    return _GUI._.gui_column_proxytype_socks5_tt();
                case SOCKS4:
                    return _GUI._.gui_column_proxytype_socks4_tt();
                case PAC:
                    return _GUI._.gui_column_proxytype_pac_tt();
                default:
                    throw new RuntimeException("Unknown Proxy Type");
                }
            }

            @Override
            protected String modelItemToString(AbstractProxySelectorImpl.Type selectedItem) {
                switch (selectedItem) {
                case DIRECT:
                    return _GUI._.gui_column_proxytype_direct();
                case HTTP:
                    return _GUI._.gui_column_proxytype_http();
                case NONE:
                    return _GUI._.gui_column_proxytype_no_proxy();
                case SOCKS4:
                    return _GUI._.gui_column_proxytype_socks4();
                case SOCKS5:
                    return _GUI._.gui_column_proxytype_socks5();
                case PAC:
                    return _GUI._.gui_column_proxytype_pac();
                }
                return null;

            }

            @Override
            public boolean isEnabled(AbstractProxySelectorImpl obj) {
                return true;
            }

            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            protected AbstractProxySelectorImpl.Type getSelectedItem(AbstractProxySelectorImpl object) {
                return object.getType();
            }

            @Override
            protected void setSelectedItem(AbstractProxySelectorImpl object, AbstractProxySelectorImpl.Type value) {
                object.setType(value);
            }

        });

        this.addColumn(new HostColumn());
        this.addColumn(new ExtTextColumn<AbstractProxySelectorImpl>(_GUI._.gui_column_user(), this) {

            private static final long serialVersionUID = -7209180150340921804L;

            @Override
            public boolean isEditable(final AbstractProxySelectorImpl value) {
                if (value instanceof NoProxySelector) {
                    return false;
                } else if (value instanceof SingleDirectGatewaySelector) {
                    return false;
                }
                return true;
            }

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            public boolean isSortable(final AbstractProxySelectorImpl obj) {
                return false;
            }

            @Override
            protected void setStringValue(String value, AbstractProxySelectorImpl object) {
                if (object.getClass() == SingleBasicProxySelectorImpl.class) {
                    ((SingleBasicProxySelectorImpl) object).setUser(value);
                } else if (object.getClass() == PacProxySelectorImpl.class) {
                    ((PacProxySelectorImpl) object).setUser(value);
                }

            }

            @Override
            public String getStringValue(AbstractProxySelectorImpl object) {
                if (object.getClass() == SingleBasicProxySelectorImpl.class) {
                    return ((SingleBasicProxySelectorImpl) object).getUser();
                } else if (object.getClass() == PacProxySelectorImpl.class) {
                    return ((PacProxySelectorImpl) object).getUser();
                }
                return "";
            }

        });
        this.addColumn(new ExtPasswordEditorColumn<AbstractProxySelectorImpl>(_GUI._.gui_column_pass(), this) {

            private static final long serialVersionUID = -7209180150340921804L;

            @Override
            public boolean isEditable(final AbstractProxySelectorImpl value) {
                if (value instanceof NoProxySelector) {
                    return false;
                } else if (value instanceof SingleDirectGatewaySelector) {
                    return false;
                }
                return true;
            }

            @Override
            public boolean isSortable(final AbstractProxySelectorImpl obj) {
                return false;
            }

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            protected void setStringValue(String value, AbstractProxySelectorImpl object) {
                if (object.getClass() == SingleBasicProxySelectorImpl.class) {
                    ((SingleBasicProxySelectorImpl) object).setPassword(value);
                } else if (object.getClass() == PacProxySelectorImpl.class) {
                    ((PacProxySelectorImpl) object).setPassword(value);
                }
            }

            @Override
            public String getStringValue(AbstractProxySelectorImpl value) {
                return super.getStringValue(value);
            }

            @Override
            protected String getPlainStringValue(AbstractProxySelectorImpl object) {
                if (object.getClass() == SingleBasicProxySelectorImpl.class) {
                    return ((SingleBasicProxySelectorImpl) object).getPassword();
                } else if (object.getClass() == PacProxySelectorImpl.class) {
                    return ((PacProxySelectorImpl) object).getPassword();
                }
                return "";
            }

        });
        this.addColumn(new ExtSpinnerColumn<AbstractProxySelectorImpl>(_GUI._.gui_column_port()) {

            /**
			 * 
			 */
            private static final long serialVersionUID = 7770193565380136904L;

            @Override
            protected Number getNumber(AbstractProxySelectorImpl value) {
                if (value.getClass() == SingleBasicProxySelectorImpl.class) {
                    ((SingleBasicProxySelectorImpl) value).getPort();
                }
                return -1;
            }

            public int getDefaultWidth() {
                return 80;
            }

            protected boolean isDefaultResizable() {
                return false;
            }

            @Override
            public boolean isSortable(final AbstractProxySelectorImpl obj) {
                return false;
            }

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            public boolean isEditable(final AbstractProxySelectorImpl value) {
                if (value instanceof NoProxySelector) {
                    return false;
                } else if (value instanceof SingleDirectGatewaySelector) {
                    return false;
                } else if (value instanceof PacProxySelectorImpl) {
                    return false;
                }
                return true;
            }

            @Override
            protected void setNumberValue(Number value, AbstractProxySelectorImpl object) {
                if (object.getClass() == SingleBasicProxySelectorImpl.class) {
                    ((SingleBasicProxySelectorImpl) object).setPort(value.intValue());
                }
            }

            @Override
            public String getStringValue(AbstractProxySelectorImpl value) {
                if (value.getClass() == SingleBasicProxySelectorImpl.class) {
                    return ((SingleBasicProxySelectorImpl) value).getPort() + "";
                }
                return "";
            }
        });

        this.addColumn(new ExtCheckColumn<AbstractProxySelectorImpl>(_GUI._.gui_column_nativeauth(), this) {

            private static final long serialVersionUID = -4667150369226691276L;

            @Override
            public int getMaxWidth() {
                return 60;
            }

            public boolean isDefaultVisible() {
                return false;
            }

            @Override
            public boolean isSortable(final AbstractProxySelectorImpl obj) {
                return false;
            }

            @Override
            protected boolean getBooleanValue(AbstractProxySelectorImpl value) {
                return value.isPreferNativeImplementation();
            }

            @Override
            public boolean isEditable(AbstractProxySelectorImpl obj) {
                switch (obj.getType()) {
                case HTTP:
                case PAC:
                    return true;
                default:
                    return false;
                }
            }

            @Override
            protected void setBooleanValue(final boolean value, final AbstractProxySelectorImpl object) {
                object.setPreferNativeImplementation(value);
            }
        });

        this.addColumn(new ExtComponentColumn<AbstractProxySelectorImpl>(_GUI._.lit_filter()) {
            private JButton                   editorBtn;
            private JButton                   rendererBtn;
            private AbstractProxySelectorImpl editing;
            protected MigPanel                editor;
            protected RendererMigPanel        renderer;
            private RenderLabel               label;

            {
                editorBtn = new JButton("");
                editorBtn.setFocusable(false);
                editorBtn.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (editing != null) {
                            System.out.println(editing);
                            try {
                                Dialog.I().showDialog(new ProxyDetailsDialog(editing));
                            } catch (DialogClosedException e1) {
                                e1.printStackTrace();
                            } catch (DialogCanceledException e1) {
                                e1.printStackTrace();
                            }
                            getTable().repaint();
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
            public int getDefaultWidth() {
                return 80;
            }

            @Override
            public boolean isSortable(final AbstractProxySelectorImpl obj) {
                return false;
            }

            @Override
            protected JComponent getInternalEditorComponent(AbstractProxySelectorImpl value, boolean isSelected, int row, int column) {
                return editor;
            }

            @Override
            public boolean onSingleClick(MouseEvent e, AbstractProxySelectorImpl obj) {
                return super.onSingleClick(e, obj);
            }

            @Override
            protected JComponent getInternalRendererComponent(AbstractProxySelectorImpl value, boolean isSelected, boolean hasFocus, int row, int column) {
                return renderer;
            }

            @Override
            public void configureEditorComponent(AbstractProxySelectorImpl value, boolean isSelected, int row, int column) {
                editing = value;
                int entries = 0;
                if (value.getFilter() != null) {
                    entries = value.getFilter().size();
                }
                if ((value.getFilter() == null || value.getFilter().getType() == FilterList.Type.BLACKLIST)) {
                    editorBtn.setText(_GUI._.proxytable_edit_btn_blacklist(entries));
                } else {
                    editorBtn.setText(_GUI._.proxytable_edit_btn_whitelist(entries));
                }
            }

            @Override
            public void configureRendererComponent(AbstractProxySelectorImpl value, boolean isSelected, boolean hasFocus, int row, int column) {
                int entries = 0;
                if (value.getFilter() != null) {
                    entries = value.getFilter().size();
                }
                if ((value.getFilter() == null || value.getFilter().getType() == FilterList.Type.BLACKLIST)) {
                    rendererBtn.setText(_GUI._.proxytable_edit_btn_blacklist(entries));
                } else {
                    rendererBtn.setText(_GUI._.proxytable_edit_btn_whitelist(entries));
                }
            }

            @Override
            public void resetEditor() {
            }

            @Override
            public void resetRenderer() {
            }

        });

        this.addColumn(new ExtComponentColumn<AbstractProxySelectorImpl>(_GUI._.lit_problems()) {
            private JButton                   editorBtn;
            private JButton                   rendererBtn;
            private AbstractProxySelectorImpl editing;
            protected MigPanel                editor;
            protected RendererMigPanel        renderer;
            private RenderLabel               label;

            {
                editorBtn = new JButton("");

                editorBtn.setFocusable(false);
                editorBtn.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (editing != null) {
                            SwingUtilities.invokeLater(new Runnable() {

                                public void run() {
                                    ToolTipController.getInstance().show(getModel().getTable().createExtTooltip(null));
                                }
                            });
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
            public int getDefaultWidth() {
                return 80;
            }

            @Override
            public boolean isSortable(final AbstractProxySelectorImpl obj) {
                return false;
            }

            @Override
            protected JComponent getInternalEditorComponent(AbstractProxySelectorImpl value, boolean isSelected, int row, int column) {
                return editor;
            }

            @Override
            public boolean onSingleClick(MouseEvent e, AbstractProxySelectorImpl obj) {
                return super.onSingleClick(e, obj);
            }

            @Override
            protected JComponent getInternalRendererComponent(AbstractProxySelectorImpl value, boolean isSelected, boolean hasFocus, int row, int column) {
                return renderer;
            }

            @Override
            public boolean isEnabled(AbstractProxySelectorImpl obj) {
                final List<ConnectionBan> bl = obj.getBanList();
                if (bl == null || bl.size() == 0) {
                    return true;
                } else {
                    return true;
                }
            }

            @Override
            public void configureRendererComponent(AbstractProxySelectorImpl value, boolean isSelected, boolean hasFocus, int row, int column) {
                final List<ConnectionBan> bl = value.getBanList();
                if (bl == null || bl.size() == 0) {
                    rendererBtn.setIcon(new AbstractIcon(IconKey.ICON_THUMBS_UP, 16));
                    rendererBtn.setText(_GUI._.proxytablemodel_problems(0));
                } else {
                    rendererBtn.setIcon(new AbstractIcon(IconKey.ICON_THUMBS_DOWN, 16));
                    rendererBtn.setText(_GUI._.proxytablemodel_problems(bl.size()));
                }
            }

            @Override
            public void configureEditorComponent(AbstractProxySelectorImpl value, boolean isSelected, int row, int column) {
                editing = value;
                final List<ConnectionBan> bl = value.getBanList();
                if (bl == null || bl.size() == 0) {
                    editorBtn.setIcon(new AbstractIcon(IconKey.ICON_THUMBS_UP, 16));
                    editorBtn.setText(_GUI._.proxytablemodel_problems(0));
                } else {
                    editorBtn.setIcon(new AbstractIcon(IconKey.ICON_THUMBS_DOWN, 16));
                    editorBtn.setText(_GUI._.proxytablemodel_problems(bl.size()));
                }

            }

            private String appendDescription(String description, String proxyDetailsDialog_ban_time_global) {
                if (StringUtils.isEmpty(description)) {
                    return proxyDetailsDialog_ban_time_global;
                }
                return proxyDetailsDialog_ban_time_global + " (" + description + ")";
            }

            @Override
            public ExtTooltip createToolTip(Point position, AbstractProxySelectorImpl obj) {
                final List<ConnectionBan> bl = obj.getBanList();
                final StringBuilder sb = new StringBuilder();
                if (bl != null && bl.size() > 0) {
                    for (ConnectionBan b : bl) {
                        if (sb.length() > 0) {
                            sb.append("\r\n");
                        }
                        sb.append("- " + b.toString());
                    }
                }
                String txt = sb.toString();
                if (txt == null || txt.length() == 0) {
                    txt = _GUI._.proxyDetailsDialog_ban_noban();
                }
                this.tooltip.setTipText(txt);
                return this.tooltip;
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