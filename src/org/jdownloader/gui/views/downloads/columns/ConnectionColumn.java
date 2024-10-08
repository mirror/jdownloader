package org.jdownloader.gui.views.downloads.columns;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.swing.components.tooltips.ToolTipController;
import org.appwork.swing.components.tooltips.TooltipPanel;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtDefaultRowSorter;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.renderer.RenderLabel;
import org.appwork.utils.swing.renderer.RendererMigPanel;
import org.jdownloader.DomainInfo;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.plugins.SkipReason;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.proxy.PacProxySelectorImpl;
import jd.controlling.proxy.ProxyController;
import jd.controlling.proxy.SelectedProxy;
import jd.controlling.reconnect.ipcheck.BalancedWebIPCheck;
import jd.controlling.reconnect.ipcheck.IP;
import jd.controlling.reconnect.ipcheck.IPCheckException;
import jd.gui.swing.jdgui.GUIUtils;
import jd.http.ProxySelectorInterface;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;
import net.miginfocom.swing.MigLayout;

public class ConnectionColumn extends ExtColumn<AbstractNode> {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private MigPanel          panel;
    private RenderLabel[]     labels;
    private final Icon        resumeIndicator;
    private final Icon        directConnection;
    private final Icon        proxyConnection;
    private final Icon        connections;
    private final int         labelsSize       = 6;
    private final Icon        skipped;
    private final Icon        forced;
    private final Icon        tls;
    private DownloadWatchDog  dlWatchdog;
    private final Icon        url;

    public JPopupMenu createHeaderPopup() {
        return FileColumn.createColumnPopup(this, getMinWidth() == getMaxWidth() && getMaxWidth() > 0);
    }

