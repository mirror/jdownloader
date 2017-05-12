//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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
package org.jdownloader.extensions.shutdown;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.plugins.AddonPanel;

import org.appwork.controlling.StateEvent;
import org.appwork.controlling.StateEventListener;
import org.appwork.controlling.StateMachine;
import org.appwork.shutdown.BasicShutdownRequest;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.shutdown.ShutdownVetoException;
import org.appwork.utils.Application;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.controlling.contextmenu.ActionData;
import org.jdownloader.controlling.contextmenu.ContextMenuManager;
import org.jdownloader.controlling.contextmenu.MenuContainerRoot;
import org.jdownloader.controlling.contextmenu.MenuExtenderHandler;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.MenuLink;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.shutdown.actions.ShutdownToggleAction;
import org.jdownloader.extensions.shutdown.translate.ShutdownTranslation;
import org.jdownloader.gui.mainmenu.MenuManagerMainmenu;
import org.jdownloader.gui.mainmenu.container.ExtensionsMenuContainer;
import org.jdownloader.gui.toolbar.MenuManagerMainToolbar;

public class ShutdownExtension extends AbstractExtension<ShutdownConfig, ShutdownTranslation> implements StateEventListener, MenuExtenderHandler {
    private ShutdownConfigPanel     configPanel;
    private final ShutdownInterface shutdownInterface;
    private ShutdownThread          thread = null;

    public ShutdownConfigPanel getConfigPanel() {
        return configPanel;
    }

    @Override
    public boolean isHeadlessRunnable() {
        return true;
    }

    public boolean hasConfigPanel() {
        return true;
    }

    public ShutdownExtension() throws StartException {
        setTitle(T.jd_plugins_optional_jdshutdown());
        switch (CrossSystem.getOSFamily()) {
        case WINDOWS:
            shutdownInterface = new WindowsShutdownInterface(this);
            break;
        case MAC:
            shutdownInterface = new MacShutdownInterface(this);
            break;
        case BSD:
        case LINUX:
            shutdownInterface = new UnixShutdownInterface(this);
            break;
        default:
            shutdownInterface = new DefaultShutdownInterface(this);
            break;
        }
    }

    @Override
    public String getIconKey() {
        return "logout";
    }

    @Override
    protected void stop() throws StopException {
        if (shutdownInterface != null) {
            DownloadWatchDog.getInstance().getStateMachine().removeListener(this);
            if (!Application.isHeadless()) {
                MenuManagerMainToolbar.getInstance().unregisterExtender(this);
                MenuManagerMainmenu.getInstance().unregisterExtender(this);
            }
            logger.info("ShutdownExtension:Stopped");
        }
    }

    @Override
    protected void start() throws StartException {
        if (shutdownInterface == null) {
            throw new StartException("Not supported");
        }
        if (!Application.isHeadless()) {
            MenuManagerMainToolbar.getInstance().registerExtender(this);
            MenuManagerMainmenu.getInstance().registerExtender(this);
        }
        if (!getSettings().isShutdownActiveByDefaultEnabled()) {
            CFG_SHUTDOWN.SHUTDOWN_ACTIVE.setValue(false);
        } else {
            CFG_SHUTDOWN.SHUTDOWN_ACTIVE.setValue(true);
        }
        DownloadWatchDog.getInstance().getStateMachine().addListener(this);
        logger.info("ShutdownExtension:Started");
    }

    protected ShutdownInterface getShutdownInterface() {
        return shutdownInterface;
    }

    @Override
    public String getDescription() {
        return T.jd_plugins_optional_jdshutdown_description();
    }

    @Override
    public AddonPanel<ShutdownExtension> getGUI() {
        return null;
    }

    @Override
    protected void initExtension() throws StartException {
        if (!Application.isHeadless()) {
            configPanel = new ShutdownConfigPanel(this);
        }
    }

    public void onStateChange(StateEvent event) {
        if (event.getNewState() == DownloadWatchDog.STOPPED_STATE) {
            final boolean active = getSettings().isShutdownActive();
            if (!active) {
                logger.info("ShutdownExtension is not active!");
            } else {
                logger.info("ShutdownExtension is active!");
                if (DownloadWatchDog.getInstance().getSession().getDownloadsStarted() > 0) {
                    final ShutdownRequest request = ShutdownController.getInstance().collectVetos(new BasicShutdownRequest(true));
                    if (request.hasVetos()) {
                        logger.info("Vetos: " + request.getVetos().size() + " Wait until there is no veto");
                        for (ShutdownVetoException e : request.getVetos()) {
                            logger.log(e);
                            logger.info(e.getSource() + "");
                        }
                        new Thread("Wait to Shutdown") {
                            public void run() {
                                while (true) {
                                    final StateMachine sm = DownloadWatchDog.getInstance().getStateMachine();
                                    if (sm.isState(DownloadWatchDog.PAUSE_STATE, DownloadWatchDog.RUNNING_STATE, DownloadWatchDog.STOPPING_STATE)) {
                                        logger.info("Cancel Shutdown.");
                                        return;
                                    }
                                    final ShutdownRequest request = ShutdownController.getInstance().collectVetos(new BasicShutdownRequest(true));
                                    if (!request.hasVetos()) {
                                        logger.info("No Vetos");
                                        if (sm.isState(DownloadWatchDog.IDLE_STATE, DownloadWatchDog.STOPPED_STATE)) {
                                            startShutdownThread();
                                            return;
                                        }
                                    }
                                    logger.info("Vetos: " + request.getVetos().size() + " Wait until there is no veto");
                                    for (ShutdownVetoException e : request.getVetos()) {
                                        logger.log(e);
                                        logger.info(e.getSource() + "");
                                    }
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e) {
                                        return;
                                    }
                                }
                            }
                        }.start();
                    } else {
                        startShutdownThread();
                    }
                } else {
                    logger.info("No downloads have been started in last session! ByPass ShutdownExtension!");
                }
            }
        }
    }

    protected void startShutdownThread() {
        final Thread thread = this.thread;
        if (thread == null || !thread.isAlive()) {
            final ShutdownThread shutdown = new ShutdownThread(this);
            this.thread = shutdown;
            shutdown.start();
        }
    }

    public void onStateUpdate(StateEvent event) {
    }

    @Override
    public boolean isQuickToggleEnabled() {
        return false;
    }

    @Override
    public MenuItemData updateMenuModel(ContextMenuManager manager, MenuContainerRoot mr) {
        if (manager instanceof MenuManagerMainmenu) {
            final ExtensionsMenuContainer container = new ExtensionsMenuContainer();
            container.add(org.jdownloader.extensions.shutdown.actions.ShutdownToggleAction.class);
            return container;
        } else if (manager instanceof MenuManagerMainToolbar) {
            // try to search a toggle action and queue it after it.
            for (int i = mr.getItems().size() - 1; i >= 0; i--) {
                final MenuItemData mid = mr.getItems().get(i);
                if (mid.getActionData() == null || !mid.getActionData()._isValidDataForCreatingAnAction() || mid instanceof MenuLink) {
                    continue;
                }
                final boolean val = mid._isValidated();
                try {
                    mid._setValidated(true);
                    if (mid.createAction().isToggle()) {
                        mr.getItems().add(i + 1, new MenuItemData(new ActionData(ShutdownToggleAction.class)));
                        return null;
                    }
                } catch (Exception e) {
                    logger.log(e);
                } finally {
                    mid._setValidated(val);
                }
            }
            // no toggle action found. append action at the end.
            mr.add(ShutdownToggleAction.class);
        }
        return null;
    }
}