package org.jdownloader.controlling.packagizer;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.controlling.TaskQueue;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.PackagizerInterface;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.linkcrawler.PackageInfo;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.OnlineStatusFilter;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.OnlineStatusFilter.OnlineStatus;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.OnlineStatusFilter.OnlineStatusMatchtype;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.ConfigEvent;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.IO;
import org.appwork.utils.ModifyLock;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.EventSuppressor;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.controlling.FileCreationEvent;
import org.jdownloader.controlling.FileCreationListener;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.controlling.Priority;
import org.jdownloader.controlling.filter.RegexFilter;
import org.jdownloader.controlling.filter.RegexFilter.MatchType;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.BooleanStatus;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchive;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFile;
import org.jdownloader.jd1import.JD1Importer;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;

public class PackagizerController implements PackagizerInterface, FileCreationListener {
    private final PackagizerSettings              config;
    private volatile ArrayList<PackagizerRule>    list                  = new ArrayList<PackagizerRule>();
    private final PackagizerControllerEventSender eventSender;
    private volatile List<PackagizerRuleWrapper>  rules                 = null;
    public static final String                    ORGFILENAME           = "orgfilename";
    public static final String                    ORGFILENAMEWITHOUTEXT = "orgfilenamewithoutext";
    public static final String                    ORGFILETYPE           = "orgfiletype";
    public static final String                    HOSTER                = "hoster";
    public static final String                    SOURCE                = "source";
    public static final String                    ENV                   = "env";
    public static final String                    PACKAGENAME           = "packagename";
    public static final String                    SIMPLEDATE            = "simpledate";
    public static final String                    INDEXOF               = "indexof";
    private static final PackagizerController     INSTANCE              = new PackagizerController(false);
    public static final String                    ORGPACKAGENAME        = "orgpackagename";
    private HashMap<String, PackagizerReplacer>   replacers             = new HashMap<String, PackagizerReplacer>();
    private final boolean                         testInstance;
    private final KeyHandler<Object>              ruleListHandler;

    public static PackagizerController getInstance() {
        return INSTANCE;
    }

    public static PackagizerController createEmptyTestInstance() {
        return new PackagizerController(true);
    }

    public boolean isTestInstance() {
        return testInstance;
    }

    private ArrayList<PackagizerRule> readConfig() {
        ArrayList<PackagizerRule> list = new ArrayList<PackagizerRule>();
        if (config == null) {
            return list;
        } else {
            try {
                list = config.getRuleList();
            } catch (Throwable e) {
                // restoring list may fail.
                LogController.CL(false).log(e);
            }
            final int sizeLoaded = list != null ? list.size() : 0;
            list = addDefaultRules(importJD1(list));
            final int sizeNow = list != null ? list.size() : 0;
            if (sizeLoaded != sizeNow) {
                save(list);
            }
            return list;
        }
    }

    private ArrayList<PackagizerRule> addDefaultRules(ArrayList<PackagizerRule> list) {
        if (list == null) {
            list = new ArrayList<PackagizerRule>();
        }
        ArrayList<PackagizerRule> ret = new ArrayList<PackagizerRule>();
        HashSet<String> dupefinder = new HashSet<String>();
        DisableRevFilesPackageRule disableRevFilesRule = null;
        SubFolderByPackageRule subFolderByPackgeRule = null;
        SubFolderByPluginRule subFolderByPluginRule = null;
        DisableParFilesPackageRule disableParFilesRule = null;
        for (PackagizerRule rule : list) {
            final PackagizerRule clone = JSonStorage.restoreFromString(JSonStorage.serializeToJson(rule), new TypeRef<PackagizerRule>() {
            });
            clone.setCreated(-1);
            if (!dupefinder.add(JSonStorage.serializeToJson(clone))) {
                //
                continue;
            }
            if (SubFolderByPackageRule.ID.equals(rule.getId())) {
                if (dupefinder.add(rule.getId())) {
                    final SubFolderByPackageRule r = new SubFolderByPackageRule();
                    ret.add(r);
                    r.init();
                    r.setEnabled(rule.isEnabled());
                    subFolderByPackgeRule = r;
                }
            } else if (SubFolderByPluginRule.ID.equals(rule.getId())) {
                if (dupefinder.add(rule.getId())) {
                    final SubFolderByPluginRule r = new SubFolderByPluginRule();
                    ret.add(r);
                    r.init();
                    r.setEnabled(rule.isEnabled());
                    subFolderByPluginRule = r;
                }
            } else if (DisableRevFilesPackageRule.ID.equals(rule.getId())) {
                if (dupefinder.add(rule.getId())) {
                    final DisableRevFilesPackageRule r = new DisableRevFilesPackageRule();
                    ret.add(r);
                    r.init();
                    r.setEnabled(rule.isEnabled());
                    disableRevFilesRule = r;
                }
            } else if (DisableParFilesPackageRule.ID.equals(rule.getId())) {
                if (dupefinder.add(rule.getId())) {
                    final DisableParFilesPackageRule r = new DisableParFilesPackageRule();
                    ret.add(r);
                    r.init();
                    r.setEnabled(rule.isEnabled());
                    disableParFilesRule = r;
                }
            } else {
                ret.add(rule);
            }
        }
        if (disableRevFilesRule == null) {
            ret.add(disableRevFilesRule = new DisableRevFilesPackageRule());
            disableRevFilesRule.init();
        }
        if (disableParFilesRule == null) {
            ret.add(disableParFilesRule = new DisableParFilesPackageRule());
            disableParFilesRule.init();
        }
        if (subFolderByPackgeRule == null) {
            ret.add(subFolderByPackgeRule = new SubFolderByPackageRule());
            subFolderByPackgeRule.init();
        }
        if (subFolderByPluginRule == null) {
            ret.add(subFolderByPluginRule = new SubFolderByPluginRule());
            subFolderByPluginRule.init();
        }
        return ret;
    }

