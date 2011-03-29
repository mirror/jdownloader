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

package jd.plugins.optional.jdpremserv;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import jd.Main;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.JDController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.interfaces.SwitchPanelEvent;
import jd.gui.swing.jdgui.interfaces.SwitchPanelListener;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.AddonPanel;
import jd.plugins.optional.ExtensionConfigPanel;
import jd.plugins.optional.PluginOptional;
import jd.plugins.optional.StartException;
import jd.plugins.optional.StopException;
import jd.plugins.optional.jdpremserv.gui.JDPremServGui;

import org.appwork.shutdown.ShutdownVetoException;

public class PremServExtension extends PluginOptional implements ActionListener, ControlListener {

    private MenuAction    activateAction;

    private JDPremServGui tab;

    public ExtensionConfigPanel getConfigPanel() {
        return null;
    }

    public boolean hasConfigPanel() {
        return false;
    }

    public PremServExtension() throws StartException {
        super(null);

    }

    public void actionPerformed(ActionEvent e) {
        // Enable/disable GUI Tab
        if (e.getSource() == activateAction) {
            setGuiEnable(activateAction.isSelected());
        }
    }

    public void controlEvent(ControlEvent event) {
        switch (event.getEventID()) {
        case ControlEvent.CONTROL_INIT_COMPLETE:
            startServer();
        }
    }

    private void startServer() {
        try {
            JDPremServServer.getInstance().start(getPluginConfig().getIntegerProperty("PORT", 8080));
        } catch (Exception e) {
            logger.severe("Could not start JDPremServer: " + e.getMessage());
        }
    }

    private void stopServer() {
        try {
            JDPremServServer.getInstance().stop();
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
    }

    private void initGUI() {

        tab = new JDPremServGui();
        tab.getBroadcaster().addListener(new SwitchPanelListener() {

            @Override
            public void onPanelEvent(SwitchPanelEvent event) {
                if (event.getEventID() == SwitchPanelEvent.ON_REMOVE) {
                    setGuiEnable(false);
                }
            }

        });

    }

    public void setGuiEnable(boolean b) {
        if (b) {
            if (tab == null) {
                initGUI();
            }
            SwingGui.getInstance().setContent(tab);
        } else {
            if (tab != null) {
                SwingGui.getInstance().disposeView(tab);
                try {
                    stop();
                } catch (StopException e) {
                    e.printStackTrace();
                }
                tab = null;
            }
        }
        if (activateAction != null && activateAction.isSelected() != b) activateAction.setSelected(b);
    }

    @Override
    public String getIconKey() {
        return "gui.images.chat";
    }

    public void onShutdown() {
    }

    public boolean onShutdownRequest() throws ShutdownVetoException {
        return false;
    }

    public void onShutdownVeto(ArrayList<ShutdownVetoException> vetos) {
    }

    @Override
    protected void stop() throws StopException {
        stopServer();
        JDController.getInstance().removeControlListener(this);
    }

    @Override
    protected void start() throws StartException {
        // this method is called ones after the addon has been loaded

        activateAction = new MenuAction("PremServ", getIconKey());
        activateAction.setActionListener(this);
        activateAction.setSelected(false);

        if (Main.isInitComplete()) startServer();

        JDController.getInstance().addControlListener(this);
    }

    @Override
    protected void initSettings(ConfigContainer config) {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), "PORT", "Server", 1024, 65535, 1).setDefaultValue(8080));

    }

    @Override
    public String getConfigID() {
        return "jdpremserv";
    }

    @Override
    public String getAuthor() {
        return "AppWork";
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public AddonPanel getGUI() {
        return null;
    }

    @Override
    public ArrayList<MenuAction> getMenuAction() {
        // add main menu items.. this item is used to show/hide GUi
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();

        menu.add(activateAction);

        return menu;
    }

    @Override
    protected void initExtension() throws StartException {
    }

}
