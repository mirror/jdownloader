package org.jdownloader.update;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;

import jd.Launcher;
import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.JDGui;
import jd.parser.Regex;
import jd.utils.JDUtilities;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.config.JsonConfig;
import org.appwork.update.exchange.UpdateFile;
import org.appwork.update.inapp.AppUpdater;
import org.appwork.update.inapp.RestartViaUpdaterEvent;
import org.appwork.update.inapp.UpdaterGUI;
import org.appwork.update.standalone.KeyChangeRequest;
import org.appwork.update.updateclient.Actions;
import org.appwork.update.updateclient.InstalledFile;
import org.appwork.update.updateclient.LastChanceException;
import org.appwork.update.updateclient.Parameters;
import org.appwork.update.updateclient.UpdaterState;
import org.appwork.update.updateclient.event.UpdaterEvent;
import org.appwork.update.updateclient.event.UpdaterListener;
import org.appwork.update.updateclient.http.ClientUpdateRequiredException;
import org.appwork.update.updateclient.http.HTTPIOException;
import org.appwork.update.updateclient.http.UpdateServerException;
import org.appwork.update.updateclient.translation.T;
import org.appwork.utils.Application;
import org.appwork.utils.Hash;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.controlling.JDRestartController;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.logging.LogController;
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

    private UpdateProgress icon;
    private boolean        jars;

    public void run() {

        if (readVersion() < 919) {

            // bypass updaterloop when removing files
            JDRestartController.getInstance().restartViaUpdater(true);

        }
        try {
            if (this.tryLastChance()) {
                Log.L.info("Last Chance Restorer - Exit NOw");
                System.exit(1);
            }
        } catch (LastChanceException e) {
            Dialog.getInstance().showMessageDialog(_GUI._.JDUpdater_run_lasttryfailed_title_(), _GUI._.JDUpdater_run_lasttry_msg_());
        }
        super.run();
    }

    protected byte[] getLastChanceBytes() throws UnsupportedEncodingException, HTTPIOException, InterruptedException, ClientUpdateRequiredException, UpdateServerException, KeyChangeRequest {
        Parameters jar = null;
        Parameters jarHash = null;
        try {
            jar = new Parameters("jar", getJarName());
        } catch (Throwable e) {
            Log.exception(e);
        }

        try {
            jarHash = new Parameters("jarhash", Hash.getMD5(Application.getResource(getJarName())));
        } catch (Throwable e) {

        }
        if (Launcher.PARAMETERS.hasCommandSwitch("lasttry")) {
            return get(Actions.LAST_CHANCE, Parameters.APP_ID, Parameters.BRANCH_ID, jar, jarHash, new Parameters("test", "test"));
        } else {
            return get(Actions.LAST_CHANCE, Parameters.APP_ID, Parameters.BRANCH_ID, jar, jarHash);
        }

    }

    @Override
    public boolean canUnInstallDirect(File localFile, InstalledFile ifile) {
        return super.canUnInstallDirect(localFile, ifile);
    }

    public java.util.List<File> getFilesToInstallDirect(java.util.List<File> filesToInstall) {

        // check if there are jars to update
        jars = false;
        for (File f : filesToInstall) {

            if (f.getName().toLowerCase(Locale.ENGLISH).endsWith(".jar")) {
                jars = true;
                break;
            }

        }

        return filesToInstall;
    }

    @Override
    public boolean canInstallDirect(File next, UpdateFile uf) {

        // try to install plugins without restart
        if (next.getName().equals("build.json")) return true;
        if (next.getName().endsWith(".lng")) return true;
        if (!jars) {
            // only direct update class files if all jars are up2date
            String p = next.getAbsolutePath();

            String[] matches = new Regex(p, ".*[\\\\/]jd[\\\\/]plugins[\\\\/](.*?)[\\\\/](.+?)\\.class").getRow(0);
            if (matches != null && "hoster".equalsIgnoreCase(matches[0])) {
                return true;
            } else if (matches != null && "decrypter".equalsIgnoreCase(matches[0])) { return true; }
        }
        return super.canInstallDirect(next, uf);
    }

    protected String getUrl(Actions action, Parameters[] parameters) throws UnsupportedEncodingException {
        String ret = super.getUrl(action, parameters);
        HashMap<String, Object> build = JSonStorage.restoreFrom(Application.getResource("build.json"), new HashMap<String, Object>());
        StringBuilder sb = new StringBuilder();
        sb.append(ret);

        for (Entry<String, Object> e : build.entrySet()) {
            sb.append("&");
            sb.append(e.getKey());
            sb.append("=");
            sb.append(URLEncoder.encode(e.getValue() + "", "UTF-8"));

        }

        return sb.toString();
    }

    private boolean iconVisible;

    /**
     * Create a new instance of JDUpdater. This is a singleton class. Access the only existing instance by using {@link #getInstance()}.
     */
    private JDUpdater() {
        super();
        setLogger(LogController.getInstance().getLogger("Updater"));
        setRestartArgs(Launcher.PARAMETERS);
        setRestartAfterUpdaterUpdateAction(RestartViaUpdaterEvent.getInstance());
        icon = new UpdateProgress();
        ((org.appwork.swing.components.circlebar.ImagePainter) icon.getValueClipPainter()).setBackground(Color.LIGHT_GRAY);
        ((org.appwork.swing.components.circlebar.ImagePainter) icon.getValueClipPainter()).setForeground(Color.GREEN);
        icon.setTitle(_GUI._.JDUpdater_JDUpdater_object_icon());
        icon.setEnabled(true);
        icon.addMouseListener(new MouseListener() {

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseClicked(MouseEvent e) {

                JDUpdater.getInstance().startUpdate(false);
            }
        });

        org.jdownloader.controlling.JDRestartController.getInstance().setUpdater(this);
        this.getEventSender().addListener(new UpdaterListener() {

            private int currentStepSize = 0;

            public void onUpdaterModuleStart(UpdaterEvent arg0) {
            }

            public void onUpdaterModuleProgress(UpdaterEvent arg0, int percent) {
                final int dynamicPercent = (int) (getProgress() + currentStepSize * percent / 100.0f);
                icon.setValue(dynamicPercent);

            }

            public void onUpdaterModuleEnd(UpdaterEvent arg0) {
            }

            public void onUpdaterEvent(UpdaterEvent arg0) {

            }

            public void onStateExit(UpdaterState arg0) {

                if (arg0 == stateBranchUpdate) {
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            // JDGui.getInstance().getStatusBar().remove(icon);
                            // // icon.setIndeterminate(true);
                            icon.setValue(0);
                            icon.setIndeterminate(false);
                            if (!iconVisible) {
                                iconVisible = true;
                                JDGui.getInstance().getStatusBar().add(icon);
                            }
                        }
                    };

                } else if (arg0 == stateFilter) {

                    Log.L.finer("Files to Install:");
                    Log.L.finer(JSonStorage.toString(getFilesToInstall()));
                    Log.L.finer("Files to Download:");
                    Log.L.finer(JSonStorage.toString(getUpdates()));

                    Log.L.finer("Files to Remove:");
                    Log.L.finer(JSonStorage.toString(getFilesToRemove()));

                } else if (arg0 == stateDone || isBreakPointed() || arg0 == stateError) {
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            if (getTotalTodoCount() <= 0) {
                                JDGui.getInstance().getStatusBar().remove(icon);
                                iconVisible = false;
                            } else {
                                icon.setIndeterminate(true);
                                icon.setTitle(_GUI._.JDUpdater_JDUpdater_updates_available_title_());
                                icon.setDescription(_GUI._.JDUpdater_JDUpdater_updates_available_msg_(getTotalTodoCount()));
                            }
                        }
                    };

                }

                int size = getTotalTodoCount();

                SwingGui.getInstance().getMainFrame().setTitle(JDUtilities.getJDTitle(size));

            }

            public void onStateEnter(final UpdaterState state) {

                this.currentStepSize = state.getChildren().size() == 0 ? 1 : ((UpdaterState) state.getChildren().get(0)).getProgress() - state.getProgress();
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {

                        if (state == stateDone) {
                            icon.setDescription(T._.UpdateServer_UpdaterGui_onStateChange_successful());
                        } else if (state == stateDownloadData) {
                            // icon.setIndeterminate(false);
                            icon.setDescription(T._.UpdateServer_UpdaterGui_onStateChange_download());
                            icon.setValue(state.getProgress());
                        } else if (state == stateBranchUpdate) {
                            // icon.setIndeterminate(true);
                            icon.setDescription(T._.UpdateServer_UpdaterGui_onStateChange_branchlist());
                            icon.setValue(state.getProgress());

                        } else if (state == stateDownloadHashList) {
                            icon.setValue(state.getProgress());
                            icon.setDescription(T._.UpdateServer_UpdaterGui_onStateChange_hashlist());
                            // icon.setIndeterminate(true);
                        } else if (state == stateCreatePackage) {
                            // icon.setIndeterminate(true);
                            icon.setValue(state.getProgress());
                            icon.setDescription(T._.UpdateServer_UpdaterGui_onStateChange_package());

                        } else if (state == stateExtract) {
                            // icon.setIndeterminate(true);
                            icon.setValue(state.getProgress());
                            icon.setDescription(T._.UpdateServer_UpdaterGui_onStateChange_extract());
                        } else if (state == stateFilter) {
                            // icon.setIndeterminate(false);
                            icon.setValue(state.getProgress());
                            icon.setDescription(T._.UpdateServer_UpdaterGui_onStateChange_filter());
                        } else if (state == stateSelfUpdate) {
                            // icon.setIndeterminate(true);

                        } else if (state == stateInstall) {
                            // icon.setIndeterminate(false);
                            icon.setDescription(T._.UpdateServer_UpdaterGui_onStateChange_install());
                            icon.setValue(state.getProgress());
                        } else if (state == stateDirectInstall) {
                            // icon.setIndeterminate(false);
                            icon.setDescription(T._.UpdateServer_UpdaterGui_onStateChange_directinstall());
                            icon.setValue(state.getProgress());
                        } else if (state == stateError) {
                        } else if (state == stateWaitForUnlock) {
                            // icon.setIndeterminate(true);

                        }
                    }
                };

            }

            public void onDirectInstalls(java.util.List<File> parameter) {
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

    protected void onDownloadsAreAvailableForInstallation() {
        if (!JsonConfig.create(GeneralSettings.class).isSilentUpdateEnabled()) {
            super.onDownloadsAreAvailableForInstallation();
        }

    }

    @Override
    protected UpdaterGUI createUpdaterGui(AppUpdater appUpdater) {
        return new JDUpdaterGUI();
    }

    public String getBranchInUse() {
        return storage.getActiveBranch();
    }

    public boolean isSelfUpdateRequested() {
        return this.selfUpdateInfo != null;
    }

}
