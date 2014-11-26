package org.jdownloader.controlling.packagizer;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.controlling.TaskQueue;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.PackagizerInterface;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.PackageInfo;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.OnlineStatusFilter;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.OnlineStatusFilter.OnlineStatus;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.OnlineStatusFilter.OnlineStatusMatchtype;
import jd.plugins.DownloadLink;

import org.appwork.exceptions.WTFException;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.predefined.changeevent.ChangeEvent;
import org.appwork.utils.event.predefined.changeevent.ChangeEventSender;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging.Log;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.controlling.FileCreationEvent;
import org.jdownloader.controlling.FileCreationListener;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.controlling.Priority;
import org.jdownloader.controlling.filter.NoDownloadLinkException;
import org.jdownloader.controlling.filter.RegexFilter;
import org.jdownloader.controlling.filter.RegexFilter.MatchType;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.BooleanStatus;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchive;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFile;
import org.jdownloader.jd1import.JD1Importer;

public class PackagizerController implements PackagizerInterface, FileCreationListener {
    private PackagizerSettings                    config;
    private ArrayList<PackagizerRule>             list;
    private ChangeEventSender                     eventSender;
    private java.util.List<PackagizerRuleWrapper> fileFilter;
    private java.util.List<PackagizerRuleWrapper> urlFilter;

    public static final String                    ORGFILENAME    = "orgfilename";
    public static final String                    ORGFILETYPE    = "orgfiletype";
    public static final String                    HOSTER         = "hoster";
    public static final String                    SOURCE         = "source";

    public static final String                    PACKAGENAME    = "packagename";
    public static final String                    SIMPLEDATE     = "simpledate";

    private static final PackagizerController     INSTANCE       = new PackagizerController(false);
    public static final String                    ORGPACKAGENAME = "orgpackagename";
    private HashMap<String, PackagizerReplacer>   replacers      = new HashMap<String, PackagizerReplacer>();
    private boolean                               testInstance   = false;

    public static PackagizerController getInstance() {
        return INSTANCE;
    }

    public static PackagizerController createEmptyTestInstance() {
        return new PackagizerController(true);
    }

    public boolean isTestInstance() {
        return testInstance;
    }

