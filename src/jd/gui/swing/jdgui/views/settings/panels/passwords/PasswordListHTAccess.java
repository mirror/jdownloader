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

package jd.gui.swing.jdgui.views.settings.panels.passwords;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.controlling.HTACCESSController;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;
import jd.utils.locale.JDL;

public class PasswordListHTAccess extends ConfigPanel {

    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.settings.panels.passwords.PasswordListHTAccess.";

    public static String getTitle() {
        return JDL.L(JDL_PREFIX + "general.title", "HTAccess logins");
    }

    public static String getIconKey() {
        return "gui.images.htaccess";
    }

    private static final long serialVersionUID = 3383448498625377495L;

    public PasswordListHTAccess() {
        super();

        init();
    }

    @Override
    protected ConfigContainer setupContainer() {
        ConfigContainer container = new ConfigContainer();

        container.setGroup(new ConfigGroup(getTitle(), getIconKey()));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_LISTCONTROLLED, HTACCESSController.getInstance(), JDL.L("plugins.http.htaccess", "List of all HTAccess passwords. Each line one password.")));

        return container;
    }

}
