package jd.gui.swing.jdgui.views.settings.panels.downloadandnetwork;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.controlling.proxy.ProxyInfo;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.table.ExtTable;
import org.appwork.utils.swing.table.SelectionHighlighter;

public class ProxyConfig extends ConfigPanel implements ActionListener {

    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.settings.panels.downloadandnetwork.proxyconfig.";

    public static String getTitle() {
        return JDL.L(JDL_PREFIX + "proxy.title", "ProxySettings");
    }

    public static String getIconKey() {
        return "gui.images.proxy";
    }

    private static final long   serialVersionUID = -521958649780869375L;

    private ExtTable<ProxyInfo> table;

    private ProxyTableModel     tablemodel;

    public ProxyConfig() {
        super();
        init(false);
    }

    public void actionPerformed(ActionEvent e) {
    }

    @Override
    protected ConfigContainer setupContainer() {
        table = new ExtTable<ProxyInfo>(tablemodel = new ProxyTableModel(), "proxyTable") {
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
        JPanel p = new JPanel(new MigLayout("ins 0,wrap 1", "[fill,grow]", "[fill,grow][]"));
        p.add(new JScrollPane(table));
        ConfigContainer container = new ConfigContainer();

        container.setGroup(new ConfigGroup(getTitle(), getIconKey()));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMPONENT, p, "growy, pushy"));

        return container;
    }

}