    public PackagizerController(boolean testInstance) {
        this.testInstance = testInstance;
        eventSender = new ChangeEventSender();
        if (!isTestInstance()) {
            config = JsonConfig.create(PackagizerSettings.class);

            try {
                list = config.getRuleList();
            } catch (Throwable e) {
                // restoring list may fail.
            }
            if (list == null) {
                list = new ArrayList<PackagizerRule>();
            }
            JD1Importer jd1Importer = new JD1Importer();

            if (config.isTryJD1ImportEnabled() && jd1Importer.isAvailable()) {
                config.setTryJD1ImportEnabled(false);
                Map<String, Object> data = jd1Importer.getHashMap("JD Package Customizer");
                if (data != null) {
                    ArrayList<HashMap<String, Object>> settings = JSonStorage.convert(data.get("SETTINGS"), new TypeRef<ArrayList<HashMap<String, Object>>>() {
                    });
                    data = null;
                    System.gc();
                    if (settings != null) {
                        for (HashMap<String, Object> map : settings) {
                            PackagizerRule rule = new PackagizerRule();
                            String regex = (String) map.get("regex");
                            String password = (String) map.get("password");
                            String name = (String) map.get("name");
                            boolean extract = Boolean.TRUE.equals(map.get("extract"));
                            boolean enabled = Boolean.TRUE.equals(map.get("enabled"));
                            boolean useSubDirectory = Boolean.TRUE.equals(map.get("useSubDirectory"));
                            int priority = ((Number) map.get("priority")).intValue();
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
                            switch (priority) {
                            case -1:
                                rule.setPriority(Priority.LOWER);
                                break;
                            case 0:
                                rule.setPriority(Priority.DEFAULT);
                                break;
                            case 1:
                                rule.setPriority(Priority.HIGH);
                                break;
                            case 2:
                                rule.setPriority(Priority.HIGHER);
                                break;
                            case 3:
                                rule.setPriority(Priority.HIGHEST);
                                break;
                            }

                            rule.setFilenameFilter(new RegexFilter(true, MatchType.EQUALS, regex, true));
                            list.add(rule);
                        }
                        config.setRuleList(list);
                    }

                }
            }
        }

        if (!isTestInstance()) {
            ArrayList<PackagizerRule> newList = new ArrayList<PackagizerRule>();
            HashSet<String> dupefinder = new HashSet<String>();

            boolean revFileRule = false;

            SubFolderByPackageRule subFolderByPackgeRule = null;
            for (PackagizerRule rule : list) {
                PackagizerRule clone = JSonStorage.restoreFromString(JSonStorage.serializeToJson(rule), new TypeRef<PackagizerRule>() {
                });
                clone.setCreated(-1);
                if (!dupefinder.add(JSonStorage.serializeToJson(clone))) {
                    //
                    continue;
                }
                if (SubFolderByPackageRule.ID.equals(rule.getId())) {
                    SubFolderByPackageRule r;
                    if (!dupefinder.add(rule.getId())) {
                        continue;
                    }
                    newList.add(r = new SubFolderByPackageRule());
                    r.init();
                    r.setEnabled(rule.isEnabled());
                    subFolderByPackgeRule = r;
                    continue;

                }
                if (DisableRevFilesPackageRule.ID.equals(rule.getId())) {
                    DisableRevFilesPackageRule r;
                    if (!dupefinder.add(rule.getId())) {
                        continue;
                    }
                    newList.add(r = new DisableRevFilesPackageRule());
                    r.init();
                    r.setEnabled(rule.isEnabled());
                    revFileRule = true;
                    continue;

                }

                newList.add(rule);
            }
            if (subFolderByPackgeRule == null) {
                newList.add(subFolderByPackgeRule = new SubFolderByPackageRule());
                subFolderByPackgeRule.init();

            }
            if (!revFileRule) {
                DisableRevFilesPackageRule dis;
                newList.add(dis = new DisableRevFilesPackageRule());
                dis.init();
            }
            list = newList;
        }
        update();

        if (!isTestInstance()) {
            ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

                @Override
                public void onShutdown(final ShutdownRequest shutdownRequest) {
                    synchronized (PackagizerController.this) {
                        if (config != null) {
                            // System.out.println(JSonStorage.serializeToJson(list));
                            config.setRuleList(list);
                        }
                    }
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
                return SIMPLEDATE;
            }

            public String replace(String modifiers, CrawledLink link, String input, PackagizerRuleWrapper lgr) {
                if (modifiers == null) {
                    return input;
                }
                String format = modifiers;
                String dateString = new SimpleDateFormat(format).format(new Date());
                return Pattern.compile("<jd:simpledate:" + format + "/?>").matcher(input).replaceAll(Matcher.quoteReplacement(dateString));
            }

        });
        addReplacer(new PackagizerReplacer() {

            public String getID() {
                return SOURCE;
            }

            public String replace(String modifiers, CrawledLink link, String input, PackagizerRuleWrapper lgr) {
                if (modifiers == null) {
                    return input;
                }
                int id = Integer.parseInt(modifiers);
                String[] sources = link.getSourceUrls();
                String txt = input;
                // the i counter allows us to write regular expressions that adress a certain line only.
                String pattern = lgr.getSourceRule().getPattern().pattern();
                boolean indexed = pattern.matches("^\\-?\\d+\\\\\\. .+");
                boolean inverted = pattern.startsWith("-");

                if (sources == null) {
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

                    String s = sources[j];
                    if (s == null) {
                        continue;
                    }
                    String toMatch = indexed ? (inverted ? "-" : "") + (i++) + ". " + s : s;

                    Regex regex = new Regex(toMatch, lgr.getSourceRule().getPattern());
                    if (regex.matches()) {
                        String[] values = regex.getRow(0);
                        if (values[id - 1] != null) {
                            txt = Pattern.compile("<jd:source:" + id + "/?>").matcher(txt).replaceAll(Matcher.quoteReplacement(values[id - 1]));
                        }
                    } else {
                        regex = new Regex(s, lgr.getSourceRule().getPattern());
                        if (regex.matches()) {
                            String[] values = regex.getRow(0);
                            if (values[id - 1] != null) {
                                txt = Pattern.compile("<jd:source:" + id + "/?>").matcher(txt).replaceAll(Matcher.quoteReplacement(values[id - 1]));
                            }
                        }
                    }

                }

                return txt;
            }

        });

