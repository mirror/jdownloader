package org.jdownloader.api.downloads;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadLinkCandidate;
import jd.controlling.downloadcontroller.DownloadLinkCandidateResult;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.DownloadWatchDogProperty;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.downloadcontroller.event.DownloadWatchdogListener;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLinkProperty;
import jd.plugins.FilePackage;
import jd.plugins.FilePackageProperty;
import jd.plugins.FilePackageView;

import org.appwork.exceptions.WTFException;
import org.appwork.remoteapi.events.EventObject;
import org.appwork.remoteapi.events.EventPublisher;
import org.appwork.remoteapi.events.EventsAPI;
import org.appwork.remoteapi.events.RemoteAPIEventsSender;
import org.appwork.remoteapi.events.SimpleEventObject;
import org.appwork.remoteapi.events.Subscriber;
import org.appwork.remoteapi.events.local.LocalEventsAPIListener;
import org.appwork.remoteapi.exceptions.BadParameterException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.SimpleMapper;
import org.appwork.storage.TypeRef;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.api.downloads.v2.DownloadLinkAPIStorableV2;
import org.jdownloader.api.downloads.v2.FilePackageAPIStorableV2;
import org.jdownloader.api.downloads.v2.LinkQueryStorable;
import org.jdownloader.api.downloads.v2.PackageQueryStorable;
import org.jdownloader.controlling.download.DownloadControllerListener;
import org.jdownloader.extensions.extraction.ExtractionStatus;
import org.jdownloader.myjdownloader.client.bindings.interfaces.DownloadsEventsInterface;
import org.jdownloader.plugins.ConditionalSkipReason;
import org.jdownloader.plugins.FinalLinkState;

public class DownloadControllerEventPublisher implements EventPublisher, DownloadControllerListener, LocalEventsAPIListener, DownloadControllerEventPublisherInterface, DownloadWatchdogListener {

    private enum BASIC_EVENT {
        REFRESH_STRUCTURE,
        REMOVE_CONTENT,
        ADD_CONTENT,
        REFRESH_CONTENT,
        LINK_UPDATE,
        PACKAGE_UPDATE
    }

    private CopyOnWriteArraySet<RemoteAPIEventsSender> remoteEventSenders      = new CopyOnWriteArraySet<RemoteAPIEventsSender>();
    public static final List<String>                   EVENT_ID_LIST;
    private ConcurrentHashMap<Long, DownloadLink>      linksWithPluginProgress = new ConcurrentHashMap<Long, DownloadLink>();
    private ConcurrentHashMap<Long, ChannelCollector>  collectors              = new ConcurrentHashMap<Long, ChannelCollector>();

    public static List<String>                         INTERVAL_EVENT_ID_LIST;
    private ScheduledExecutorService                   executer;
    private EventsAPI                                  eventsAPI;
    private long                                       backEndChangeID;
    private long                                       contentChangesCounter;
    static {
        EVENT_ID_LIST = new ArrayList<String>();
        for (BASIC_EVENT t : BASIC_EVENT.values()) {
            EVENT_ID_LIST.add(t.name());
        }
        //
        HashMap<String, Object> map = new SimpleMapper().convert(new LinkQueryStorableDummy(), new TypeRef<HashMap<String, Object>>() {
        });
        for (Entry<String, Object> es : map.entrySet()) {
            EVENT_ID_LIST.add(BASIC_EVENT.LINK_UPDATE.name() + "." + es.getKey());
        }

        map = new SimpleMapper().convert(new PackageQueryStorableDummy(), new TypeRef<HashMap<String, Object>>() {
        });
        for (Entry<String, Object> es : map.entrySet()) {
            EVENT_ID_LIST.add(BASIC_EVENT.PACKAGE_UPDATE.name() + "." + es.getKey());
        }

        INTERVAL_EVENT_ID_LIST = new ArrayList<String>();
        INTERVAL_EVENT_ID_LIST.add(BASIC_EVENT.LINK_UPDATE.name() + ".speed");
        INTERVAL_EVENT_ID_LIST.add(BASIC_EVENT.LINK_UPDATE.name() + ".bytesLoaded");
        INTERVAL_EVENT_ID_LIST.add(BASIC_EVENT.LINK_UPDATE.name() + ".eta");
        INTERVAL_EVENT_ID_LIST.add(BASIC_EVENT.LINK_UPDATE.name() + ".bytesTotal");
        INTERVAL_EVENT_ID_LIST.add(BASIC_EVENT.LINK_UPDATE.name() + ".status");

        INTERVAL_EVENT_ID_LIST.add(BASIC_EVENT.PACKAGE_UPDATE.name() + ".speed");
        INTERVAL_EVENT_ID_LIST.add(BASIC_EVENT.PACKAGE_UPDATE.name() + ".bytesLoaded");
        INTERVAL_EVENT_ID_LIST.add(BASIC_EVENT.PACKAGE_UPDATE.name() + ".eta");
        INTERVAL_EVENT_ID_LIST.add(BASIC_EVENT.PACKAGE_UPDATE.name() + ".bytesTotal");
        INTERVAL_EVENT_ID_LIST.add(BASIC_EVENT.PACKAGE_UPDATE.name() + ".status");
    }

