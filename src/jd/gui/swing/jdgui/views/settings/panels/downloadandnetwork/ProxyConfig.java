package jd.gui.swing.jdgui.views.settings.panels.downloadandnetwork;

import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;

import jd.controlling.proxy.ProxyInfo;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;

import org.appwork.app.gui.MigPanel;
import org.appwork.utils.swing.table.utils.MinimumSelectionObserver;
import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;

public class ProxyConfig extends AbstractConfigPanel {

    public String getTitle() {
        return _JDT._.gui_settings_proxy_title();
    }

    private static final long serialVersionUID = -521958649780869375L;

    private ProxyTable        table;

    private JButton           btnAdd;

    private JButton           btnRemove;

    public ProxyConfig() {
        super();

        this.addHeader(getTitle(), NewTheme.I().getIcon("proxy_rotate", 32));
        this.addDescriptionPlain(_JDT._.gui_settings_proxy_description());

        table = new ProxyTable();

        JScrollPane sp = new JScrollPane(table);
        this.add(sp, "gapleft 37,growx, pushx,spanx,pushy,growy");
        MigPanel toolbar = new MigPanel("ins 0", "[][][grow,fill]", "");
        btnAdd = new JButton(new ProxyAddAction(table));
        ProxyDeleteAction dl;
        btnRemove = new JButton(dl = new ProxyDeleteAction(table));
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

        toolbar.add(btnAdd, "sg 1");
        toolbar.add(btnRemove, "sg 1");

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
        table.update();
    }

}