        addReplacer(new PackagizerReplacer() {

            private Pattern pat = Pattern.compile("<jd:orgfilename/?>");

            public String replace(String modifiers, CrawledLink link, String input, PackagizerRuleWrapper lgr) {
                if (modifiers != null) {
                    String rep = new Regex(link.getName(), lgr.getFileNameRule().getPattern()).getRow(0)[Integer.parseInt(modifiers) - 1];
                    return Pattern.compile("<jd:orgfilename:" + modifiers + "/?>").matcher(input).replaceAll(Matcher.quoteReplacement(rep));
                }
                return pat.matcher(input).replaceAll(Matcher.quoteReplacement(link.getName()));
            }

            public String getID() {
                return ORGFILENAME;
            }

        });
        addReplacer(new PackagizerReplacer() {

            private Pattern pat = Pattern.compile("<jd:" + ORGPACKAGENAME + "/?>");

            public String replace(String modifiers, CrawledLink link, String input, PackagizerRuleWrapper lgr) {
                String packagename = null;
                if (link != null && link.getParentNode() != null) {
                    packagename = link.getParentNode().getName();
                }
                if (StringUtils.isEmpty(packagename) && link != null && link.getDesiredPackageInfo() != null) {
                    packagename = link.getDesiredPackageInfo().getName();
                }
                if (StringUtils.isEmpty(packagename)) {
                    return input;
                }
                if (modifiers != null) {
                    Pattern patt = lgr.getPackageNameRule().getPattern();
                    String[] matches = new Regex(packagename, patt).getRow(0);
                    return Pattern.compile("<jd:" + ORGPACKAGENAME + ":" + modifiers + "/?>").matcher(input).replaceAll(Matcher.quoteReplacement(matches[Integer.parseInt(modifiers) - 1]));
                    //
                }
                return pat.matcher(input).replaceAll(Matcher.quoteReplacement(packagename));
            }

            public String getID() {
                return ORGPACKAGENAME;
            }

        });
        addReplacer(new PackagizerReplacer() {

            private Pattern pat = Pattern.compile("<jd:orgfiletype/?>");

            public String replace(String modifiers, CrawledLink link, String input, PackagizerRuleWrapper lgr) {
                String name = link.getName();
                String fileType = new Regex(name, "\\.([0-9a-zA-Z]+)$").getMatch(0);
                if (fileType == null) {
                    fileType = "";
                }
                if (modifiers != null) {
                    return Pattern.compile("<jd:orgfiletype:" + modifiers + "/?>").matcher(input).replaceAll(Matcher.quoteReplacement(new Regex(fileType, lgr.getFileNameRule().getPattern()).getRow(0)[Integer.parseInt(modifiers) - 1]));
                }
                return pat.matcher(input).replaceAll(Matcher.quoteReplacement(fileType));
            }

            public String getID() {
                return ORGFILETYPE;
            }

        });

        addReplacer(new PackagizerReplacer() {

            public String replace(String modifiers, CrawledLink link, String input, PackagizerRuleWrapper lgr) {
                if (modifiers == null) {
                    return input;
                }
                int id = Integer.parseInt(modifiers);
                Regex regex = new Regex(link.getURL(), lgr.getHosterRule().getPattern());
                if (regex.matches()) {
                    String[] values = regex.getRow(0);
                    return Pattern.compile("<jd:hoster:" + id + "/?>").matcher(input).replaceAll(Matcher.quoteReplacement(values[id - 1]));
                }
                return input;
            }

            public String getID() {
                return HOSTER;
            }

        });

