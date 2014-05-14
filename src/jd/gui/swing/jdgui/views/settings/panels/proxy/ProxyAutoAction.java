package jd.gui.swing.jdgui.views.settings.panels.proxy;

import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import jd.controlling.TaskQueue;
import jd.controlling.proxy.AbstractProxySelectorImpl;
import jd.controlling.proxy.PacProxySelectorImpl;
import jd.controlling.proxy.ProxyController;
import jd.controlling.proxy.SingleBasicProxySelectorImpl;
import jd.controlling.proxy.SingleDirectGatewaySelector;

import org.appwork.utils.StringUtils;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.HTTPProxy.TYPE;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.MessageDialogImpl;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

import com.btr.proxy.search.ProxySearchStrategy;
import com.btr.proxy.search.browser.firefox.FirefoxProxySearchStrategy;
import com.btr.proxy.search.desktop.DesktopProxySearchStrategy;
import com.btr.proxy.search.env.EnvProxySearchStrategy;
import com.btr.proxy.search.java.JavaProxySearchStrategy;
import com.btr.proxy.selector.pac.PacProxySelector;
import com.btr.proxy.selector.pac.PacScriptParser;
import com.btr.proxy.selector.pac.PacScriptSource;
import com.btr.proxy.selector.pac.UrlPacScriptSource;

public class ProxyAutoAction extends AppAction {

    public ProxyAutoAction() {
        super();
        setName(_GUI._.ProxyAutoAction_actionPerformed_d_title());
        setIconKey("plugin");
    }

    /**
     * 
     */
    private static final long serialVersionUID = -197136045388327528L;

    public void actionPerformed(ActionEvent e) {
        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                ArrayList<ProxySearchStrategy> strategies = new ArrayList<ProxySearchStrategy>();
                strategies.add(new DesktopProxySearchStrategy());

                strategies.add(new FirefoxProxySearchStrategy());

                // strategies.add(new IEProxySearchStrategy());

                strategies.add(new EnvProxySearchStrategy());

                // strategies.add(new WinProxySearchStrategy());
                //
                // strategies.add(new KdeProxySearchStrategy());
                //
                // strategies.add(new GnomeProxySearchStrategy());

                strategies.add(new JavaProxySearchStrategy());
                final int pre = ProxyController.getInstance().getList().size();
                for (ProxySearchStrategy s : strategies) {
                    ProxySelector selector;
                    try {
                        selector = s.getProxySelector();
                        if (selector == null)
                            continue;
                        if (selector instanceof PacProxySelector) {
                            Field field = PacProxySelector.class.getDeclaredField("pacScriptParser");
                            field.setAccessible(true);
                            PacScriptParser source = (PacScriptParser) field.get(selector);
                            PacScriptSource pacSource = source.getScriptSource();
                            if (pacSource != null && pacSource instanceof UrlPacScriptSource) {
                                field = UrlPacScriptSource.class.getDeclaredField("scriptUrl");
                                field.setAccessible(true);

                                Object pacURL = field.get(pacSource);
                                if (StringUtils.isNotEmpty((String) pacURL)) {
                                    addProxy(enable(new PacProxySelectorImpl((String) pacURL, null, null)));
                                }
                            }
                        } else {
                            List<Proxy> proxies = selector.select(new URI("http://google.com"));
                            if (proxies != null) {
                                for (Proxy p : proxies) {
                                    HTTPProxy httpProxy = null;
                                    switch (p.type()) {
                                    case DIRECT:
                                        if (p.address() == null) {
                                            httpProxy = new HTTPProxy(TYPE.NONE);

                                        } else {

                                            httpProxy = new HTTPProxy(((InetSocketAddress) p.address()).getAddress());
                                            addProxy(enable(new SingleDirectGatewaySelector(httpProxy)));
                                        }
                                        break;
                                    case HTTP:
                                        httpProxy = new HTTPProxy(TYPE.HTTP, ((InetSocketAddress) p.address()).getHostString(), ((InetSocketAddress) p.address()).getPort());
                                        addProxy(enable(new SingleBasicProxySelectorImpl(httpProxy)));
                                        break;
                                    case SOCKS:

                                        httpProxy = new HTTPProxy(TYPE.SOCKS5, ((InetSocketAddress) p.address()).getHostString(), ((InetSocketAddress) p.address()).getPort());
                                        addProxy(enable(new SingleBasicProxySelectorImpl(httpProxy)));
                                    }

                                }
                            }
                        }
                        // selector.select(new URI("http://google.de"));
                        System.out.println(selector);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
                if (ProxyController.getInstance().getList().size() > pre) {

                    new EDTRunner() {
                        @Override
                        protected void runInEDT() {
                            new MessageDialogImpl(0, _GUI._.ProxyAutoAction_run_added_proxies_(ProxyController.getInstance().getList().size() - pre)).show();
                        }
                    };
                } else {
                    new EDTRunner() {
                        @Override
                        protected void runInEDT() {
                            new MessageDialogImpl(0, _GUI._.ProxyAutoAction_run_added_proxies_(0)).show();
                        }
                    };
                }

                // ProxySelector myProxySelector = proxySearch.getProxySelector();

                // myProxySelector.select(uri)
                return null;
            }
        });

    }

    protected void addProxy(AbstractProxySelectorImpl enable) {
        ProxyController.getInstance().setEnabled(ProxyController.getInstance().getNone(), false);

        ProxyController.getInstance().addProxy(enable);

    }

    protected AbstractProxySelectorImpl enable(AbstractProxySelectorImpl pacProxyFactory) {
        pacProxyFactory.setEnabled(true);

        return pacProxyFactory;
    }
}
