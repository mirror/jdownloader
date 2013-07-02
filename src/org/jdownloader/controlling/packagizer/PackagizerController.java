package org.jdownloader.controlling.packagizer;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Pattern;

import jd.controlling.IOEQ;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.linkcollector.PackagizerInterface;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.PackageInfo;
import jd.plugins.DownloadLink;

import org.appwork.exceptions.WTFException;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.predefined.changeevent.ChangeEvent;
import org.appwork.utils.event.predefined.changeevent.ChangeEventSender;
import org.appwork.utils.logging.Log;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.controlling.FileCreationEvent;
import org.jdownloader.controlling.FileCreationListener;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.controlling.filter.NoDownloadLinkException;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.ArchiveSettings.BooleanStatus;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchive;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFile;

public class PackagizerController implements PackagizerInterface, FileCreationListener {
    private PackagizerSettings                    config;
    private ArrayList<PackagizerRule>             list;
    private ChangeEventSender                     eventSender;
    private java.util.List<PackagizerRuleWrapper> fileFilter;
    private java.util.List<PackagizerRuleWrapper> urlFilter;

    public static final String                    ORGFILENAME  = "orgfilename";
    public static final String                    ORGFILETYPE  = "orgfiletype";
    public static final String                    HOSTER       = "hoster";
    public static final String                    SOURCE       = "source";

    public static final String                    PACKAGENAME  = "packagename";
    public static final String                    SIMPLEDATE   = "simpledate";

