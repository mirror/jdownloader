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
import jd.utils.locale.JDL;

public class PremiumJTableModel extends JDTableModel {

    private static final long serialVersionUID = 896539023856191287L;

    private ArrayList<HostPluginWrapper> plugins;
    private TreeMap<String, HostAccounts> hosts;

    public static final String IDENT_PREFIX = "jd.gui.swing.jdgui.settings.panels.premium.PremiumJTableModel.";

    public PremiumJTableModel(String configname) {
        super(configname);
        plugins = HostPluginWrapper.getHostWrapper();
        hosts = new TreeMap<String, HostAccounts>();
    }

    protected void initColumns() {
        this.addColumn(new HosterColumn(JDL.L(IDENT_PREFIX + "hoster", "Hoster"), this));
        this.addColumn(new EnabledColumn(JDL.L(IDENT_PREFIX + "enabled", "Enabled"), this));
        this.addColumn(new UserColumn(JDL.L(IDENT_PREFIX + "user", "User"), this));
        this.addColumn(new PassColumn(JDL.L(IDENT_PREFIX + "pass", "Password"), this));
        this.addColumn(new StatusColumn(JDL.L(IDENT_PREFIX + "status", "Status"), this));
        this.addColumn(new ExpireDateColumn(JDL.L(IDENT_PREFIX + "expiredate", "ExpireDate"), this));
        this.addColumn(new TrafficLeftColumn(JDL.L(IDENT_PREFIX + "trafficleft", "Trafficleft"), this));
        this.addColumn(new TrafficShareColumn(JDL.L(IDENT_PREFIX + "trafficshare", "TrafficShare"), this));
        this.addColumn(new PremiumPointsColumn(JDL.L(IDENT_PREFIX + "premiumpoints", "PremiumPoints"), this));
        this.addColumn(new FilesNumColumn(JDL.L(IDENT_PREFIX + "filesnum", "Number of Files"), this));
        this.addColumn(new CashColumn(JDL.L(IDENT_PREFIX + "cash", "Cash"), this));
        this.addColumn(new UsedSpaceColumn(JDL.L(IDENT_PREFIX + "usedspace", "Used Space"), this));
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
                ha.hasAccountInfos(false);
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
