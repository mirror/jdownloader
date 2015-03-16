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

package org.jdownloader.extensions.folderwatchV2;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
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
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;

import org.appwork.storage.JSonMapperException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.storage.simplejson.mapper.ClassCache;
import org.appwork.storage.simplejson.mapper.Setter;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.reflection.Clazz;
import org.jdownloader.controlling.contextmenu.ContextMenuManager;
import org.jdownloader.controlling.contextmenu.MenuContainerRoot;
import org.jdownloader.controlling.contextmenu.MenuExtenderHandler;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.extraction.BooleanStatus;
import org.jdownloader.extensions.folderwatchV2.translate.FolderWatchTranslation;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.mainmenu.MenuManagerMainmenu;
import org.jdownloader.gui.toolbar.MenuManagerMainToolbar;
import org.jdownloader.plugins.controller.UpdateRequiredClassNotFoundException;
import org.jdownloader.plugins.controller.host.HostPluginController;

public class FolderWatchExtension extends AbstractExtension<FolderWatchConfig, FolderWatchTranslation> implements MenuExtenderHandler, Runnable, GenericConfigEventListener<Long> {

    private FolderWatchConfigPanel   configPanel;
    private ScheduledExecutorService scheduler;
    private final Object             lock = new Object();
    private ScheduledFuture<?>       job  = null;

    @Override
    public boolean isHeadlessRunnable() {
        return true;
    }

    public FolderWatchConfigPanel getConfigPanel() {
        return configPanel;
    }

    public boolean hasConfigPanel() {
        return true;
    }

    public FolderWatchExtension() throws StartException {
        setTitle(_.title());
    }

    @Override
    public String getIconKey() {
        return IconKey.ICON_FOLDER_ADD;
    }

    @Override
    protected void stop() throws StopException {
        if (!Application.isHeadless()) {
            MenuManagerMainToolbar.getInstance().unregisterExtender(this);
            MenuManagerMainmenu.getInstance().unregisterExtender(this);
        }
        synchronized (lock) {
            if (job != null) {
                job.cancel(false);
                job = null;
            }
            if (scheduler != null) {
                scheduler.shutdown();
                scheduler = null;
            }
        }
    }

    @Override
    protected void start() throws StartException {
        if (!Application.isHeadless()) {
            MenuManagerMainToolbar.getInstance().registerExtender(this);
            MenuManagerMainmenu.getInstance().registerExtender(this);
        }
        createDefaultFolder();
        synchronized (lock) {
            scheduler = Executors.newScheduledThreadPool(1);
            job = scheduler.scheduleAtFixedRate(this, 0, getSettings().getCheckInterval(), TimeUnit.MILLISECONDS);
        }
        CFG_FOLDER_WATCH.CHECK_INTERVAL.getEventSender().addListener(this, true);
    }

    protected void createDefaultFolder() {
        final String[] folders = getSettings().getFolders();
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
        return _.description();
    }

    @Override
    public AddonPanel<FolderWatchExtension> getGUI() {
        return null;
    }

    @Override
    protected void initExtension() throws StartException {
        if (!Application.isHeadless()) {
            configPanel = new FolderWatchConfigPanel(this);
        }
    }

    @Override
    public boolean isQuickToggleEnabled() {
        return true;
    }

    @Override
    public MenuItemData updateMenuModel(ContextMenuManager manager, MenuContainerRoot mr) {
        return null;
    }

