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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
import jd.parser.html.HTMLParser;
import jd.plugins.AddonPanel;
import jd.plugins.ContainerStatus;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.PluginsC;

import org.appwork.storage.JSonMapperException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.storage.simplejson.mapper.ClassCache;
import org.appwork.storage.simplejson.mapper.Setter;
import org.appwork.utils.Application;
import org.appwork.utils.Exceptions;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.reflection.Clazz;
import org.jdownloader.controlling.Priority;
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
import org.jdownloader.plugins.controller.container.ContainerPluginController;
import org.jdownloader.plugins.controller.host.HostPluginController;

public class FolderWatchExtension extends AbstractExtension<FolderWatchConfig, FolderWatchTranslation> implements MenuExtenderHandler, Runnable, GenericConfigEventListener<Long> {
    private FolderWatchConfigPanel   configPanel;
    private ScheduledExecutorService scheduler;
    private final Object             lock                      = new Object();
    private ScheduledFuture<?>       job                       = null;
    private boolean                  isDebugEnabled            = false;
    private PluginsC                 crawlerJobContainerPlugin = null;

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
        setTitle(T.title());
    }

    @Override
    public String getIconKey() {
        return IconKey.ICON_FOLDER_ADD;
    }

    @Override
    protected void stop() throws StopException {
        final PluginsC crawlerJobContainerPlugin = this.crawlerJobContainerPlugin;
        this.crawlerJobContainerPlugin = null;
        ContainerPluginController.getInstance().remove(crawlerJobContainerPlugin);
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
        isDebugEnabled = CFG_FOLDER_WATCH.CFG.isDebugEnabled();
        crawlerJobContainerPlugin = new CrawlerJobContainer(this);
        ContainerPluginController.getInstance().add(crawlerJobContainerPlugin);
    }

    // very primitive container support for .crawljob files. does add them as new job=bypass the original linkcrawler
    public static class CrawlerJobContainer extends PluginsC {
        private final FolderWatchExtension extension;

        public CrawlerJobContainer(FolderWatchExtension extension) {
            super("CrawlerJob", "file:/.+(\\.crawljob)$", "$Revision: 21176 $");
            this.extension = extension;
        }

        public CrawlerJobContainer newPluginInstance() {
            return new CrawlerJobContainer(extension);
        }

        @Override
        public String[] encrypt(String plain) {
            return null;
        }

        @Override
        public ContainerStatus callDecryption(File file) throws Exception {
            final ContainerStatus cs = new ContainerStatus(file);
            try {
                final String str = IO.readFileToString(file);
                if (str.trim().startsWith("[")) {
                    extension.parseJson(file, str);
                } else {
                    extension.parseProperties(file, str);
                }
                cls = new ArrayList<CrawledLink>();
                cs.setStatus(ContainerStatus.STATUS_FINISHED);
                return cs;
            } catch (final Exception e) {
                logger.log(e);
                cs.setStatus(ContainerStatus.STATUS_FAILED);
                return cs;
            }
        }
    }

    private boolean isDebugEnabled() {
        return isDebugEnabled;
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
        return T.description();
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
        final StringBuilder sb = new StringBuilder();
        sb.append("RUN");
        boolean log = false;
        try {
            String[] folders = getSettings().getFolders();
            if (folders != null) {
                for (String s : folders) {
                    if (s != null) {
                        File folder = new File(s);
                        if (!folder.isAbsolute()) {
                            folder = Application.getResource(s);
                        }
                        if (folder.exists() && folder.isDirectory()) {
                            sb.append("Scan " + s + " - " + folder.getAbsolutePath()).append("\r\n");
                            final File[] files = folder.listFiles();
                            if (files != null && files.length > 0) {
                                sb.append(Arrays.asList(files).toString()).append("\r\n");
                                for (final File file : files) {
                                    if (file.isFile() && file.length() > 0) {
                                        final String name = file.getName();
                                        if (StringUtils.endsWithCaseInsensitive(name, ".crawljob")) {
                                            log = true;
                                            try {
                                                addCrawlJob(file);
                                            } catch (Exception e) {
                                                log = true;
                                                sb.append(Exceptions.getStackTrace(e)).append("\r\n");
                                            } finally {
                                                move(file);
                                            }
                                        } else {
                                            for (final PluginsC pCon : ContainerPluginController.getInstance().list()) {
                                                if (pCon.canHandle(file.toURI().toString())) {
                                                    log = true;
                                                    try {
                                                        addContainerFile(file);
                                                        break;
                                                    } catch (Exception e) {
                                                        log = true;
                                                        sb.append(Exceptions.getStackTrace(e)).append("\r\n");
                                                    } finally {
                                                        move(file);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log = true;
            sb.append(Exceptions.getStackTrace(e)).append("\r\n");
        } finally {
            sb.append("DONE");
            if (isDebugEnabled() && log) {
                getLogger().info(sb.toString());
            }
        }
    }

    private void move(File f) {
        if (isDebugEnabled()) {
            getLogger().info("Move " + f);
        }
        final File dir = new File(f.getParentFile(), "added");
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
            if (isDebugEnabled()) {
                getLogger().info("Ignore " + f);
            }
        } else {
            getLogger().info("Parse " + f);
            final String str = IO.readFileToString(f);
            if (str.trim().startsWith("[")) {
                parseJson(f, str);
            } else {
                parseProperties(f, str);
            }
        }
    }

    private void addContainerFile(final File file) {
        final LinkCollectingJob job = new LinkCollectingJob(LinkOriginDetails.getInstance(LinkOrigin.EXTENSION, "FolderWatch:" + file.getAbsolutePath()), file.toURI().toString());
        final LinkCrawler lc = LinkCollector.getInstance().addCrawlerJob(job);
        lc.waitForCrawling();
    }

    private void parseProperties(File f, String str) {
        try {
            final ClassCache cc = ClassCache.getClassCache(CrawlJobStorable.class);
            CrawlJobStorable entry = null;
            final HashSet<String> entryDelimiter = new HashSet<String>();
            StringBuilder restText = null;
            final StringBuilder rawFile = new StringBuilder();
            parserLoop: for (String line : Regex.getLines(str)) {
                line = line.trim();
                if (restText != null) {
                    if (StringUtils.isNotEmpty(line)) {
                        restText.append(line);
                        restText.append("\r\n");
                    }
                    continue parserLoop;
                }
                if (line.startsWith("#")) {
                    /* comment line */
                    continue parserLoop;
                } else if (HTMLParser.getProtocol(line) != null) {
                    rawFile.append(line);
                    rawFile.append("\r\n");
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
                final String key = line.substring(0, i).trim();
                if (StringUtils.equalsIgnoreCase(key, "resttext")) {
                    /* special key, all lines (including current one) will go to CrawlJobStorable.setText */
                    restText = new StringBuilder();
                    final String value = line.substring(i + 1).trim();
                    if (StringUtils.isNotEmpty(value)) {
                        restText.append(value);
                        restText.append("\r\n");
                    }
                    continue parserLoop;
                }
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
            if (restText != null && restText.length() > 0) {
                if (entry == null) {
                    entry = (CrawlJobStorable) cc.getInstance();
                }
                if (entry.getText() != null) {
                    restText.append(entry.getText());
                }
                entry.setText(restText.toString());
            }
            if (rawFile.length() > 0) {
                if (entry == null) {
                    entry = (CrawlJobStorable) cc.getInstance();
                }
                if (entry.getText() != null) {
                    rawFile.append(entry.getText());
                }
                entry.setText(rawFile.toString());
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
            if (setter.getType() == BooleanStatus.class && (value == null || "null".equalsIgnoreCase(value))) {
                value = "UNSET";
            } else if (setter.getType() == boolean.class) {
                if ("true".equalsIgnoreCase(value)) {
                    value = "true";
                } else {
                    value = "false";
                }
            } else if (setter.getType() == Boolean.class) {
                if ("true".equalsIgnoreCase(value)) {
                    value = "true";
                } else if ("false".equalsIgnoreCase(value)) {
                    value = "false";
                } else if (value == null || "null".equalsIgnoreCase(value)) {
                    value = null;
                }
            } else if (Clazz.isEnum(setter.getType())) {
                if (value != null) {
                    value = value.toUpperCase(Locale.ENGLISH);
                    if (!value.startsWith("\"")) {
                        value = "\"" + value + "\"";
                    }
                }
            } else if (Clazz.isString(setter.getType())) {
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
            final LinkCollectingJob job = new LinkCollectingJob(LinkOriginDetails.getInstance(LinkOrigin.EXTENSION, "FolderWatch:" + f.getAbsolutePath()), j.getText());
            job.setDeepAnalyse(j.isDeepAnalyseEnabled());
            final CrawledLinkModifier modifier = new CrawledLinkModifier() {
                @Override
                public void modifyCrawledLink(CrawledLink link) {
                    if (StringUtils.isNotEmpty(j.getPackageName())) {
                        PackageInfo existing = link.getDesiredPackageInfo();
                        if (j.isOverwritePackagizerEnabled() || existing == null || StringUtils.isEmpty(existing.getName())) {
                            if (existing == null) {
                                existing = new PackageInfo();
                            }
                            existing.setName(j.getPackageName());
                            if (j.isOverwritePackagizerEnabled()) {
                                existing.setIgnoreVarious(true);
                            }
                            existing.setUniqueId(null);
                            link.setDesiredPackageInfo(existing);
                        }
                    }
                    final BooleanStatus extract = j.getExtractAfterDownload();
                    if (extract != null && extract != BooleanStatus.UNSET) {
                        link.getArchiveInfo().setAutoExtract(extract);
                    }
                    final Priority priority = j.getPriority();
                    if (priority != null) {
                        link.setPriority(j.getPriority());
                    }
                    final DownloadLink dlLink = link.getDownloadLink();
                    if (dlLink != null) {
                        if (StringUtils.isNotEmpty(j.getComment())) {
                            if (StringUtils.isEmpty(dlLink.getComment())) {
                                dlLink.setComment(j.getComment());
                            }
                        }
                        if (StringUtils.isNotEmpty(j.getDownloadPassword())) {
                            if (StringUtils.isEmpty(dlLink.getDownloadPassword())) {
                                dlLink.setDownloadPassword(j.getDownloadPassword());
                            }
                        }
                    }
                    if (StringUtils.isNotEmpty(j.getDownloadFolder())) {
                        PackageInfo existing = link.getDesiredPackageInfo();
                        if (j.isOverwritePackagizerEnabled() || existing == null || StringUtils.isEmpty(existing.getDestinationFolder())) {
                            if (existing == null) {
                                existing = new PackageInfo();
                            }
                            existing.setDestinationFolder(j.getDownloadFolder());
                            if (j.isOverwritePackagizerEnabled()) {
                                existing.setIgnoreVarious(true);
                            }
                            existing.setUniqueId(null);
                            link.setDesiredPackageInfo(existing);
                        }
                    }
                    if (j.getExtractPasswords() != null && j.getExtractPasswords().length > 0) {
                        final LinkedHashSet<String> list = new LinkedHashSet<String>();
                        for (String s : j.getExtractPasswords()) {
                            list.add(s);
                        }
                        link.getArchiveInfo().getExtractionPasswords().addAll(list);
                    }
                    final BooleanStatus autoConfirm = j.getAutoConfirm();
                    if (autoConfirm != null && autoConfirm != BooleanStatus.UNSET) {
                        link.setAutoConfirmEnabled(autoConfirm.getBoolean());
                    }
                    final BooleanStatus autoStart = j.getAutoStart();
                    if (autoStart != null && autoStart != BooleanStatus.UNSET) {
                        link.setAutoStartEnabled(autoStart.getBoolean());
                    }
                    final BooleanStatus forcedStart = j.getForcedStart();
                    if (forcedStart != null && forcedStart != BooleanStatus.UNSET) {
                        link.setForcedAutoStartEnabled(forcedStart.getBoolean());
                    }
                    if (j.getChunks() > 0) {
                        link.setChunks(j.getChunks());
                    }
                    if (StringUtils.isNotEmpty(j.getFilename())) {
                        link.setName(j.getFilename());
                    }
                    final BooleanStatus enabled = j.getEnabled();
                    if (enabled != null && enabled != BooleanStatus.UNSET) {
                        link.setEnabled(enabled.getBoolean());
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
                    final FilePackage fp = FilePackage.getInstance();
                    dl.setAvailableStatus(AvailableStatus.FALSE);
                    fp.setName("FolderWatch Errors");
                    // let the packagizer merge several packages that have the same name
                    fp.setProperty("ALLOW_MERGE", true);
                    fp.add(dl);
                    final CrawledLink cl = new CrawledLink(dl);
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