        addReplacer(new PackagizerReplacer() {

            public String replace(String modifiers, CrawledLink link, String input, PackagizerRuleWrapper lgr) {
                if (modifiers == null) {
                    return input;
                }
                Object property = link.getDownloadLink().getProperty(modifiers);
                if (property == null || !(property instanceof String)) {
                    return Pattern.compile("<jd:prop:" + modifiers + "/?>").matcher(input).replaceAll("");
                } else {
                    return Pattern.compile("<jd:prop:" + modifiers + "/?>").matcher(input).replaceAll(Matcher.quoteReplacement(property.toString()));
                }
            }

            public String getID() {
                return "prop";
            }

        });
    }

    private void addReplacer(PackagizerReplacer replacer) {
        this.replacers.put(replacer.getID().toLowerCase(Locale.ENGLISH), replacer);
    }

    public ChangeEventSender getEventSender() {
        return eventSender;
    }

    public java.util.List<PackagizerRule> list() {
        synchronized (this) {
            return new ArrayList<PackagizerRule>(list);
        }
    }

    public void add(PackagizerRule linkFilter) {
        if (linkFilter == null) {
            return;
        }
        synchronized (this) {
            HashSet<String> dupecheck = createDupeSet();

            if (!linkFilter.isStaticRule()) {
                if (dupecheck.add(JSonStorage.serializeToJson(linkFilter))) {
                    list.add(linkFilter);
                }
            }

            if (config != null) {
                config.setRuleList(list);
            }
        }
        update();
    }

    public void setList(java.util.List<PackagizerRule> tableData) {
        synchronized (this) {
            list.clear();
            list.addAll(tableData);
            if (config != null) {
                config.setRuleList(list);
            }
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
        // url filter only require the urls, and thus can be done
        // brefore
        // linkcheck
        ArrayList<PackagizerRuleWrapper> urlFilter = new ArrayList<PackagizerRuleWrapper>();
        ArrayList<PackagizerRuleWrapper> fileFilter = new ArrayList<PackagizerRuleWrapper>();
        synchronized (this) {
            for (PackagizerRule lgr : list) {
                if (lgr.isEnabled() && lgr.isValid()) {
                    PackagizerRuleWrapper compiled = lgr.compile();
                    if (!compiled.isRequiresLinkcheck()) {
                        urlFilter.add(compiled);
                        fileFilter.add(compiled);
                    } else {
                        fileFilter.add(compiled);
                    }
                }
            }
        }
        fileFilter.trimToSize();
        this.fileFilter = fileFilter;
        urlFilter.trimToSize();
        this.urlFilter = urlFilter;
        getEventSender().fireEvent(new ChangeEvent(this));
    }

    public void addAll(java.util.List<PackagizerRule> all) {
        if (all == null) {
            return;
        }
        synchronized (this) {

            HashSet<String> dupecheck = createDupeSet();
            for (PackagizerRule rule : all) {
                if (!rule.isStaticRule()) {
                    if (dupecheck.add(JSonStorage.serializeToJson(rule))) {
                        list.add(rule);
                    }
                }
            }
            if (config != null) {
                config.setRuleList(list);
            }
        }
        update();
    }

    private HashSet<String> createDupeSet() {
        HashSet<String> ret = new HashSet<String>();
        synchronized (this) {
            for (PackagizerRule rule : list) {

                ret.add(JSonStorage.serializeToJson(rule));

            }

        }
        return ret;
    }

    public void remove(PackagizerRule lf) {
        if (lf == null) {
            return;
        }
        synchronized (this) {
            list.remove(lf);
            if (config != null) {
                config.setRuleList(list);
            }
        }
        update();
    }

    public void runByFile(CrawledLink link) {
        if (isTestInstance() == false && !org.jdownloader.settings.staticreferences.CFG_PACKAGIZER.PACKAGIZER_ENABLED.isEnabled()) {
            return;
        }
        java.util.List<PackagizerRuleWrapper> lfileFilter = fileFilter;
        for (PackagizerRuleWrapper lgr : lfileFilter) {
            if (lgr.getAlwaysFilter() == null || !lgr.getAlwaysFilter().isEnabled()) {
                try {
                    if (!lgr.checkHoster(link)) {
                        continue;
                    }
                } catch (NoDownloadLinkException e) {
                    throw new WTFException();
                }
                try {
                    if (!lgr.checkPluginStatus(link)) {
                        continue;
                    }
                } catch (NoDownloadLinkException e) {
                    throw new WTFException();
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
                if (lgr.isRequiresLinkcheck()) {
                    if (!lgr.checkOnlineStatus(link)) {
                        continue;
                    }
                    if (!lgr.checkFileName(link)) {
                        continue;
                    }

                    if (!lgr.checkFileSize(link)) {
                        continue;
                    }
                    if (!lgr.checkFileType(link)) {
                        continue;
                    }
                }
            }
            set(link, lgr);
        }

    }

    public void runByUrl(CrawledLink link) {
        if (isTestInstance() == false && !org.jdownloader.settings.staticreferences.CFG_PACKAGIZER.PACKAGIZER_ENABLED.isEnabled()) {
            return;
        }
        java.util.List<PackagizerRuleWrapper> lurlFilter = urlFilter;
        for (PackagizerRuleWrapper lgr : lurlFilter) {
            try {
                if (!lgr.checkHoster(link)) {
                    continue;
                }
            } catch (NoDownloadLinkException e) {
                continue;
            }

            try {
                if (!lgr.checkPluginStatus(link)) {
                    continue;
                }
            } catch (NoDownloadLinkException e) {
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
            set(link, lgr);
        }

    }

    public static String PACKAGETAG = "<jd:" + PackagizerController.PACKAGENAME + ">";
    public static String DATETAG    = "<jd:" + PackagizerController.SIMPLEDATE + ":";

    public static String replaceDynamicTags(String input, String packageName) {
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

    protected void set(CrawledLink link, PackagizerRuleWrapper lgr) {
        PackageInfo dpi = link.getDesiredPackageInfo();
        if (dpi == null) {
            dpi = new PackageInfo();
        }
        boolean dpiSet = false;
        if (lgr.getRule().getChunks() >= 0) {
            /* customize chunk numbers */
            link.setChunks(lgr.getRule().getChunks());
        }
        if (!StringUtils.isEmpty(lgr.getRule().getDownloadDestination())) {
            /* customize download destination folder */
            String path = replaceVariables(lgr.getRule().getDownloadDestination(), link, lgr);
            dpiSet = true;
            dpi.setDestinationFolder(path);
        }
        if (lgr.getRule().getLinkEnabled() != null) {
            link.setEnabled(lgr.getRule().getLinkEnabled());
        }
        if (!StringUtils.isEmpty(lgr.getRule().getPackageName())) {
            /* customize package name */
            String name = replaceVariables(lgr.getRule().getPackageName(), link, lgr);
            dpiSet = true;
            dpi.setName(name);
        }
        if (lgr.getRule().getPriority() != null) {
            /* customize priority */
            link.setPriority(lgr.getRule().getPriority());
        }
        if (!StringUtils.isEmpty(lgr.getRule().getFilename())) {
            /* customize filename */
            link.setName(replaceVariables(lgr.getRule().getFilename(), link, lgr));
        }
        if (!StringUtils.isEmpty(lgr.getRule().getComment())) {
            /* customize filename */
            link.setComment(replaceVariables(lgr.getRule().getComment(), link, lgr));
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
        if (dpiSet && link.getDesiredPackageInfo() == null) {
            /* set desiredpackageinfo if not set yet */
            link.setDesiredPackageInfo(dpi);
            dpi.setPackagizerRuleMatched(true);
        }
    }

    public String replaceVariables(String txt, CrawledLink link, PackagizerRuleWrapper lgr) {
        String[][] matches = new Regex(txt, "<jd:(.*?)(:(.+?))?>").getMatches();
        if (matches != null) {
            try {
                for (String m[] : matches) {
                    PackagizerReplacer replacer = replacers.get(m[0].toLowerCase(Locale.ENGLISH));
                    if (replacer != null) {
                        txt = replacer.replace(m[2], link, txt, lgr);
                    }
                }
            } catch (final Throwable e) {
                Log.exception(e);
            }
        }
        return txt;
    }

    public void onNewFile(Object caller, File[] fileList) {
        if (!org.jdownloader.settings.staticreferences.CFG_PACKAGIZER.PACKAGIZER_ENABLED.isEnabled()) {
            return;
        }
        if (caller instanceof ExtractionController && caller != this) {
            if (((ExtractionController) caller).getArchiv() instanceof DownloadLinkArchive) {
                for (ArchiveFile af : ((ExtractionController) caller).getArchiv().getArchiveFiles()) {
                    if (af instanceof DownloadLinkArchiveFile) {
                        for (DownloadLink link : ((DownloadLinkArchiveFile) af).getDownloadLinks()) {
                            for (File f : fileList) {
                                if (f.exists()) {
                                    CrawledLink cl = new CrawledLink(link);
                                    cl.setName(f.getName());
                                    runAfterExtraction(f, cl);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void runAfterExtraction(File f, CrawledLink link) {
        java.util.List<PackagizerRuleWrapper> lfileFilter = fileFilter;
        String originalFolder = f.getParent();
        String moveToFolder = originalFolder;
        String originalFileName = link.getName();
        System.out.println(1);
        for (PackagizerRuleWrapper lgr : lfileFilter) {
            if (!StringUtils.isEmpty(lgr.getRule().getRename()) || !StringUtils.isEmpty(lgr.getRule().getMoveto())) {
                if (lgr.getAlwaysFilter() == null || !lgr.getAlwaysFilter().isEnabled()) {
                    try {
                        if (!lgr.checkHoster(link)) {
                            continue;
                        }
                    } catch (NoDownloadLinkException e) {
                        throw new WTFException();
                    }
                    try {
                        if (!lgr.checkPluginStatus(link)) {
                            continue;
                        }
                    } catch (NoDownloadLinkException e) {
                        throw new WTFException();
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
                    if (lgr.isRequiresLinkcheck()) {
                        if (!lgr.checkOnlineStatus(link)) {
                            continue;
                        }
                        if (!lgr.checkFileName(link)) {
                            continue;
                        }

                        if (!lgr.checkFileSize(link)) {
                            continue;
                        }
                        if (!lgr.checkFileType(link)) {
                            continue;
                        }
                    }
                }
                if (!StringUtils.isEmpty(lgr.getRule().getRename())) {
                    /* rename */
                    link.setName(replaceVariables(lgr.getRule().getRename(), link, lgr));
                }
                if (!StringUtils.isEmpty(lgr.getRule().getMoveto())) {
                    /* move */
                    moveToFolder = replaceVariables(lgr.getRule().getMoveto(), link, lgr);
                }
            }
        }
        if (!originalFolder.equals(moveToFolder) || !originalFileName.equals(link.getName())) {
            File newFile = new File(moveToFolder, link.getName());
            if (newFile.getParentFile().exists() == false && FileCreationManager.getInstance().mkdir(newFile.getParentFile()) == false) {
                Log.L.warning("Packagizer could not create " + newFile.getParentFile());
                return;
            }
            boolean successful = false;
            if ((successful = f.renameTo(newFile)) == false) {
                Log.L.warning("Packagizer rename failed " + f + " to" + newFile);
                try {
                    Log.L.warning("Packagizer try copy " + f + " to" + newFile);
                    IO.copyFile(f, newFile);
                    FileCreationManager.getInstance().delete(f, null);
                    successful = true;
                } catch (final Throwable e) {
                    FileCreationManager.getInstance().delete(newFile, null);
                    Log.L.warning("Packagizer could not move/rename " + f + " to" + newFile);
                }
            }
            if (successful) {
                Log.L.info("Packagizer moved/renamed " + f + " to " + newFile);
                FileCreationManager.getInstance().getEventSender().fireEvent(new FileCreationEvent(PackagizerController.this, FileCreationEvent.Type.NEW_FILES, new File[] { newFile }));
            }
        }
    }

}
