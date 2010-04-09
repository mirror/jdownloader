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

package jd.gui.swing.jdgui.views.settings.panels.hoster.columns;

import javax.swing.Icon;

import jd.HostPluginWrapper;
import jd.gui.swing.components.table.JDIconColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.utils.JDTheme;

public class PremiumColumn extends JDIconColumn {

    private static final long serialVersionUID = 7674821108904765680L;

    private final Icon icon;

    public PremiumColumn(String name, JDTableModel table) {
        super(name, table);

        icon = JDTheme.II("gui.images.premium", 16, 16);
    }

    @Override
    protected Icon getIcon(Object value) {
        return ((HostPluginWrapper) value).isLoaded() && ((HostPluginWrapper) value).isPremiumEnabled() ? icon : null;
    }

}
