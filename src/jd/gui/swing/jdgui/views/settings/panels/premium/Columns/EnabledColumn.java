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

import java.util.ArrayList;

import jd.controlling.AccountController;
import jd.gui.swing.components.table.JDCheckBoxTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.jdgui.views.settings.panels.premium.HostAccounts;
import jd.plugins.Account;

public class EnabledColumn extends JDCheckBoxTableColumn {

    private static final long serialVersionUID = -1043261559739746995L;

    public EnabledColumn(String name, JDTableModel table) {
        super(name, table);
    }

    @Override
    public boolean isEditable(Object obj) {
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
    public boolean isEnabled(Object obj) {
        return true;
    }

    @Override
    protected boolean getBooleanValue(Object value) {
        if (value instanceof Account) {
            return ((Account) value).isEnabled();
        } else {
            return ((HostAccounts) value).isEnabled();
        }
    }

    @Override
    protected void setBooleanValue(boolean value, Object object) {
        if (object instanceof Account) {
            ((Account) object).setEnabled(value);
        } else if (object instanceof HostAccounts) {
            ArrayList<Account> accs = AccountController.getInstance().getAllAccounts(((HostAccounts) object).getHost());
            if (accs == null) return;
            for (Account acc : accs) {
                acc.setEnabled(value);
            }
        }
    }

}
