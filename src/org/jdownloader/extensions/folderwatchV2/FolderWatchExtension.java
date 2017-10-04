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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkOrigin;
import jd.controlling.linkcollector.LinkOriginDetails;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledLinkModifier;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.linkcrawler.modifier.CommentModifier;
import jd.controlling.linkcrawler.modifier.DownloadFolderModifier;
import jd.controlling.linkcrawler.modifier.PackageNameModifier;
import jd.parser.html.HTMLParser;
import jd.plugins.AddonPanel;
import jd.plugins.ContainerStatus;
import jd.plugins.DownloadLink;
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
import org.appwork.utils.UniqueAlltimeID;
import org.appwork.utils.encoding.Base64;
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
import org.jdownloader.plugins.controller.container.ContainerPluginController;

public class FolderWatchExtension extends AbstractExtension<FolderWatchConfig, FolderWatchTranslation> implements MenuExtenderHandler, Runnable, GenericConfigEventListener<Long> {
    private FolderWatchConfigPanel                          configPanel;
    private final AtomicReference<ScheduledExecutorService> scheduler = new AtomicReference<ScheduledExecutorService>();
    private final AtomicReference<PluginsC>                 plugin    = new AtomicReference<PluginsC>();

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
        ContainerPluginController.getInstance().remove(plugin.getAndSet(null));
        if (!Application.isHeadless()) {
            MenuManagerMainToolbar.getInstance().unregisterExtender(this);
            MenuManagerMainmenu.getInstance().unregisterExtender(this);
        }
        stopScheduler();
    }

    private void stopScheduler() {
        final ScheduledExecutorService scheduler = this.scheduler.getAndSet(null);
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    private void startScheduler() {
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        this.scheduler.set(scheduler);
        scheduler.scheduleAtFixedRate(this, 0, getSettings().getCheckInterval(), TimeUnit.MILLISECONDS);
    }

    @Override
    protected void start() throws StartException {
        if (!Application.isHeadless()) {
            MenuManagerMainToolbar.getInstance().registerExtender(this);
            MenuManagerMainmenu.getInstance().registerExtender(this);
        }
        createDefaultFolder();
        CFG_FOLDER_WATCH.CHECK_INTERVAL.getEventSender().addListener(this, true);
        plugin.set(new CrawlerJobContainer());
        ContainerPluginController.getInstance().add(plugin.get());
        startScheduler();
    }

    // very primitive container support for .crawljob files. does add them as new job=bypass the original linkcrawler
    public static class CrawlerJobContainer extends PluginsC {
        public CrawlerJobContainer() {
            super("CrawlerJob", "file:/.+(\\.crawljob)$", "$Revision: 21176 $");
        }

        public CrawlerJobContainer newPluginInstance() {
            return new CrawlerJobContainer();
        }

        @Override
        public String[] encrypt(String plain) {
            return null;
        }

        @Override
        public boolean hideLinks() {
            return false;
        }

        private void parseProperties(final List<CrawledLink> results, String str) throws Exception {
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
                        addCrawlJobStorable(results, entry);
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
                    /*
                     * artificial delimiter for single empty line(Regex.getLines removes linux \n\n), check for duplicated keys -> new entry
                     */
                    if (entry != null && StringUtils.isNotEmpty(entry.getText())) {
                        addCrawlJobStorable(results, entry);
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
                addCrawlJobStorable(results, entry);
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
                logger.log(e);
            }
        }

        private void addCrawlJobStorable(final List<CrawledLink> results, final CrawlJobStorable crawlJob) {
            final ArrayList<CrawledLinkModifier> modifiers = new ArrayList<CrawledLinkModifier>();
            if (StringUtils.isNotEmpty(crawlJob.getPackageName())) {
                modifiers.add(new PackageNameModifier(crawlJob.getPackageName(), crawlJob.isOverwritePackagizerEnabled()));
            }
            if (crawlJob.getPriority() != null) {
                modifiers.add(new CrawledLinkModifier() {
                    @Override
                    public void modifyCrawledLink(CrawledLink link) {
                        link.setPriority(crawlJob.getPriority());
                    }
                });
            }
            if (BooleanStatus.isSet(crawlJob.getExtractAfterDownload())) {
                final BooleanStatus autoextract = crawlJob.getExtractAfterDownload();
                modifiers.add(new CrawledLinkModifier() {
                    @Override
                    public void modifyCrawledLink(CrawledLink link) {
                        link.getArchiveInfo().setAutoExtract(autoextract);
                    }
                });
            }
            if (StringUtils.isNotEmpty(crawlJob.getComment())) {
                modifiers.add(new CommentModifier(crawlJob.getComment()));
            }
            if (StringUtils.isNotEmpty(crawlJob.getDownloadPassword())) {
                modifiers.add(new CrawledLinkModifier() {
                    @Override
                    public void modifyCrawledLink(CrawledLink link) {
                        final DownloadLink dlLink = link.getDownloadLink();
                        if (dlLink != null && StringUtils.isEmpty(dlLink.getDownloadPassword())) {
                            dlLink.setDownloadPassword(crawlJob.getDownloadPassword());
                        }
                    }
                });
            }
            if (BooleanStatus.isSet(crawlJob.getAutoConfirm())) {
                final boolean autoconfirm = Boolean.TRUE.equals(crawlJob.getAutoConfirm().getBoolean());
                modifiers.add(new CrawledLinkModifier() {
                    @Override
                    public void modifyCrawledLink(CrawledLink link) {
                        link.setAutoConfirmEnabled(autoconfirm);
                    }
                });
            }
            if (BooleanStatus.isSet(crawlJob.getAutoStart())) {
                final boolean autostart = Boolean.TRUE.equals(crawlJob.getAutoStart().getBoolean());
                modifiers.add(new CrawledLinkModifier() {
                    @Override
                    public void modifyCrawledLink(CrawledLink link) {
                        link.setAutoStartEnabled(autostart);
                    }
                });
            }
            if (crawlJob.getChunks() > 0) {
                modifiers.add(new CrawledLinkModifier() {
                    @Override
                    public void modifyCrawledLink(CrawledLink link) {
                        link.setChunks(crawlJob.getChunks());
                    }
                });
            }
            if (StringUtils.isNotEmpty(crawlJob.getFilename())) {
                modifiers.add(new CrawledLinkModifier() {
                    @Override
                    public void modifyCrawledLink(CrawledLink link) {
                        link.setName(crawlJob.getFilename());
                    }
                });
            }
            if (BooleanStatus.isSet(crawlJob.getEnabled())) {
                final boolean enabled = Boolean.TRUE.equals(crawlJob.getEnabled().getBoolean());
                modifiers.add(new CrawledLinkModifier() {
                    @Override
                    public void modifyCrawledLink(CrawledLink link) {
                        link.setEnabled(enabled);
                    }
                });
            }
            if (StringUtils.isNotEmpty(crawlJob.getDownloadFolder())) {
                modifiers.add(new DownloadFolderModifier(crawlJob.getDownloadFolder(), crawlJob.isOverwritePackagizerEnabled()));
            }
            if (BooleanStatus.isSet(crawlJob.getForcedStart())) {
                modifiers.add(new CrawledLinkModifier() {
                    @Override
                    public void modifyCrawledLink(CrawledLink link) {
                        link.setForcedAutoStartEnabled(crawlJob.getForcedStart().getBoolean());
                    }
                });
            }
            if (crawlJob.getExtractPasswords() != null && crawlJob.getExtractPasswords().length > 0) {
                final List<String> list = new ArrayList<String>();
                for (final String s : crawlJob.getExtractPasswords()) {
                    if (!list.contains(s)) {
                        list.add(s);
                    }
                }
                modifiers.add(new CrawledLinkModifier() {
                    @Override
                    public void modifyCrawledLink(CrawledLink link) {
                        link.getArchiveInfo().getExtractionPasswords().addAll(list);
                    }
                });
            }
            final String jobIDentifier = "[folderwatch:" + UniqueAlltimeID.next() + "]";
            final CrawledLink currentLink = getCurrentLink();
            final LinkCollectingJob job = currentLink.getSourceJob();
            final CrawledLinkModifier jobModifier;
            if (modifiers.size() > 0) {
                jobModifier = new CrawledLinkModifier() {
                    private final boolean modify(CrawledLink link) {
                        CrawledLink source = link;
                        while (source != null) {
                            if (StringUtils.startsWithCaseInsensitive(source.getUrlLink(), jobIDentifier)) {
                                return true;
                            } else {
                                source = source.getSourceLink();
                            }
                        }
                        return false;
                    }

                    @Override
                    public void modifyCrawledLink(final CrawledLink link) {
                        if (modify(link)) {
                            for (final CrawledLinkModifier mod : modifiers) {
                                mod.modifyCrawledLink(link);
                            }
                        }
                    }
                };
            } else {
                jobModifier = null;
            }
            switch (crawlJob.getType()) {
            case NORMAL:
                final LinkCrawler lc = new LinkCrawler(false, false) {
                    private final CrawledLinkModifier crawledLinkModifier;
                    {
                        final LinkCollectingJob job;
                        if (jobModifier != null && currentLink != null && (job = currentLink.getSourceJob()) != null) {
                            if (crawlJob.isOverwritePackagizerEnabled()) {
                                job.addPostPackagizerModifier(jobModifier);
                            } else {
                                job.addPrePackagizerModifier(jobModifier);
                            }
                            crawledLinkModifier = null;
                        } else {
                            crawledLinkModifier = jobModifier;
                        }
                    }
                    private final Charset             UTF8 = Charset.forName("UTF-8");

                    @Override
                    protected CrawledLink crawledLinkFactorybyURL(CharSequence url) {
                        final CrawledLink ret = super.crawledLinkFactorybyURL(jobIDentifier + Base64.encodeToString(url.toString().getBytes(UTF8)));
                        ret.setCustomCrawledLinkModifier(crawledLinkModifier);
                        return ret;
                    }
                };
                final List<CrawledLink> ret = lc.find(null, crawlJob.getText(), null, crawlJob.isDeepAnalyseEnabled() != null ? crawlJob.isDeepAnalyseEnabled().booleanValue() : currentLink.isCrawlDeep() || job != null && job.isDeepAnalyse(), false);
                if (ret != null) {
                    results.addAll(ret);
                }
                break;
            default:
                break;
            }
        }

        @Override
        public ContainerStatus callDecryption(File file) throws Exception {
            final ContainerStatus cs = new ContainerStatus(file);
            try {
                final ArrayList<CrawledLink> results = new ArrayList<CrawledLink>();
                final String crawlJobContent = IO.readFileToString(file);
                if (crawlJobContent.trim().startsWith("[")) {
                    for (final CrawlJobStorable crawlJobStorable : JSonStorage.restoreFromString(crawlJobContent, new TypeRef<ArrayList<CrawlJobStorable>>() {
                    })) {
                        addCrawlJobStorable(results, crawlJobStorable);
                    }
                } else {
                    parseProperties(results, crawlJobContent);
                }
                cls = results;
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
        return CFG_FOLDER_WATCH.CFG.isDebugEnabled();
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
                                        final String uri = file.toURI().toString();
                                        for (final PluginsC pCon : ContainerPluginController.getInstance().list()) {
                                            if (pCon.canHandle(uri)) {
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

    private void move(final File file) {
        if (isDebugEnabled()) {
            getLogger().info("Move " + file);
        }
        final File destFolder = new File(file.getParentFile(), "added");
        destFolder.mkdirs();
        int i = 1;
        final String name = file.getName();
        File dst = new File(destFolder, name);
        while (dst.exists()) {
            dst = new File(destFolder, name + "." + i);
            i++;
        }
        file.renameTo(dst);
    }

    private void addContainerFile(final File file) {
        final LinkCollectingJob job = new LinkCollectingJob(LinkOriginDetails.getInstance(LinkOrigin.EXTENSION, "FolderWatch"), file.toURI().toString());
        final LinkCrawler lc = LinkCollector.getInstance().addCrawlerJob(job);
        lc.waitForCrawling();
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Long> keyHandler, Long invalidValue, ValidationException validateException) {
    }

    @Override
    public synchronized void onConfigValueModified(KeyHandler<Long> keyHandler, Long newValue) {
        stopScheduler();
        startScheduler();
    }
}