//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.gui.skins.simple.config;

import java.awt.BorderLayout;

import jd.config.CFGConfig;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.interaction.PackageManager;
import jd.gui.UIInterface;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * @author JD-Team
 * 
 */
public class ConfigPanelAddons extends ConfigPanel {

    private static final long serialVersionUID = 4145243293360008779L;

    private ConfigEntriesPanel cep;

    private SubConfiguration config;

    private ConfigContainer container;

    private SubPanelPluginsOptional sppo;

    private SubPanelRessources spr;

    public ConfigPanelAddons(Configuration configuration, UIInterface uiinterface) {
        super(uiinterface);
        initPanel();
        load();
    }

    public String getName() {
        return JDLocale.L("gui.config.addons.name", "Netzwerk/Download");
    }

    public void initPanel() {
        setupContainer();
        setLayout(new BorderLayout());

        add(cep = new ConfigEntriesPanel(container), BorderLayout.CENTER);
        cep.getSubPanels().get(0).add(sppo = new SubPanelPluginsOptional(JDUtilities.getConfiguration(), JDUtilities.getGUI()));
        cep.getSubPanels().get(1).add(spr = new SubPanelRessources(JDUtilities.getConfiguration(), JDUtilities.getGUI()));
    }

    public void load() {
        loadConfigEntries();
    }

    public void save() {
        cep.save();
        sppo.save();
        spr.save();
        config.save();
        CFGConfig.getConfig("JDU").save();
        new PackageManager().interact(this);
    }

    public void setupContainer() {
        config = JDUtilities.getSubConfig("DOWNLOAD");
        container = new ConfigContainer(this);
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_CONTAINER, new ConfigContainer(this, JDLocale.L("gui.config.addons.settings.tab", "Settings"))));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_CONTAINER, new ConfigContainer(this, JDLocale.L("gui.config.addons.install.tab", "Installation & updates"))));
    }

}
