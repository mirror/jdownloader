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
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.table.ExtTable;
import org.appwork.utils.swing.table.ExtTableModel;
import org.appwork.utils.swing.table.SelectionHighlighter;
import org.appwork.utils.swing.table.columns.ExtCheckColumn;
import org.appwork.utils.swing.table.columns.ExtIconColumn;
import org.appwork.utils.swing.table.columns.ExtLongColumn;
import org.appwork.utils.swing.table.columns.ExtTextColumn;

public class ConfigPanelPluginForHost extends ConfigPanel implements ActionListener, MouseListener {

    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.settings.panels.ConfigPanelPluginForHost.";

    public static String getTitle() {
        return JDL.L(JDL_PREFIX + "host.title", "Hoster & Premium");
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
            this.addColumn(new ExtTextColumn<HostPluginWrapper>(JDL.L("gui.column_host", "Host"), this) {

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
            this.addColumn(new ExtLongColumn<HostPluginWrapper>(JDL.L("gui.column_version", "Version"), this) {

                private static final long serialVersionUID = 8197242738899719303L;

                @Override
                protected long getLong(HostPluginWrapper value) {
                    return value.getVersion();
                }

            });
            this.addColumn(new ExtCheckColumn<HostPluginWrapper>(JDL.L("gui.column_useplugin", "Use Plugin"), this) {

                private static final long serialVersionUID = 4765934516215953012L;

                @Override
                public boolean isEditable(HostPluginWrapper obj) {
                    return true;
                }

                @Override
                protected boolean getBooleanValue(HostPluginWrapper value) {
                    return value.isEnabled();
                }

                @Override
                protected void setBooleanValue(boolean value, HostPluginWrapper object) {
                    object.setEnabled(value);
                    PremiumMenu.getInstance().update();
                }

            });
            this.addColumn(new ExtCheckColumn<HostPluginWrapper>(JDL.L("gui.column_agbchecked", "Accepted"), this) {

                private static final long serialVersionUID = 6843580898685333774L;

                @Override
                public boolean isEditable(HostPluginWrapper obj) {
                    return true;
                }

                @Override
                protected boolean getBooleanValue(HostPluginWrapper value) {
                    return value.isAGBChecked();
                }

                @Override
                protected void setBooleanValue(boolean value, HostPluginWrapper object) {
                    object.setAGBChecked(value);
                }

            });
            this.addColumn(new PremiumColumn(JDL.L("gui.column_premium", "Premium"), this));
            this.addColumn(new ExtIconColumn<HostPluginWrapper>(JDL.L("gui.column_settings", "Settings"), this) {

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
            this.addColumn(new TosColumn(JDL.L("gui.column_tos", "TOS"), this));
        }
    }

    private static final long                  serialVersionUID = -5219586497809869375L;

    private final ArrayList<HostPluginWrapper> pluginsForHost;

    private ExtTable<HostPluginWrapper>        table;

    private InternalTableModel                 tablemodel;

    private JButton                            btnEdit;

    private JCheckBox                          chkUseAll;

    public ConfigPanelPluginForHost() {
        super();

        pluginsForHost = new ArrayList<HostPluginWrapper>(HostPluginWrapper.getHostWrapper());
        Collections.sort(pluginsForHost);

        init();
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

        btnEdit = new JButton(JDL.L("gui.btn_settings", "Settings"));
        btnEdit.setEnabled(false);
        btnEdit.addActionListener(this);
        btnEdit.setIcon(JDTheme.II("gui.images.config.home", 16, 16));

        /*
         * TODO: please someone implement as tableheader instead of checkbox
         */
        chkUseAll = new JCheckBox(JDL.L(JDL_PREFIX + "useAll", "Use all Hosts"));
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
