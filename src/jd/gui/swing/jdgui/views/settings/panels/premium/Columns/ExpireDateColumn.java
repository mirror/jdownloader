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

package jd.gui.swing.jdgui.views.settings.panels.premium.Columns;

import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.components.table.JDTextTableColumn;
import jd.gui.swing.jdgui.views.settings.panels.premium.HostAccounts;
import jd.nutils.Formatter;
import jd.plugins.Account;
import jd.plugins.AccountInfo;

import org.jdownloader.gui.translate._GUI;

public class ExpireDateColumn extends JDTextTableColumn {

    private static final long serialVersionUID = -5291590062503352550L;

    public ExpireDateColumn(String name, JDTableModel table) {
        super(name, table);
    }

    @Override
    public boolean isEnabled(Object obj) {
        if (obj == null) return false;
        if (obj instanceof Account) return ((Account) obj).isEnabled();
        if (obj instanceof HostAccounts) return ((HostAccounts) obj).isEnabled();
        return true;
    }

    @Override
    public boolean isSortable(Object obj) {
        return false;
    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
    }

    @Override
    protected String getStringValue(Object value) {
        if (value instanceof Account) {
            Account ac = (Account) value;
            AccountInfo ai = ac.getAccountInfo();
            if (!ac.isValid()) {
                return _GUI._.jd_gui_swing_jdgui_settings_panels_premium_PremiumTableRenderer_invalidAccount();
            } else if (ai == null) {
                return _GUI._.jd_gui_swing_jdgui_settings_panels_premium_PremiumTableRenderer_unknown();
            } else {
                if (ai.getValidUntil() == -1) {
                    return _GUI._.jd_gui_swing_jdgui_settings_panels_premium_PremiumTableRenderer_unlimited();
                } else if (ai.isExpired()) {
                    return _GUI._.jd_gui_swing_jdgui_settings_panels_premium_PremiumTableRenderer_expired();
                } else {
                    return Formatter.formatTime(ai.getValidUntil());
                }
            }
        }
        return "";
    }

}