    public static class PackageQueryStorableDummy extends PackageQueryStorable {
        public String toJsonString() {
            return null;
        }
    }

    public static class LinkQueryStorableDummy extends LinkQueryStorable {
        public String toJsonString() {
            return null;
        }
    }

    public DownloadControllerEventPublisher(EventsAPI eventsapi) {
        RemoteAPIController.validateInterfaces(DownloadControllerEventPublisherInterface.class, DownloadsEventsInterface.class);
        this.eventsAPI = eventsapi;
        eventsapi.getLocalEventSender().addListener(this);

    }

    @Override
    public String[] getPublisherEventIDs() {
        return EVENT_ID_LIST.toArray(new String[] {});
    }

    @Override
    public String getPublisherName() {
        return "downloads";
    }

    @Override
    public synchronized void register(RemoteAPIEventsSender eventsAPI) {
        boolean wasEmpty = remoteEventSenders.isEmpty();
        remoteEventSenders.add(eventsAPI);
        if (wasEmpty && remoteEventSenders.isEmpty() == false) {
            DownloadController.getInstance().addListener(this, true);
            DownloadWatchDog.getInstance().getEventSender().addListener(this, true);
        }
    }

    @Override
    public synchronized void unregister(RemoteAPIEventsSender eventsAPI) {
        remoteEventSenders.remove(eventsAPI);
        if (remoteEventSenders.isEmpty()) {
            DownloadController.getInstance().removeListener(this);
            DownloadWatchDog.getInstance().getEventSender().removeListener(this);
        }
    }

    @Override
    public void onDownloadControllerAddedPackage(FilePackage pkg) {
        fire(BASIC_EVENT.ADD_CONTENT.name(), null, BASIC_EVENT.ADD_CONTENT.name());
        flushBuffer();
    }

    @Override
    public void onDownloadControllerStructureRefresh(FilePackage pkg) {
        long newChange = DownloadController.getInstance().getPackageControllerChanges();
        if (backEndChangeID == newChange) {
            // avoid dupe events
            return;
        }
        backEndChangeID = newChange;
        fire(BASIC_EVENT.REFRESH_STRUCTURE.name(), null, BASIC_EVENT.REFRESH_STRUCTURE.name());
        flushBuffer();

    }

    @Override
    public void onDownloadControllerStructureRefresh() {
        long newChange = DownloadController.getInstance().getPackageControllerChanges();
        if (backEndChangeID == newChange) {
            // avoid dupe events
            return;
        }
        backEndChangeID = newChange;
        fire(BASIC_EVENT.REFRESH_STRUCTURE.name(), null, BASIC_EVENT.REFRESH_STRUCTURE.name());
        flushBuffer();
    }

    @Override
    public void onDownloadControllerStructureRefresh(AbstractNode node, Object param) {
        long newChange = DownloadController.getInstance().getPackageControllerChanges();
        if (backEndChangeID == newChange) {
            // avoid dupe events
            return;
        }
        backEndChangeID = newChange;
        fire(BASIC_EVENT.REFRESH_STRUCTURE.name(), null, BASIC_EVENT.REFRESH_STRUCTURE.name());
        flushBuffer();
    }

