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

package org.jdownloader.extensions.folderwatch;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkOrigin;
import jd.controlling.linkcollector.LinkOriginDetails;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledLinkModifier;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.linkcrawler.PackageInfo;
import jd.plugins.AddonPanel;
import jd.plugins.DownloadLink;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.contextmenu.ContextMenuManager;
import org.jdownloader.controlling.contextmenu.MenuContainerRoot;
import org.jdownloader.controlling.contextmenu.MenuExtenderHandler;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.extraction.BooleanStatus;
import org.jdownloader.extensions.folderwatch.translate.FolderWatchTranslation;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.mainmenu.MenuManagerMainmenu;
import org.jdownloader.gui.toolbar.MenuManagerMainToolbar;

public class FolderWatchExtension extends AbstractExtension<FolderWatchConfig, FolderWatchTranslation> implements MenuExtenderHandler, Runnable, GenericConfigEventListener<Long> {

    private FolderWatchConfigPanel   configPanel;
    private ScheduledExecutorService scheduler;
    private Object                   lock = new Object();

    public FolderWatchConfigPanel getConfigPanel() {
        return configPanel;
    }

    public boolean hasConfigPanel() {
        return true;
    }

    public FolderWatchExtension() throws StartException {
        setTitle("FolderWatch");

    }

    @Override
    public String getIconKey() {
        return IconKey.ICON_FOLDER_ADD;
    }

    // @Override
    // public Object interact(String command, Object parameter) {
    // if (command == null) return null;
    // if (command.equals("shutdown")) this.shutdown();
    // if (command.equals("hibernate")) this.hibernate();
    // if (command.equals("standby")) this.standby();
    // return null;
    // }

    @Override
    protected void stop() throws StopException {

        MenuManagerMainToolbar.getInstance().unregisterExtender(this);
        MenuManagerMainmenu.getInstance().unregisterExtender(this);
        synchronized (lock) {
            if (scheduler != null) {
                scheduler.shutdown();
                scheduler = null;
            }
        }
    }

    @Override
    protected void start() throws StartException {

        MenuManagerMainToolbar.getInstance().registerExtender(this);
        MenuManagerMainmenu.getInstance().registerExtender(this);
        createDefaultFolder();
        synchronized (lock) {
            scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(this, 0, getSettings().getCheckInterval(), TimeUnit.MILLISECONDS);
        }

        CFG_FOLDER_WATCH.CHECK_INTERVAL.getEventSender().addListener(this, true);
        // if (menuAction == null) {
        // menuAction = new ShutdownEnableToggle(null);
        // }

    }

    protected void createDefaultFolder() {
        String[] folders = getSettings().getFolders();
        if (folders != null) {
            for (String s : folders) {
                if (s != null && "folderwatch".equals(s)) {
                    Application.getResource("folderwatch").mkdirs();
                }
            }
        }
    }

    @Override
    public String getDescription() {
        return "Add All Links found in a Folder";
    }

    @Override
    public AddonPanel<FolderWatchExtension> getGUI() {
        return null;
    }

    // @Override
    // public List<JMenuItem> getMenuAction() {
    // java.util.List<JMenuItem> menu = new ArrayList<JMenuItem>();
    // menu.add(new JCheckBoxMenuItem(menuAction));
    // return menu;
    // }

    @Override
    protected void initExtension() throws StartException {
        // ConfigContainer cc = new ConfigContainer(getName());
        // initSettings(cc);
        configPanel = new FolderWatchConfigPanel(this);
    }

    @Override
    public boolean isQuickToggleEnabled() {
        return true;
    }

    @Override
    public MenuItemData updateMenuModel(ContextMenuManager manager, MenuContainerRoot mr) {
        // if (manager instanceof MenuManagerMainmenu) {
        // ExtensionsMenuContainer container = new ExtensionsMenuContainer();
        // container.add(org.jdownloader.extensions.shutdown.actions.ShutdownToggleAction.class);
        // return container;
        // } else if (manager instanceof MenuManagerMainToolbar) {
        // // try to search a toggle action and queue it after it.
        // for (int i = mr.getItems().size() - 1; i >= 0; i--) {
        // MenuItemData mid = mr.getItems().get(i);
        // if (mid.getActionData() == null) continue;
        // boolean val = mid._isValidated();
        // try {
        // mid._setValidated(true);
        // if (mid.createAction().isToggle()) {
        //
        // mr.getItems().add(i + 1, new MenuItemData(new ActionData(ShutdownToggleAction.class)));
        // return null;
        // }
        // } catch (Exception e) {
        // e.printStackTrace();
        // } finally {
        // mid._setValidated(val);
        // }
        // }
        // // no toggle action found. append action at the end.
        // mr.add(ShutdownToggleAction.class);
        //
        // }
        return null;
    }

