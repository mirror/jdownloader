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

package jd.gui.swing.jdgui.views.settings.panels.hoster;

import javax.swing.Icon;

import jd.HostPluginWrapper;
import jd.controlling.AccountController;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

import org.appwork.utils.swing.table.ExtTableModel;
import org.appwork.utils.swing.table.columns.ExtIconColumn;

public class PremiumColumn extends ExtIconColumn<HostPluginWrapper> {

    private static final long       serialVersionUID = 7674821108904765680L;
    private static final String     JDL_PREFIX       = "jd.gui.swing.jdgui.views.settings.panels.hoster.columns.PremiumColumn.";

    private final AccountController controller;

    private final Icon              iconYellow;
    private final Icon              iconGreen;
    private final Icon              iconRed;

    private final String            stringNoAccount;
    private final String            stringValidAccount;
    private final String            stringExpiredAccount;

    public PremiumColumn(String name, ExtTableModel<HostPluginWrapper> table) {
        super(name, table);

        controller = AccountController.getInstance();

        iconYellow = JDTheme.II("gui.images.premium", 16, 16);
        iconGreen = JDTheme.II("gui.images.premium.enabled", 16, 16);
        iconRed = JDTheme.II("gui.images.premium.disabled", 16, 16);

        stringNoAccount = JDL.L(JDL_PREFIX + "noAccount", "You have no accounts for this host!");
        stringValidAccount = JDL.L(JDL_PREFIX + "valid", "You have a valid account for this host.");
        stringExpiredAccount = JDL.L(JDL_PREFIX + "expired", "All accounts for this host are expired!");
    }

    @Override
    public boolean isSortable(HostPluginWrapper obj) {
        return true;
    }

    @Override
    protected Icon getIcon(HostPluginWrapper value) {
        HostPluginWrapper hpw = (value);
        if (!hpw.isPremiumEnabled()) return null;
        if (controller.hasValidAccount(hpw.getHost())) return iconGreen;
        if (!controller.hasAccounts(hpw.getHost())) return iconRed;
        return iconYellow;
    }

    @Override
    protected String getToolTip(HostPluginWrapper value) {
        HostPluginWrapper hpw = (value);
        if (!hpw.isPremiumEnabled()) return null;
        if (controller.hasValidAccount(hpw.getHost())) return stringValidAccount;
        if (!controller.hasAccounts(hpw.getHost())) return stringExpiredAccount;
        return stringNoAccount;
    }

    @Override
    protected int getMaxWidth() {
        return 70;
    }

}
