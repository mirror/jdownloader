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

package jd.plugins.optional.neembuu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.event.ControlEvent;
import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.interfaces.SwitchPanelEvent;
import jd.gui.swing.jdgui.interfaces.SwitchPanelListener;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.utils.locale.JDL;

@OptionalPlugin(rev = "$Revision: 11760 $", id = "neembuu", hasGui = true, interfaceversion = 5)
public class Neembuu extends PluginOptional {

    private MenuAction activateAction;

    private NeembuuTab tab;

    public Neembuu(PluginWrapper wrapper) {
        super(wrapper);

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Enable/disable GUI Tab
        if (e.getSource() == activateAction) {
            setGuiEnable(activateAction.isSelected());
        }
    }

    @Override
    public void onControlEvent(ControlEvent event) {
        // receiver all control events. reconnects,
        // downloadstarts/end,pluginstart/pluginend
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        // add main menu items.. this item is used to show/hide GUi
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();
        MenuAction m;

        menu.add(m = activateAction);
        if (tab == null || !tab.isVisible()) {
            m.setSelected(false);
        } else {
            m.setSelected(true);
        }
        return menu;
    }

    @Override
    public boolean initAddon() {
        // this method is called ones after the addon has been loaded

        activateAction = new MenuAction("chat", 0);
        activateAction.setActionListener(this);
        activateAction.setIcon(this.getIconKey());
        activateAction.setSelected(false);

        return true;
    }

    private void initConfigEntries() {

        // create a browsefile setting entry
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BROWSEFILE, getPluginConfig(), "MOUNTLOCATION", "Mountlocation"));
        // combobox entry.

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), "MODE", new String[] { "No restrictions", "limit download to required speed", "..." }, JDL.L("plugins.jdchat.userlistposition", "Download AI Mode:")).setDefaultValue(0));
    }

    @SuppressWarnings("unchecked")
    private void initGUI() {

        tab = new NeembuuTab();
        tab.getBroadcaster().addListener(new SwitchPanelListener() {

            @Override
            public void onPanelEvent(SwitchPanelEvent event) {
                if (event.getID() == SwitchPanelEvent.ON_REMOVE) {
                    setGuiEnable(false);
                }
            }

        });

    }

    @Override
    public void onExit() {
        // addon disabled/tabe closed
    }

    @Override
    public void setGuiEnable(boolean b) {
        if (b) {

            if (tab == null) {
                initGUI();

            }
            SwingGui.getInstance().setContent(tab);

        } else {

            if (tab != null) {
                SwingGui.getInstance().disposeView(tab);
                this.stopAddon();
                tab = null;
            }

        }
        if (activateAction != null && activateAction.isSelected() != b) activateAction.setSelected(b);
    }

    @Override
    public String getIconKey() {
        // should use an own icon later
        return "gui.images.chat";
    }

}
