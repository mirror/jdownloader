package jd.gui.swing.jdgui.views.settings.panels.proxy;

import java.awt.Color;
import java.awt.Point;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.Icon;
import javax.swing.JLabel;

import jd.controlling.proxy.AbstractProxySelectorImpl;
import jd.controlling.proxy.NoProxySelector;
import jd.controlling.proxy.PacProxySelectorImpl;
import jd.controlling.proxy.SingleBasicProxySelectorImpl;
import jd.controlling.proxy.SingleDirectGatewaySelector;
import jd.controlling.reconnect.ipcheck.BalancedWebIPCheck;
import jd.controlling.reconnect.ipcheck.IP;
import jd.controlling.reconnect.ipcheck.IPCheckException;
import jd.http.ProxySelectorInterface;
import jd.http.Request;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.JSonStorage;
import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.swing.components.tooltips.TooltipPanel;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class HostColumn extends ExtTextColumn<AbstractProxySelectorImpl> {
    public HostColumn() {
        super(_GUI.T.gui_column_host2());
    }

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Override
    protected Icon getIcon(AbstractProxySelectorImpl value) {
        return super.getIcon(value);
    }

    @Override
    public boolean isSortable(final AbstractProxySelectorImpl obj) {
        return false;
    }

    @Override
    protected void setStringValue(String value, AbstractProxySelectorImpl object) {
        if (object instanceof NoProxySelector) {
            java.awt.Toolkit.getDefaultToolkit().beep();
            return;
        } else if (object instanceof SingleDirectGatewaySelector) {
            ((SingleDirectGatewaySelector) object).getProxy().setLocal(value);
            return;
        } else if (object instanceof SingleBasicProxySelectorImpl) {
            ((SingleBasicProxySelectorImpl) object).setHost(value);
            return;
        } else if (object instanceof PacProxySelectorImpl) {
            ((PacProxySelectorImpl) object).setPACUrl(value);
            return;
        }
        throw new IllegalStateException("Unknown Factory Type: " + object.getClass());
    }

    @Override
    public String getStringValue(AbstractProxySelectorImpl value) {
        try {
            if (value instanceof NoProxySelector) {
                return "";
            } else if (value instanceof SingleDirectGatewaySelector) {
                return ((SingleDirectGatewaySelector) value).getProxy().getLocal();
            } else if (value instanceof SingleBasicProxySelectorImpl) {
                return ((SingleBasicProxySelectorImpl) value).getProxy().getHost();
            } else if (value instanceof PacProxySelectorImpl) {
                return ((PacProxySelectorImpl) value).getPACUrl();
            }
            throw new IllegalStateException("Unknown Factory Type: " + value.getClass());
        } catch (Throwable e) {
            return "Invalid Proxy: " + JSonStorage.serializeToJson(value.toProxyData());
        }
    }

    @Override
    public boolean isEditable(AbstractProxySelectorImpl value) {
        if (value instanceof NoProxySelector) {
            return false;
        } else if (value instanceof SingleDirectGatewaySelector) {
            return true;
        }
        return true;
    }

    @Override
    public boolean isEnabled(AbstractProxySelectorImpl obj) {
        return true;
    }

    private static final AtomicLong               TASK      = new AtomicLong(0);
    private static final ScheduledExecutorService SCHEDULER = DelayedRunnable.getNewScheduledExecutorService();

    private class ConnectionTooltip extends ExtTooltip {
        /**
         *
         */
        private static final long serialVersionUID = -6581783135666367021L;

        public ConnectionTooltip(final AbstractProxySelectorImpl impl) {
            JLabel lbl;
            this.panel = new TooltipPanel("ins 3,wrap 1", "[grow,fill]", "[grow,fill]");
            final String proxyString = impl.toDetailsString();
            final Icon icon;
            if (AbstractProxySelectorImpl.Type.DIRECT.equals(impl.getType()) || AbstractProxySelectorImpl.Type.NONE.equals(impl.getType())) {
                icon = NewTheme.I().getIcon("modem", 16);
            } else {
                icon = NewTheme.I().getIcon("proxy_rotate", 16);
            }
            panel.add(lbl = new JLabel(_GUI.T.ConnectionColumn_getStringValue_connection(proxyString + " (000.000.000.000)"), icon, JLabel.LEADING));
            SwingUtils.setOpaque(lbl, false);
            lbl.setForeground(new Color(this.getConfig().getForegroundColor()));
            final JLabel finalLbl = lbl;
            final long taskID = TASK.incrementAndGet();
            SCHEDULER.execute(new Runnable() {
                @Override
                public void run() {
                    if (taskID == TASK.get()) {
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
                                return impl.getProxiesByURL(uri);
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
            this.panel.setOpaque(false);
            if (panel.getComponentCount() > 0) {
                add(panel);
            }
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
    public ExtTooltip createToolTip(Point position, AbstractProxySelectorImpl obj) {
        if (obj instanceof AbstractProxySelectorImpl) {
            final ConnectionTooltip ret = new ConnectionTooltip(obj);
            if (ret.getComponentCount() > 0) {
                return ret;
            }
        }
        return null;
    }

    @Override
    public void setValue(Object value, AbstractProxySelectorImpl object) {
        super.setValue(value, object);
    }

    @Override
    public int getDefaultWidth() {
        return 200;
    }

    @Override
    public int getMinWidth() {
        return 100;
    }

    @Override
    public boolean isHidable() {
        return false;
    }
}
