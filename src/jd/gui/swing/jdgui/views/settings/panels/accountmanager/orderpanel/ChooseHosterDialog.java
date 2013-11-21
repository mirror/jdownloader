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

package jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel;

import javax.swing.Icon;

import jd.gui.UserIO;

import org.appwork.utils.locale._AWU;
import org.appwork.utils.swing.dialog.SearchComboBoxDialog;
import org.jdownloader.DomainInfo;
import org.jdownloader.gui.translate._GUI;

public class ChooseHosterDialog extends SearchComboBoxDialog<DomainInfo> {

    public ChooseHosterDialog(final String string, DomainInfo[] domainInfos) {

        super(UserIO.NO_ICON, _GUI._.NewRuleAction_actionPerformed_choose_hoster_(), string, domainInfos, null, null, _AWU.T.lit_continue(), null);

    }

    @Override
    protected Icon getIconByValue(DomainInfo value) {
        return value.getFavIcon();
    }

    @Override
    protected String getStringByValue(DomainInfo value) {
        return value.getTld();
    }

}