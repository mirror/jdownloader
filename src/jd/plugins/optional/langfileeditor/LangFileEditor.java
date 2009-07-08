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

package jd.plugins.optional.langfileeditor;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.MenuItem;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.SingletonPanel;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;

@OptionalPlugin(rev = "$Revision$", id = "langfileditor", interfaceversion = 4)
/**
 * Editor for jDownloader language files. Gets JDLocale.L() and JDLocale.LF()
 * entries from source and compares them to the keypairs in the language file.
 * 
 * @author eXecuTe
 * @author Greeny
 */
public class LangFileEditor extends PluginOptional {

    private final SingletonPanel lfe;

    public LangFileEditor(PluginWrapper wrapper) {
        super(wrapper);
        lfe = new SingletonPanel(LFEGui.class, this.getPluginConfig());
        initConfigEntries();
    }

    private void initConfigEntries() {
        ConfigEntry cfg;

        ConfigEntry cond;
        config.addEntry(cond = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), LFEGui.PROPERTY_SVN_ACCESS_ANONYMOUS, "Do not upload(SVN) changes on save").setDefaultValue(true));

        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), LFEGui.PROPERTY_SVN_ACCESS_USER, "Upload(SVN) Username"));
      
        cfg.setEnabledCondidtion(cond, "==", false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, getPluginConfig(), LFEGui.PROPERTY_SVN_ACCESS_PASS, "Upload (SVN) Password"));
       
        cfg.setEnabledCondidtion(cond, "==", false);

    }

    // @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof MenuItem && ((MenuItem) e.getSource()).getActionID() == 0) {
            SimpleGUI.CURRENTGUI.getContentPane().display(lfe.getPanel());
        }
    }

    // @Override
    public boolean initAddon() {
        return true;
    }

    // @Override
    public void onExit() {
    }

    // @Override
    public ArrayList<MenuItem> createMenuitems() {
        ArrayList<MenuItem> menu = new ArrayList<MenuItem>();

        menu.add(new MenuItem(MenuItem.NORMAL, "Show", 0).setActionListener(this));

        return menu;
    }

    // @Override
    public String getCoder() {
        return "Greeny";
    }

    // @Override
    public String getIconKey() {
        return "gui.splash.languages";
    }

}