package org.jdownloader.controlling.packagizer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Pattern;

import jd.config.Property;
import jd.controlling.IOEQ;
import jd.controlling.linkcollector.PackagizerInterface;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.PackageInfo;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.hoster.Offline;

import org.appwork.exceptions.WTFException;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.predefined.changeevent.ChangeEvent;
import org.appwork.utils.event.predefined.changeevent.ChangeEventSender;
import org.appwork.utils.logging.Log;
import org.jdownloader.controlling.UniqueID;
import org.jdownloader.controlling.filter.NoDownloadLinkException;

public class PackagizerController implements PackagizerInterface {
    private PackagizerSettings                  config;
    private ArrayList<PackagizerRule>           list;
    private ChangeEventSender                   eventSender;
    private ArrayList<PackagizerRuleWrapper>    fileFilter;
    private ArrayList<PackagizerRuleWrapper>    urlFilter;

    public static final String                  ORGFILENAME          = "orgfilename";
    public static final String                  HOSTER               = "hoster";
    public static final String                  SOURCE               = "source";
    public static final String                  PACKAGENAME          = "packagename";
    public static final String                  SIMPLEDATE           = "simpledate";
    public static final String                  ALLOW_MERGE          = "ALLOW_MERGE";
    private static final UniqueID               PERMANENT_OFFLINE_ID = new UniqueID();

    private static final PackagizerController   INSTANCE             = new PackagizerController();
    private HashMap<String, PackagizerReplacer> replacers            = new HashMap<String, PackagizerReplacer>();

    public static PackagizerController getInstance() {
        return INSTANCE;
    }

    private PackagizerController() {
        config = JsonConfig.create(PackagizerSettings.class);
        eventSender = new ChangeEventSender();
        list = config.getRuleList();
        if (list == null) list = new ArrayList<PackagizerRule>();
        update();
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public void run() {
                synchronized (PackagizerController.this) {
                    config.setRuleList(list);
                }
            }

            @Override
            public String toString() {
                return "save packagizer...";
            }
        });
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

    public ArrayList<PackagizerRule> list() {
        synchronized (this) {
            return new ArrayList<PackagizerRule>(list);
        }
    }

    public void add(PackagizerRule linkFilter) {
        if (linkFilter == null) return;
        synchronized (this) {
            list.add(linkFilter);
            config.setRuleList(list);
            update();
        }

    }

    public void setList(ArrayList<PackagizerRule> tableData) {
        synchronized (this) {
            list.clear();
            list.addAll(tableData);
            config.setRuleList(list);

            update();
        }
    }

    public void update() {
        IOEQ.add(new Runnable() {

            public void run() {
                updateInternal();
            }

        }, true);
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

    public void addAll(ArrayList<PackagizerRule> all) {
        if (all == null) return;
        synchronized (this) {
            list.addAll(all);
            config.setRuleList(list);
            update();
        }

    }

    public void remove(PackagizerRule lf) {
        if (lf == null) return;
        synchronized (this) {
            list.remove(lf);
            config.setRuleList(list);
            update();
        }

    }

    public void runByFile(CrawledLink link) {
        convertFilePackageInfos(link);
        permanentOffline(link);
        ArrayList<PackagizerRuleWrapper> lfileFilter = fileFilter;
        for (PackagizerRuleWrapper lgr : lfileFilter) {
            try {
                if (!lgr.checkHoster(link)) continue;
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
            set(link, lgr);
        }
    }

    private PackageInfo convertFilePackageInfos(CrawledLink link) {
        if (!link.getDownloadLink().isDefaultFilePackage()) {
            PackageInfo fpi = link.getDesiredPackageInfo();
            if (fpi == null) fpi = new PackageInfo();
            FilePackage dp = link.getDownloadLink().getFilePackage();
            fpi.setDestinationFolder(dp.getDownloadDirectory());
            fpi.setAutoExtractionEnabled(dp.isPostProcessing());
            fpi.setName(dp.getName());
            if (Boolean.FALSE.equals(dp.getBooleanProperty(ALLOW_MERGE, false))) {
                fpi.setUniqueId(dp.getUniqueID());
            }
            dp.setProperty(ALLOW_MERGE, Property.NULL);
            for (String s : dp.getPasswordList()) {
                fpi.getExtractionPasswords().add(s);
            }
            link.setDesiredPackageInfo(fpi);
            return fpi;
        }
        return null;
    }

    private void permanentOffline(CrawledLink link) {
        DownloadLink dl = link.getDownloadLink();
        if (dl != null && dl.getDefaultPlugin() instanceof Offline) {
            PackageInfo dpi = link.getDesiredPackageInfo();
            if (dpi == null) {
                dpi = new PackageInfo();
                link.setDesiredPackageInfo(dpi);
            }
            dpi.setName("Permanent Offline");
            dpi.setUniqueId(PERMANENT_OFFLINE_ID);
        }
    }

    public void runByUrl(CrawledLink link) {
        convertFilePackageInfos(link);
        permanentOffline(link);
        ArrayList<PackagizerRuleWrapper> lurlFilter = urlFilter;
        for (PackagizerRuleWrapper lgr : lurlFilter) {
            try {
                if (!lgr.checkHoster(link)) continue;
            } catch (NoDownloadLinkException e) {
                continue;
            }
            if (!lgr.checkSource(link)) continue;
            set(link, lgr);
        }
    }

    private void set(CrawledLink link, PackagizerRuleWrapper lgr) {
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
            if (b) {
                dpiSet = true;
                dpi.setAutoExtractionEnabled(b);
            }
        }
        if ((b = lgr.getRule().isAutoAddEnabled()) != null) {
            /* customize auto add */
            if (b) {
                dpiSet = true;
                dpi.setAutoAddEnabled(b);
            }
        }
        if ((b = lgr.getRule().isAutoStartEnabled()) != null) {
            /* customize auto start */
            if (b) {
                dpiSet = true;
                dpi.setAutoStartEnabled(b);
            }
        }
        if (dpiSet && link.getDesiredPackageInfo() == null) {
            /* set desiredpackageinfo if not set yet */
            link.setDesiredPackageInfo(dpi);
        }
    }

    private String replaceVariables(String txt, CrawledLink link, PackagizerRuleWrapper lgr) {
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

}
