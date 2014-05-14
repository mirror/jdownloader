package jd.gui.swing.jdgui.views.settings.panels.proxy;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;

import jd.controlling.TaskQueue;
import jd.controlling.proxy.AbstractProxySelectorImpl;
import jd.controlling.proxy.AbstractProxySelectorImpl.Type;
import jd.controlling.proxy.ProxyController;
import jd.controlling.proxy.ProxyEvent;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.exttable.utils.MinimumSelectionObserver;
import org.appwork.utils.event.DefaultEventListener;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;

public class ProxyConfig extends AbstractConfigPanel implements DefaultEventListener<ProxyEvent<AbstractProxySelectorImpl>> {

    public String getTitle() {
        return _JDT._.gui_settings_proxy_title();
    }

    private static final long  serialVersionUID = -521958649780869375L;

    private ProxyTable         table;

    private ExtButton          btnAdd;

    private ExtButton          btnRemove;
    private ExtButton          btnAuto;

    private ScheduledFuture<?> timer            = null;

    private ExtButton          btImport;

    private ExtButton          btExport;

    private ExtButton          im;

    private ExtButton          expPopup;

    private ExtButton          impPopup;

    public ProxyConfig() {
        super();

        this.addHeader(getTitle(), NewTheme.I().getIcon("proxy_rotate", 32));
        this.addDescriptionPlain(_JDT._.gui_settings_proxy_description());
        this.addDescriptionPlain(_JDT._.gui_settings_proxy_description_new());
        table = new ProxyTable();

        JScrollPane sp = new JScrollPane(table);
        this.add(sp, "gapleft 37,growx, pushx,spanx,pushy,growy");
        MigPanel toolbar = new MigPanel("ins 0", "[][][][grow,fill][]0[][]0[]", "[]");
        toolbar.setOpaque(false);
        btnAdd = new ExtButton(new ProxyAddAction(table));
        btnAuto = new ExtButton(new ProxyAutoAction());
        ProxyDeleteAction dl;
        btnRemove = new ExtButton(dl = new ProxyDeleteAction(table));
        btImport = new ExtButton(new ImportPlainTextAction(table));
        btExport = new ExtButton(new ExportPlainTextAction(table));

        impPopup = new ExtButton(new ImportPopupAction(btImport, table)) {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            public void setBounds(int x, int y, int width, int height) {
                super.setBounds(x - 2, y, width + 2, height);
            }
        };
        expPopup = new ExtButton(new ExportPopupAction(btExport, table)) {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            public void setBounds(int x, int y, int width, int height) {
                super.setBounds(x - 2, y, width + 2, height);
            }
        };
        // tb.add(, "height 26!,sg 2");
        //
        // tb.add(, "height 26!,sg 2");
        table.getSelectionModel().addListSelectionListener(new MinimumSelectionObserver(table, dl, 1) {
            @Override
            public void valueChanged(final ListSelectionEvent e) {
                boolean canremove = false;
                java.util.List<AbstractProxySelectorImpl> selected = ProxyConfig.this.table.getModel().getSelectedObjects();
                if (selected != null) {
                    for (AbstractProxySelectorImpl pi : selected) {
                        if (pi.getType() == Type.NONE)
                            continue;
                        canremove = true;
                        break;

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

    }

    @Override
    public ImageIcon getIcon() {
        return NewTheme.I().getIcon("proxy_rotate", ConfigPanel.ICON_SIZE);
    }

    @Override
    public void save() {
    }

    @Override
    public void updateContents() {
        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                final List<AbstractProxySelectorImpl> list = ProxyController.getInstance().getList();
                new EDTRunner() {
                    @Override
                    protected void runInEDT() {

                        table.getModel()._fireTableStructureChanged(list, false);

                        table.repaint();
                    }
                };
                return null;
            }
        });

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jdownloader.gui.settings.AbstractConfigPanel#onShow()
     */
    @Override
    protected void onShow() {
        super.onShow();
        ScheduledFuture<?> ltimer = timer;
        if (ltimer != null) {
            ltimer.cancel(false);
        }
        ltimer = TaskQueue.TIMINGQUEUE.scheduleWithFixedDelay(new Runnable() {

            public void run() {
                table.repaint();
            }

        }, 250, 1000, TimeUnit.MILLISECONDS);
        timer = ltimer;
        ProxyController.getInstance().getEventSender().addListener(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jdownloader.gui.settings.AbstractConfigPanel#onHide()
     */
    @Override
    protected void onHide() {
        super.onHide();
        ScheduledFuture<?> ltimer = timer;
        timer = null;
        if (ltimer != null) {
            ltimer.cancel(false);
        }
        ProxyController.getInstance().getEventSender().removeListener(this);
    }

    public void onEvent(ProxyEvent<AbstractProxySelectorImpl> event) {
        final List<AbstractProxySelectorImpl> list = ProxyController.getInstance().getList();
        switch (event.getType()) {
        case REFRESH:
            new EDTRunner() {
                @Override
                protected void runInEDT() {
                    table.getModel()._fireTableStructureChanged(list, false);

                    table.repaint();
                };
            };
            break;
        default:
            new EDTRunner() {
                @Override
                protected void runInEDT() {
                    table.getModel()._fireTableStructureChanged(list, false);

                    table.repaint();
                };
            };
            break;
        }
    }

}
