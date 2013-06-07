package org.jdownloader.api.polling;

import java.util.ArrayList;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.linkcollector.LinkCollector;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.remoteapi.APIQuery;
import org.appwork.remoteapi.QueryResponseMap;
import org.jdownloader.api.captcha.CaptchaAPI;
import org.jdownloader.api.captcha.CaptchaAPIImpl;

public class PollingAPIImpl implements PollingAPI {

    private DownloadWatchDog dwd = DownloadWatchDog.getInstance();
    private APIQuery         queryParams;

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

        return result;
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
                        if (dwd.getRunningDownloadLinks().contains(dl)) {
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

        QueryResponseMap eventData = new QueryResponseMap();
        eventData.put("data", fpas);
        prs.setEventData(eventData);

        prs.setEventData(eventData);

        return prs;
    }

    private PollingResultAPIStorable getJDState() {
        PollingResultAPIStorable prs = new PollingResultAPIStorable();
        prs.setEventName("jdState");

        QueryResponseMap eventData = new QueryResponseMap();
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

        QueryResponseMap eventData = new QueryResponseMap();
        eventData.put("data", status);
        prs.setEventData(eventData);

        return prs;
    }

    private CaptchaAPI captchaAPI = new CaptchaAPIImpl();

    private PollingResultAPIStorable getCaptchasWaiting() {
        PollingResultAPIStorable prs = new PollingResultAPIStorable();
        prs.setEventName("captchasWaiting");

        QueryResponseMap eventData = new QueryResponseMap();
        eventData.put("data", captchaAPI.list());
        prs.setEventData(eventData);

        return prs;
    }
}
