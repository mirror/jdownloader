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

import net.miginfocom.swing.MigLayout;

import org.jdownloader.extensions.AbstractConfigPanel;
import org.jdownloader.images.Theme;
import org.jdownloader.translate.JDT;

public class Logins extends AbstractConfigPanel {

    private SettingsTabbedPane tabbed;

    public String getTitle() {
        return JDT._.gui_settings_logins_title();
    }

    public Logins() {
        super();
        setLayout(new MigLayout("ins 0, wrap 1", "[grow,fill]", "[grow,fill]"));
        tabbed = new SettingsTabbedPane();
        add(tabbed);
        tabbed.addTab(new PremiumTab(), Theme.getIcon("settings/premium", 20), JDT._.gui_settings_logins_premium());
        tabbed.addTab(new PremiumTab(), Theme.getIcon("settings/users", 20), JDT._.gui_settings_logins_htaccess());
        tabbed.addTab(new PremiumTab(), Theme.getIcon("settings/users_ftp", 20), JDT._.gui_settings_logins_ftp());
        // add(new PremiumTab());

    }

    @Override
    public ImageIcon getIcon() {
        return Theme.getIcon("settings/logins", 32);
    }

    @Override
    protected void onShow() {
    }

    @Override
    protected void onHide() {
    }
}