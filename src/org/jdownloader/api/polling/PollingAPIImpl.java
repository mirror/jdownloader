package org.jdownloader.api.polling;

import java.util.ArrayList;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.remoteapi.APIQuery;
import org.appwork.remoteapi.QueryResponseMap;

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

            // if packages is expanded in view, current state of all running links inside the package
            if (expandedPackageUUIDs.contains(fp.getUniqueID().getID())) {
                for (DownloadLink dl : fp.getChildren()) {
                    if (dwd.getRunningDownloadLinks().contains(dl)) {
                        PollingAPIDownloadLinkStorable pdls = new PollingAPIDownloadLinkStorable(dl);
                        fps.getLinks().add(pdls);
                    }
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

        String status = "UNKNOWN";
        if (dwd.getRunningDownloadLinks() != null && dwd.getRunningDownloadLinks().isEmpty()) {
            status = "STOPPED";
        } else if (dwd.isPaused()) {
            status = "PAUSED";
        } else {
            status = "RUNNING";
        }

        QueryResponseMap eventData = new QueryResponseMap();
        eventData.put("data", status);
        prs.setEventData(eventData);

        return prs;
    }
}