    @Override
    public void onDownloadControllerRemovedPackage(FilePackage pkg) {
        fire(BASIC_EVENT.REMOVE_CONTENT.name(), null, null);
        flushBuffer();
    }

    @Override
    public void onDownloadControllerRemovedLinklist(List<DownloadLink> list) {
        fire(BASIC_EVENT.REMOVE_CONTENT.name(), null, null);
        flushBuffer();
    }

    @Override
    public void onDownloadControllerUpdatedData(DownloadLink dl, DownloadLinkProperty property) {

        if (property != null) {
            HashMap<String, Object> dls = null;
            // [DATA_UPDATE.extractionStatus, DATA_UPDATE.finished, DATA_UPDATE.priority, DATA_UPDATE.speed, DATA_UPDATE.url,
            // DATA_UPDATE.enabled, DATA_UPDATE.skipped, DATA_UPDATE.running, DATA_UPDATE.bytesLoaded, DATA_UPDATE.eta,
            // DATA_UPDATE.maxResults, DATA_UPDATE.packageUUIDs, DATA_UPDATE.host, DATA_UPDATE.comment, DATA_UPDATE.bytesTotal,
            // DATA_UPDATE.startAt, DATA_UPDATE.status]
            System.out.println("Property Change: " + property.getProperty());
            switch (property.getProperty()) {
            case ARCHIVE:
                break;
            case ARCHIVE_ID:
                // //archive properties changed;
                break;

            case AVAILABILITY:
                break;
            case CHUNKS:
                break;
            case COMMENT:
                dls = new HashMap<String, Object>();
                dls.put("uuid", dl.getUniqueID().getID());
                dls.put("comment", dl.getComment());
                fire(BASIC_EVENT.LINK_UPDATE.name() + ".comment", dls, BASIC_EVENT.LINK_UPDATE.name() + ".comment." + dl.getUniqueID().getID() + "");

                break;
            case BROWSER_URL:
            case DOWNLOAD_URL:
                dls = new HashMap<String, Object>();
                dls.put("uuid", dl.getUniqueID().getID());
                dls.put("url", dl.getView().getDownloadUrl());
                fire(BASIC_EVENT.LINK_UPDATE.name() + ".url", dls, BASIC_EVENT.LINK_UPDATE.name() + ".url." + dl.getUniqueID().getID() + "");

                break;
            case CONDITIONAL_SKIPPED:
                ConditionalSkipReason conditionalSkipReason = dl.getConditionalSkipReason();

                pushStatus(dl);

                break;

            case DOWNLOAD_PASSWORD:
                break;

            case DOWNLOADSIZE:
                dls = new HashMap<String, Object>();
                dls.put("uuid", dl.getUniqueID().getID());
                dls.put("bytesTotal", dl.getView().getBytesTotalEstimated());
                fire(BASIC_EVENT.LINK_UPDATE.name() + ".bytesTotal", dls, BASIC_EVENT.LINK_UPDATE.name() + ".bytesTotal." + dl.getUniqueID().getID() + "");

                break;
            case DOWNLOADSIZE_VERIFIED:
                dls = new HashMap<String, Object>();
                dls.put("uuid", dl.getUniqueID().getID());
                dls.put("bytesTotal", dl.getView().getBytesTotalEstimated());
                fire(BASIC_EVENT.LINK_UPDATE.name() + ".bytesTotal", dls, BASIC_EVENT.LINK_UPDATE.name() + ".bytesTotal." + dl.getUniqueID().getID() + "");

                break;
            case ENABLED:
                dls = new HashMap<String, Object>();
                dls.put("uuid", dl.getUniqueID().getID());
                dls.put("enabled", dl.isEnabled());
                fire(BASIC_EVENT.LINK_UPDATE.name() + ".enabled", dls, BASIC_EVENT.LINK_UPDATE.name() + ".enabled." + dl.getUniqueID().getID() + "");

                break;
            case EXTRACTION_STATUS:
                dls = new HashMap<String, Object>();
                dls.put("uuid", dl.getUniqueID().getID());
                ExtractionStatus es = dl.getExtractionStatus();
                dls.put("extractionStatus", es == null ? null : es.toString());
                fire(BASIC_EVENT.LINK_UPDATE.name() + ".extractionStatus", dls, BASIC_EVENT.LINK_UPDATE.name() + ".extractionStatus." + dl.getUniqueID().getID() + "");

                pushStatus(dl);
                break;
            case FINAL_STATE:
                dls = new HashMap<String, Object>();
                dls.put("uuid", dl.getUniqueID().getID());
                dls.put("finished", (FinalLinkState.CheckFinished(dl.getFinalLinkState())));
                fire(BASIC_EVENT.LINK_UPDATE.name() + ".finished", dls, BASIC_EVENT.LINK_UPDATE.name() + ".finished." + dl.getUniqueID().getID() + "");

                final FinalLinkState finalLinkState = dl.getFinalLinkState();

                pushStatus(dl);

                break;
            case LINKSTATUS:
                break;
            case MD5:
                break;
            case NAME:
                dls = new HashMap<String, Object>();
                dls.put("uuid", dl.getUniqueID().getID());
                dls.put("name", dl.getView().getDisplayName());
                fire(BASIC_EVENT.LINK_UPDATE.name() + ".name", dls, BASIC_EVENT.LINK_UPDATE.name() + ".name." + dl.getUniqueID().getID() + "");

                break;
            case PLUGIN_PROGRESS:
                synchronized (linksWithPluginProgress) {
                    if (dl.getPluginProgress() == null) {
                        linksWithPluginProgress.remove(dl.getUniqueID().getID());
                        pushDiff(dl);
                        cleanup(dl);
                    } else {
                        linksWithPluginProgress.put(dl.getUniqueID().getID(), dl);
                        updateExecuter(true);

                    }
                }
                break;
            case PRIORITY:
                dls = new HashMap<String, Object>();
                dls.put("uuid", dl.getUniqueID().getID());
                dls.put("priority", org.jdownloader.myjdownloader.client.bindings.PriorityStorable.valueOf(dl.getPriorityEnum().name()));
                fire(BASIC_EVENT.LINK_UPDATE.name() + ".priority", dls, BASIC_EVENT.LINK_UPDATE.name() + ".priority." + dl.getUniqueID().getID() + "");

                break;
            case RESET:
                break;
            case RESUMABLE:
                break;
            case SHA1:
                break;
            case SKIPPED:
                // SkipReason skipReason = dl.getSkipReason();

                pushStatus(dl);

                dls = new HashMap<String, Object>();
                dls.put("uuid", dl.getUniqueID().getID());
                dls.put("skipped", dl.isSkipped());
                fire(BASIC_EVENT.LINK_UPDATE.name() + ".skipped", dls, BASIC_EVENT.LINK_UPDATE.name() + ".skipped." + dl.getUniqueID().getID() + "");

                break;
            case SPEED_LIMIT:
                break;
            case URL_PROTECTION:
                break;
            case VARIANT:
                break;
            case VARIANTS:
                break;
            case VARIANTS_ENABLED:
                break;

            }
        }
        long newContentChangesCounter = DownloadController.getInstance().getContentChanges();
        if (newContentChangesCounter != this.contentChangesCounter) {
            // avoid dupes
            this.contentChangesCounter = newContentChangesCounter;
            fire(BASIC_EVENT.REFRESH_CONTENT.name(), null, BASIC_EVENT.REFRESH_CONTENT.name());
        }
        flushBuffer();
    }

