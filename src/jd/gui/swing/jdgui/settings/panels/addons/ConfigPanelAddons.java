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

package jd.gui.swing.jdgui.settings.panels.addons;

import java.util.ArrayList;
import java.util.Collections;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;

import jd.OptionalPluginWrapper;
import jd.gui.swing.components.table.JDTable;
import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.jdgui.settings.ConfigPanel;
import jd.gui.swing.jdgui.settings.panels.addons.columns.ActivateColumn;
import jd.gui.swing.jdgui.settings.panels.addons.columns.AuthorColumn;
import jd.gui.swing.jdgui.settings.panels.addons.columns.NeedsColumn;
import jd.gui.swing.jdgui.settings.panels.addons.columns.PluginColumn;
import jd.gui.swing.jdgui.settings.panels.addons.columns.VersionColumn;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

/**
 * @author JD-Team
 * 
 */
public class ConfigPanelAddons extends ConfigPanel {
    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.settings.panels.ConfigPanelAddons.";

    @Override
    public String getBreadcrum() {
        return JDL.L(this.getClass().getName() + ".breadcrum", this.getClass().getSimpleName());
    }

    public static String getTitle() {
        return JDL.L(JDL_PREFIX + "addons.title", "Extensions");
    }

    private class InternalTableModel extends JDTableModel {

        private static final long serialVersionUID = 5847076032639053531L;

        public InternalTableModel() {
            super("hostertable");

            list.clear();
            list.addAll(pluginsOptional);
        }

        @Override
        protected void initColumns() {
            this.addColumn(new ActivateColumn(JDL.L("gui.column_status", "Aktivieren"), this));
            this.addColumn(new PluginColumn(JDL.L("gui.column_plugin", "Plugin"), this));
            this.addColumn(new VersionColumn(JDL.L("gui.column_version", "Version"), this));
            this.addColumn(new AuthorColumn(JDL.L("gui.column_coder", "Ersteller"), this));
            this.addColumn(new NeedsColumn(JDL.L("gui.column_needs", "Needs"), this));
        }

        @Override
        public void refreshModel() {
        }

    }

    private static final long serialVersionUID = 4145243293360008779L;

    private ArrayList<OptionalPluginWrapper> pluginsOptional;

    private JDTable table;

    public ConfigPanelAddons() {
        super();
        pluginsOptional = new ArrayList<OptionalPluginWrapper>(OptionalPluginWrapper.getOptionalWrapper());
        Collections.sort(pluginsOptional);
        initPanel();
        load();
    }

    @Override
    public void initPanel() {
        table = new JDTable(new InternalTableModel());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setMaxWidth(80);

        panel.setLayout(new MigLayout("ins 5,wrap 1", "[fill,grow]", "[fill,grow]"));
        panel.add(new JScrollPane(table));

        JTabbedPane tabbed = new JTabbedPane();
        tabbed.setOpaque(false);
        tabbed.add(getBreadcrum(), panel);

        this.add(tabbed);
    }

}