    @Override
    public void run() {
        if (CFG_FOLDER_WATCH.CFG.isDebugEnabled()) {
            getLogger().info("RUN");
        }
        try {

            String[] folders = getSettings().getFolders();
            if (folders != null) {
                for (String s : folders) {

                    if (s != null) {
                        File folder = new File(s);
                        if (!folder.isAbsolute()) {
                            folder = Application.getResource(s);
                        }
                        if (CFG_FOLDER_WATCH.CFG.isDebugEnabled()) {
                            getLogger().info("Scan " + s + " - " + folder.getAbsolutePath());
                            getLogger().info("exists: " + folder.exists());
                            getLogger().info("isDirectory: " + folder.isDirectory());
                        }
                        if (folder.exists() && folder.isDirectory()) {
                            if (CFG_FOLDER_WATCH.CFG.isDebugEnabled()) {
                                getLogger().info(Arrays.toString(folder.list()));
                            }
                            for (File f : folder.listFiles(new FilenameFilter() {

                                @Override
                                public boolean accept(File dir, String name) {
                                    if (CFG_FOLDER_WATCH.CFG.isDebugEnabled()) {
                                        getLogger().info(name + " : " + name.toLowerCase(Locale.ENGLISH).endsWith(".crawljob"));
                                    }
                                    return name.toLowerCase(Locale.ENGLISH).endsWith(".crawljob");
                                }
                            })) {
                                try {
                                    addCrawlJob(f);

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

        } finally {
            if (CFG_FOLDER_WATCH.CFG.isDebugEnabled()) {
                getLogger().info("DONE");
            }
        }
    }

    private void move(File f) {
        if (CFG_FOLDER_WATCH.CFG.isDebugEnabled()) {
            getLogger().info("Move " + f);
        }
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
        if (f.length() == 0) {
            if (CFG_FOLDER_WATCH.CFG.isDebugEnabled()) {
                getLogger().info("Ignore " + f);
            }
            return;
        }
        getLogger().info("Parse " + f);
        String str = IO.readFileToString(f);
        if (str.trim().startsWith("[")) {
            parseJson(f, str);
        } else {
            parseProperties(f, str);
        }
        move(f);
    }

    private void parseProperties(File f, String str) {
        try {
            final ClassCache cc = ClassCache.getClassCache(CrawlJobStorable.class);
            CrawlJobStorable entry = null;
            final HashSet<String> entryDelimiter = new HashSet<String>();
            parserLoop: for (String line : Regex.getLines(str)) {
                line = line.trim();
                if (line.startsWith("#")) {
                    /* comment line */
                    continue parserLoop;
                }
                final int i = line.indexOf("=");
                if (i <= 0) {
                    /* delimiter with two or more empty lines */
                    if (entry != null && StringUtils.isNotEmpty(entry.getText())) {
                        addJob(f, entry);
                    }
                    entry = null;
                    entryDelimiter.clear();
                    continue parserLoop;
                }
                final String key = line.substring(0, i);
                if (entryDelimiter.contains(key)) {
                    /* artificial delimiter for single empty line(Regex.getLines removes linux \n\n), check for duplicated keys -> new entry */
                    if (entry != null && StringUtils.isNotEmpty(entry.getText())) {
                        addJob(f, entry);
                    }
                    entry = null;
                    entryDelimiter.clear();
                }
                entryDelimiter.add(key);
                final Setter setter = cc.getSetter(key);
                if (setter == null) {
                    getLogger().info("Unknown key: " + key);
                    continue parserLoop;
                }
                if (i + 1 >= line.length()) {
                    getLogger().info("Empty value for key: " + key);
                    continue parserLoop;
                }
                final String value = line.substring(i + 1).trim();
                if (entry == null) {
                    entry = (CrawlJobStorable) cc.getInstance();
                }
                set(setter, entry, value);
            }
            if (entry != null && StringUtils.isNotEmpty(entry.getText())) {
                /* last entry */
                addJob(f, entry);
            }
        } catch (Exception e) {
            getLogger().log(e);
        }
    }

    private void set(Setter setter, CrawlJobStorable entry, String value) {
        try {
            if (setter.getType() == BooleanStatus.class && "null".equalsIgnoreCase(value)) {
                value = "UNSET";
            }
            if (Clazz.isEnum(setter.getType())) {
                value = value.toUpperCase(Locale.ENGLISH);
                if (!value.startsWith("\"")) {
                    value = "\"" + value + "\"";
                }
            }
            if (Clazz.isString(setter.getType())) {
                try {
                    final String stringValue;
                    if (!value.startsWith("\"")) {
                        /** needed as spaces(eg "a b"->"a") and 'l'(eg 1ltest"->"1") cause issues **/
                        stringValue = "\"" + value + "\"";
                    } else {
                        stringValue = value;
                    }
                    final Object setValue = JSonStorage.restoreFromString(stringValue, new TypeRef<Object>(setter.getType()) {

                    });
                    setter.setValue(entry, setValue);
                    return;
                } catch (JSonMapperException e) {
                    setter.setValue(entry, value);
                    return;
                }
            }
            final Object setValue = JSonStorage.restoreFromString(value, new TypeRef<Object>(setter.getType()) {

            });
            setter.setValue(entry, setValue);
        } catch (Throwable e) {
            getLogger().log(e);
        }
    }

    protected void parseJson(File f, String str) {
        for (final CrawlJobStorable j : JSonStorage.restoreFromString(str, new TypeRef<ArrayList<CrawlJobStorable>>() {
        })) {
            addJob(f, j);
        }
    }

    protected void addJob(File f, final CrawlJobStorable j) {
        getLogger().info("AddJob \r\n" + JSonStorage.toString(j));
        switch (j.getType()) {
        case NORMAL:
            final LinkCollectingJob job = new LinkCollectingJob(new LinkOriginDetails(LinkOrigin.EXTENSION, "FolderWatch:" + f.getAbsolutePath()), j.getText());
            job.setDeepAnalyse(j.isDeepAnalyseEnabled());

            final CrawledLinkModifier modifier = new CrawledLinkModifier() {

                private PackageInfo getPackageInfo(CrawledLink link, boolean createIfNotExisting) {
                    PackageInfo packageInfo = link.getDesiredPackageInfo();
                    if (packageInfo != null || createIfNotExisting == false) {
                        return packageInfo;
                    }
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
                            if (j.isOverwritePackagizerEnabled()) {
                                existing.setIgnoreVarious(true);
                            }
                            existing.setUniqueId(null);
                        }
                    }

                    if (!BooleanStatus.UNSET.equals(j.getExtractAfterDownload())) {
                        BooleanStatus existing = BooleanStatus.UNSET;
                        if (link.hasArchiveInfo()) {
                            existing = link.getArchiveInfo().getAutoExtract();
                        }
                        if (j.isOverwritePackagizerEnabled() || existing == null || BooleanStatus.UNSET.equals(existing)) {
                            link.getArchiveInfo().setAutoExtract(j.getExtractAfterDownload());
                        }
                    }
                    if (j.isOverwritePackagizerEnabled()) {
                        link.setPriority(j.getPriority());
                    }
                    DownloadLink dlLink = link.getDownloadLink();
                    if (dlLink != null) {
                        if (StringUtils.isNotEmpty(j.getComment())) {
                            if (j.isOverwritePackagizerEnabled() || StringUtils.isEmpty(dlLink.getComment())) {
                                dlLink.setComment(j.getComment());
                            }
                        }
                        if (StringUtils.isNotEmpty(j.getDownloadPassword())) {
                            if (j.isOverwritePackagizerEnabled() || StringUtils.isEmpty(dlLink.getDownloadPassword())) {
                                dlLink.setDownloadPassword(j.getDownloadPassword());
                            }
                        }
                    }
                    if (StringUtils.isNotEmpty(j.getDownloadFolder())) {
                        PackageInfo existing = getPackageInfo(link, false);
                        if (j.isOverwritePackagizerEnabled() || existing == null || StringUtils.isEmpty(existing.getDestinationFolder())) {
                            existing = getPackageInfo(link, true);
                            existing.setDestinationFolder(j.getDownloadFolder());
                            if (j.isOverwritePackagizerEnabled()) {
                                existing.setIgnoreVarious(true);
                            }
                            existing.setUniqueId(null);
                        }
                    }
                    if (j.getExtractPasswords() != null && j.getExtractPasswords().length > 0) {
                        HashSet<String> list = new HashSet<String>();
                        for (String s : j.getExtractPasswords()) {
                            list.add(s);
                        }
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
            };
            job.setCrawledLinkModifierPrePackagizer(modifier);
            if (j.isOverwritePackagizerEnabled()) {
                job.setCrawledLinkModifierPostPackagizer(modifier);
            }
            final LinkCrawler lc = LinkCollector.getInstance().addCrawlerJob(job);
            lc.waitForCrawling();
            if (lc.getCrawledLinksFoundCounter() == 0) {
                try {
                    final DownloadLink dl = new DownloadLink(HostPluginController.getInstance().get("directhttp").getPrototype(null), j.getText(), "folder.watch", j.getText(), false);
                    FilePackage fp = FilePackage.getInstance();
                    dl.setAvailableStatus(AvailableStatus.FALSE);
                    fp.setName("FolderWatch Errors");
                    // let the packagizer merge several packages that have the same name
                    fp.setProperty("ALLOW_MERGE", true);
                    fp.add(dl);
                    CrawledLink cl = new CrawledLink(dl);
                    cl.setName(j.getText());
                    switch (j.getAutoStart()) {
                    case FALSE:
                        cl.setAutoStartEnabled(false);
                        break;
                    case TRUE:
                        cl.setAutoStartEnabled(true);
                        break;
                    default:
                    }

                    switch (j.getAutoConfirm()) {
                    case FALSE:
                        cl.setAutoConfirmEnabled(false);
                        break;
                    case TRUE:
                        cl.setAutoConfirmEnabled(true);
                        break;
                    default:
                    }

                    LinkCollector.getInstance().addCrawledLink(cl);
                } catch (UpdateRequiredClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            break;
        }
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Long> keyHandler, Long invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Long> keyHandler, Long newValue) {
        synchronized (lock) {
            if (job != null) {
                job.cancel(false);
                job = null;
            }
            if (scheduler != null) {
                job = scheduler.scheduleAtFixedRate(this, 0, getSettings().getCheckInterval(), TimeUnit.MILLISECONDS);
            }
        }

    }
}