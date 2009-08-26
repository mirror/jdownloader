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

package jd.gui.swing.jdgui.settings.panels.passwords;

import javax.swing.JTabbedPane;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.controlling.PasswordListController;
import jd.gui.swing.jdgui.settings.ConfigPanel;
import jd.gui.swing.jdgui.settings.GUIConfigEntry;
import jd.utils.locale.JDL;

public class PasswordList extends ConfigPanel {
    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.settings.panels.PasswordList.";

    public String getBreadcrum() {
        return JDL.L(this.getClass().getName() + ".breadcrum", this.getClass().getSimpleName());
    }

    public static String getTitle() {
        return JDL.L(JDL_PREFIX + "general.title", "Archive passwords");
    }

    private static final long serialVersionUID = 3383448498625377495L;

    public PasswordList(Configuration configuration) {
        super();

        initPanel();
        // load();
    }

    @Override
    public void initPanel() {
        // ConfigEntry conditionEntry;

        // this.passwordConfig = new
        // ConfigContainer(JDL.L("plugins.optional.jdunrar.config.passwordtab",
        // "List of passwords"));
        // config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CONTAINER,
        // passwordConfig));
        //
        // passwordConfig.addEntry();
        //
        //   

        addGUIConfigEntry(new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_LISTCONTROLLED, PasswordListController.getInstance(), JDL.LF("plugins.optional.jdunrar.config.passwordlist2", "List of all passwords. Each line one password. Available passwords: %s", ""))));
        // addGUIConfigEntry(new GUIConfigEntry(new
        // ConfigEntry(ConfigContainer.TYPE_CHECKBOX, CONFIGURATION,
        // Configuration.LOGGER_FILELOG,
        // JDLocale.L("gui.config.general.filelogger",
        // "Erstelle Logdatei im ./logs/ Ordner")).setDefaultValue(false).setGroup(logging)));

        JTabbedPane tabbed = new JTabbedPane();

        tabbed.setOpaque(false);
        tabbed.add(getBreadcrum(), panel);

        this.add(tabbed);
    }

    @Override
    public void load() {
        loadConfigEntries();
    }

    @Override
    public void save() {
        saveConfigEntries();

    }

}
