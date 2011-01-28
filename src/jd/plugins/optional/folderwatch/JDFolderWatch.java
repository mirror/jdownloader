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
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.ConfigurationListener;
import jd.config.SubConfiguration;
import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.gui.UserIO;
import jd.gui.swing.components.JDFileChooser;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.actions.ToolBarAction.Types;
import jd.gui.swing.jdgui.interfaces.SwitchPanelEvent;
import jd.gui.swing.jdgui.interfaces.SwitchPanelListener;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.gui.swing.jdgui.views.linkgrabber.LinkGrabberPanel;
import jd.nutils.JDFlags;
import jd.nutils.JDHash;
import jd.nutils.OSDetector;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.plugins.optional.folderwatch.core.FileMonitoring;
import jd.plugins.optional.folderwatch.core.FileMonitoringListener;
import jd.plugins.optional.folderwatch.data.History;
import jd.plugins.optional.folderwatch.data.HistoryEntry;
import jd.plugins.optional.remotecontrol.helppage.HelpPage;
import jd.plugins.optional.remotecontrol.helppage.Table;
import jd.plugins.optional.remotecontrol.utils.RemoteSupport;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

@OptionalPlugin(rev = "$Revision$", id = "folderwatch", hasGui = false, interfaceversion = 7)
public class JDFolderWatch extends PluginOptional implements FileMonitoringListener, ConfigurationListener, RemoteSupport {

    private static final String    JDL_PREFIX           = "plugins.optional.folderwatch.JDFolderWatch.";

    private final SubConfiguration subConfig;

    private boolean                isEnabled            = false;

    private MenuAction             toggleAction;
    private MenuAction             showGuiAction;

    private FolderWatchPanel       historyGui           = null;
    private FolderWatchView        view                 = null;

    private Vector<String>         folderlist;
    private boolean                folderlistHasChanged = false;

    private boolean                isOption_recursive;
    private boolean                isOption_import;
    private boolean                isOption_importAndDelete;
    private boolean                isOption_history;

    private FileMonitoring         monitoringThread;

    public JDFolderWatch(PluginWrapper wrapper) {
        super(wrapper);
        subConfig = getPluginConfig();

        isEnabled = subConfig.getBooleanProperty(FolderWatchConstants.PROPERTY_ENABLED, false);

        initOptionVars();

        History.setEntries(getHistoryEntriesFromConfig());
        historyCleanup(null);

        initConfigGui();
    }

    @SuppressWarnings("unchecked")
    private void initOptionVars() {
        folderlist = (Vector<String>) subConfig.getProperty(FolderWatchConstants.PROPERTY_FOLDER_LIST);

        if (folderlist == null) {
            folderlist = new Vector<String>();
            subConfig.setProperty(FolderWatchConstants.PROPERTY_FOLDER_LIST, folderlist);
        }

        if (OSDetector.isWindows()) {
            isOption_recursive = subConfig.getBooleanProperty(FolderWatchConstants.PROPERTY_OPTION_RECURSIVE, false);
        } else {
            isOption_recursive = false;
        }

        isOption_import = subConfig.getBooleanProperty(FolderWatchConstants.PROPERTY_OPTION_IMPORT, false);
        isOption_importAndDelete = subConfig.getBooleanProperty(FolderWatchConstants.PROPERTY_OPTION_IMPORT_DELETE, false);
        isOption_history = subConfig.getBooleanProperty(FolderWatchConstants.PROPERTY_OPTION_HISTORY, false);
    }

    @Override
    public String getIconKey() {
        return "gui.images.folderwatch";
    }

