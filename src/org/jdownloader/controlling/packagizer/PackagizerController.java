package org.jdownloader.controlling.packagizer;

import java.util.ArrayList;

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
            if (!lgr.checkOnlineStatus(link)) continue;

            if (!lgr.checkFileName(link)) continue;
            if (!lgr.checkFileSize(link)) continue;
            if (!lgr.checkFileType(link)) continue;

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
        if (lgr.getRule().getDownloadDestination() != null) {
            String path = replaceVariables(lgr.getRule().getDownloadDestination(), link, lgr);

            link.getDesiredPackageInfo().setDestinationFolder(lgr.getRule().getDownloadDestination());
        }
        if (lgr.getRule().getPackageName() != null) {
            String name = replaceVariables(lgr.getRule().getPackageName(), link, lgr);
            link.getDesiredPackageInfo().setName(lgr.getRule().getPackageName());
        }
        link.setPriority(lgr.getRule().getPriority());
        link.setForcedName(replaceVariables(lgr.getRule().getFilename(), link, lgr));
        lgr.getRule().isAutoAddEnabled();
        link.getDesiredPackageInfo().setAutoExtractionEnabled(lgr.getRule().isAutoExtractionEnabled());
        link.getDesiredPackageInfo().setAutoAddEnabled(lgr.getRule().isAutoAddEnabled());
        link.getDesiredPackageInfo().setAutoStartEnabled(lgr.getRule().isAutoStartEnabled());

    }

    private String replaceVariables(String txt, CrawledLink link, PackagizerRuleWrapper lgr) {
        String[] matches = new Regex(txt, "<jd:(.+?)>").getRow(0);
        for (String m : matches) {

        }
        return txt;

    }

}
