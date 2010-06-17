//jDownloader - Downloadmanager
//Copyright (C) 2010 JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.optional.folderwatch;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.SubConfiguration;
import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.actions.ToolBarAction.Types;
import jd.gui.swing.jdgui.interfaces.SwitchPanelEvent;
import jd.gui.swing.jdgui.interfaces.SwitchPanelListener;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;
import name.pachler.nio.file.ClosedWatchServiceException;
import name.pachler.nio.file.FileSystems;
import name.pachler.nio.file.Path;
import name.pachler.nio.file.Paths;
import name.pachler.nio.file.StandardWatchEventKind;
import name.pachler.nio.file.WatchEvent;
import name.pachler.nio.file.WatchKey;
import name.pachler.nio.file.WatchService;

@OptionalPlugin(rev = "$Revision$", id = "folderwatch", hasGui = true, interfaceversion = 5)
public class FolderWatch extends PluginOptional {

    private static final String JDL_PREFIX = "plugins.optional.FolderWatch.";

    private final SubConfiguration subConfig;

    private boolean isEnabled = false;

    private FolderWatchView view;

    private MenuAction toggleAction;

    private MenuAction showGuiAction;

    private String folder;

    private class WatchServiceThread extends Thread {

        private WatchService watchService;

        public WatchServiceThread() {
            this.watchService = FileSystems.getDefault().newWatchService();
        }

        public WatchServiceThread(String path) {
            this();
            register(path);
        }

        public void register(String path) {
            Path watchedPath = Paths.get(path);

            @SuppressWarnings("unused")
            WatchKey key = null;

            try {
                key = watchedPath.register(watchService, StandardWatchEventKind.ENTRY_CREATE, StandardWatchEventKind.ENTRY_DELETE);
            } catch (UnsupportedOperationException uox) {
                System.err.println("file watching not supported!");
                // handle this error here
            } catch (IOException iox) {
                System.err.println("I/O errors");
                // handle this error here
            }
        }

        public void run() {
            while (true) {
                // take() will block until a file has been created/deleted
                WatchKey signalledKey;
                try {
                    signalledKey = watchService.take();
                } catch (InterruptedException ix) {
                    // we'll ignore being interrupted
                    continue;
                } catch (ClosedWatchServiceException cwse) {
                    // other thread closed watch service
                    System.out.println("watch service closed, terminating.");
                    break;
                }

                // get list of events from key
                List<WatchEvent<?>> list = signalledKey.pollEvents();

                // VERY IMPORTANT! call reset() AFTER pollEvents() to allow the
                // key to be reported again by the watch service
                signalledKey.reset();

                // we'll simply print what has happened; real applications
                // will do something more sensible here
                for (WatchEvent<?> e : list) {
                    String message = "";

                    if (e.kind() == StandardWatchEventKind.ENTRY_CREATE) {
                        Path context = (Path) e.context();
                        String filename = context.toString();
                        message = filename + " created";

                        if (isContainer(filename)) {
                            importContainer(folder + "/" + filename);
                        }
                    } else if (e.kind() == StandardWatchEventKind.ENTRY_DELETE) {
                        Path context = (Path) e.context();
                        message = context.toString() + " deleted";
                    } else if (e.kind() == StandardWatchEventKind.OVERFLOW) {
                        message = "OVERFLOW: more changes happened than we could retreive";
                    }

                    System.out.println(message);
                }
            }
        }
    }

    public FolderWatch(PluginWrapper wrapper) {
        super(wrapper);

        subConfig = getPluginConfig();
        initConfig();
    }

    @Override
    public String getIconKey() {
        return "gui.images.taskpanes.linkgrabber";
    }

    @Override
    public boolean initAddon() {
        this.isEnabled = subConfig.getBooleanProperty(FolderWatchConstants.CONFIG_KEY_ENABLED, true);

        if (this.isEnabled) {
            this.folder = subConfig.getStringProperty(FolderWatchConstants.CONFIG_KEY_FOLDER, "/tmp");

            if (this.folder != null && (this.folder.equals("") == false)) {
                // TODO: After changing folder restart thread
                WatchServiceThread watching = new WatchServiceThread(this.folder);
                watching.start();
            }

        }

        logger.info("FolderWatch OK");
        return true;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getID() == 0) {
            try {
                subConfig.setProperty(FolderWatchConstants.CONFIG_KEY_ENABLED, toggleAction.isSelected());
                subConfig.save();
            } catch (Exception ex) {
                JDLogger.exception(ex);
            }

            // do something
        } else if (e.getID() == 1) {
            if (showGuiAction.isSelected()) {
                showGui();
            } else {
                view.close();
            }
        }
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();