    public ConnectionColumn() {
        super(_GUI.T.ConnectionColumn_ConnectionColumn(), null);
        panel = new RendererMigPanel("ins 0 0 0 0", "[]", "[grow,fill]");
        labels = new RenderLabel[labelsSize + 1];
        // panel.add(Box.createGlue(), "pushx,growx");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < labels.length; i++) {
            labels[i] = new RenderLabel();
            // labels[i].setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1,
            // Color.RED));
            labels[i].setOpaque(false);
            labels[i].setBackground(null);
            if (sb.length() > 0) {
                sb.append("1");
            }
            sb.append("[18!]");
            panel.add(labels[i]);
        }
        dlWatchdog = DownloadWatchDog.getInstance();
        skipped = NewTheme.I().getIcon(IconKey.ICON_SKIPPED, 16);
        forced = NewTheme.I().getIcon(IconKey.ICON_MEDIA_PLAYBACK_START_FORCED, 16);
        resumeIndicator = NewTheme.I().getIcon(IconKey.ICON_REFRESH, 16);
        directConnection = NewTheme.I().getIcon(IconKey.ICON_MODEM, 16);
        proxyConnection = NewTheme.I().getIcon(IconKey.ICON_PROXY_ROTATE, 16);
        connections = NewTheme.I().getIcon(IconKey.ICON_CHUNKS, 16);
        tls = NewTheme.I().getIcon(IconKey.ICON_INFO, 16);
        url = NewTheme.I().getIcon(IconKey.ICON_URL, 16);
        panel.setLayout(new MigLayout("ins 0 0 0 0", sb.toString(), "[grow,fill]"));
        // panel.add(Box.createGlue(), "pushx,growx");
        this.setRowSorter(new ExtDefaultRowSorter<AbstractNode>() {
            @Override
            public int compare(final AbstractNode o1, final AbstractNode o2) {
                final long l1 = getDownloads(o1);
                final long l2 = getDownloads(o2);
                if (l1 == l2) {
                    return 0;
                }
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
    public boolean onSingleClick(MouseEvent e, AbstractNode obj) {
        return super.onSingleClick(e, obj);
    }

    @Override
    public boolean onDoubleClick(MouseEvent e, AbstractNode obj) {
        if (obj instanceof DownloadLink) {
            ConnectionTooltip tt = new ConnectionTooltip((DownloadLink) obj) {
                public boolean isLastHiddenEnabled() {
                    return false;
                }
            };
            ToolTipController.getInstance().show(tt);
            return true;
        }
        return false;
    }

    private int getDownloads(AbstractNode value) {
        if (value instanceof DownloadLink) {
            SingleDownloadController dlc = ((DownloadLink) value).getDownloadLinkController();
            if (dlc != null) {
                DownloadInterface dli = dlc.getDownloadInstance();
                if (dli != null) {
                    return 1;
                }
            }
        } else if (value instanceof FilePackage) {
            return DownloadWatchDog.getInstance().getDownloadsbyFilePackage((FilePackage) value);
        }
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
        if (obj instanceof DownloadLink) {
            return ((DownloadLink) obj).isEnabled();
        }
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
        return labelsSize * 19 + 14;
    }

    public void configureRendererComponent(AbstractNode value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof DownloadLink) {
            final DownloadLink dlLink = (DownloadLink) value;
            final SingleDownloadController sdc = dlLink.getDownloadLinkController();
            final DownloadInterface dli;
            if (sdc != null) {
                dli = sdc.getDownloadInstance();
            } else {
                dli = null;
            }
            int labelIndex = 0;
            if (dlLink.isSkipped()) {
                labels[labelIndex].setIcon(skipped);
                labels[labelIndex].setVisible(true);
                labelIndex++;
            }
            if (dlWatchdog.isLinkForced(dlLink) && dlLink.getFinalLinkState() == null && dlLink.isEnabled()) {
                labels[labelIndex].setIcon(forced);
                labels[labelIndex].setVisible(true);
                labelIndex++;
            }
            if (dlLink.isResumeable() && dlLink.getFinalLinkState() == null) {
                labels[labelIndex].setIcon(resumeIndicator);
                labels[labelIndex].setVisible(true);
                labelIndex++;
            }
            if (dli != null && sdc != null) {
                final HTTPProxy proxy = sdc.getUsedProxy();
                if (proxy != null && proxy.isRemote()) {
                    labels[labelIndex].setIcon(proxyConnection);
                    labels[labelIndex].setVisible(true);
                } else {
                    labels[labelIndex].setIcon(directConnection);
                    labels[labelIndex].setVisible(true);
                }
                labelIndex++;
                if (sdc.getAccount() != null && sdc.getAccount().getPlugin() != null) {
                    final PluginForHost plugin = sdc.getAccount().getPlugin();
                    final DomainInfo domainInfo = DomainInfo.getInstance(plugin.getHost(dlLink, sdc.getAccount(), false));
                    if (domainInfo != null) {
                        final Icon icon = domainInfo.getFavIcon();
                        labels[labelIndex].setIcon(icon);
                        labels[labelIndex].setVisible(true);
                        labelIndex++;
                    }
                } else {
                    final PluginForHost plugin = sdc.getProcessingPlugin();
                    final DomainInfo domainInfo = plugin != null ? DomainInfo.getInstance(sdc.getProcessingPlugin().getHost(dlLink, sdc.getAccount(), false)) : null;
                    final Icon icon;
                    if (domainInfo != null) {
                        icon = domainInfo.getFavIcon();
                    } else {
                        icon = url;
                    }
                    labels[labelIndex].setIcon(icon);
                    labels[labelIndex].setVisible(true);
                    labelIndex++;
                }
                labels[labelIndex].setText("" + dli.getManagedConnetionHandler().size());
                labels[labelIndex].setIcon(connections);
                labels[labelIndex].setVisible(true);
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
            if (ret.getComponentCount() > 0) {
                return ret;
            }
        }
        return null;
    }

    private static final AtomicLong               TASK      = new AtomicLong(0);
    private static final ScheduledExecutorService SCHEDULER = DelayedRunnable.getNewScheduledExecutorService();

    private class ConnectionTooltip extends ExtTooltip {
        /**
         *
         */
        private static final long serialVersionUID = -6581783135666367021L;

        public ConnectionTooltip(final DownloadLink link) {
            this.panel = new TooltipPanel("ins 3,wrap 1", "[grow,fill]", "[grow,fill]");
            final SingleDownloadController sdc = link.getDownloadLinkController();
            final DownloadInterface dli;
            if (sdc != null) {
                dli = sdc.getDownloadInstance();
            } else {
                dli = null;
            }
            {
                if (dlWatchdog.isLinkForced(link) && link.getFinalLinkState() == null && link.isEnabled()) {
                    if (dli == null) {
                        add(new JLabel(_GUI.T.ConnectionColumn_DownloadIsForcedWaiting(), forced, JLabel.LEADING));
                    } else {
                        add(new JLabel(_GUI.T.ConnectionColumn_DownloadIsForced(), forced, JLabel.LEADING));
                    }
                }
                final SkipReason skipReason = link.getSkipReason();
                if (skipReason != null) {
                    add(new JLabel(skipReason.getExplanation(ConnectionColumn.this, link), skipped, JLabel.LEADING));
                }
                if (link.isResumeable() && link.getFinalLinkState() == null) {
                    add(new JLabel(_GUI.T.ConnectionColumn_DownloadIsResumeable(), resumeIndicator, JLabel.LEADING));
                }
            }
            if (sdc != null) {
                {
                    /* connection? */
                    final HTTPProxy proxy;
                    if (sdc.getUsedProxy() == null) {
                        proxy = HTTPProxy.NONE;
                    } else {
                        proxy = sdc.getUsedProxy();
                    }
                    final SelectedProxy selectedProxy = ProxyController.getSelectedProxy(proxy);
                    final String proxyString;
                    if (selectedProxy != null && selectedProxy.getSelector() != null) {
                        if (selectedProxy.getSelector() instanceof PacProxySelectorImpl) {
                            proxyString = selectedProxy.getSelector().toDetailsString() + "@" + proxy.toString();
                        } else {
                            proxyString = selectedProxy.getSelector().toDetailsString();
                        }
                    } else {
                        proxyString = proxy.toString();
                    }
                    final HTTPProxy finalProxy = proxy;
                    final JLabel finalLbl = new JLabel(_GUI.T.ConnectionColumn_getStringValue_connection(proxyString + " (000.000.000.000)"), proxy.isRemote() ? proxyConnection : directConnection, JLabel.LEADING);
                    add(finalLbl);
                    final long taskID = TASK.incrementAndGet();
                    SCHEDULER.execute(new Runnable() {
                        @Override
                        public void run() {
                            if (taskID == TASK.get()) {
                                final List<HTTPProxy> proxies = new ArrayList<HTTPProxy>();
                                proxies.add(finalProxy);
                                final BalancedWebIPCheck ipCheck = new BalancedWebIPCheck(new ProxySelectorInterface() {
                                    @Override
                                    public boolean updateProxy(Request request, int retryCounter) {
                                        return false;
                                    }

                                    @Override
                                    public boolean reportConnectException(Request request, int retryCounter, IOException e) {
                                        return false;
                                    }

                                    @Override
                                    public List<HTTPProxy> getProxiesByURL(URL uri) {
                                        return proxies;
                                    }
                                });
                                try {
                                    final IP ip = ipCheck.getExternalIP();
                                    new EDTRunner() {
                                        @Override
                                        protected void runInEDT() {
                                            finalLbl.setText(_GUI.T.ConnectionColumn_getStringValue_connection(proxyString + " (" + ip.getIP() + ")"));
                                        }
                                    };
                                } catch (IPCheckException e1) {
                                    e1.printStackTrace();
                                }
                            }
                        }
                    });
                }
                if (sdc.getAccount() != null && sdc.getAccount().getPlugin() != null) {
                    /* account in use? */
                    final PluginForHost plugin = sdc.getAccount().getPlugin();
                    final DomainInfo domainInfo = DomainInfo.getInstance(plugin.getHost(link, sdc.getAccount(), false));
                    if (domainInfo != null) {
                        final Icon icon = domainInfo.getFavIcon();
                        add(new JLabel(_GUI.T.ConnectionColumn_DownloadUsesAccount(GUIUtils.getAccountName(sdc.getAccount().getUser())), icon, JLabel.LEADING));
                    }
                }
            }
            if (dli != null) {
                final PluginForHost plugin = sdc.getProcessingPlugin();
                final DomainInfo domainInfo = plugin != null ? DomainInfo.getInstance(sdc.getProcessingPlugin().getHost(link, sdc.getAccount(), false)) : null;
                final Icon icon;
                if (domainInfo != null) {
                    icon = domainInfo.getFavIcon();
                } else {
                    icon = url;
                }
                final URLConnectionAdapter con = dli.getConnection();
                if (con != null) {
                    add(new JLabel(_GUI.T.ConnectionColumn_getStringValue_from(con.getURL().getProtocol() + "@" + dli.getDownloadable().getHost()), icon, JLabel.LEADING));
                    final String cipher = con.getCipherSuite();
                    if (cipher != null) {
                        add(new JLabel(cipher, tls, JLabel.LEADING));
                    }
                } else {
                    add(new JLabel(_GUI.T.ConnectionColumn_getStringValue_from(dli.getDownloadable().getHost()), icon, JLabel.LEADING));
                }
                add(new JLabel(_GUI.T.ConnectionColumn_getStringValue_chunks(dli.getManagedConnetionHandler().size()), connections, JLabel.LEADING));
            }
            this.panel.setOpaque(false);
            if (panel.getComponentCount() > 0) {
                add(panel);
            }
        }

        private JLabel add(JLabel lbl) {
            SwingUtils.setOpaque(lbl, false);
            Color color = UIManager.getColor(ExtTooltip.APPWORK_TOOLTIP_FOREGROUND);
            if (color != null) {
                lbl.setForeground(color);
            }
            panel.add(lbl);
            return lbl;
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
        for (int i = 0; i < labels.length; i++) {
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