    @Override
    public boolean initAddon() {
        subConfig.addConfigurationListener(this);

        startWatching(isEnabled);
        logger.info("FolderWatch: OK");

        return true;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getID() == 0) {
            try {
                subConfig.setProperty(FolderWatchConstants.PROPERTY_ENABLED, toggleAction.isSelected());
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

    private void initConfigGui() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        final DefaultListModel listModel = new DefaultListModel();
        for (String folder : folderlist) {
            listModel.addElement(folder);
        }

        final JList list = new JList(listModel);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        final JDFileChooser filechooser = new JDFileChooser();
        filechooser.setFileSelectionMode(JDFileChooser.DIRECTORIES_ONLY);
        filechooser.setMultiSelectionEnabled(true);

        config.setGroup(new ConfigGroup(JDL.L(JDL_PREFIX + "option.label.folderlist", "Folder list"), getIconKey()));

        JButton addButton = new JButton("add");
        addButton.setIcon(JDTheme.II("gui.images.add", 16, 16));
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                filechooser.showOpenDialog(null);

                for (File file : filechooser.getSelectedFiles()) {
                    folderlist.add(file.getAbsolutePath());
                    listModel.addElement(file.getAbsolutePath());

                    folderlistHasChanged = true;
                }
            }
        });

        JButton removeButton = new JButton("remove");
        removeButton.setIcon(JDTheme.II("gui.images.delete", 16, 16));
        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                while (!list.isSelectionEmpty()) {
                    folderlist.remove(list.getSelectedIndex());
                    listModel.remove(list.getSelectedIndex());

                    folderlistHasChanged = true;
                }
            }
        });

        JPanel p = new JPanel(new MigLayout("", "[]min[][grow,fill]min[grow, fill]"));

        p.add(addButton, "span,split,align center");
        p.add(removeButton, "");

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMPONENT, new JScrollPane(list), "growx,pushx"));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMPONENT, p, ""));

        config.setGroup(new ConfigGroup(JDL.L(JDL_PREFIX + "option.label.otheractions", "Actions"), getIconKey()));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String folder = (String) list.getSelectedValue();
                if (folder != null) openInFilebrowser(folder);
            }
        }, JDL.L(JDL_PREFIX + "openfolder", "open folder"), JDL.L(JDL_PREFIX + "openfolder.long", "Open selected folder with file manager:"), JDTheme.II("gui.images.package", 16, 16)));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (JDFlags.hasSomeFlags(UserIO.getInstance().requestConfirmDialog(UserIO.NO_COUNTDOWN, JDL.L(JDL_PREFIX + "option.emptyfolder.message", "Are you sure you want to delete all container files?")), UserIO.RETURN_OK)) {
                    String folder = (String) list.getSelectedValue();
                    if (folder != null) emptyFolder(folder);
                }
            }
        }, JDL.L(JDL_PREFIX + "emptyfolder", "empty folder"), JDL.L(JDL_PREFIX + "emptyfolder.long", "Delete all container files within sel. folder"), JDTheme.II("gui.images.clear", 16, 16)));

        config.setGroup(new ConfigGroup(JDL.L(JDL_PREFIX + "option.label.options", "Options"), getIconKey()));

        if (OSDetector.isWindows()) {
            config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, FolderWatchConstants.PROPERTY_OPTION_RECURSIVE, JDL.L(JDL_PREFIX + "recursive", "Watch registered folders recursively")).setDefaultValue(false));
        }

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, FolderWatchConstants.PROPERTY_OPTION_IMPORT, JDL.L(JDL_PREFIX + "option.import", "Import container when found")).setDefaultValue(true));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, FolderWatchConstants.PROPERTY_OPTION_IMPORT_DELETE, JDL.L(JDL_PREFIX + "option.importdelete", "Import container when found and delete it afterwards")).setDefaultValue(false));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, FolderWatchConstants.PROPERTY_OPTION_HISTORY, JDL.L(JDL_PREFIX + "option.history", "Add history entry for every found container")).setDefaultValue(true));
    }

    private void showGui() {
        if (view == null) {
            view = new FolderWatchView();
            view.getBroadcaster().addListener(new SwitchPanelListener() {
                @SuppressWarnings("deprecation")
                @Override
                public void onPanelEvent(SwitchPanelEvent event) {
                    if (event.getEventID() == SwitchPanelEvent.ON_REMOVE) {
                        showGuiAction.setSelected(false);
                    }
                }
            });

            historyCleanup(null);

            historyGui = new FolderWatchPanel(getPluginConfig());
            view.setContent(historyGui);
            view.setInfoPanel(historyGui.getInfoPanel());
        }
        showGuiAction.setSelected(true);
        JDGui.getInstance().setContent(view);
    }

    private boolean startWatching(boolean param) {
        if (param == true) {
            monitoringThread = new FileMonitoring();

            for (String folder : folderlist) {
                monitoringThread.register(folder, isOption_recursive);
            }

            monitoringThread.addListener(this);
            monitoringThread.start();

            logger.info("Watch service started");

            return true;
        } else {
            if (monitoringThread != null) {
                if (monitoringThread.isAlive()) {
                    monitoringThread.done();
                    monitoringThread = null;
                }
            }

            logger.info("Watch service closed");
        }
        return false;
    }

    public void onMonitoringFileCreate(String absPath) {
        if (isContainer(absPath)) {
            if (isOption_import || isOption_importAndDelete) {
                importContainer(absPath);
            }

            if (isOption_importAndDelete) {
                final String container = absPath;
                Thread t = new Thread(new Runnable() {
                    public void run() {
                        try {
                            while (LinkGrabberPanel.getLinkGrabber().isRunning()) {
                                Thread.sleep(1000);
                            }

                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        deleteContainer(container);
                        logger.info(container + "deleted");
                    }
                });
                t.run();
            }

            if (isOption_history) {
                boolean isExisting = isOption_importAndDelete ? false : true;

                HistoryEntry entry = new HistoryEntry(new File(absPath), JDHash.getMD5(new File(absPath)), isExisting);

                if (!isOption_import && !isOption_importAndDelete) {
                    entry.setImportDate(null);
                }

                historyAdd(entry);
            }
        }
    }

    public void onMonitoringFileDelete(String filename) {
        historyCleanup(filename);
    }

    private ArrayList<HistoryEntry> getHistoryEntriesFromConfig() {
        return subConfig.getGenericProperty(FolderWatchConstants.PROPERTY_HISTORY, new ArrayList<HistoryEntry>());
    }

    private void historyAdd(HistoryEntry entry) {
        History.add(entry);
        History.updateEntries();

        subConfig.setProperty(FolderWatchConstants.PROPERTY_HISTORY, History.getEntries());
        subConfig.save();

        if (historyGui != null) historyGui.refresh();
    }

    private void historyCleanup(String filename) {

        if (filename == null)
            History.updateEntries();
        else
            History.updateEntry(filename);

        subConfig.setProperty(FolderWatchConstants.PROPERTY_HISTORY, History.getEntries());
        subConfig.save();

        if (historyGui != null) historyGui.refresh();
    }

    public void importContainer(String path) {
        importContainer(new File(path));
    }

    public void importContainer(File container) {
        if (isContainer(container)) {
            JDController.loadContainerFile(container, false, false);
        }
    }

    private void deleteContainer(String path) {
        deleteContainer(new File(path));
    }

    private void deleteContainer(File container) {
        if (isContainer(container)) container.delete();
    }

    private void openInFilebrowser(String path) {
        JDUtilities.openExplorer(new File(path));
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

    public void onPreSave(SubConfiguration subConfiguration) {
    }

    public void onPostSave(SubConfiguration subConfiguration) {
        boolean recursiveOptionChanged = (isOption_recursive == subConfig.getBooleanProperty(FolderWatchConstants.PROPERTY_OPTION_RECURSIVE)) ? false : true;

        // TODO: unregister directories, so you don't have to
        // restart service
        if (folderlistHasChanged || (recursiveOptionChanged && OSDetector.isWindows())) {
            logger.info("Options have been changed that require the service to restart...");

            startWatching(false);
            startWatching(true);
        }

        folderlistHasChanged = false;
        initOptionVars();
    }

    @Override
    public void onExit() {

    }

    public Object handleRemoteCmd(String cmd) {
        if (cmd.matches("(?is).*/addon/folderwatch/action/start")) {
            toggleAction.setSelected(true);
            subConfig.setProperty(FolderWatchConstants.PROPERTY_ENABLED, true);
            subConfig.save();

            startWatching(true);

            return "JD FolderWatch has been started.";
        } else if (cmd.matches("(?is).*/addon/folderwatch/action/stop")) {
            toggleAction.setSelected(false);
            subConfig.setProperty(FolderWatchConstants.PROPERTY_ENABLED, false);
            subConfig.save();

            startWatching(false);

            return "JD FolderWatch has been stopped.";
        }

        return null;
    }

    // More commands to come ;)
    public void initCmdTable() {
        Table t = HelpPage.createTable(new Table(this.getHost()));

        t.setCommand("/addon/folderwatch/action/start");
        t.setInfo("Starts JD FolderWatch watching service.");

        t.setCommand("/addon/folderwatch/action/stop");
        t.setInfo("Stops JD FolderWatch watching service.");

        // not implemented yet:
        /*
         * t.setCommand("/addon/folderwatch/action/register/%X%");
         * t.setInfo("");
         * 
         * t.setCommand("/addon/folderwatch/action/unregister/%X%");
         * t.setInfo("");
         * 
         * t.setCommand("/addon/folderwatch/action/unregister/all");
         * t.setInfo("");
         * 
         * t.setCommand("/addon/folderwatch/action/emptyfolders");
         * t.setInfo("");
         * 
         * t.setCommand("/addon/folderwatch/action/clearhistory");
         * t.setInfo("");
         * 
         * t.setCommand("/addon/folderwatch/get/history"); t.setInfo("");
         * 
         * t.setCommand("/addon/folderwatch/get/folders"); t.setInfo("");
         * 
         * t.setCommand("/addon/folderwatch/set/recursive/(true|false)");
         * t.setInfo("");
         * 
         * t.setCommand("/addon/folderwatch/set/importdelete/(true|false)");
         * t.setInfo("");
         * 
         * t.setCommand("/addon/folderwatch/set/history/(true|false)");
         * t.setInfo("");
         * 
         * t.setCommand("/addon/folderwatch/set/autoimport/(true|false)");
         * t.setInfo("");
         */
    }
}
