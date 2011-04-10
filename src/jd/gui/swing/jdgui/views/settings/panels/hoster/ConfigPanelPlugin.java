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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import jd.gui.swing.jdgui.menu.PremiumMenu;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;
import jd.utils.JDTheme;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.table.ExtTable;
import org.appwork.utils.swing.table.ExtTableModel;
import org.appwork.utils.swing.table.SelectionHighlighter;
import org.appwork.utils.swing.table.columns.ExtIconColumn;
import org.appwork.utils.swing.table.columns.ExtLongColumn;
import org.appwork.utils.swing.table.columns.ExtTextColumn;
import org.jdownloader.gui.translate.T;

public class ConfigPanelPlugin extends ConfigPanel implements ActionListener, MouseListener {

    @Override
    public String getTitle() {
        return T._.jd_gui_swing_jdgui_settings_panels_ConfigPanelPlugin_plugins_title();
    }

    @Override
    public Icon getIcon() {
        return JDTheme.II(getIconKey(), ConfigPanel.ICON_SIZE, ConfigPanel.ICON_SIZE);
    }

    public static String getIconKey() {
        return "gui.images.config.host";
    }

    private class InternalTableModel extends ExtTableModel<HostPluginWrapper> {

        private static final long serialVersionUID = -5584463272737285033L;

        public InternalTableModel() {
            super("hosterTable");
            tableData = new ArrayList<HostPluginWrapper>(pluginsForHost);
        }

        @Override
        protected void initColumns() {
            this.addColumn(new ExtTextColumn<HostPluginWrapper>(T._.gui_column_plugin(), this) {

                private static final long serialVersionUID = -7209180150340921804L;

                @Override
                protected String getStringValue(HostPluginWrapper value) {
                    return value.getHost();
                }

                @Override
                protected Icon getIcon(HostPluginWrapper value) {
                    return value.getIcon();
                }

            });
            this.addColumn(new ExtLongColumn<HostPluginWrapper>(T._.gui_column_version(), this) {

                private static final long serialVersionUID = 8197242738899719303L;

                @Override
                protected long getLong(HostPluginWrapper value) {
                    return value.getVersion();
                }

            });
            this.addColumn(new ExtIconColumn<HostPluginWrapper>(T._.gui_column_settings(), this) {

                private static final long serialVersionUID = 4948749148702891718L;

                private final Icon        icon             = JDTheme.II("gui.images.config.home", 16, 16);

                @Override
                public boolean isSortable(HostPluginWrapper obj) {
                    return true;
                }

                @Override
                protected Icon getIcon(HostPluginWrapper value) {
                    return value.hasConfig() ? icon : null;
                }

                @Override
                protected int getMaxWidth() {
                    return 70;
                }

            });
            this.addColumn(new TosColumn(T._.gui_column_tos(), this));
        }
    }

    private static final long                  serialVersionUID = -5219586497809869375L;

    private final ArrayList<HostPluginWrapper> pluginsForHost;

    private ExtTable<HostPluginWrapper>        table;

    private InternalTableModel                 tablemodel;

    private JButton                            btnEdit;

    private JCheckBox                          chkUseAll;

    public ConfigPanelPlugin() {
        super();

        pluginsForHost = new ArrayList<HostPluginWrapper>(HostPluginWrapper.getHostWrapper());
        Collections.sort(pluginsForHost);

        init(false);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == chkUseAll) {
            toggleUseAll();
        } else if (e.getSource() == btnEdit) {
            editEntry(table.getExtTableModel().getSelectedObjects().get(0));
        }
    }

    private void editEntry(HostPluginWrapper hpw) {
        UserIF.getInstance().requestPanel(UserIF.Panels.CONFIGPANEL, hpw.getPlugin().getConfig());
    }

    @Override
    protected ConfigContainer setupContainer() {
        table = new ExtTable<HostPluginWrapper>(tablemodel = new InternalTableModel(), "hosterTable");
        table.setSearchEnabled(true);
        table.addRowHighlighter(new SelectionHighlighter(null, new Color(200, 200, 200, 80)));
        table.addMouseListener(this);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (table.getSelectedRow() < 0) return;
                btnEdit.setEnabled(table.getExtTableModel().getSelectedObjects().get(0).hasConfig());
            }
        });

        btnEdit = new JButton(T._.gui_btn_settings());
        btnEdit.setEnabled(false);
        btnEdit.addActionListener(this);
        btnEdit.setIcon(JDTheme.II("gui.images.config.home", 16, 16));

        /*
         * TODO: please someone implement as tableheader instead of checkbox
         */
        chkUseAll = new JCheckBox(T._.jd_gui_swing_jdgui_settings_panels_ConfigPanelPlugin_useAll());
        chkUseAll.setEnabled(true);
        chkUseAll.addActionListener(this);
        chkUseAll.setHorizontalAlignment(JCheckBox.RIGHT);
        chkUseAll.setHorizontalTextPosition(JCheckBox.LEADING);
        chkUseAll.setSelected(isAllInUse());

        JPanel p = new JPanel(new MigLayout("ins 0,wrap 1", "[fill,grow]", "[fill,grow][]"));
        p.add(new JScrollPane(table));
        p.add(btnEdit, "split 2, w pref!");
        p.add(chkUseAll);

        ConfigContainer container = new ConfigContainer();

        container.setGroup(new ConfigGroup(getTitle(), getIconKey()));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMPONENT, p, "growy, pushy"));

        return container;
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() > 1) {
            HostPluginWrapper hpw = table.getExtTableModel().getSelectedObjects().get(0);
            if (hpw.hasConfig()) {
                editEntry(hpw);
            }
        }
    }

    private void toggleUseAll() {
        boolean checkvalue = !isAllInUse();

        for (HostPluginWrapper plugin : pluginsForHost) {
            plugin.setEnabled(checkvalue);
        }

        PremiumMenu.getInstance().update();
        tablemodel.fireTableStructureChanged();
    }

    private boolean isAllInUse() {
        for (HostPluginWrapper plugin : pluginsForHost) {
            if (!plugin.isEnabled()) { return false; }
        }

        return true;
    }

    private void setUseAllStatus() {
        chkUseAll.setSelected(isAllInUse());
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
        setUseAllStatus();
    }

}