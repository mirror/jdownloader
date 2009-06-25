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
import jd.config.MenuItem;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.SingletonPanel;
import jd.plugins.PluginOptional;
import jd.utils.locale.JDL;

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
        lfe = new SingletonPanel(LFEGui.class);
    }

    // @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof MenuItem && ((MenuItem) e.getSource()).getActionID() == 0) {
            SimpleGUI.CURRENTGUI.getContentPane().display(lfe.getPanel());
        }
    }

    // @Override
    public String getRequirements() {
        return "JRE 1.5+";
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

        menu.add(new MenuItem(MenuItem.NORMAL, getHost(), 0).setActionListener(this));

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

    // @Override
    public String getHost() {
        return JDL.L("plugins.optional.langfileeditor.name", "Language File Editor");
    }

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    public static int getAddonInterfaceVersion() {
        return 3;
    }

}