    private static final PackagizerController     INSTANCE     = new PackagizerController(false);
    private HashMap<String, PackagizerReplacer>   replacers    = new HashMap<String, PackagizerReplacer>();
    private boolean                               testInstance = false;

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
        }
        if (list == null) list = new ArrayList<PackagizerRule>();

        ArrayList<PackagizerRule> newList = new ArrayList<PackagizerRule>();
        boolean subfolderFound = false;
        boolean revFileRule = false;
        for (PackagizerRule rule : list) {

            if (SubFolderByPackageRule.ID.equals(rule.getId())) {
                newList.add(new SubFolderByPackageRule());
                subfolderFound = true;
                break;

            }
            if (DisableRevFilesPackageRule.ID.equals(rule.getId())) {
                newList.add(new DisableRevFilesPackageRule());
                revFileRule = true;
                break;

            }
            newList.add(rule);
        }
        if (!subfolderFound) {
            newList.add(new SubFolderByPackageRule());
        }
        if (!revFileRule) {
            newList.add(new DisableRevFilesPackageRule());
        }
        list = newList;
        update();

        if (!isTestInstance()) {
            ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

                @Override
                public void onShutdown(final ShutdownRequest shutdownRequest) {
                    synchronized (PackagizerController.this) {
                        if (config != null) config.setRuleList(list);
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
                if (modifiers == null) return input;
                String format = modifiers;
                String dateString = new SimpleDateFormat(format).format(new Date());
                return Pattern.compile("<jd:simpledate:" + format + "/?>").matcher(input).replaceAll(dateString);
            }

        });
        addReplacer(new PackagizerReplacer() {

            public String getID() {
                return SOURCE;
            }

            public String replace(String modifiers, CrawledLink link, String input, PackagizerRuleWrapper lgr) {
                if (modifiers == null) return input;
                int id = Integer.parseInt(modifiers);
                CrawledLink src = link;
                String txt = input;
                while ((src = src.getSourceLink()) != null) {
                    Regex regex = new Regex(src.getURL(), lgr.getSourceRule().getPattern());
                    if (regex.matches()) {
                        String[] values = regex.getRow(0);
                        txt = Pattern.compile("<jd:source:" + id + "/?>").matcher(txt).replaceAll(values[id - 1]);
                    }
                }
                return txt;
            }

        });

        addReplacer(new PackagizerReplacer() {

            private Pattern pat = Pattern.compile("<jd:orgfilename/?>");

            public String replace(String modifiers, CrawledLink link, String input, PackagizerRuleWrapper lgr) {
                if (modifiers != null) { return Pattern.compile("<jd:orgfilename:" + modifiers + "/?>").matcher(input).replaceAll(new Regex(link.getName(), lgr.getFileNameRule().getPattern()).getRow(0)[Integer.parseInt(modifiers) - 1]); }
                return pat.matcher(input).replaceAll(link.getName());
            }

            public String getID() {
                return ORGFILENAME;
            }

        });

        addReplacer(new PackagizerReplacer() {

            private Pattern pat = Pattern.compile("<jd:orgfiletype/?>");

            public String replace(String modifiers, CrawledLink link, String input, PackagizerRuleWrapper lgr) {
                String fileType = new Regex(link.getName(), "\\.([0-9a-zA-Z]+)$").getMatch(0);

                if (modifiers != null) { return Pattern.compile("<jd:orgfiletype:" + modifiers + "/?>").matcher(input).replaceAll(new Regex(fileType, lgr.getFileNameRule().getPattern()).getRow(0)[Integer.parseInt(modifiers) - 1]); }
                return pat.matcher(input).replaceAll(fileType);
            }

            public String getID() {
                return ORGFILETYPE;
            }

        });

        addReplacer(new PackagizerReplacer() {

            public String replace(String modifiers, CrawledLink link, String input, PackagizerRuleWrapper lgr) {
                if (modifiers == null) return input;
                int id = Integer.parseInt(modifiers);
                Regex regex = new Regex(link.getURL(), lgr.getHosterRule().getPattern());
                if (regex.matches()) {
                    String[] values = regex.getRow(0);
                    return Pattern.compile("<jd:hoster:" + id + "/?>").matcher(input).replaceAll(values[id - 1]);
                }
                return input;
            }

            public String getID() {
                return HOSTER;
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
        if (linkFilter == null) return;
        synchronized (this) {
            list.add(linkFilter);
            if (config != null) config.setRuleList(list);
            update();
        }

    }

    public void setList(java.util.List<PackagizerRule> tableData) {
        synchronized (this) {
            list.clear();
            list.addAll(tableData);
            if (config != null) config.setRuleList(list);

            update();
        }
    }

    public void update() {
        if (isTestInstance()) {
            updateInternal();
        } else {
            IOEQ.add(new Runnable() {

                public void run() {
                    updateInternal();
                }

            }, true);
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
        if (all == null) return;
        synchronized (this) {
            list.addAll(all);
            if (config != null) config.setRuleList(list);
            update();
        }

    }

    public void remove(PackagizerRule lf) {
        if (lf == null) return;
        synchronized (this) {
            list.remove(lf);
            if (config != null) config.setRuleList(list);
            update();
        }

    }

    public void runByFile(CrawledLink link) {
        if (isTestInstance() == false && !org.jdownloader.settings.staticreferences.CFG_PACKAGIZER.PACKAGIZER_ENABLED.isEnabled()) return;
        java.util.List<PackagizerRuleWrapper> lfileFilter = fileFilter;
        for (PackagizerRuleWrapper lgr : lfileFilter) {
            if (lgr.getAlwaysFilter() == null || !lgr.getAlwaysFilter().isEnabled()) {
                try {
                    if (!lgr.checkHoster(link)) continue;
                } catch (NoDownloadLinkException e) {
                    throw new WTFException();
                }
                try {
                    if (!lgr.checkPluginStatus(link)) continue;
                } catch (NoDownloadLinkException e) {
                    throw new WTFException();
                }
                if (!lgr.checkSource(link)) continue;
                if (lgr.isRequiresLinkcheck()) {
                    if (!lgr.checkOnlineStatus(link)) continue;
                    if (!lgr.checkFileName(link)) continue;
                    if (!lgr.checkFileSize(link)) continue;
                    if (!lgr.checkFileType(link)) continue;
                }
            }
            set(link, lgr);
        }
    }

    public void runByUrl(CrawledLink link) {
        if (isTestInstance() == false && !org.jdownloader.settings.staticreferences.CFG_PACKAGIZER.PACKAGIZER_ENABLED.isEnabled()) return;
        java.util.List<PackagizerRuleWrapper> lurlFilter = urlFilter;
        for (PackagizerRuleWrapper lgr : lurlFilter) {
            try {
                if (!lgr.checkHoster(link)) continue;
            } catch (NoDownloadLinkException e) {
                continue;
            }

            try {
                if (!lgr.checkPluginStatus(link)) continue;
            } catch (NoDownloadLinkException e) {
                continue;
            }
            if (!lgr.checkSource(link)) continue;
            set(link, lgr);
        }
    }

    public static String replaceDynamicTags(String input, String packageName) {
        String ret = input;
        if (StringUtils.isEmpty(ret)) return ret;
        String PACKAGETAG = "<jd:" + PackagizerController.PACKAGENAME + ">";
        String DATETAG = "<jd:" + PackagizerController.SIMPLEDATE + ":";
        if (ret.contains(PACKAGETAG)) {
            if (StringUtils.isEmpty(packageName)) {
                ret = ret.replace(PACKAGETAG, "");
            } else {
                ret = ret.replace(PACKAGETAG, CrossSystem.alleviatePathParts(packageName));
            }
            ret = ret.replace("//", "/").replace("\\\\", "\\");
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
        return ret;
    }

    protected void set(CrawledLink link, PackagizerRuleWrapper lgr) {
        PackageInfo dpi = link.getDesiredPackageInfo();
        if (dpi == null) dpi = new PackageInfo();
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
        Boolean b = null;
        if ((b = lgr.getRule().isAutoExtractionEnabled()) != null) {
            /* customize auto extract */
            link.getArchiveInfo().setAutoExtract(b ? BooleanStatus.TRUE : BooleanStatus.FALSE);

        }
        if ((b = lgr.getRule().isAutoAddEnabled()) != null) {
            /* customize auto add */
            if (b) {
                link.setAutoConfirmEnabled(b);
            }
        }
        if ((b = lgr.getRule().isAutoStartEnabled()) != null) {
            /* customize auto start */
            if (b) {
                link.setAutoStartEnabled(b);
            }
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
                    if (replacer != null) txt = replacer.replace(m[2], link, txt, lgr);
                }
            } catch (final Throwable e) {
                Log.exception(e);
            }
        }
        return txt;
    }

    public void onNewFile(Object caller, File[] fileList) {
        if (!org.jdownloader.settings.staticreferences.CFG_PACKAGIZER.PACKAGIZER_ENABLED.isEnabled()) return;
        if (caller instanceof SingleDownloadController) {
            // do nothing
            return;
        } else if (caller == this) {
            // do nothing - avoid rename loops here
            return;
        } else if (caller instanceof ExtractionController) {
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
        for (PackagizerRuleWrapper lgr : lfileFilter) {
            if (!StringUtils.isEmpty(lgr.getRule().getRename()) || !StringUtils.isEmpty(lgr.getRule().getMoveto())) {
                if (lgr.getAlwaysFilter() == null || !lgr.getAlwaysFilter().isEnabled()) {
                    try {
                        if (!lgr.checkHoster(link)) continue;
                    } catch (NoDownloadLinkException e) {
                        throw new WTFException();
                    }
                    try {
                        if (!lgr.checkPluginStatus(link)) continue;
                    } catch (NoDownloadLinkException e) {
                        throw new WTFException();
                    }
                    if (!lgr.checkSource(link)) continue;
                    if (lgr.isRequiresLinkcheck()) {
                        if (!lgr.checkOnlineStatus(link)) continue;
                        if (!lgr.checkFileName(link)) continue;
                        if (!lgr.checkFileSize(link)) continue;
                        if (!lgr.checkFileType(link)) continue;
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
            if (newFile.getParentFile().exists() == false && newFile.getParentFile().mkdirs() == false) {
                Log.L.warning("Packagizer could not create " + newFile.getParentFile());
                return;
            }
            boolean successful = false;
            if ((successful = f.renameTo(newFile)) == false) {
                Log.L.warning("Packagizer rename failed " + f + " to" + newFile);
                try {
                    Log.L.warning("Packagizer try copy " + f + " to" + newFile);
                    IO.copyFile(f, newFile);
                    f.delete();
                    successful = true;
                } catch (final Throwable e) {
                    newFile.delete();
                    Log.L.warning("Packagizer could not move/rename " + f + " to" + newFile);
                }
            }
            if (successful) {
                Log.L.info("Packagizer moved/renamed " + f + " to " + newFile);
                FileCreationManager.getInstance().getEventSender().fireEvent(new FileCreationEvent(this, FileCreationEvent.Type.NEW_FILES, new File[] { newFile }));
            }
        }
    }

    @Override
    public void onRemoveFile(Object caller, File[] fileList) {
    }

}