    private void pushStatus(DownloadLink dl) {

        HashMap<String, Object> dls = new HashMap<String, Object>();
        dls.put("uuid", dl.getUniqueID().getID());

        DownloadLinkAPIStorableV2 dlss = RemoteAPIController.getInstance().getDownloadsAPIV2().setStatus(new DownloadLinkAPIStorableV2(), dl, this);

        dls.put("statusIconKey", dlss.getStatusIconKey());
        dls.put("status", dlss.getStatus());

        fire(BASIC_EVENT.LINK_UPDATE.name() + ".status", dls, BASIC_EVENT.LINK_UPDATE.name() + ".status." + dl.getUniqueID().getID() + "");

        // package

        dls = new HashMap<String, Object>();
        dls.put("uuid", dl.getUniqueID().getID());

        FilePackageView fpView = new FilePackageView(dl.getFilePackage());
        fpView.setItems(null);
        FilePackageAPIStorableV2 dpss = RemoteAPIController.getInstance().getDownloadsAPIV2().setStatus(new FilePackageAPIStorableV2(), fpView);
        dls.put("statusIconKey", dpss.getStatusIconKey());
        dls.put("status", dpss.getStatus());

        fire(BASIC_EVENT.PACKAGE_UPDATE.name() + ".status", dls, BASIC_EVENT.PACKAGE_UPDATE.name() + ".status." + dl.getUniqueID().getID() + "");

    }

