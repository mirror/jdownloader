package jd.gui.swing.jdgui.views.settings.panels.proxy;

import java.awt.event.ActionEvent;

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
            final String txt = Dialog.getInstance().showInputDialog(Dialog.STYLE_LARGE, _GUI._.ProxyConfig_actionPerformed_import_title_(), _GUI._.ProxyConfig_actionPerformed_import_proxies_explain_(), null, NewTheme.I().getIcon("proxy", 32), null, null);
            TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    for (String s : Regex.getLines(txt)) {
                        try {
                            final HTTPProxy proxy = HTTPProxy.parseHTTPProxy(s);
                            ProxyController.getInstance().addProxy(ProxyController.getInstance().convert(proxy));
                        } catch (Throwable e2) {
                            e2.printStackTrace();
                        }
                    }

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