    @Override
    public void run() {
        try {
            System.out.println("RUN " + System.currentTimeMillis());
            String[] folders = getSettings().getFolders();
            if (folders != null) {
                for (String s : folders) {
                    if (s != null) {
                        File folder = new File(s);
                        if (!folder.isAbsolute()) folder = Application.getResource(s);
                        if (folder.exists() && folder.isDirectory()) {
                            for (File f : folder.listFiles(new FilenameFilter() {

                                @Override
                                public boolean accept(File dir, String name) {
                                    return name.toLowerCase(Locale.ENGLISH).endsWith(".crawljob");
                                }
                            })) {
                                try {
                                    addCrawlJob(f);

                                    move(f);
                                } catch (Exception e) {
                                    getLogger().log(e);

                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            getLogger().log(e);

        }
    }

    private void move(File f) {
        File dir = new File(f.getParentFile(), "added");
        dir.mkdirs();
        int i = 1;
        File dst = new File(dir, f.getName() + "." + i);
        while (dst.exists()) {
            dst = new File(dir, f.getName() + "." + i);
            i++;
        }
        f.renameTo(dst);
    }

    private void addCrawlJob(File f) throws IOException {

        for (final CrawlJobStorable j : JSonStorage.restoreFromString(IO.readFileToString(f), new TypeRef<ArrayList<CrawlJobStorable>>() {
        })) {
            switch (j.getType()) {
            case NORMAL:

                LinkCollectingJob job = new LinkCollectingJob(new LinkOriginDetails(LinkOrigin.EXTENSION, "FolderWatch:" + f.getAbsolutePath()), j.getText());
                job.setDeepAnalyse(j.isDeepAnalyseEnabled());

                job.setCrawledLinkModifier(new CrawledLinkModifier() {

                    private PackageInfo getPackageInfo(CrawledLink link, boolean createIfNotExisting) {
                        PackageInfo packageInfo = link.getDesiredPackageInfo();
                        if (packageInfo != null || createIfNotExisting == false) return packageInfo;
                        packageInfo = new PackageInfo();
                        link.setDesiredPackageInfo(packageInfo);
                        return packageInfo;
                    }

                    @Override
                    public void modifyCrawledLink(CrawledLink link) {
                        if (StringUtils.isNotEmpty(j.getPackageName())) {
                            PackageInfo existing = getPackageInfo(link, false);
                            if (j.isOverwritePackagizerEnabled() || existing == null || StringUtils.isEmpty(existing.getName())) {
                                existing = getPackageInfo(link, true);
                                existing.setName(j.getPackageName());
                                existing.setUniqueId(null);
                            }
                        }

                        if (!BooleanStatus.UNSET.equals(j.getExtractAfterDownload())) {
                            BooleanStatus existing = BooleanStatus.UNSET;
                            if (link.hasArchiveInfo()) {
                                existing = link.getArchiveInfo().getAutoExtract();
                            }
                            if (j.isOverwritePackagizerEnabled() || existing == null || BooleanStatus.UNSET.equals(existing)) link.getArchiveInfo().setAutoExtract(j.getExtractAfterDownload());
                        }
                        if (j.isOverwritePackagizerEnabled()) {
                            link.setPriority(j.getPriority());
                        }
                        DownloadLink dlLink = link.getDownloadLink();
                        if (dlLink != null) {
                            if (StringUtils.isNotEmpty(j.getComment())) {
                                if (j.isOverwritePackagizerEnabled() || StringUtils.isEmpty(dlLink.getComment())) dlLink.setComment(j.getComment());
                            }
                            if (StringUtils.isNotEmpty(j.getDownloadPassword())) {
                                if (j.isOverwritePackagizerEnabled() || StringUtils.isEmpty(dlLink.getDownloadPassword())) dlLink.setDownloadPassword(j.getDownloadPassword());
                            }
                        }
                        if (StringUtils.isNotEmpty(j.getDownloadFolder())) {
                            PackageInfo existing = getPackageInfo(link, false);
                            if (j.isOverwritePackagizerEnabled() || existing == null || StringUtils.isEmpty(existing.getDestinationFolder())) {
                                existing = getPackageInfo(link, true);
                                existing.setDestinationFolder(j.getDownloadFolder());
                                existing.setUniqueId(null);
                            }
                        }
                        if (j.getExtractPasswords() != null && j.getExtractPasswords().length > 0) {
                            HashSet<String> list = new HashSet<String>();
                            for (String s : j.getExtractPasswords())
                                list.add(s);
                            link.getArchiveInfo().getExtractionPasswords().addAll(list);

                        }

                        if (j.getAutoConfirm() != BooleanStatus.UNSET) {
                            link.setAutoConfirmEnabled(j.getAutoConfirm().getBoolean());
                        }
                        if (j.getAutoStart() != BooleanStatus.UNSET) {
                            link.setAutoStartEnabled(j.getAutoStart().getBoolean());
                        }
                        if (j.getForcedStart() != BooleanStatus.UNSET) {
                            link.setForcedAutoStartEnabled(j.getForcedStart().getBoolean());
                        }
                        if (j.getChunks() > 0) {
                            link.setChunks(j.getChunks());
                        }
                        if (StringUtils.isNotEmpty(j.getFilename())) {
                            link.setName(j.getFilename());

                        }
                    }
                });

                LinkCrawler lc = LinkCollector.getInstance().addCrawlerJob(job);

                break;

            }

        }

    }

    @Override
    public void onConfigValidatorError(KeyHandler<Long> keyHandler, Long invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Long> keyHandler, Long newValue) {
        synchronized (lock) {

            scheduler.scheduleAtFixedRate(this, 0, getSettings().getCheckInterval(), TimeUnit.MILLISECONDS);
        }

    }
}