    private ArrayList<PackagizerRule> importJD1(ArrayList<PackagizerRule> list) {
        if (config != null && config.isTryJD1ImportEnabled()) {
            final JD1Importer jd1Importer = new JD1Importer();
            if (jd1Importer.isAvailable()) {
                config.setTryJD1ImportEnabled(false);
                try {
                    final Map<String, Object> data = jd1Importer.getHashMap("JD Package Customizer");
                    if (data != null) {
                        final ArrayList<HashMap<String, Object>> settings = JSonStorage.convert(data.get("SETTINGS"), new TypeRef<ArrayList<HashMap<String, Object>>>() {
                        });
                        if (settings != null) {
                            if (list == null) {
                                list = new ArrayList<PackagizerRule>();
                            }
                            for (HashMap<String, Object> map : settings) {
                                final PackagizerRule rule = new PackagizerRule();
                                final String regex = (String) map.get("regex");
                                final String password = (String) map.get("password");
                                final String name = (String) map.get("name");
                                final boolean extract = Boolean.TRUE.equals(map.get("extract"));
                                final boolean enabled = Boolean.TRUE.equals(map.get("enabled"));
                                final boolean useSubDirectory = Boolean.TRUE.equals(map.get("useSubDirectory"));
                                final int priority = ((Number) map.get("priority")).intValue();
                                String downloadDir = (String) map.get("downloadDir");
                                if (downloadDir == null) {
                                    downloadDir = "";
                                }
                                if (useSubDirectory) {
                                    downloadDir += File.separator + "<jd:packagename>";
                                }
                                rule.setName(name);
                                rule.setAutoExtractionEnabled(extract);
                                rule.setEnabled(enabled);
                                rule.setOnlineStatusFilter(new OnlineStatusFilter(OnlineStatusMatchtype.IS, true, OnlineStatus.ONLINE));
                                if (StringUtils.isNotEmpty(downloadDir)) {
                                    rule.setDownloadDestination(downloadDir);
                                }
                                if (StringUtils.isNotEmpty(password)) {
                                    ExtractionExtension.getInstance().addPassword(password);
                                }
                                rule.setPriority(Priority.getPriority(priority));
                                rule.setFilenameFilter(new RegexFilter(true, MatchType.EQUALS, regex, true));
                                list.add(rule);
                            }
                            save(list);
                        }
                    }
                } catch (final Throwable e) {
                    LogController.CL(false).log(e);
                }
            }
        }
        return list;
    }

