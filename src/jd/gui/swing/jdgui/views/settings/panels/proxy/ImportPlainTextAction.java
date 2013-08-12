package jd.gui.swing.jdgui.views.settings.panels.proxy;

import java.awt.event.ActionEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import jd.controlling.TaskQueue;
import jd.controlling.proxy.ProxyController;

import org.appwork.utils.Regex;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class ImportPlainTextAction extends AppAction {

    public ImportPlainTextAction(ProxyTable table) {
        setName(_GUI._.LinkgrabberFilter_LinkgrabberFilter_import());
        setIconKey("import");
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        try {
            String txt = Dialog.getInstance().showInputDialog(Dialog.STYLE_LARGE, _GUI._.ProxyConfig_actionPerformed_import_title_(), _GUI._.ProxyConfig_actionPerformed_import_proxies_explain_(), null, NewTheme.I().getIcon("proxy", 32), null, null);
            final java.util.List<HTTPProxy> list = new ArrayList<HTTPProxy>();
            for (String s : Regex.getLines(txt)) {
                try {
                    int i = s.indexOf("://");
                    String protocol = "http";
                    if (i > 0) {
                        protocol = s.substring(0, i);
                        s = "http://" + s.substring(i + 3);
                    }
                    URL url = null;

                    url = new URL(s);

                    String user = null;
                    String pass = null;
                    String userInfo = url.getUserInfo();
                    if (userInfo != null) {
                        int in = userInfo.indexOf(":");
                        if (in >= 0) {
                            user = (userInfo.substring(0, in));
                            pass = (userInfo.substring(in + 1));
                        } else {
                            user = (userInfo);
                        }
                    }
                    if ("socks5".equalsIgnoreCase(protocol)) {

                        final HTTPProxy ret = new HTTPProxy(HTTPProxy.TYPE.SOCKS5, url.getHost(), url.getPort());
                        ret.setUser(user);
                        ret.setPass(pass);
                        list.add(ret);
                    } else if ("socks4".equalsIgnoreCase(protocol)) {
                        final HTTPProxy ret = new HTTPProxy(HTTPProxy.TYPE.SOCKS4, url.getHost(), url.getPort());
                        ret.setUser(user);
                        list.add(ret);
                    } else {
                        final HTTPProxy ret = new HTTPProxy(HTTPProxy.TYPE.HTTP, url.getHost(), url.getPort());
                        ret.setUser(user);
                        ret.setPass(pass);
                        list.add(ret);

                    }
                } catch (MalformedURLException e2) {
                    e2.printStackTrace();
                }

            }
            TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    ProxyController.getInstance().addProxy(list);
                    return null;
                }
            });
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
    }

}
