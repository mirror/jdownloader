package org.jdownloader.update;

import java.io.File;
import java.util.ArrayList;

import jd.gui.swing.SwingGui;
import jd.parser.Regex;
import jd.utils.JDUtilities;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.config.JsonConfig;
import org.appwork.update.exchange.UpdateFile;
import org.appwork.update.inapp.AppUpdater;
import org.appwork.update.inapp.RestartController;
import org.appwork.update.inapp.UpdaterGUI;
import org.appwork.update.updateclient.InstalledFile;
import org.appwork.update.updateclient.UpdaterState;
import org.appwork.update.updateclient.event.UpdaterEvent;
import org.appwork.update.updateclient.event.UpdaterListener;
import org.appwork.utils.logging.Log;
import org.jdownloader.plugins.controller.crawler.CrawlerPluginController;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.settings.GeneralSettings;

public class JDUpdater extends AppUpdater {
    private static final JDUpdater INSTANCE = new JDUpdater();

    /**
     * get the only existing instance of JDUpdater. This is a singleton
     * 
     * @return
     */
    public static JDUpdater getInstance() {
        return JDUpdater.INSTANCE;
    }

    @Override
    public boolean canUnInstallDirect(File localFile, InstalledFile ifile) {
        return super.canUnInstallDirect(localFile, ifile);
    }

    @Override
    public boolean canInstallDirect(File next, UpdateFile uf) {

        // try to install plugins without restart
        String p = next.getAbsolutePath();
        String[] matches = new Regex(p, ".*[\\\\/]jd[\\\\/]plugins[\\\\/](.*?)[\\\\/](.+?)\\.class").getRow(0);
        if (matches != null && "hoster".equalsIgnoreCase(matches[0])) {
            return true;
        } else if (matches != null && "decrypter".equalsIgnoreCase(matches[0])) { return true; }
        return super.canInstallDirect(next, uf);
    }

    public boolean installDirectFilesEnabled() {
        return JsonConfig.create(GeneralSettings.class).isDirectUpdateEnabled();
    }

    /**
     * Create a new instance of JDUpdater. This is a singleton class. Access the
     * only existing instance by using {@link #getInstance()}.
     */
    private JDUpdater() {
        super();
        RestartController.getInstance().setUpdater(this);
        this.getEventSender().addListener(new UpdaterListener() {

            public void onUpdaterModuleStart(UpdaterEvent arg0) {
            }

            public void onUpdaterModuleProgress(UpdaterEvent arg0, int arg1) {
            }

            public void onUpdaterModuleEnd(UpdaterEvent arg0) {
            }

            public void onUpdaterEvent(UpdaterEvent arg0) {
            }

            public void onStateExit(UpdaterState arg0) {
                if (arg0 == stateFilter) {

                    Log.L.finer("Files to Install:");
                    Log.L.finer(JSonStorage.toString(getFilesToInstall()));
                    Log.L.finer("Files to Download:");
                    Log.L.finer(JSonStorage.toString(getUpdates()));

                    Log.L.finer("Files to Remove:");
                    Log.L.finer(JSonStorage.toString(getFilesToRemove()));

                } else if (arg0 == stateError) {
                    // Exception exc = getException();

                }
                int size = getTotalTodoCount();

                SwingGui.getInstance().getMainFrame().setTitle(JDUtilities.getJDTitle(size));

            }

            public void onStateEnter(UpdaterState arg0) {

            }

            public void onDirectInstalls(ArrayList<File> parameter) {
                boolean hasHostPlugins = false;
                boolean hasCrawlPlugins = false;
                for (File f : parameter) {
                    String[] matches = new Regex(f.getAbsolutePath(), ".*[\\\\/]jd[\\\\/]plugins[\\\\/](.*?)[\\\\/](.+?)\\.class").getRow(0);
                    if (matches != null && "hoster".equalsIgnoreCase(matches[0])) {
                        hasHostPlugins = true;
                        if (hasCrawlPlugins) break;
                    } else if (matches != null && "decrypter".equalsIgnoreCase(matches[0])) {
                        hasCrawlPlugins = true;
                        if (hasHostPlugins) break;
                    }
                }
                if (hasHostPlugins) {
                    HostPluginController.getInstance().init(true);
                }
                if (hasCrawlPlugins) {
                    CrawlerPluginController.getInstance().init(true);
                }

            }

            public void onLog(String parameter) {
            }
        });

    }

    @Override
    protected UpdaterGUI createUpdaterGui(AppUpdater appUpdater) {
        return new JDUpdaterGUI();
    }

    public String getBranchInUse() {
        return storage.getActiveBranch();
    }

}
