package org.jdownloader.gui.views.downloads.columns;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.download.DownloadInterface;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.swing.components.tooltips.ToolTipController;
import org.appwork.swing.components.tooltips.TooltipPanel;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtDefaultRowSorter;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.renderer.RenderLabel;
import org.appwork.utils.swing.renderer.RendererMigPanel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class ConnectionColumn extends ExtColumn<AbstractNode> {

    /**
     * 
     */
    private static final long serialVersionUID   = 1L;

    private MigPanel          panel;
    private RenderLabel[]     labels;
    private final ImageIcon   resumeIndicator;
    private final ImageIcon   accountInUse;
    private final ImageIcon   directConnection;
    private final ImageIcon   proxyConnection;
    private final ImageIcon   connections;

    private final int         DEFAULT_ICON_COUNT = 4;

    public ConnectionColumn() {
        super(_GUI._.ConnectionColumn_ConnectionColumn(), null);
        panel = new RendererMigPanel("ins 0 0 0 0", "[]", "[grow,fill]");
        labels = new RenderLabel[DEFAULT_ICON_COUNT + 1];

        // panel.add(Box.createGlue(), "pushx,growx");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= DEFAULT_ICON_COUNT; i++) {
            labels[i] = new RenderLabel();
            // labels[i].setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1,
            // Color.RED));
            labels[i].setOpaque(false);
            labels[i].setBackground(null);
            if (sb.length() > 0) sb.append("1");
            sb.append("[18!]");
            panel.add(labels[i]);

        }

        resumeIndicator = NewTheme.I().getIcon("refresh", 16);
        directConnection = NewTheme.I().getIcon("modem", 16);
        proxyConnection = NewTheme.I().getIcon("proxy_rotate", 16);
        accountInUse = NewTheme.I().getIcon("users", 16);
        connections = NewTheme.I().getIcon("paralell", 16);
        panel.setLayout(new MigLayout("ins 0 0 0 0", sb.toString(), "[]"));
        // panel.add(Box.createGlue(), "pushx,growx");
        this.setRowSorter(new ExtDefaultRowSorter<AbstractNode>() {

            @Override
            public int compare(final AbstractNode o1, final AbstractNode o2) {
                final long l1 = getDownloads(o1);
                final long l2 = getDownloads(o2);
                if (l1 == l2) { return 0; }
                if (this.getSortOrderIdentifier() == ExtColumn.SORT_ASC) {
                    return l1 > l2 ? -1 : 1;
                } else {
                    return l1 < l2 ? -1 : 1;
                }
            }

        });

        resetRenderer();
    }

    @Override
    protected boolean onSingleClick(MouseEvent e, AbstractNode obj) {
        return super.onSingleClick(e, obj);
    }

    @Override
    protected boolean onDoubleClick(MouseEvent e, AbstractNode obj) {
        if (obj instanceof DownloadLink) {

            ConnectionTooltip tt = new ConnectionTooltip((DownloadLink) obj) {
                public boolean isLastHiddenEnabled() {
                    return false;
                }
            };
            ToolTipController.getInstance().show(tt);
            return true;
        }
        ;

        return false;
    }

    private int getDownloads(AbstractNode value) {
        if (value instanceof DownloadLink) {
            SingleDownloadController dlc = ((DownloadLink) value).getDownloadLinkController();
            if (dlc != null) {
                DownloadInterface dli = ((DownloadLink) value).getDownloadInstance();
                if (dli != null) return 1;
            }
        } else if (value instanceof FilePackage) { return DownloadWatchDog.getInstance().getDownloadsbyFilePackage((FilePackage) value); }
        return 0;
    }

    @Override
    public Object getCellEditorValue() {
        return null;
    }

    @Override
    protected boolean isDefaultResizable() {
        return false;
    }

    @Override
    public boolean isEditable(AbstractNode obj) {
        return false;
    }

    @Override
    public boolean isEnabled(AbstractNode obj) {
        if (obj instanceof DownloadLink) { return ((DownloadLink) obj).isEnabled(); }
        return false;
    }

    public boolean isPaintWidthLockIcon() {
        return false;
    }

    @Override
    public boolean isSortable(AbstractNode obj) {
        return true;
    }

    @Override
    public void setValue(Object value, AbstractNode object) {

    }

    @Override
    public int getDefaultWidth() {
        return DEFAULT_ICON_COUNT * 19 + 7;
    }

    public void configureRendererComponent(AbstractNode value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof DownloadLink) {
            DownloadLink dlLink = (DownloadLink) value;
            DownloadInterface dli = dlLink.getDownloadInstance();
            SingleDownloadController sdc = dlLink.getDownloadLinkController();
            int index = 0;
            if (dlLink.isResumeable()) {
                labels[index].setIcon(resumeIndicator);
                labels[index].setVisible(true);
                index++;
            }
            if (dli != null && sdc != null) {
                HTTPProxy proxy = sdc.getCurrentProxy();
                if (proxy != null && proxy.isRemote()) {
                    labels[index].setIcon(proxyConnection);
                    labels[index].setVisible(true);
                } else {
                    labels[index].setIcon(directConnection);
                    labels[index].setVisible(true);
                }
                index++;
                if (sdc.getAccount() != null) {
                    labels[index].setIcon(accountInUse);
                    labels[index].setVisible(true);
                    index++;
                }
                labels[index].setText("" + dli.getChunksDownloading());
                labels[index].setIcon(connections);
                labels[index].setVisible(true);
            }
        }
    }

    @Override
    public JComponent getEditorComponent(AbstractNode value, boolean isSelected, int row, int column) {
        return null;
    }

    @Override
    public JComponent getRendererComponent(AbstractNode value, boolean isSelected, boolean hasFocus, int row, int column) {
        return panel;
    }

    @Override
    public void resetEditor() {
    }

    @Override
    public ExtTooltip createToolTip(Point position, AbstractNode obj) {
        if (obj instanceof DownloadLink) {
            ConnectionTooltip ret = new ConnectionTooltip((DownloadLink) obj);
            if (ret.getComponentCount() > 0) return ret;
        }
        return null;
    }

    private class ConnectionTooltip extends ExtTooltip {

        /**
         * 
         */
        private static final long serialVersionUID = -6581783135666367021L;

        public ConnectionTooltip(DownloadLink link) {
            JLabel lbl;
            this.panel = new TooltipPanel("ins 3,wrap 1", "[grow,fill]", "[grow,fill]");
            DownloadInterface dli = link.getDownloadInstance();
            SingleDownloadController sdc = link.getDownloadLinkController();
            {
                /* is the Link resumeable */
                if (link.isResumeable()) {
                    panel.add(lbl = new JLabel(_GUI._.ConnectionColumn_DownloadIsResumeable(), resumeIndicator, JLabel.LEADING));
                    SwingUtils.setOpaque(lbl, false);
                    lbl.setForeground(new Color(this.getConfig().getForegroundColor()));
                }
            }
            if (sdc != null) {
                {
                    /* connection? */
                    HTTPProxy proxy = sdc.getCurrentProxy();
                    if (proxy == null) proxy = HTTPProxy.NONE;
                    panel.add(lbl = new JLabel(_GUI._.ConnectionColumn_getStringValue_connection(proxy), proxy.isRemote() ? proxyConnection : directConnection, JLabel.LEADING));
                    SwingUtils.setOpaque(lbl, false);
                    lbl.setForeground(new Color(this.getConfig().getForegroundColor()));
                }
                if (sdc.getAccount() != null) {
                    /* account in use? */
                    panel.add(lbl = new JLabel(_GUI._.ConnectionColumn_DownloadUsesAccount(sdc.getAccount().getUser()), accountInUse, JLabel.LEADING));
                    SwingUtils.setOpaque(lbl, false);
                    lbl.setForeground(new Color(this.getConfig().getForegroundColor()));
                }
            }
            if (dli != null) {
                panel.add(lbl = new JLabel(_GUI._.ConnectionColumn_getStringValue_chunks(dli.getChunksDownloading()), connections, JLabel.LEADING));
                SwingUtils.setOpaque(lbl, false);
                lbl.setForeground(new Color(this.getConfig().getForegroundColor()));
            }
            this.panel.setOpaque(false);
            if (panel.getComponentCount() > 0) add(panel);
        }

        @Override
        public TooltipPanel createContent() {
            return null;
        }

        @Override
        public String toText() {
            return null;
        }

    }

    @Override
    public void resetRenderer() {
        for (int i = 0; i <= DEFAULT_ICON_COUNT; i++) {
            labels[i].setVisible(false);
            labels[i].setText(null);
        }
        this.panel.setOpaque(false);
        this.panel.setBackground(null);
    }

    @Override
    public void configureEditorComponent(AbstractNode value, boolean isSelected, int row, int column) {
    }

}