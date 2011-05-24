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

package org.jdownloader.extensions.folderwatch;

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

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.SubConfiguration;
import jd.controlling.JDController;
import jd.controlling.JSonWrapper;
import jd.gui.UserIO;
import jd.gui.swing.components.JDFileChooser;
import jd.gui.swing.jdgui.views.linkgrabber.LinkGrabberPanel;
import jd.nutils.JDFlags;
import jd.nutils.JDHash;
import jd.nutils.OSDetector;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.folderwatch.core.FileMonitoring;
import org.jdownloader.extensions.folderwatch.core.FileMonitoringListener;
import org.jdownloader.extensions.folderwatch.data.History;
import org.jdownloader.extensions.folderwatch.data.HistoryEntry;
import org.jdownloader.extensions.folderwatch.translate.T;
import org.jdownloader.extensions.remotecontrol.helppage.HelpPage;
import org.jdownloader.extensions.remotecontrol.helppage.Table;
import org.jdownloader.images.NewTheme;

public class FolderWatchExtension extends AbstractExtension<FolderWatchConfig> implements FileMonitoringListener, ActionListener {

    private JSonWrapper          subConfig;

    private boolean              isEnabled            = false;

    private FolderWatchPanel     historyGui           = null;
    private FolderWatchView      view                 = null;

    private Vector<String>       folderlist;
    private boolean              folderlistHasChanged = false;

    private boolean              isOption_recursive;
    private boolean              isOption_import;
    private boolean              isOption_importAndDelete;
    private boolean              isOption_history;

    private JList                guiFolderList;

    private FileMonitoring       monitoringThread;

    private ExtensionConfigPanel configPanel;

    public ExtensionConfigPanel<FolderWatchExtension> getConfigPanel() {
        return configPanel;
    }

    public boolean hasConfigPanel() {
        return true;
    }

    public FolderWatchExtension() throws StartException {
        super("FolderWatch");

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
        return "folder_add";
    }

    public void actionPerformed(ActionEvent e) {

    }

    private void addListModelEntry(String folder) {
        DefaultListModel listModel = (DefaultListModel) guiFolderList.getModel();

        listModel.addElement(folder + " (" + getNumberOfContainerFiles(folder) + ")");
    }

    private void updateListModelSelection() {
        if (!guiFolderList.isSelectionEmpty()) {
            for (int index : guiFolderList.getSelectedIndices()) {
                updateListModelEntry(index);
            }
        }
    }

    private void updateListModelEntry(int index) {
        DefaultListModel listModel = (DefaultListModel) guiFolderList.getModel();

        String folder = folderlist.get(index);
        listModel.set(index, folder + " (" + getNumberOfContainerFiles(folder) + ")");
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
            File file = new File(absPath);
            updateListModelEntry(folderlist.indexOf(file.getParent()));

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
        if (isContainer(container)) {
            container.delete();
        }
    }

    private void openInFilebrowser(String path) {
        File dir = new File(path);

        if (dir.exists()) {
            JDUtilities.openExplorer(dir);
        } else {
            UserIO.getInstance().requestConfirmDialog(UserIO.NO_COUNTDOWN, T._.plugins_optional_folderwatch_JDFolderWatch_action_openfolder_errormessage());
        }
    }

    private boolean emptyFolder(String path) {
        return emptyFolder(new File(path));
    }

    private boolean emptyFolder(File folder) {
        if (folder.exists() && folder.isDirectory()) {
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
        if (!file.exists()) return false;

        return isContainer(file.getName());
    }

    private boolean isContainer(String path) {
        return path.matches(".+\\.(dlc|ccf|rsdf)");
    }

    private int getNumberOfContainerFiles(String path) {
        return getNumberOfContainerFiles(new File(path));
    }

    private int getNumberOfContainerFiles(File path) {
        int n = 0;

        if (path.isDirectory()) {
            for (File file : path.listFiles()) {
                if (isContainer(file)) n++;
            }
        }

        return n;
    }

    // TODO
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

    // More commands to come ;)
    public void initCmdTable() {
        Table t = HelpPage.createTable(new Table(getName()));

        t.setCommand("/addon/folderwatch/action/start");
        t.setInfo("Starts FolderWatch watching service.");

        t.setCommand("/addon/folderwatch/action/stop");
        t.setInfo("Stops FolderWatch watching service.");

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

    @Override
    protected void stop() throws StopException {
    }

    @Override
    protected void start() throws StartException {

        // subConfig.addConfigurationListener(this);

        startWatching(isEnabled);
        logger.info("FolderWatch: OK");

    }

    protected void initSettings(ConfigContainer config) {

        final JDFileChooser filechooser = new JDFileChooser();
        filechooser.setFileSelectionMode(JDFileChooser.DIRECTORIES_ONLY);
        filechooser.setMultiSelectionEnabled(true);

        final DefaultListModel listModel = new DefaultListModel();
        guiFolderList = new JList(listModel);
        guiFolderList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        for (String folder : folderlist)
            addListModelEntry(folder);

        config.setGroup(new ConfigGroup(T._.plugins_optional_folderwatch_JDFolderWatch_gui_label_folderlist(), getIconKey()));

        JButton addButton = new JButton(T._.plugins_optional_folderwatch_JDFolderWatch_gui_folderlist_add());
        addButton.setIcon(NewTheme.I().getIcon("add", 16));
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                filechooser.showOpenDialog(null);

                for (File file : filechooser.getSelectedFiles()) {
                    if (!folderlist.contains(file.getAbsolutePath())) {
                        folderlist.add(file.getAbsolutePath());
                        addListModelEntry(file.getAbsolutePath());

                        folderlistHasChanged = true;
                    }
                }
            }
        });

