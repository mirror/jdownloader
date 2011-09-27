package jd.gui.swing.jdgui.views.settings.panels.downloadandnetwork;

import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;

import jd.controlling.IOEQ;
import jd.controlling.proxy.ProxyController;
import jd.controlling.proxy.ProxyEvent;
import jd.controlling.proxy.ProxyInfo;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;

import org.appwork.app.gui.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.exttable.utils.MinimumSelectionObserver;
import org.appwork.utils.event.DefaultEventListener;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;

public class ProxyConfig extends AbstractConfigPanel implements DefaultEventListener<ProxyEvent<ProxyInfo>> {

    public String getTitle() {
        return _JDT._.gui_settings_proxy_title();
    }

    private static final long serialVersionUID = -521958649780869375L;

    private ProxyTable        table;

    private ExtButton         btnAdd;

    private ExtButton         btnRemove;

    public ProxyConfig() {
        super();

        this.addHeader(getTitle(), NewTheme.I().getIcon("proxy_rotate", 32));
        this.addDescriptionPlain(_JDT._.gui_settings_proxy_description());

        table = new ProxyTable();

        JScrollPane sp = new JScrollPane(table);
        this.add(sp, "gapleft 37,growx, pushx,spanx,pushy,growy");
        MigPanel toolbar = new MigPanel("ins 0", "[][][grow,fill]", "[]");
        btnAdd = new ExtButton(new ProxyAddAction(table));

        ProxyDeleteAction dl;
        btnRemove = new ExtButton(dl = new ProxyDeleteAction(table));
        table.getSelectionModel().addListSelectionListener(new MinimumSelectionObserver(table, dl, 1) {
            @Override
            public void valueChanged(final ListSelectionEvent e) {
                boolean canremove = false;
                ArrayList<ProxyInfo> selected = ProxyConfig.this.table.getExtTableModel().getSelectedObjects();
                if (selected != null) {
                    for (ProxyInfo pi : selected) {
                        if (pi.getProxy().isRemote()) {
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

        add(toolbar, "gapleft 37,growx,spanx");

    }

    @Override
    public ImageIcon getIcon() {
        return NewTheme.I().getIcon("proxy", ConfigPanel.ICON_SIZE);
    }

    @Override
    public void save() {
    }

    @Override
    public void updateContents() {
        IOEQ.add(new Runnable() {

            public void run() {
                table.getExtTableModel()._fireTableStructureChanged(ProxyController.getInstance().getList(), false);
            }

        }, true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jdownloader.gui.settings.AbstractConfigPanel#onShow()
     */
    @Override
    protected void onShow() {
        super.onShow();
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
        ProxyController.getInstance().getEventSender().removeListener(this);
    }

    public void onEvent(ProxyEvent<ProxyInfo> event) {
        switch (event.getType()) {
        case REFRESH:
            new EDTRunner() {
                @Override
                protected void runInEDT() {
                    table.repaint();
                };
            };
            break;
        default:
            updateContents();
            break;
        }
    }

}
