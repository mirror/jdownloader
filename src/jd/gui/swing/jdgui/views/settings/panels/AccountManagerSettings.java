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

package jd.gui.swing.jdgui.views.settings.panels;

import javax.swing.ImageIcon;

import jd.gui.swing.jdgui.views.settings.panels.accountmanager.AccountManager;

import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.images.Theme;
import org.jdownloader.translate.JDT;

public class AccountManagerSettings extends AbstractConfigPanel {

    private static final long serialVersionUID = -7963763730328793139L;

    public String getTitle() {
        return JDT._.gui_settings_premium_title();
    }

    public AccountManagerSettings() {
        super();
        this.addHeader(getTitle(), Theme.getIcon("settings/premium", 32));
        this.addDescription(JDT._.gui_settings_premium_description());

        add(AccountManager.getInstance());
    }

    @Override
    public ImageIcon getIcon() {
        return Theme.getIcon("settings/premium", 32);
    }

    @Override
    public void save() {
    }

    @Override
    public void updateContents() {
    }
}