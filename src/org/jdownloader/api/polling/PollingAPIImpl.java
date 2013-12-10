package org.jdownloader.api.polling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.remoteapi.APIQuery;
import org.jdownloader.api.captcha.CaptchaAPI;
import org.jdownloader.api.captcha.CaptchaAPISolver;
import org.jdownloader.api.jd.AggregatedNumbersAPIStorable;
import org.jdownloader.controlling.AggregatedCrawlerNumbers;
import org.jdownloader.controlling.AggregatedNumbers;
import org.jdownloader.gui.views.SelectionInfo;

public class PollingAPIImpl implements PollingAPI {

    private DownloadWatchDog   dwd = DownloadWatchDog.getInstance();
    private DownloadController dc  = DownloadController.getInstance();
    private LinkCollector      lc  = LinkCollector.getInstance();
    private APIQuery           queryParams;

    @Override
    public List<PollingResultAPIStorable> poll(APIQuery queryParams) {
        this.queryParams = queryParams;

        List<PollingResultAPIStorable> result = new ArrayList<PollingResultAPIStorable>();

        if (queryParams.containsKey("downloadProgress")) {
            result.add(getDownloadProgress());
        }
        if (queryParams.containsKey("jdState")) {
            result.add(getJDState());
        }
        if (queryParams.containsKey("linkGrabberState")) {
            result.add(getLinkGrabberState());
        }
        if (queryParams.containsKey("captchasWaiting")) {
            result.add(getCaptchasWaiting());
        }
        if (queryParams.containsKey("aggregatedNumbers")) {
            result.add(getAggregatedNumbers());
        }

        return result;
    }

    private PollingResultAPIStorable getAggregatedNumbers() {
        PollingResultAPIStorable prs = new PollingResultAPIStorable();
        prs.setEventName("aggregatedNumbers");
        SelectionInfo<FilePackage, DownloadLink> selDc = null;
        boolean dcrl = dc.readLock();
        try {
            selDc = new SelectionInfo<FilePackage, DownloadLink>(null, dc.getPackages(), false);
        } finally {
            dc.readUnlock(dcrl);
        }
        boolean lcrl = lc.readLock();
        SelectionInfo<CrawledPackage, CrawledLink> selLc = null;
        try {
            selLc = new SelectionInfo<CrawledPackage, CrawledLink>(null, lc.getPackages(), false);
        } finally {
            lc.readUnlock(lcrl);
        }

        org.jdownloader.myjdownloader.client.json.JsonMap eventData = new org.jdownloader.myjdownloader.client.json.JsonMap();
        eventData.put("data", new AggregatedNumbersAPIStorable(new AggregatedNumbers(selDc), new AggregatedCrawlerNumbers(selLc)));
        prs.setEventData(eventData);

        return prs;
    }

    @SuppressWarnings("rawtypes")
    private PollingResultAPIStorable getDownloadProgress() {

        // get packageUUIDs who should be filled with download progress of the containing links e.g because they are expanded in the
        // view
        List<Long> expandedPackageUUIDs = new ArrayList<Long>();
        if (!queryParams._getQueryParam("downloadProgress", List.class, new ArrayList()).isEmpty()) {
            List uuidsFromQuery = queryParams._getQueryParam("downloadProgress", List.class, new ArrayList());
            for (Object o : uuidsFromQuery) {
                try {
                    expandedPackageUUIDs.add((Long) o);
                } catch (ClassCastException e) {
                    continue;
                }
            }
        }

        PollingResultAPIStorable prs = new PollingResultAPIStorable();
        prs.setEventName("downloadProgress");

        List<PollingAPIFilePackageStorable> fpas = new ArrayList<PollingAPIFilePackageStorable>();

        for (FilePackage fp : dwd.getRunningFilePackages()) {
            PollingAPIFilePackageStorable fps = new PollingAPIFilePackageStorable(fp);
            fps.setSpeed(dwd.getDownloadSpeedbyFilePackage(fp));

            // if packages is expanded in view, current state of all running links inside the package
            if (expandedPackageUUIDs.contains(fp.getUniqueID().getID())) {
                boolean readL = fp.getModifyLock().readLock();
                try {
                    for (DownloadLink dl : fp.getChildren()) {
                        if (dwd.getRunningDownloadLinks().contains(dl.getDownloadLinkController())) {
                            PollingAPIDownloadLinkStorable pdls = new PollingAPIDownloadLinkStorable(dl);
                            fps.getLinks().add(pdls);
                        }
                    }
                } finally {
                    fp.getModifyLock().readUnlock(readL);
                }
            }
            fpas.add(fps);
        }

        org.jdownloader.myjdownloader.client.json.JsonMap eventData = new org.jdownloader.myjdownloader.client.json.JsonMap();
        eventData.put("data", fpas);
        prs.setEventData(eventData);

        return prs;
    }

    private PollingResultAPIStorable getJDState() {
        PollingResultAPIStorable prs = new PollingResultAPIStorable();
        prs.setEventName("jdState");

        org.jdownloader.myjdownloader.client.json.JsonMap eventData = new org.jdownloader.myjdownloader.client.json.JsonMap();
        eventData.put("data", dwd.getStateMachine().getState().getLabel());

        prs.setEventData(eventData);
        return prs;
    }

    private PollingResultAPIStorable getLinkGrabberState() {
        PollingResultAPIStorable prs = new PollingResultAPIStorable();
        prs.setEventName("linkGrabberState");

        LinkCollector lc = LinkCollector.getInstance();

        String status = "UNKNOWN";
        if (lc.getLinkChecker().isRunning()) {
            status = "RUNNING";
        } else {
            status = "IDLE";
        }

        org.jdownloader.myjdownloader.client.json.JsonMap eventData = new org.jdownloader.myjdownloader.client.json.JsonMap();
        eventData.put("data", status);
        prs.setEventData(eventData);

        return prs;
    }

    private CaptchaAPI captchaAPI = CaptchaAPISolver.getInstance();

    private PollingResultAPIStorable getCaptchasWaiting() {
        PollingResultAPIStorable prs = new PollingResultAPIStorable();
        prs.setEventName("captchasWaiting");

        org.jdownloader.myjdownloader.client.json.JsonMap eventData = new org.jdownloader.myjdownloader.client.json.JsonMap();
        eventData.put("data", captchaAPI.list());
        prs.setEventData(eventData);

        return prs;
    }
}
