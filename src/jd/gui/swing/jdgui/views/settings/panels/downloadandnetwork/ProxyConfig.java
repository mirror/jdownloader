package jd.gui.swing.jdgui.views.settings.panels.downloadandnetwork;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;

import jd.controlling.proxy.ProxyInfo;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

import org.appwork.utils.images.IconIO;
import org.appwork.utils.swing.table.ExtTable;
import org.appwork.utils.swing.table.SelectionHighlighter;
import org.jdownloader.extensions.AbstractConfigPanel;
import org.jdownloader.translate.JDT;

public class ProxyConfig extends AbstractConfigPanel {

    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.settings.panels.downloadandnetwork.proxyconfig.";

    public String getTitle() {
        return JDL.L(JDL_PREFIX + "proxy.title", "ProxySettings");
    }

    private static final long   serialVersionUID = -521958649780869375L;

    private ExtTable<ProxyInfo> table;

    private JButton             btnAdd;

    private JButton             btnRemove;

    public ProxyConfig() {
        super();

        this.addHeader(getTitle(), "gui.images.proxy");
        this.addDescription(JDT._.gui_settings_proxy_description());

        table = new ExtTable<ProxyInfo>(new ProxyTableModel(), "proxyTable") {
            /**
             * 
             */
            private static final long serialVersionUID = 7638505074419527640L;

            @Override
            protected boolean onShortcutDelete(final ArrayList<ProxyInfo> selectedObjects, final KeyEvent evt, final boolean direct) {
                new ProxyDeleteAction(selectedObjects).actionPerformed(null);
                return true;
            }
        };
        table.setSearchEnabled(true);
        table.addRowHighlighter(new SelectionHighlighter(null, new Color(200, 200, 200, 80)));
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.addMouseListener(new ProxyContextMenu(table));
        JScrollPane sp = new JScrollPane(table);
        this.add(sp, "gapleft 37,growx, pushx,spanx,pushy,growy");
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        btnAdd = new JButton(JDT._.basics_add(), IconIO.getImageIcon(ProxyConfig.class.getResource("/org/jdownloader/img/add.png"), 20));
        btnRemove = new JButton(JDT._.basics_remove(), IconIO.getImageIcon(ProxyConfig.class.getResource("/org/jdownloader/img/remove.png"), 20));
        btnAdd.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                new ProxyAddAction().actionPerformed(e);
            }
        });
        btnRemove.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                new ProxyDeleteAction(table.getExtTableModel().getSelectedObjects()).actionPerformed(e);
            }
        });
        toolbar.add(btnAdd);
        toolbar.add(btnRemove);

        add(toolbar, "gapleft 37,growx,spanx");

    }

    @Override
    protected void onShow() {
    }

    @Override
    protected void onHide() {
    }

    @Override
    public ImageIcon getIcon() {
        return JDTheme.II("gui.images.proxy", ConfigPanel.ICON_SIZE, ConfigPanel.ICON_SIZE);
    }

}
