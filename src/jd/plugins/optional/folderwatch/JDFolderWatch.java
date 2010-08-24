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
import jd.config.ConfigurationListener;
import jd.config.SubConfiguration;
import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.actions.ToolBarAction.Types;
import jd.gui.swing.jdgui.interfaces.SwitchPanelEvent;
import jd.gui.swing.jdgui.interfaces.SwitchPanelListener;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.nutils.JDHash;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.plugins.optional.folderwatch.data.HistoryData;
import jd.plugins.optional.folderwatch.data.HistoryDataEntry;
import jd.plugins.optional.remotecontrol.helppage.HelpPage;
import jd.plugins.optional.remotecontrol.helppage.Table;
import jd.plugins.optional.remotecontrol.utils.RemoteSupport;
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

import org.appwork.utils.Regex;

@SuppressWarnings("unused")
@OptionalPlugin(rev = "$Revision$", id = "folderwatch", hasGui = false, interfaceversion = 5)
public class JDFolderWatch extends PluginOptional implements ConfigurationListener, RemoteSupport {

    private static final String JDL_PREFIX = "plugins.optional.folderwatch.JDFolderWatch.";

    private final SubConfiguration subConfig;

    public static HistoryData history;

    // option/mode flags
    private boolean isEnabled = false;
    private boolean isRecursive = false;
    private boolean isAutodelete = false;
    private boolean isHistoryOnly = false;
    private boolean isDeleteCascade = false;

    private MenuAction toggleAction;
    private MenuAction showGuiAction;

    private FolderWatchView view;
    private String folder;

    private class WatchServiceThread extends Thread {

        private WatchService watchService;

        private volatile boolean isDone = false;

        public WatchServiceThread() {
            this.watchService = FileSystems.getDefault().newWatchService();
        }

        public WatchServiceThread(String path) {
            this();
            register(path);
        }

