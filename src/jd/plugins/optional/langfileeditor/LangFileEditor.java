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
import jd.config.MenuAction;
import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.SingletonPanel;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.utils.locale.JDL;

@OptionalPlugin(rev = "$Revision$", id = "langfileditor", interfaceversion = 4)
/**
 * Editor for jDownloader language files. Gets JDLocale.L() and JDLocale.LF()
 * entries from source and compares them to the keypairs in the language file.
 * 
 * @author eXecuTe
 * @author Greeny
 * @author coalado
 */
public class LangFileEditor extends PluginOptional {

    private final SingletonPanel lfe;
    protected MenuAction activateAction;
    private LFEView lfeView;

    public LangFileEditor(PluginWrapper wrapper) {
        super(wrapper);
        lfe = new SingletonPanel(LFEGui.class, this.getPluginConfig());
        initConfigEntries();
    }

    private void initConfigEntries() {
        ConfigEntry cfg;
        ConfigEntry cond;

        config.addEntry(cond = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), LFEGui.PROPERTY_SVN_ACCESS_ANONYMOUS, "Do not upload (SVN) changes on save").setDefaultValue(true));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), LFEGui.PROPERTY_SVN_ACCESS_USER, "Upload (SVN) Username"));
        cfg.setEnabledCondidtion(cond, "==", false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, getPluginConfig(), LFEGui.PROPERTY_SVN_ACCESS_PASS, "Upload (SVN) Password"));
        cfg.setEnabledCondidtion(cond, "==", false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof MenuAction && ((MenuAction) e.getSource()).getActionID() == 0) {
            if (lfeView == null) lfeView = new LFEView(lfe.getPanel(), this);

            if (((MenuAction) e.getSource()).isSelected()) {

                SwingGui.getInstance().setContent(lfeView);
            } else {
                lfeView.close();
            }

        }
    }

    @Override
    public boolean initAddon() {
        activateAction = new MenuAction(MenuAction.TOGGLE, JDL.L("jd.plugins.optional.langfileeditor.LangFileEditor.show", "Show"), 0).setActionListener(this);
        activateAction.setSelected(false);
        return true;
    }

    @Override
    public void onExit() {
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();

        menu.add(activateAction);

        return menu;
    }

    @Override
    public String getCoder() {
        return "Greeny";
    }

    @Override
    public String getIconKey() {
        return "gui.splash.languages";
    }

}