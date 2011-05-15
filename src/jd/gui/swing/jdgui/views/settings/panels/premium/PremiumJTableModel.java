//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.gui.swing.jdgui.views.settings.panels.premium;

import java.util.ArrayList;
import java.util.TreeMap;

import jd.HostPluginWrapper;
import jd.controlling.AccountController;
import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.jdgui.views.settings.panels.premium.Columns.CashColumn;
import jd.gui.swing.jdgui.views.settings.panels.premium.Columns.EnabledColumn;
import jd.gui.swing.jdgui.views.settings.panels.premium.Columns.ExpireDateColumn;
import jd.gui.swing.jdgui.views.settings.panels.premium.Columns.FilesNumColumn;
import jd.gui.swing.jdgui.views.settings.panels.premium.Columns.HosterColumn;
import jd.gui.swing.jdgui.views.settings.panels.premium.Columns.PassColumn;
import jd.gui.swing.jdgui.views.settings.panels.premium.Columns.PremiumPointsColumn;
import jd.gui.swing.jdgui.views.settings.panels.premium.Columns.StatusColumn;
import jd.gui.swing.jdgui.views.settings.panels.premium.Columns.TrafficLeftColumn;
import jd.gui.swing.jdgui.views.settings.panels.premium.Columns.TrafficShareColumn;
import jd.gui.swing.jdgui.views.settings.panels.premium.Columns.UsedSpaceColumn;
import jd.gui.swing.jdgui.views.settings.panels.premium.Columns.UserColumn;
import jd.plugins.Account;
import jd.plugins.AccountInfo;

import org.jdownloader.gui.translate._GUI;

public class PremiumJTableModel extends JDTableModel {

    private static final long             serialVersionUID = 896539023856191287L;

    private ArrayList<HostPluginWrapper>  plugins;
    private TreeMap<String, HostAccounts> hosts;

    public static final String            IDENT_PREFIX     = "jd.gui.swing.jdgui.settings.panels.premium.PremiumJTableModel.";

    public PremiumJTableModel(String configname) {
        super(configname);
        plugins = HostPluginWrapper.getHostWrapper();
        hosts = new TreeMap<String, HostAccounts>();
    }

    protected void initColumns() {
        this.addColumn(new HosterColumn(_GUI._.jd_gui_swing_jdgui_settings_panels_premium_PremiumJTableModel_hoster(), this));
        this.addColumn(new EnabledColumn(_GUI._.jd_gui_swing_jdgui_settings_panels_premium_PremiumJTableModel_enabled(), this));
        this.addColumn(new UserColumn(_GUI._.jd_gui_swing_jdgui_settings_panels_premium_PremiumJTableModel_user(), this));
        this.addColumn(new PassColumn(_GUI._.jd_gui_swing_jdgui_settings_panels_premium_PremiumJTableModel_pass(), this));
        this.addColumn(new StatusColumn(_GUI._.jd_gui_swing_jdgui_settings_panels_premium_PremiumJTableModel_status(), this));
        this.addColumn(new ExpireDateColumn(_GUI._.jd_gui_swing_jdgui_settings_panels_premium_PremiumJTableModel_expiredate(), this));
        this.addColumn(new TrafficLeftColumn(_GUI._.jd_gui_swing_jdgui_settings_panels_premium_PremiumJTableModel_trafficleft(), this));
        this.addColumn(new TrafficShareColumn(_GUI._.jd_gui_swing_jdgui_settings_panels_premium_PremiumJTableModel_trafficshare(), this));
        this.addColumn(new PremiumPointsColumn(_GUI._.jd_gui_swing_jdgui_settings_panels_premium_PremiumJTableModel_premiumpoints(), this));
        this.addColumn(new FilesNumColumn(_GUI._.jd_gui_swing_jdgui_settings_panels_premium_PremiumJTableModel_filesnum(), this));
        this.addColumn(new CashColumn(_GUI._.jd_gui_swing_jdgui_settings_panels_premium_PremiumJTableModel_cash(), this));
        this.addColumn(new UsedSpaceColumn(_GUI._.jd_gui_swing_jdgui_settings_panels_premium_PremiumJTableModel_usedspace(), this));
    }

    @Override
    public void refreshModel() {
        synchronized (list) {
            list.clear();
            long traffic = 0;
            boolean gotenabled = false;
            for (HostPluginWrapper plugin : plugins) {
                ArrayList<Account> accs = AccountController.getInstance().getAllAccounts(plugin.getHost());
                if (accs.size() == 0) continue;
                HostAccounts ha = hosts.get(plugin.getHost());
                if (ha == null) {
                    ha = new HostAccounts(plugin.getHost());
                    hosts.put(plugin.getHost(), ha);
                }
                list.add(ha);
                traffic = 0;
                gotenabled = false;
                ha.setHasAccountInfos(false);
                for (Account acc : accs) {
                    list.add(acc);
                    if (acc.isEnabled()) gotenabled = true;
                    AccountInfo ai = acc.getAccountInfo();
                    if (ai != null) {
                        ha.hasAccountInfos(true);
                        if (acc.isValid()) {
                            if (ai.isUnlimitedTraffic()) {
                                traffic = -1;
                            } else {
                                if (traffic != -1) traffic += ai.getTrafficLeft();
                            }
                        }
                    }
                }
                ha.setTraffic(traffic);
                ha.setEnabled(gotenabled);
            }
        }
    }

}