    // private HashMap<String, Object> cache = new HashMap<String, Object>();
    private HashMap<Subscriber, List<EventObject>> buffer = new HashMap<Subscriber, List<EventObject>>(); ;

    private void fire(String eventID, Object dls, String collapseKey) {
        synchronized (this) {

            ArrayList<Subscriber> subscribers = eventsAPI.getSubscribers();
            SimpleEventObject eventObject = new SimpleEventObject(this, eventID, dls, collapseKey);
            for (Subscriber subscriber : subscribers) {

                pushToBuffer(subscriber, eventObject);

            }

        }
    }

    @Override
    public void onDownloadControllerUpdatedData(FilePackage pkg, FilePackageProperty property) {

        if (property != null) {
            HashMap<String, Object> dls = null;
            // [DATA_UPDATE.extractionStatus, DATA_UPDATE.finished, DATA_UPDATE.priority, DATA_UPDATE.speed, DATA_UPDATE.url,
            // DATA_UPDATE.enabled, DATA_UPDATE.skipped, DATA_UPDATE.running, DATA_UPDATE.bytesLoaded, DATA_UPDATE.eta,
            // DATA_UPDATE.maxResults, DATA_UPDATE.packageUUIDs, DATA_UPDATE.host, DATA_UPDATE.comment, DATA_UPDATE.bytesTotal,
            // DATA_UPDATE.startAt, DATA_UPDATE.status]

            switch (property.getProperty()) {
            case COMMENT:
                dls = new HashMap<String, Object>();
                dls.put("uuid", pkg.getUniqueID().getID());
                dls.put("comment", pkg.getComment());
                fire(BASIC_EVENT.PACKAGE_UPDATE.name() + ".comment", dls, BASIC_EVENT.PACKAGE_UPDATE.name() + ".comment." + pkg.getUniqueID().getID() + "");

                break;
            case FOLDER:
                break;
            case NAME:
                dls = new HashMap<String, Object>();
                dls.put("uuid", pkg.getUniqueID().getID());
                dls.put("name", pkg.getName());
                fire(BASIC_EVENT.PACKAGE_UPDATE.name() + ".name", dls, BASIC_EVENT.PACKAGE_UPDATE.name() + ".name." + pkg.getUniqueID().getID() + "");

                break;
            case PRIORITY:
                dls = new HashMap<String, Object>();
                dls.put("uuid", pkg.getUniqueID().getID());
                dls.put("priority", org.jdownloader.myjdownloader.client.bindings.PriorityStorable.valueOf(pkg.getPriorityEnum().name()));
                fire(BASIC_EVENT.PACKAGE_UPDATE.name() + ".priority", dls, BASIC_EVENT.PACKAGE_UPDATE.name() + ".priority." + pkg.getUniqueID().getID() + "");

            }
        }

        fire(BASIC_EVENT.REFRESH_CONTENT.name(), null, BASIC_EVENT.REFRESH_CONTENT.name());
        flushBuffer();

    }

    @Override
    public void onDownloadControllerUpdatedData(DownloadLink downloadlink) {

        fire(BASIC_EVENT.REFRESH_CONTENT.name(), null, BASIC_EVENT.REFRESH_CONTENT.name());
        flushBuffer();
    }

    @Override
    public void onDownloadControllerUpdatedData(FilePackage pkg) {
        fire(BASIC_EVENT.REFRESH_CONTENT.name(), null, BASIC_EVENT.REFRESH_CONTENT.name());
        flushBuffer();
    }