        public void register(String path) {
            Path watchedPath = Paths.get(path);
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

        public void done() {
            isDone = true;
        }

        public void run() {
            while (!isDone) {
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
                            String absPath = folder + "/" + filename;

                            String md5Hash = importContainer(absPath);

                            history.add(new HistoryDataEntry(filename, absPath, md5Hash));
                            subConfig.setProperty(FolderWatchConstants.CONFIG_KEY_HISTORY, history);
                            subConfig.save();
                        }
                    } else if (e.kind() == StandardWatchEventKind.ENTRY_DELETE) {
                        Path context = (Path) e.context();

                        history.updateEntries();
                        subConfig.setProperty(FolderWatchConstants.CONFIG_KEY_HISTORY, history);
                        subConfig.save();

                        message = context.toString() + " deleted";
                    } else if (e.kind() == StandardWatchEventKind.OVERFLOW) {
                        message = "OVERFLOW: more changes happened than we could retreive";
                    }

                    // debugging only
                    System.out.println(message);
                }
            }
        }
    }

    private WatchServiceThread watchingServiceThread;

    public JDFolderWatch(PluginWrapper wrapper) {
        super(wrapper);

        subConfig = getPluginConfig();
        history = subConfig.getGenericProperty(FolderWatchConstants.CONFIG_KEY_HISTORY, new HistoryData());

        initConfig();
    }

    @Override
    public String getIconKey() {
        return "gui.images.taskpanes.linkgrabber";
    }

    @Override
    public boolean initAddon() {
        isEnabled = subConfig.getBooleanProperty(FolderWatchConstants.CONFIG_KEY_ENABLED, true);
        folder = subConfig.getStringProperty(FolderWatchConstants.CONFIG_KEY_FOLDER, "");

        startWatching(isEnabled);

        logger.info("FolderWatch: OK");
        return true;
    }

    private boolean startWatching(boolean param) {
        if (param == true) {
            if (folder != null && !folder.equals("")) {
                watchingServiceThread = new WatchServiceThread(folder);
                watchingServiceThread.start();
            }
            return true;
        } else {
            if (watchingServiceThread.isAlive()) {
                watchingServiceThread.done();
            }
            watchingServiceThread = null;
        }
        return false;
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
            startWatching(toggleAction.isSelected());
        } else if (e.getID() == 1) {
            if (showGuiAction.isSelected())
                showGui();
            else
                view.close();
        }
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();

        toggleAction = new MenuAction("folderwatch.toggle", 0);
        toggleAction.setActionListener(this);
        toggleAction.setSelected(isEnabled);

        showGuiAction = new MenuAction("folderwatch.history", 1);
        showGuiAction.setActionListener(this);
        showGuiAction.setType(Types.TOGGLE);

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

        config.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, FolderWatchConstants.CONFIG_KEY_HISTORYONLY, JDL.L(JDL_PREFIX + "historyonly", "Adds containers to history but doesn't import them.")).setDefaultValue(false));
        ce.setEnabled(false);

        config.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, FolderWatchConstants.CONFIG_KEY_DELETECASCADE, JDL.L(JDL_PREFIX + "deletecascade", "Deletes also related history entry when container gets deleted.")).setDefaultValue(false));
        ce.setEnabled(false);
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

            FolderWatchGui gui = new FolderWatchGui(getPluginConfig());
            view.setContent(gui);
            // view.setInfoPanel(gui.getInfoPanel());
        }
        showGuiAction.setSelected(true);
        JDGui.getInstance().setContent(view);
    }

    @Override
    public void onExit() {
    }

    private String importContainer(String path) {
        return importContainer(new File(path));
    }

    private String importContainer(File container) {
        if (isContainer(container)) {
            JDController.loadContainerFile(container, false, false);
        }
        return JDHash.getMD5(container);
    }

    private void deleteContainer(String path) {
        deleteContainer(new File(path));
    }

    private void deleteContainer(File container) {
        if (isContainer(container)) container.delete();
    }

    private boolean emptyFolder(String path) {
        return emptyFolder(new File(path));
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

    private boolean isContainer(String path) {
        return path.matches(".+\\.(dlc|ccf|rsdf)");
    }

    private boolean isContainer(File file) {
        return isContainer(file.getName());
    }

    private void openInFilebrowser(String path) {
    }

    public void onPreSave(SubConfiguration subConfiguration) {
        startWatching(false);
    }

    public void onPostSave(SubConfiguration subConfiguration) {
        startWatching(true);
    }

    public Object handleRemoteCmd(String cmd) {
        if (cmd.matches("(?is).*/addon/folderwatch/start")) {
            toggleAction.setSelected(true);
            subConfig.setProperty(FolderWatchConstants.CONFIG_KEY_ENABLED, true);
            subConfig.save();
            startWatching(true);

            return "JD FolderWatch has been started.";
        } else if (cmd.matches("(?is).*/addon/folderwatch/stop")) {
            toggleAction.setSelected(false);
            subConfig.setProperty(FolderWatchConstants.CONFIG_KEY_ENABLED, false);
            subConfig.save();
            startWatching(false);

            return "JD FolderWatch has been stopped.";
        } else if (cmd.matches("(?is).*/addon/folderwatch/registerfolder/.+")) {
            String folder = new Regex(cmd, "(?is).*/addon/folderwatch/registerfolder/(.+)").getMatch(0);
            watchingServiceThread.register(folder);

            return folder + " has been registered to be watched.";
        }
        return null;
    }

    // More commands to come ;)
    public void initCmdTable() {
        Table t = HelpPage.createTable(new Table(this.getHost()));

        t.setCommand("/addon/folderwatch/start");
        t.setInfo("Starts JD FolderWatch watching service.");

        t.setCommand("/addon/folderwatch/stop");
        t.setInfo("Stops JD FolderWatch watching service.");

        t.setCommand("/addon/folderwatch/registerfolder/%X%");
        t.setInfo("Adds a path to the list of folders that will be watched. You can register as many folders as you like.");
    }
}
