package jd.gui.swing.jdgui.views.settings.panels.proxy;

import java.util.List;

import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;

import jd.controlling.proxy.AbstractProxySelectorImpl;
import jd.controlling.proxy.AbstractProxySelectorImpl.Type;
import jd.controlling.proxy.ProxyController;
import jd.controlling.proxy.ProxyEvent;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.exttable.utils.MinimumSelectionObserver;
import org.appwork.utils.event.DefaultEventListener;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.translate._JDT;

public class ProxyConfig extends AbstractConfigPanel implements DefaultEventListener<ProxyEvent<AbstractProxySelectorImpl>> {
    public String getTitle() {
        return _JDT.T.gui_settings_proxy_title();
    }

    private static final long     serialVersionUID = -521958649780869375L;
    private final DelayedRunnable delayer;

    public ProxyConfig() {
        super();
        this.addHeader(getTitle(), new AbstractIcon(IconKey.ICON_PROXY_ROTATE, 32));
        this.addDescriptionPlain(_JDT.T.gui_settings_proxy_description());
        this.addDescriptionPlain(_JDT.T.gui_settings_proxy_description_new());
        final ProxyTable proxyTable = new ProxyTable();
        final JScrollPane sp = new JScrollPane(proxyTable);
        this.add(sp, "gapleft 37,growx, pushx,spanx,pushy,growy");
        final MigPanel toolbar = new MigPanel("ins 0", "[][][][grow,fill][]0[][]0[]", "[]");
        toolbar.setOpaque(false);
        final ExtButton btnAdd = new ExtButton(new ProxyAddAction(proxyTable));
        final ExtButton btnAuto = new ExtButton(new ProxyAutoAction());
        ProxyDeleteAction dl;
        final ExtButton btnRemove = new ExtButton(dl = new ProxyDeleteAction(proxyTable));
        final ExtButton btImport = new ExtButton(new ImportPlainTextAction(proxyTable));
        final ExtButton btExport = new ExtButton(new ExportPlainTextAction(proxyTable));
        final ExtButton impPopup = new ExtButton(new ImportPopupAction(btImport, proxyTable)) {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            public void setBounds(int x, int y, int width, int height) {
                super.setBounds(x - 2, y, width + 2, height);
            }
        };
        final ExtButton expPopup = new ExtButton(new ExportPopupAction(btExport, proxyTable)) {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            public void setBounds(int x, int y, int width, int height) {
                super.setBounds(x - 2, y, width + 2, height);
            }
        };
        proxyTable.getSelectionModel().addListSelectionListener(new MinimumSelectionObserver(proxyTable, dl, 1) {
            @Override
            public void valueChanged(final ListSelectionEvent e) {
                boolean canremove = false;
                final List<AbstractProxySelectorImpl> selected = proxyTable.getModel().getSelectedObjects();
                if (selected != null) {
                    for (final AbstractProxySelectorImpl pi : selected) {
                        if (pi.getType() == Type.NONE) {
                            continue;
                        } else {
                            canremove = true;
                            break;
                        }
                    }
                }
                action.setEnabled(canremove);
            }
        });
        toolbar.add(btnAdd, "sg 1,height 26!");
        toolbar.add(btnRemove, "sg 1,height 26!");
        toolbar.add(btnAuto, "gapleft 5,height 26!");
        toolbar.add(Box.createHorizontalGlue(), "pushx,growx");
        toolbar.add(btImport, "sg 2,height 26!");
        toolbar.add(impPopup, "height 26!,width 12!,aligny top");
        toolbar.add(btExport, "sg 2,height 26!");
        toolbar.add(expPopup, "height 26!,width 12!,aligny top");
        add(toolbar, "gapleft 37,growx,spanx");
        delayer = new DelayedRunnable(50, 150) {
            @Override
            public void delayedrun() {
                proxyTable.getModel()._fireTableStructureChanged(ProxyController.getInstance().getList(), false);
            }
        };
    }

    @Override
    public Icon getIcon() {
        return new AbstractIcon(IconKey.ICON_PROXY_ROTATE, ConfigPanel.ICON_SIZE);
    }

    @Override
    public void save() {
    }

    @Override
    public void updateContents() {
        delayer.resetAndStart();
    }

    @Override
    protected void onShow() {
        super.onShow();
        ProxyController.getInstance().getEventSender().addListener(this);
    }

    @Override
    protected void onHide() {
        super.onHide();
        ProxyController.getInstance().getEventSender().removeListener(this);
    }

    public void onEvent(ProxyEvent<AbstractProxySelectorImpl> event) {
        delayer.resetAndStart();
    }
}
