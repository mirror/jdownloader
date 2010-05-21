//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.swing.jdgui.views.settings.panels.hoster;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jd.HostPluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.gui.UserIF;
import jd.gui.swing.components.table.JDTable;
import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;
import jd.gui.swing.jdgui.views.settings.panels.hoster.columns.AcceptColumn;
import jd.gui.swing.jdgui.views.settings.panels.hoster.columns.HostColumn;
import jd.gui.swing.jdgui.views.settings.panels.hoster.columns.PremiumColumn;
import jd.gui.swing.jdgui.views.settings.panels.hoster.columns.SettingsColumn;
import jd.gui.swing.jdgui.views.settings.panels.hoster.columns.TosColumn;
import jd.gui.swing.jdgui.views.settings.panels.hoster.columns.UseColumn;
import jd.gui.swing.jdgui.views.settings.panels.hoster.columns.VersionColumn;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class ConfigPanelPluginForHost extends ConfigPanel implements ActionListener, MouseListener {

    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.settings.panels.ConfigPanelPluginForHost.";

    public static String getTitle() {
        return JDL.L(JDL_PREFIX + "host.title", "Hoster & Premium");
    }

    public static String getIconKey() {
        return "gui.images.config.host";
    }

    private class InternalTableModel extends JDTableModel {

        private static final long serialVersionUID = -5584463272737285033L;

        public InternalTableModel() {
            super("hostertable");

            list.clear();
            list.addAll(pluginsForHost);
        }

        @Override
        protected void initColumns() {
            this.addColumn(new HostColumn(JDL.L("gui.column_host", "Host"), this));
            this.addColumn(new VersionColumn(JDL.L("gui.column_version", "Version"), this));
            this.addColumn(new PremiumColumn(JDL.L("gui.column_premium", "Premium"), this));
            this.addColumn(new SettingsColumn(JDL.L("gui.column_settings", "Settings"), this));
            this.addColumn(new AcceptColumn(JDL.L("gui.column_agbchecked", "Accepted"), this));
            this.addColumn(new TosColumn(JDL.L("gui.column_tos", "TOS"), this));
            this.addColumn(new UseColumn(JDL.L("gui.column_useplugin", "Use Plugin"), this));
        }

        @Override
        public void refreshModel() {
        }

    }

    private static final long serialVersionUID = -5219586497809869375L;

    private ArrayList<HostPluginWrapper> pluginsForHost;

    private JDTable table;

    private JButton btnEdit;

    public ConfigPanelPluginForHost() {
        super();

        pluginsForHost = new ArrayList<HostPluginWrapper>(HostPluginWrapper.getHostWrapper());
        Collections.sort(pluginsForHost);

        init();
    }

    public void actionPerformed(ActionEvent e) {
        editEntry(pluginsForHost.get(table.getSelectedRow()));
    }

    private void editEntry(HostPluginWrapper hpw) {
        UserIF.getInstance().requestPanel(UserIF.Panels.CONFIGPANEL, hpw.getPlugin().getConfig());
    }

    @Override
    protected ConfigContainer setupContainer() {
        table = new JDTable(new InternalTableModel());
        table.addMouseListener(this);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (table.getSelectedRow() < 0) return;
                HostPluginWrapper hpw = pluginsForHost.get(table.getSelectedRow());
                btnEdit.setEnabled(hpw.hasConfig());
            }
        });

        btnEdit = new JButton(JDL.L("gui.btn_settings", "Settings"));
        btnEdit.setEnabled(false);
        btnEdit.addActionListener(this);
        btnEdit.setIcon(JDTheme.II("gui.images.config.home", 16, 16));

        JPanel p = new JPanel(new MigLayout("ins 0,wrap 1", "[fill,grow]", "[fill,grow][]"));
        p.add(new JScrollPane(table));
        p.add(btnEdit, "w pref!");

        ConfigContainer container = new ConfigContainer();

        container.setGroup(new ConfigGroup(getTitle(), getIconKey()));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMPONENT, p, "growy, pushy"));

        return container;
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() > 1) {
            HostPluginWrapper hpw = pluginsForHost.get(table.getSelectedRow());
            if (hpw.hasConfig()) {
                editEntry(hpw);
            }
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

}