        JButton removeButton = new JButton(T._.plugins_optional_folderwatch_JDFolderWatch_gui_folderlist_remove());
        removeButton.setIcon(NewTheme.I().getIcon("delete", 16));
        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                while (!guiFolderList.isSelectionEmpty()) {
                    folderlist.remove(guiFolderList.getSelectedIndex());
                    listModel.remove(guiFolderList.getSelectedIndex());

                    folderlistHasChanged = true;
                }
            }
        });

        JPanel p = new JPanel(new MigLayout("", "[]min[][grow,fill]min[grow, fill]"));

        p.add(addButton, "span,split,align center");
        p.add(removeButton, "");

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMPONENT, new JScrollPane(guiFolderList), "growx,pushx"));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMPONENT, p, ""));

        config.setGroup(new ConfigGroup(T._.plugins_optional_folderwatch_JDFolderWatch_gui_label_actions(), getIconKey()));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!guiFolderList.isSelectionEmpty()) {
                    String folder = folderlist.get(guiFolderList.getSelectedIndex());
                    openInFilebrowser(folder);
                }
            }
        }, T._.plugins_optional_folderwatch_JDFolderWatch_gui_action_openfolder(), T._.plugins_optional_folderwatch_JDFolderWatch_gui_action_openfolder_long(), NewTheme.I().getIcon("package_closed", 16)));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (JDFlags.hasSomeFlags(UserIO.getInstance().requestConfirmDialog(UserIO.NO_COUNTDOWN, T._.plugins_optional_folderwatch_JDFolderWatch_gui_action_emptyfolder_message()), UserIO.RETURN_OK)) {
                    String folder = folderlist.get(guiFolderList.getSelectedIndex());

                    emptyFolder(folder);
                    updateListModelSelection();
                }
            }
        }, T._.plugins_optional_folderwatch_JDFolderWatch_gui_action_emptyfolder(), T._.plugins_optional_folderwatch_JDFolderWatch_gui_action_emptyfolder_long(), NewTheme.I().getIcon("clear", 16)));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getGUI().setActive(true);
                getGUI().toFront();
            }
        }, T._.plugins_optional_folderwatch_JDFolderWatch_gui_action_showhistory(), T._.plugins_optional_folderwatch_JDFolderWatch_gui_action_showhistory_long(), NewTheme.I().getIcon("event", 16)));

        config.setGroup(new ConfigGroup(T._.plugins_optional_folderwatch_JDFolderWatch_gui_label_options(), getIconKey()));

        if (OSDetector.isWindows()) {
            config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, FolderWatchConstants.PROPERTY_OPTION_RECURSIVE, T._.plugins_optional_folderwatch_JDFolderWatch_gui_option_recursive()).setDefaultValue(false));
        }

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, FolderWatchConstants.PROPERTY_OPTION_IMPORT, T._.plugins_optional_folderwatch_JDFolderWatch_gui_option_import()).setDefaultValue(true));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, FolderWatchConstants.PROPERTY_OPTION_IMPORT_DELETE, T._.plugins_optional_folderwatch_JDFolderWatch_gui_option_importdelete()).setDefaultValue(false));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, FolderWatchConstants.PROPERTY_OPTION_HISTORY, T._.plugins_optional_folderwatch_JDFolderWatch_gui_option_history()).setDefaultValue(true));

    }

    @Override
    public String getConfigID() {
        return "folderwatch";
    }

    @Override
    public String getAuthor() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public FolderWatchView getGUI() {
        return view;
    }

    @Override
    protected void initExtension() throws StartException {
        subConfig = getPluginConfig();

        isEnabled = subConfig.getBooleanProperty(FolderWatchConstants.PROPERTY_ENABLED, false);

        ConfigContainer cc = new ConfigContainer(getName());
        initOptionVars();
        initSettings(cc);
        configPanel = createPanelFromContainer(cc);

        History.setEntries(getHistoryEntriesFromConfig());
        historyCleanup(null);
        initGUI();
    }

    private void initGUI() {

        view = new FolderWatchView(this);
        FolderWatchPanel panel = new FolderWatchPanel(subConfig, this);
        view.setContent(panel);
        view.setInfoPanel(panel.getInfoPanel());
    }
}