    @Override
    public void onEventsChannelUpdate(Subscriber subscriber) {

        ChannelCollector collector = collectors.get(subscriber.getSubscriptionID());
        if (collector != null) {
            collector.updateSubscriptions();
        }

        int counter = 0;
        for (Entry<Long, ChannelCollector> es : collectors.entrySet()) {
            if (es.getValue().hasIntervalSubscriptions()) {
                counter++;
            }
        }
        updateExecuter(counter > 0);

    }

    private void updateExecuter(boolean b) {
        synchronized (this) {
            if (b) {
                if (executer == null) {

                    executer = Executors.newScheduledThreadPool(1);
                    executer.scheduleAtFixedRate(new Runnable() {
                        int terminationRounds = 0;

                        @Override
                        public void run() {
                            boolean kill = true;
                            int events = 0;
                            Set<SingleDownloadController> activeDownloads = DownloadWatchDog.getInstance().getRunningDownloadLinks();

                            HashSet<DownloadLink> linksToProcess = null;
                            HashSet<FilePackage> packagesToProcess = null;

                            if (true) {
                                for (Entry<Long, ChannelCollector> es : collectors.entrySet()) {
                                    if (es.getValue().hasIntervalSubscriptions()) {
                                        kill = false;
                                        if (System.currentTimeMillis() - es.getValue().getLastPush() >= es.getValue().getInterval()) {
                                            es.getValue().setLastPush(System.currentTimeMillis());
                                            if (linksToProcess == null) {
                                                linksToProcess = new HashSet<DownloadLink>();
                                                for (SingleDownloadController dl : activeDownloads) {
                                                    linksToProcess.add(dl.getDownloadLink());
                                                }
                                                linksToProcess.addAll(linksWithPluginProgress.values());
                                            }
                                            for (DownloadLink dl : linksToProcess) {
                                                HashMap<String, Object> diff = es.getValue().getDiff(dl);

                                                for (Entry<String, Object> entry : diff.entrySet()) {
                                                    HashMap<String, Object> dls = new HashMap<String, Object>();
                                                    dls.put("uuid", dl.getUniqueID().getID());
                                                    dls.put(entry.getKey(), entry.getValue());
                                                    SimpleEventObject eventObject = new SimpleEventObject(DownloadControllerEventPublisher.this, BASIC_EVENT.LINK_UPDATE.name() + "." + entry.getKey(), dls, BASIC_EVENT.LINK_UPDATE.name() + "." + entry.getKey() + "." + dl.getUniqueID().getID() + "");
                                                    List<Long> publishTo = new ArrayList<Long>();

                                                    pushToBuffer(es.getValue().getSubscriber(), eventObject);
                                                    events++;
                                                }
                                            }
                                            if (packagesToProcess == null) {
                                                packagesToProcess = new HashSet<FilePackage>();

                                                for (DownloadLink dl : linksToProcess) {
                                                    FilePackage p = dl.getParentNode();
                                                    if (p != null) {
                                                        packagesToProcess.add(p);
                                                    }
                                                }

                                            }
                                            for (FilePackage p : packagesToProcess) {
                                                HashMap<String, Object> diff = es.getValue().getDiff(p);

                                                for (Entry<String, Object> entry : diff.entrySet()) {
                                                    HashMap<String, Object> dls = new HashMap<String, Object>();
                                                    dls.put("uuid", p.getUniqueID().getID());
                                                    dls.put(entry.getKey(), entry.getValue());
                                                    SimpleEventObject eventObject = new SimpleEventObject(DownloadControllerEventPublisher.this, BASIC_EVENT.PACKAGE_UPDATE.name() + "." + entry.getKey(), dls, BASIC_EVENT.LINK_UPDATE.name() + "." + entry.getKey() + "." + p.getUniqueID().getID() + "");
                                                    pushToBuffer(es.getValue().getSubscriber(), eventObject);
                                                    events++;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (events > 0) {
                                flushBuffer();
                            }
                            if (kill) {
                                terminationRounds++;
                                if (terminationRounds > 10) {
                                    updateExecuter(false);
                                    throw new RuntimeException("Exit Executer");
                                }
                            } else {
                                terminationRounds = 0;
                            }

                        }
                    }, 0, 100, TimeUnit.MILLISECONDS);
                }
            } else {
                if (executer == null) {
                    return;
                }
                executer.shutdownNow();

                executer = null;
            }
        }
    }

    protected void flushBuffer() {
        synchronized (buffer) {
            if (buffer.size() == 0) {
                return;
            }
            System.out.println("Flush Buffer " + buffer.size());

            for (Entry<Subscriber, List<EventObject>> es : buffer.entrySet()) {

                eventsAPI.push(es.getKey(), es.getValue());

            }
            buffer.clear();
        }

    }

    @Override
    public void onEventChannelOpened(Subscriber s) {

        collectors.put(s.getSubscriptionID(), new ChannelCollector(s));

    }

    @Override
    public void onEventChannelClosed(Subscriber s) {

        collectors.remove(s.getSubscriptionID());

    }

    @Override
    public boolean setStatusEventInterval(long channelID, long interval) {

        ChannelCollector collector = collectors.get(channelID);
        if (collector != null) {
            collector.setInterval(interval);
            return true;
        }

        return false;
    }

    @Override
    public void onDownloadWatchdogDataUpdate() {
    }

    @Override
    public void onDownloadWatchdogStateIsIdle() {
    }

    @Override
    public void onDownloadWatchdogStateIsPause() {
    }

    @Override
    public void onDownloadWatchdogStateIsRunning() {
    }

    @Override
    public void onDownloadWatchdogStateIsStopped() {
    }

    @Override
    public void onDownloadWatchdogStateIsStopping() {
    }

    @Override
    public void onDownloadControllerStart(SingleDownloadController downloadController, DownloadLinkCandidate candidate) {
        DownloadLink dl = candidate.getLink();
        HashMap<String, Object> dls = new HashMap<String, Object>();
        dls.put("uuid", dl.getUniqueID().getID());
        dls.put("running", true);
        fire(BASIC_EVENT.LINK_UPDATE.name() + ".running", dls, BASIC_EVENT.LINK_UPDATE.name() + ".running." + dl.getUniqueID().getID() + "");
        flushBuffer();
        for (Entry<Long, ChannelCollector> es : collectors.entrySet()) {
            if (es.getValue().hasIntervalSubscriptions()) {
                es.getValue().updateBase(dl);
            }
        }
        updateExecuter(true);

    }

    @Override
    public void onDownloadControllerStopped(SingleDownloadController downloadController, DownloadLinkCandidate candidate, DownloadLinkCandidateResult result) {

        DownloadLink dl = candidate.getLink();
        HashMap<String, Object> dls = new HashMap<String, Object>();
        dls.put("uuid", dl.getUniqueID().getID());
        dls.put("running", false);
        fire(BASIC_EVENT.LINK_UPDATE.name() + ".running", dls, BASIC_EVENT.LINK_UPDATE.name() + ".running." + dl.getUniqueID().getID() + "");

        pushDiff(dl);
        cleanup(dl);
        flushBuffer();

    }

    private void cleanup(DownloadLink dl2) {
        Set<SingleDownloadController> activeDownloads = DownloadWatchDog.getInstance().getRunningDownloadLinks();

        HashSet<DownloadLink> linksToProcess = new HashSet<DownloadLink>();
        for (SingleDownloadController dl : activeDownloads) {
            linksToProcess.add(dl.getDownloadLink());
        }
        linksToProcess.addAll(linksWithPluginProgress.values());

        for (Entry<Long, ChannelCollector> es : collectors.entrySet()) {
            es.getValue().cleanUp(linksToProcess);
        }
    }

    private void pushDiff(DownloadLink dl) {

        for (Entry<Long, ChannelCollector> es : collectors.entrySet()) {
            if (es.getValue().hasIntervalSubscriptions()) {

                es.getValue().setLastPush(System.currentTimeMillis());

                HashMap<String, Object> diff = es.getValue().getDiff(dl);

                for (Entry<String, Object> entry : diff.entrySet()) {
                    HashMap<String, Object> dls = new HashMap<String, Object>();
                    dls.put("uuid", dl.getUniqueID().getID());
                    dls.put(entry.getKey(), entry.getValue());
                    EventObject eventObject = new SimpleEventObject(DownloadControllerEventPublisher.this, BASIC_EVENT.LINK_UPDATE.name() + "." + entry.getKey(), dls, BASIC_EVENT.LINK_UPDATE.name() + "." + entry.getKey() + "." + dl.getUniqueID().getID() + "");
                    // List<Long> publishTo = new ArrayList<Long>();
                    // publishTo.add(es.getValue().getSubscriber().getSubscriptionID());
                    pushToBuffer(es.getValue().getSubscriber(), eventObject);

                }

                FilePackage p = dl.getParentNode();
                if (p != null) {
                    diff = es.getValue().getDiff(p);

                    for (Entry<String, Object> entry : diff.entrySet()) {
                        HashMap<String, Object> dls = new HashMap<String, Object>();
                        dls.put("uuid", p.getUniqueID().getID());
                        dls.put(entry.getKey(), entry.getValue());
                        SimpleEventObject eventObject = new SimpleEventObject(DownloadControllerEventPublisher.this, BASIC_EVENT.PACKAGE_UPDATE.name() + "." + entry.getKey(), dls, BASIC_EVENT.LINK_UPDATE.name() + "." + entry.getKey() + "." + p.getUniqueID().getID() + "");
                        // List<Long> publishTo = new ArrayList<Long>();
                        // publishTo.add(es.getValue().getSubscriber().getSubscriptionID());
                        pushToBuffer(es.getValue().getSubscriber(), eventObject);
                        // for (RemoteAPIEventsSender eventSender : remoteEventSenders) {
                        // eventSender.publishEvent(eventObject, publishTo);
                        // }
                    }
                }

            }
        }

    }

    private void pushToBuffer(Subscriber subscriber, EventObject eventObject) {
        synchronized (buffer) {

            ChannelCollector col = collectors.get(subscriber.getSubscriptionID());
            if (col == null) {

                // Closed channel?
                return;
            }
            Object dls = eventObject.getEventdata();
            if (dls != null && dls instanceof HashMap) {
                if (col != null) {
                    HashMap<String, Object> copy = new HashMap<String, Object>((HashMap) dls);

                    if (!col.updateDupeCache(eventObject.getEventid() + "." + copy.remove("uuid"), copy)) {
                        return;
                    }
                }
            }
            System.out.println(getClass().getName() + "FireEvent -> " + subscriber.getSubscriptionID() + " : " + eventObject.getEventid() + " - " + JSonStorage.serializeToJson(dls));

            List<EventObject> lst = this.buffer.get(subscriber);
            if (lst == null) {
                lst = new ArrayList<EventObject>();
                buffer.put(subscriber, lst);
            }
            lst.add(eventObject);
        }

    }

    @Override
    public void onDownloadWatchDogPropertyChange(DownloadWatchDogProperty propertyChange) {

    }

    @Override
    public DownloadListDiffStorable queryLinks(LinkQueryStorable queryParams, int diffID) throws BadParameterException {

        // List<DownloadLinkAPIStorableV2> newList = RemoteAPIController.getInstance().getDownloadsAPIV2().queryLinks(queryParams);
        //
        // ArrayList<DownloadLinkAPIStorableV2> oldList = new ArrayList<DownloadLinkAPIStorableV2>();
        // HashMap<Long, DownloadLinkAPIStorableV2> newListMap = toIDMap(newList);
        // HashMap<Long, DownloadLinkAPIStorableV2> oldListMap = toIDMap(oldList);
        //
        //
        // for(int i=0;i<oldList.size();i++){
        // old=
        // }
        throw new WTFException("Not Implemented");
        // return null;
    }

    // private HashMap<Long, DownloadLinkAPIStorableV2> toIDMap(List<DownloadLinkAPIStorableV2> newList) {
    // HashMap<Long, DownloadLinkAPIStorableV2> ret = new HashMap<Long, DownloadLinkAPIStorableV2>();
    // for (DownloadLinkAPIStorableV2 dl : newList)
    // ret.put(dl.getUuid(), dl);
    // return ret;
    // }

}
