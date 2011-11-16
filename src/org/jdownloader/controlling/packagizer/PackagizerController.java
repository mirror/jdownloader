package org.jdownloader.controlling.packagizer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

import jd.controlling.IOEQ;
import jd.controlling.linkcollector.PackagizerInterface;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.PackageInfo;
import jd.plugins.FilePackage;

import org.appwork.exceptions.WTFException;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.predefined.changeevent.ChangeEvent;
import org.appwork.utils.event.predefined.changeevent.ChangeEventSender;
import org.jdownloader.controlling.filter.NoDownloadLinkException;

public class PackagizerController implements PackagizerInterface {
    private static final PackagizerController INSTANCE = new PackagizerController();

    public static PackagizerController getInstance() {
        return INSTANCE;
    }

    private PackagizerSettings               config;
    private ArrayList<PackagizerRule>        list;
    private ChangeEventSender                eventSender;
    private ArrayList<PackagizerRuleWrapper> fileFilter;
    private ArrayList<PackagizerRuleWrapper> urlFilter;
    public static final String               FILENAME    = "filename";
    public static final String               ORGFILENAME = "orgfilename";
    public static final String               HOSTER      = "hoster";
    public static final String               SOURCE      = "source";
    public static final String               SIMPLEDATE  = "simpledate";
    public static final String               PACKAGENAME = "packagename";

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

        if (link.getDownloadLink().getFilePackage() != FilePackage.getDefaultFilePackage()) {
            FilePackage dp = link.getDownloadLink().getFilePackage();
            PackageInfo fpi = link.getDesiredPackageInfo();
            fpi.setDestinationFolder(dp.getDownloadDirectory());
            fpi.setAutoExtractionEnabled(dp.isPostProcessing());
            fpi.setName(dp.getName());
            fpi.setUniqueId(dp.getUniqueID());
            for (String s : dp.getPasswordList()) {
                fpi.getExtractionPasswords().add(s);
            }
        }

        for (PackagizerRuleWrapper lgr : fileFilter) {

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

    public void runByUrl(CrawledLink link) {
        if (link.getDownloadLink().getFilePackage() != FilePackage.getDefaultFilePackage()) {
            FilePackage dp = link.getDownloadLink().getFilePackage();
            PackageInfo fpi = link.getDesiredPackageInfo();
            fpi.setDestinationFolder(dp.getDownloadDirectory());
            fpi.setAutoExtractionEnabled(dp.isPostProcessing());
            fpi.setName(dp.getName());
            fpi.setUniqueId(dp.getUniqueID());
            for (String s : dp.getPasswordList()) {
                fpi.getExtractionPasswords().add(s);
            }
        }

        for (PackagizerRuleWrapper lgr : urlFilter) {
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

        if (lgr.getRule().getChunks() >= 0) {
            link.setChunks(lgr.getRule().getChunks());
        }
        if (!StringUtils.isEmpty(lgr.getRule().getDownloadDestination())) {
            String path = replaceVariables(lgr.getRule().getDownloadDestination(), link, lgr);

            link.getDesiredPackageInfo().setDestinationFolder(path);
        }
        if (!StringUtils.isEmpty(lgr.getRule().getPackageName())) {
            String name = replaceVariables(lgr.getRule().getPackageName(), link, lgr);
            link.getDesiredPackageInfo().setName(name);
        }
        if (lgr.getRule().getPriority() != null) link.setPriority(lgr.getRule().getPriority());
        if (!StringUtils.isEmpty(lgr.getRule().getFilename())) {
            link.setForcedName(replaceVariables(lgr.getRule().getFilename(), link, lgr));
        }

        link.getDesiredPackageInfo().setAutoExtractionEnabled(lgr.getRule().isAutoExtractionEnabled());
        link.getDesiredPackageInfo().setAutoAddEnabled(lgr.getRule().isAutoAddEnabled());
        link.getDesiredPackageInfo().setAutoStartEnabled(lgr.getRule().isAutoStartEnabled());

    }

    private String replaceVariables(String txt, CrawledLink link, PackagizerRuleWrapper lgr) {
        String[] matches = new Regex(txt, "<jd:(.+?)>").getColumn(0);
        if (matches != null) {
            for (String m : matches) {
                try {
                    if (m.toLowerCase(Locale.ENGLISH).startsWith(SOURCE + ":")) {
                        int id = Integer.parseInt(m.substring(SOURCE.length() + 1));
                        CrawledLink src = link;
                        while ((src = src.getSourceLink()) != null) {
                            Regex regex = new Regex(src.getURL(), lgr.getSourceRule().getPattern());
                            if (regex.matches()) {
                                String[] values = regex.getRow(0);

                                txt = Pattern.compile("<jd:" + SOURCE + ":" + id + "/?>").matcher(txt).replaceAll(values[id - 1]);
                            }
                        }

                    }
                    if (m.toLowerCase(Locale.ENGLISH).startsWith(SIMPLEDATE + ":")) {
                        String format = m.substring(SIMPLEDATE.length() + 1);
                        String dateString = new SimpleDateFormat(format).format(new Date());
                        txt = Pattern.compile("<jd:" + SIMPLEDATE + ":" + format + "/?>").matcher(txt).replaceAll(dateString);
                    }
                    if (m.toLowerCase(Locale.ENGLISH).startsWith(HOSTER + ":")) {
                        int id = Integer.parseInt(m.substring(HOSTER.length() + 1));
                        CrawledLink src = link;

                        Regex regex = new Regex(link.getURL(), lgr.getHosterRule().getPattern());
                        if (regex.matches()) {
                            String[] values = regex.getRow(0);

                            txt = Pattern.compile("<jd:" + HOSTER + ":" + id + "/?>").matcher(txt).replaceAll(values[id - 1]);
                        }

                    }

                    if (ORGFILENAME.equalsIgnoreCase(m)) {
                        txt = Pattern.compile("<jd:" + ORGFILENAME + "/?>").matcher(txt).replaceAll(link.getName());

                    }
                    if (PACKAGENAME.equalsIgnoreCase(m)) {
                        // keep
                    }
                    if (m.toLowerCase(Locale.ENGLISH).startsWith(ORGFILENAME + ":")) {
                        String id = m.substring(ORGFILENAME.length() + 1);
                        txt = Pattern.compile("<jd:" + ORGFILENAME + ":" + id + "/?>").matcher(txt).replaceAll(new Regex(link.getName(), lgr.getFileNameRule().getPattern()).getRow(0)[Integer.parseInt(id) - 1]);
                    } else if (FILENAME.equalsIgnoreCase(m)) {
                        txt = Pattern.compile("<jd:" + FILENAME + "/?>").matcher(txt).replaceAll(compileFilename(link, lgr));

                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
        return txt;

    }

    private String compileFilename(CrawledLink link, PackagizerRuleWrapper lgr) {
        return null;
    }

}