    public PackagizerController(boolean testInstance) {
        this.testInstance = testInstance;
        eventSender = new PackagizerControllerEventSender();
        if (!isTestInstance()) {
            config = JsonConfig.create(PackagizerSettings.class);
            ruleListHandler = config._getStorageHandler().getKeyHandler("RuleList");
        } else {
            ruleListHandler = null;
            config = null;
        }
        if (!isTestInstance()) {
            list = readConfig();
        }
        update();
        if (!isTestInstance()) {
            ruleListHandler.getEventSender().addListener(new GenericConfigEventListener<Object>() {
                @Override
                public void onConfigValueModified(KeyHandler<Object> keyHandler, Object newValue) {
                    list = readConfig();
                    update();
                }

                @Override
                public void onConfigValidatorError(KeyHandler<Object> keyHandler, Object invalidValue, ValidationException validateException) {
                }
            });
            ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {
                @Override
                public void onShutdown(final ShutdownRequest shutdownRequest) {
                    save(list);
                }

                @Override
                public long getMaxDuration() {
                    return 0;
                }

                @Override
                public String toString() {
                    return "save packagizer...";
                }
            });
            FileCreationManager.getInstance().getEventSender().addListener(this);
        }
        addReplacer(new PackagizerReplacer() {
            public String getID() {
                return ENV;
            }

            public String replace(REPLACEVARIABLE replaceVariable, String modifiers, CrawledLink link, String input, PackagizerRuleWrapper lgr) {
                if (StringUtils.isNotEmpty(modifiers)) {
                    final String env = System.getenv(modifiers);
                    return Pattern.compile("<jd:env:" + modifiers + "/?>").matcher(input).replaceAll(Matcher.quoteReplacement(preprocessReplacement(replaceVariable, env)));
                }
                return input;
            }
        });
        addReplacer(new PackagizerReplacer() {
            public String getID() {
                return SIMPLEDATE;
            }

            public String replace(REPLACEVARIABLE replaceVariable, String modifiers, CrawledLink link, String input, PackagizerRuleWrapper lgr) {
                if (StringUtils.isNotEmpty(modifiers)) {
                    final String dateString = new SimpleDateFormat(modifiers).format(new Date());
                    return Pattern.compile("<jd:simpledate:" + modifiers + "/?>").matcher(input).replaceAll(Matcher.quoteReplacement(preprocessReplacement(replaceVariable, dateString)));
                }
                return input;
            }
        });
        addReplacer(new PackagizerReplacer() {
            public String getID() {
                return SOURCE;
            }

            public String replace(REPLACEVARIABLE replaceVariable, String modifiers, CrawledLink link, String input, PackagizerRuleWrapper lgr) {
                if (StringUtils.isEmpty(modifiers)) {
                    return input;
                }
                final int id = Integer.parseInt(modifiers);
                String output = input;
                // the i counter allows us to write regular expressions that address a certain line only.
                final String pattern = lgr.getSourceRule().getPattern().pattern();
                final boolean indexed = pattern.matches("^\\-?\\d+\\\\\\. .+");
                final boolean inverted = indexed && pattern.startsWith("-");
                final String[] sources;
                if (link.getSourceUrls() != null) {
                    sources = link.getSourceUrls();
                } else {
                    /* the first link never has sourceURLs */
                    sources = new String[2];
                    sources[0] = link.getURL();
                    LinkCollectingJob job = link.getSourceJob();
                    if (job != null) {
                        sources[1] = job.getCustomSourceUrl();
                    }
                }
                int i = 1;
                for (int j = inverted ? 0 : sources.length - 1; (inverted ? (j < sources.length) : (j >= 0)); j = (inverted ? (j + 1) : (j - 1))) {
                    final String s = sources[j];
                    if (s == null) {
                        continue;
                    }
                    final String toMatch = indexed ? (inverted ? "-" : "") + (i++) + ". " + s : s;
                    Regex regex = new Regex(toMatch, lgr.getSourceRule().getPattern());
                    String[] values = null;
                    if (regex.matches()) {
                        values = regex.getRow(0);
                    } else {
                        regex = new Regex(s, lgr.getSourceRule().getPattern());
                        if (regex.matches()) {
                            values = regex.getRow(0);
                        }
                    }
                    if (values != null && values.length > (id - 1)) {
                        final String value = Encoding.urlDecode(stringOrEmpty(values[id - 1]), false);
                        output = Pattern.compile("<jd:source:" + id + "\\s*/?\\s*>").matcher(output).replaceAll(Matcher.quoteReplacement(preprocessReplacement(replaceVariable, value)));
                    }
                }
                return output;
            }
        });
        addReplacer(new PackagizerReplacer() {
            private final Pattern pat = Pattern.compile("<jd:" + ORGFILENAME + "\\s*/?\\s*>");

            public String replace(REPLACEVARIABLE replaceVariable, String modifiers, CrawledLink link, String input, PackagizerRuleWrapper lgr) {
                if (StringUtils.isNotEmpty(modifiers)) {
                    final String rep = stringOrEmpty(new Regex(link.getName(), lgr.getFileNameRule().getPattern()).getMatch(Integer.parseInt(modifiers) - 1));
                    return Pattern.compile("<jd:" + ORGFILENAME + ":" + modifiers + "\\s*/?\\s*>").matcher(input).replaceAll(Matcher.quoteReplacement(preprocessReplacement(replaceVariable, rep)));
                }
                return pat.matcher(input).replaceAll(Matcher.quoteReplacement(preprocessReplacement(replaceVariable, link.getName())));
            }

            public String getID() {
                return ORGFILENAME;
            }
        });
        addReplacer(new PackagizerReplacer() {
            private final Pattern pat = Pattern.compile("<jd:" + ORGPACKAGENAME + "\\s*/?\\s*>");

            public String replace(REPLACEVARIABLE replaceVariable, String modifiers, CrawledLink link, String input, PackagizerRuleWrapper lgr) {
                String packagename = null;
                if (link != null) {
                    final CrawledPackage parentNode = link.getParentNode();
                    if (parentNode != null) {
                        packagename = parentNode.getName();
                    }
                    final PackageInfo dpi = link.getDesiredPackageInfo();
                    if (StringUtils.isEmpty(packagename) && dpi != null) {
                        packagename = dpi.getName();
                    }
                }
                if (StringUtils.isEmpty(packagename)) {
                    packagename = "";
                }
                if (StringUtils.isNotEmpty(modifiers)) {
                    final Pattern patt = lgr.getPackageNameRule().getPattern();
                    final String[] matches = new Regex(packagename, patt).getRow(0);
                    return Pattern.compile("<jd:" + ORGPACKAGENAME + ":" + modifiers + "\\s*/?\\s*>").matcher(input).replaceAll(Matcher.quoteReplacement(preprocessReplacement(replaceVariable, stringOrEmpty(matches[Integer.parseInt(modifiers) - 1]))));
                    //
                }
                return pat.matcher(input).replaceAll(Matcher.quoteReplacement(preprocessReplacement(replaceVariable, packagename)));
            }

            public String getID() {
                return ORGPACKAGENAME;
            }
        });
        addReplacer(new PackagizerReplacer() {
            private final Pattern pat = Pattern.compile("<jd:" + DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH + "/?>");

            public String replace(REPLACEVARIABLE replaceVariable, String modifiers, CrawledLink link, String input, PackagizerRuleWrapper lgr) {
                String subFolder = null;
                final DownloadLink dlLink = link.getDownloadLink();
                if (dlLink != null) {
                    Object subFolderByPlugin = dlLink.getProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH);
                    if (subFolderByPlugin != null && subFolderByPlugin instanceof String) {
                        final String pathParts[] = ((String) subFolderByPlugin).split("/");
                        final StringBuilder sb = new StringBuilder();
                        for (String pathPart : pathParts) {
                            if (sb.length() > 0) {
                                sb.append("/");
                            }
                            pathPart = preprocessReplacement(replaceVariable, pathPart);
                            if (StringUtils.isNotEmpty(pathPart)) {
                                sb.append(pathPart);
                            }
                        }
                        subFolder = sb.toString();
                        if (CrossSystem.isAbsolutePath(subFolder)) {
                            subFolder = null;
                        }
                    }
                }
                if (StringUtils.isEmpty(subFolder)) {
                    return pat.matcher(input).replaceAll(Matcher.quoteReplacement(""));
                } else {
                    return pat.matcher(input).replaceAll(Matcher.quoteReplacement(subFolder));
                }
            }

            public String getID() {
                return DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH;
            }
        });
        addReplacer(new PackagizerReplacer() {
            private final Pattern pat = Pattern.compile("<jd:orgfiletype/?>");

            public String replace(REPLACEVARIABLE replaceVariable, String modifiers, CrawledLink link, String input, PackagizerRuleWrapper lgr) {
                String fileType = new Regex(link.getName(), "\\.([0-9a-zA-Z]+)$").getMatch(0);
                if (fileType == null) {
                    fileType = "";
                }
                if (StringUtils.isNotEmpty(modifiers)) {
                    return Pattern.compile("<jd:orgfiletype:" + modifiers + "/?>").matcher(input).replaceAll(Matcher.quoteReplacement(preprocessReplacement(replaceVariable, stringOrEmpty(new Regex(fileType, lgr.getFileNameRule().getPattern()).getRow(0)[Integer.parseInt(modifiers) - 1]))));
                } else {
                    return pat.matcher(input).replaceAll(Matcher.quoteReplacement(preprocessReplacement(replaceVariable, fileType)));
                }
            }

            public String getID() {
                return ORGFILETYPE;
            }
        });
        addReplacer(new PackagizerReplacer() {
            private final Pattern pat = Pattern.compile("<jd:orgfilenamewithoutext/?>");

            public String replace(REPLACEVARIABLE replaceVariable, String modifiers, CrawledLink link, String input, PackagizerRuleWrapper lgr) {
                final String name = new Regex(link.getName(), "(.+)\\.[0-9a-zA-Z]+$").getMatch(0);
                if (name == null) {
                    return pat.matcher(input).replaceAll("");
                } else {
                    return pat.matcher(input).replaceAll(Matcher.quoteReplacement(preprocessReplacement(replaceVariable, name)));
                }
            }

            public String getID() {
                return ORGFILENAMEWITHOUTEXT;
            }
        });
        addReplacer(new PackagizerReplacer() {
            public String replace(REPLACEVARIABLE replaceVariable, String modifiers, CrawledLink link, String input, PackagizerRuleWrapper lgr) {
                if (StringUtils.isEmpty(modifiers)) {
                    return input;
                }
                final int id = Integer.parseInt(modifiers);
                final Regex regex = new Regex(link.getURL(), lgr.getHosterRule().getPattern());
                if (regex.matches()) {
                    final String[] values = regex.getRow(0);
                    return Pattern.compile("<jd:hoster:" + id + "/?>").matcher(input).replaceAll(Matcher.quoteReplacement(preprocessReplacement(replaceVariable, Encoding.urlDecode(stringOrEmpty(values[id - 1]), false))));
                }
                return input;
            }

            public String getID() {
                return HOSTER;
            }
        });
        addReplacer(new PackagizerReplacer() {
            public String replace(REPLACEVARIABLE replaceVariable, String modifiers, CrawledLink link, String input, PackagizerRuleWrapper lgr) {
                if (StringUtils.isEmpty(modifiers) || link.getDownloadLink() == null) {
                    return input;
                }
                final Object property = link.getDownloadLink().getProperty(modifiers);
                if (property == null || (!(property instanceof String) && !(property instanceof Number))) {
                    return Pattern.compile("<jd:prop:" + modifiers + "/?>").matcher(input).replaceAll("");
                } else {
                    return Pattern.compile("<jd:prop:" + modifiers + "/?>").matcher(input).replaceAll(Matcher.quoteReplacement(preprocessReplacement(replaceVariable, property.toString())));
                }
            }

            public String getID() {
                return "prop";
            }
        });
    }

    private String preprocessReplacement(REPLACEVARIABLE replaceVariable, final String string) {
        if (REPLACEVARIABLE.DIRECTORY.equals(replaceVariable)) {
            return CrossSystem.alleviatePathParts(string);
        } else {
            return string;
        }
    }

    private String stringOrEmpty(String input) {
        if (StringUtils.isEmpty(input)) {
            return "";
        } else {
            return input;
        }
    }

    private void addReplacer(PackagizerReplacer replacer) {
        this.replacers.put(replacer.getID().toLowerCase(Locale.ENGLISH), replacer);
    }

    public PackagizerControllerEventSender getEventSender() {
        return eventSender;
    }

    public java.util.List<PackagizerRule> list() {
        synchronized (this) {
            return new ArrayList<PackagizerRule>(list);
        }
    }

    public void add(PackagizerRule linkFilter) {
        if (linkFilter != null) {
            synchronized (this) {
                final HashSet<String> dupecheck = createDupeSet();
                if (!linkFilter.isStaticRule()) {
                    if (dupecheck.add(JSonStorage.serializeToJson(linkFilter))) {
                        list.add(linkFilter);
                    }
                }
                save(list);
            }
            update();
        }
    }

    public void setList(List<PackagizerRule> tableData) {
        if (tableData == null) {
            tableData = new ArrayList<PackagizerRule>();
        }
        synchronized (this) {
            list.clear();
            list.addAll(tableData);
            save(list);
        }
        update();
    }

    public void update() {
        if (isTestInstance()) {
            updateInternal();
        } else {
            TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {
                @Override
                protected Void run() throws RuntimeException {
                    updateInternal();
                    return null;
                }
            });
        }
    }

    private void updateInternal() {
        // url filter only require the urls, and thus can be done before linkcheck
        final ArrayList<PackagizerRuleWrapper> newRules = new ArrayList<PackagizerRuleWrapper>();
        synchronized (this) {
            for (final PackagizerRule lgr : list) {
                if (lgr.isEnabled() && lgr.isValid()) {
                    try {
                        final PackagizerRuleWrapper compiled = lgr.compile();
                        lgr._setBroken(false);
                        newRules.add(compiled);
                    } catch (final Throwable e) {
                        lgr.setEnabled(false);
                        lgr._setBroken(true);
                        LogController.CL().log(e);
                    }
                }
            }
            if (!isTestInstance()) {
                if (rules != null && newRules.size() != rules.size()) {
                    save(list);
                }
            }
        }
        rules = newRules;
        if (getEventSender().hasListener()) {
            getEventSender().fireEvent(new PackagizerControllerEvent() {
                @Override
                public void sendTo(PackagizerControllerListener listener) {
                    listener.onPackagizerUpdate();
                }
            });
        }
    }

    public void addAll(java.util.List<PackagizerRule> all) {
        if (all != null && all.size() > 0) {
            synchronized (this) {
                final HashSet<String> dupecheck = createDupeSet();
                for (PackagizerRule rule : all) {
                    if (!rule.isStaticRule()) {
                        if (dupecheck.add(JSonStorage.serializeToJson(rule))) {
                            list.add(rule);
                        }
                    }
                }
                save(list);
            }
            update();
        }
    }

    private HashSet<String> createDupeSet() {
        final HashSet<String> ret = new HashSet<String>();
        synchronized (this) {
            for (final PackagizerRule rule : list) {
                ret.add(JSonStorage.serializeToJson(rule));
            }
        }
        return ret;
    }

    public void remove(PackagizerRule lf) {
        if (lf != null) {
            synchronized (this) {
                list.remove(lf);
                save(list);
            }
            update();
        }
    }

    private synchronized final void save(ArrayList<PackagizerRule> rules) {
        if (config != null) {
            final EventSuppressor<ConfigEvent> eventSuppressor;
            if (ruleListHandler != null) {
                final Thread thread = Thread.currentThread();
                eventSuppressor = new EventSuppressor<ConfigEvent>() {
                    @Override
                    public boolean suppressEvent(ConfigEvent eventType) {
                        return Thread.currentThread() == thread;
                    }
                };
                ruleListHandler.getEventSender().addEventSuppressor(eventSuppressor);
            } else {
                eventSuppressor = null;
            }
            try {
                config.setRuleList(rules);
            } finally {
                if (ruleListHandler != null) {
                    ruleListHandler.getEventSender().removeEventSuppressor(eventSuppressor);
                }
            }
        }
    }

    public void runByFile(final CrawledLink link) {
        if (getEventSender().hasListener()) {
            getEventSender().fireEvent(new PackagizerControllerEvent() {
                @Override
                public void sendTo(PackagizerControllerListener listener) {
                    listener.onPackagizerRunAfterLinkcheck(link);
                }
            });
        }
        if (isTestInstance() == false && !org.jdownloader.settings.staticreferences.CFG_PACKAGIZER.PACKAGIZER_ENABLED.isEnabled()) {
            return;
        }
        applyPackagizerRules(link, rules, true);
    }

    public void runByUrl(final CrawledLink link) {
        if (getEventSender().hasListener()) {
            getEventSender().fireEvent(new PackagizerControllerEvent() {
                @Override
                public void sendTo(PackagizerControllerListener listener) {
                    listener.onPackagizerRunBeforeLinkcheck(link);
                }
            });
        }
        if (isTestInstance() == false && !org.jdownloader.settings.staticreferences.CFG_PACKAGIZER.PACKAGIZER_ENABLED.isEnabled()) {
            return;
        }
        applyPackagizerRules(link, rules, false);
    }

    private void applyPackagizerRules(final CrawledLink link, final List<PackagizerRuleWrapper> rules, final boolean afterOnlineCheck) {
        if (rules != null) {
            for (final PackagizerRuleWrapper lgr : rules) {
                if (lgr.getAlwaysFilter() == null || !lgr.getAlwaysFilter().isEnabled()) {
                    if (!lgr.checkHoster(link)) {
                        continue;
                    }
                    if (!lgr.checkPluginStatus(link)) {
                        continue;
                    }
                    if (!lgr.checkOrigin(link)) {
                        continue;
                    }
                    if (!lgr.checkConditions(link)) {
                        continue;
                    }
                    if (!lgr.checkSource(link)) {
                        continue;
                    }
                    if (!lgr.checkPackageName(link)) {
                        continue;
                    }
                    if (!lgr.checkFileType(link)) {
                        continue;
                    }
                    if (!lgr.checkOnlineStatus(link)) {
                        continue;
                    }
                    if (!lgr.checkFileName(link)) {
                        continue;
                    }
                    if (!lgr.checkFileSize(link)) {
                        continue;
                    }
                }
                set(link, lgr);
            }
        }
    }

    public static String PACKAGETAG = "<jd:" + PackagizerController.PACKAGENAME + ">";
    public static String DATETAG    = "<jd:" + PackagizerController.SIMPLEDATE + ":";
    public static String INDEXOFTAG = "<jd:" + PackagizerController.INDEXOF + ">";

    private final static int padLength(final int size) {
        if (size < 10) {
            return 1;
        } else if (size < 100) {
            return 2;
        } else if (size < 1000) {
            return 3;
        } else if (size < 10000) {
            return 4;
        } else if (size < 100000) {
            return 5;
        } else if (size < 1000000) {
            return 6;
        } else if (size < 10000000) {
            return 7;
        } else {
            return 8;// hello djmakinera
        }
    }

    public static String replaceDynamicTags(String input, String packageName, AbstractNode node) {
        if (StringUtils.isEmpty(input)) {
            return input;
        }
        String ret = input;
        if (ret.contains("<jd:")) {
            if (ret.contains(PACKAGETAG)) {
                if (StringUtils.isEmpty(packageName)) {
                    ret = ret.replace(PACKAGETAG, "");
                } else {
                    ret = ret.replace(PACKAGETAG, CrossSystem.alleviatePathParts(packageName));
                }
                ret = CrossSystem.fixPathSeparators(ret);
            }
            if (ret.contains(INDEXOFTAG)) {
                AbstractPackageNode parentNode = null;
                if (!(node instanceof AbstractPackageChildrenNode) || (parentNode = ((AbstractPackageChildrenNode<AbstractPackageNode>) node).getParentNode()) == null) {
                    ret = ret.replace(INDEXOFTAG, "");
                } else {
                    final ModifyLock modifyLock = parentNode.getModifyLock();
                    final int index;
                    final int size;
                    final boolean readL = modifyLock.readLock();
                    try {
                        index = parentNode.indexOf((AbstractPackageChildrenNode) node);
                        size = parentNode.getChildren().size();
                    } finally {
                        modifyLock.readUnlock(readL);
                    }
                    if (index >= 0) {
                        final String replacement = String.format(Locale.US, "%0" + padLength(size) + "d", index + 1);
                        ret = ret.replace(INDEXOFTAG, replacement);
                    } else {
                        ret = ret.replace(INDEXOFTAG, "");
                    }
                }
                ret = CrossSystem.fixPathSeparators(ret);
            }
            if (ret.contains(DATETAG)) {
                int start = ret.indexOf(DATETAG);
                int end = start + DATETAG.length();
                while (end < ret.length() && ret.charAt(end) != '>') {
                    end++;
                }
                try {
                    SimpleDateFormat format = new SimpleDateFormat(ret.substring(start + DATETAG.length(), end));
                    ret = ret.replace(ret.substring(start, end + 1), format.format(new Date()));
                } catch (Throwable e) {
                    ret = ret.replace(ret.substring(start, end + 1), "");
                }
            }
        }
        return ret.trim();
    }

    public static enum REPLACEVARIABLE {
        DIRECTORY,
        COMMENT,
        FILENAME,
        PACKAGENAME
    }

    protected void set(CrawledLink link, PackagizerRuleWrapper lgr) {
        if (SubFolderByPackageRule.ID.equals(lgr.getRule().getId()) && StringUtils.contains(CFG_GENERAL.DEFAULT_DOWNLOAD_FOLDER.getValue(), "<jd:")) {
            // ignore SubFolderByPackageRule when default folder already contains variables
            return;
        } else {
            boolean packageNameFlag = false;
            String packageNameValue = null;
            boolean packageDirectoryFlag = false;
            String packageDirectoryValue = null;
            boolean fileNameFlag = false;
            String fileNameValue = null;
            if (lgr.getRule().getChunks() >= 0) {
                /* customize chunk numbers */
                link.setChunks(lgr.getRule().getChunks());
            }
            if (!StringUtils.isEmpty(lgr.getRule().getDownloadDestination())) {
                /* customize download destination folder */
                packageDirectoryFlag = true;
                packageDirectoryValue = replaceVariables(REPLACEVARIABLE.DIRECTORY, lgr.getRule().getDownloadDestination(), link, lgr);
            }
            if (lgr.getRule().getLinkEnabled() != null) {
                link.setEnabled(lgr.getRule().getLinkEnabled());
            }
            if (!StringUtils.isEmpty(lgr.getRule().getPackageName())) {
                /* customize package name */
                packageNameFlag = true;
                packageNameValue = replaceVariables(REPLACEVARIABLE.PACKAGENAME, lgr.getRule().getPackageName(), link, lgr);
            }
            if (lgr.getRule().getPriority() != null) {
                /* customize priority */
                link.setPriority(lgr.getRule().getPriority());
            }
            if (!StringUtils.isEmpty(lgr.getRule().getFilename())) {
                /* customize filename */
                fileNameFlag = true;
                fileNameValue = replaceVariables(REPLACEVARIABLE.FILENAME, lgr.getRule().getFilename(), link, lgr);
            }
            if (fileNameFlag && StringUtils.isNotEmpty(fileNameValue)) {
                link.setName(fileNameValue);
            }
            if (packageNameFlag) {
                final PackageInfo dpi;
                if (link.getDesiredPackageInfo() != null) {
                    if (StringUtils.isNotEmpty(packageNameValue)) {
                        dpi = link.getDesiredPackageInfo();
                    } else {
                        dpi = null;
                    }
                } else {
                    if (StringUtils.isNotEmpty(packageNameValue)) {
                        dpi = new PackageInfo();
                    } else {
                        dpi = null;
                    }
                }
                if (dpi != null) {
                    dpi.setPackagizerRuleMatched(true);
                    dpi.setName(packageNameValue);
                    link.setDesiredPackageInfo(dpi);
                }
            }
            if (packageDirectoryFlag) {
                final PackageInfo dpi;
                if (link.getDesiredPackageInfo() != null) {
                    if (StringUtils.isNotEmpty(packageDirectoryValue)) {
                        dpi = link.getDesiredPackageInfo();
                    } else {
                        dpi = null;
                    }
                } else {
                    if (StringUtils.isNotEmpty(packageDirectoryValue)) {
                        dpi = new PackageInfo();
                    } else {
                        dpi = null;
                    }
                }
                if (dpi != null) {
                    dpi.setPackagizerRuleMatched(true);
                    dpi.setDestinationFolder(packageDirectoryValue);
                    link.setDesiredPackageInfo(dpi);
                }
            }
            if (!StringUtils.isEmpty(lgr.getRule().getComment())) {
                /* customize filename */
                link.setComment(replaceVariables(REPLACEVARIABLE.COMMENT, lgr.getRule().getComment(), link, lgr));
            }
            Boolean b = null;
            if ((b = lgr.getRule().isAutoExtractionEnabled()) != null) {
                /* customize auto extract */
                link.getArchiveInfo().setAutoExtract(b ? BooleanStatus.TRUE : BooleanStatus.FALSE);
            }
            if ((b = lgr.getRule().isAutoAddEnabled()) != null) {
                /* customize auto add */
                link.setAutoConfirmEnabled(b);
            }
            if ((b = lgr.getRule().isAutoStartEnabled()) != null) {
                /* customize auto start */
                link.setAutoStartEnabled(b);
            }
            if ((b = lgr.getRule().isAutoForcedStartEnabled()) != null) {
                /* customize auto start */
                link.setForcedAutoStartEnabled(b);
            }
        }
    }

    public String replaceVariables(final REPLACEVARIABLE replaceVariable, String txt, CrawledLink link, PackagizerRuleWrapper lgr) {
        final String[][] matches = new Regex(txt, "<jd:([^>:]+)(?::(.+?))?\\s*/?\\s*>").getMatches();
        if (matches != null) {
            for (String m[] : matches) {
                try {
                    final PackagizerReplacer replacer = replacers.get(m[0].toLowerCase(Locale.ENGLISH));
                    if (replacer != null) {
                        final String modifier;
                        if (StringUtils.isEmpty(m[1])) {
                            modifier = null;
                        } else {
                            modifier = m[1];
                        }
                        txt = replacer.replace(replaceVariable, modifier, link, txt, lgr);
                    }
                } catch (final Throwable e) {
                    LogController.CL(false).log(e);
                }
            }
        }
        return txt;
    }

    public void onNewFile(Object caller, File[] fileList) {
        if (org.jdownloader.settings.staticreferences.CFG_PACKAGIZER.PACKAGIZER_ENABLED.isEnabled()) {
            if (caller instanceof ExtractionController && caller != this) {
                if (((ExtractionController) caller).getArchive() instanceof DownloadLinkArchive) {
                    CrawledPackage crawledPackage = null;
                    for (final ArchiveFile af : ((ExtractionController) caller).getArchive().getArchiveFiles()) {
                        if (af instanceof DownloadLinkArchiveFile) {
                            for (final DownloadLink link : ((DownloadLinkArchiveFile) af).getDownloadLinks()) {
                                for (final File file : fileList) {
                                    if (file.exists()) {
                                        final CrawledLink cl = new CrawledLink(link) {
                                            @Override
                                            protected void passwordForward(DownloadLink dlLink) {
                                                /* not needed and used in constructor */
                                            }
                                        };
                                        cl.setName(file.getName());
                                        final ArrayList<String> sourceURLs = new ArrayList<String>();
                                        String url = link.getOriginUrl();
                                        if (url != null) {
                                            sourceURLs.add(url);
                                        }
                                        url = link.getReferrerUrl();
                                        if (url != null) {
                                            sourceURLs.add(url);
                                        }
                                        url = link.getContainerUrl();
                                        if (url != null) {
                                            sourceURLs.add(url);
                                        }
                                        url = link.getContentUrl();
                                        if (url != null) {
                                            sourceURLs.add(url);
                                        }
                                        url = link.getCustomUrl();
                                        if (url != null) {
                                            sourceURLs.add(url);
                                        }
                                        if (sourceURLs.size() > 0) {
                                            cl.setSourceUrls(sourceURLs.toArray(new String[0]));
                                        }
                                        final FilePackage filePackage = link.getLastValidFilePackage();
                                        if (filePackage != null) {
                                            crawledPackage = new CrawledPackage();
                                            crawledPackage.setName(filePackage.getName());
                                            crawledPackage.setDownloadFolder(filePackage.getDownloadDirectory());
                                            cl.setParentNode(crawledPackage);
                                        } else {
                                            cl.setParentNode(crawledPackage);
                                        }
                                        runAfterExtraction(file, cl);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private final static String ORGPACKAGETAG = "<jd:" + PackagizerController.ORGPACKAGENAME + ">";

    private void runAfterExtraction(File file, CrawledLink dummyLink) {
        final String originalFolder = file.getParent();
        String moveToFolder = originalFolder;
        final String originalFileName = dummyLink.getName();
        for (PackagizerRuleWrapper lgr : rules) {
            String renameRule = lgr.getRule().getRename();
            String moveRule = lgr.getRule().getMoveto();
            if (!StringUtils.isEmpty(renameRule) || !StringUtils.isEmpty(moveRule)) {
                if (lgr.getAlwaysFilter() == null || !lgr.getAlwaysFilter().isEnabled()) {
                    if (!lgr.checkHoster(dummyLink)) {
                        continue;
                    }
                    if (!lgr.checkPluginStatus(dummyLink)) {
                        continue;
                    }
                    if (!lgr.checkOrigin(dummyLink)) {
                        continue;
                    }
                    if (!lgr.checkConditions(dummyLink)) {
                        continue;
                    }
                    if (!lgr.checkSource(dummyLink)) {
                        continue;
                    }
                    if (!lgr.checkPackageName(dummyLink)) {
                        continue;
                    }
                    if (!lgr.checkFileType(dummyLink)) {
                        continue;
                    }
                    if (!lgr.checkFileName(dummyLink)) {
                        continue;
                    }
                    if (!lgr.checkFileSize(dummyLink)) {
                        continue;
                    }
                }
                if (!StringUtils.isEmpty(renameRule)) {
                    renameRule = renameRule.replace(PACKAGETAG, ORGPACKAGETAG);
                    dummyLink.setName(replaceVariables(REPLACEVARIABLE.FILENAME, renameRule, dummyLink, lgr));
                }
                if (!StringUtils.isEmpty(moveRule)) {
                    moveRule = moveRule.replace(PACKAGETAG, ORGPACKAGETAG);
                    moveToFolder = replaceVariables(REPLACEVARIABLE.DIRECTORY, moveRule, dummyLink, lgr);
                }
            }
        }
        if (!originalFolder.equals(moveToFolder) || !originalFileName.equals(dummyLink.getName())) {
            final File newFile = new File(moveToFolder, dummyLink.getName());
            if (newFile.getParentFile().exists() == false && FileCreationManager.getInstance().mkdir(newFile.getParentFile()) == false) {
                LogController.CL(false).warning("Packagizer could not create " + newFile.getParentFile());
                return;
            }
            boolean successful = false;
            if ((successful = file.renameTo(newFile)) == false) {
                final LogSource log = LogController.CL(false);
                log.warning("Packagizer rename failed " + file + " to" + newFile);
                try {
                    log.warning("Packagizer try copy " + file + " to" + newFile);
                    IO.copyFile(file, newFile);
                    FileCreationManager.getInstance().delete(file, null);
                    successful = true;
                } catch (final Throwable e) {
                    FileCreationManager.getInstance().delete(newFile, null);
                    log.warning("Packagizer could not move/rename " + file + " to" + newFile);
                }
            }
            if (successful) {
                LogController.CL(false).info("Packagizer moved/renamed " + file + " to " + newFile);
                FileCreationManager.getInstance().getEventSender().fireEvent(new FileCreationEvent(PackagizerController.this, FileCreationEvent.Type.NEW_FILES, new File[] { newFile }));
            }
        }
    }

    @Override
    public void onNewFolder(Object caller, File folder) {
    }
}