        toggleAction = new MenuAction("folderwatch", 0);
        toggleAction.setTitle(JDL.L(JDL_PREFIX + "menu.toggle", "Activated"));
        toggleAction.setActionListener(this);
        toggleAction.setSelected(this.isEnabled);
        // toggleAction.setIcon(this.getIconKey());

        showGuiAction = new MenuAction("folderwatch", 1);
        showGuiAction.setTitle(JDL.L(JDL_PREFIX + "menu.history", "Show history"));
        showGuiAction.setActionListener(this);
        showGuiAction.setType(Types.TOGGLE);
        // showGuiAction.setIcon(this.getIconKey());

        menu.add(toggleAction);
        menu.add(new MenuAction(Types.SEPARATOR));
        menu.add(showGuiAction);

        return menu;
    }

    private void initConfig() {
        ConfigEntry ce = null;

        config.setGroup(new ConfigGroup(getHost(), getIconKey()));

        config.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_BROWSEFOLDER, subConfig, FolderWatchConstants.CONFIG_KEY_FOLDER, JDL.L("plugins.optional.FolderWatch.folder", "Folder to watch:")));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));

        config.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_BUTTON, this, JDL.L(JDL_PREFIX + "openfolder", "open folder"), JDL.L(JDL_PREFIX + "openfolder.long", "Open folder in local file manager:"), JDTheme.II("gui.images.package", 16, 16)));
        ce.setEnabled(false);

        config.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_BUTTON, this, JDL.L(JDL_PREFIX + "emptyfolder", "empty folder"), JDL.L(JDL_PREFIX + "emptyfolder.long", "Delete all container files:"), JDTheme.II("gui.images.clear ", 16, 16)));
        ce.setEnabled(false);

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));

        config.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, FolderWatchConstants.CONFIG_KEY_RECURSIVE, JDL.L(JDL_PREFIX + "recursive", "Watch recursively?")).setDefaultValue(true));
        ce.setEnabled(false);

        config.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, FolderWatchConstants.CONFIG_KEY_AUTODELETE, JDL.L(JDL_PREFIX + "autodelete", "Delete container after importing?")).setDefaultValue(false));
        ce.setEnabled(false);

        // config.addEntry(new ConfigEntry(ConfigContainer.TYPE_RADIOFIELD,
        // subConfig, FolderWatchConstants.CONFIG_KEY_AUTODOWNLOAD,
        // JDL.L(JDL_PREFIX + "autodownload",
        // "import and download containers")));
        // config.addEntry(new ConfigEntry(ConfigContainer.TYPE_RADIOFIELD,
        // subConfig, FolderWatchConstants.CONFIG_KEY_IMPORTONLY,
        // JDL.L(JDL_PREFIX + "importonly", "just import containers")));
    }

    private void showGui() {
        if (view == null) {
            view = new FolderWatchView();
            view.getBroadcaster().addListener(new SwitchPanelListener() {

                @Override
                public void onPanelEvent(SwitchPanelEvent event) {
                    if (event.getID() == SwitchPanelEvent.ON_REMOVE) showGuiAction.setSelected(false);
                }

            });
            // FolderWatchGui gui = new FolderWatchGui(getPluginConfig());
            // view.setContent(gui);
            // view.setInfoPanel(gui.getInfoPanel());
        }

        showGuiAction.setSelected(true);
        JDGui.getInstance().setContent(view);
    }

    @Override
    public void setGuiEnable(boolean value) {
        if (value) {
            showGui();
        } else {
            if (view != null) view.close();
        }
    }

    @Override
    public void onExit() {
    }

    private void importContainer(String path) {
        importContainer(new File(path));
    }

    private void importContainer(File container) {
        if (isContainer(container)) {
            JDController.loadContainerFile(container, false, false);
        }
    }

    private void deleteContainer(File container) {
        if (isContainer(container)) {
            container.delete();
        }
    }

    private void openFolder(File folder) {

    }

    private boolean emptyFolder(File folder) {
        if (folder.isDirectory()) {
            for (File file : folder.listFiles()) {
                if (isContainer(file)) {
                    file.delete();
                }
            }

            return true;
        }

        return false;
    }

    private boolean isContainer(File file) {
        return isContainer(file.getName());
    }

    private boolean isContainer(String path) {
        if (path.matches(".+\\.(dlc|ccf|rsdf)")) { return true; }

        return false;
    }
}
