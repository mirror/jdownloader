package org.jdownloader.api.downloads;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadLinkCandidate;
import jd.controlling.downloadcontroller.DownloadLinkCandidateResult;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.DownloadWatchDogProperty;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.downloadcontroller.event.DownloadWatchdogListener;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.PackageController;
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
import org.appwork.storage.SimpleMapper;
import org.appwork.storage.TypeRef;
import org.appwork.utils.event.queue.Queue;
import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.api.downloads.v2.DownloadLinkAPIStorableV2;
import org.jdownloader.api.downloads.v2.FilePackageAPIStorableV2;
import org.jdownloader.api.downloads.v2.LinkQueryStorable;
import org.jdownloader.api.downloads.v2.PackageQueryStorable;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.controlling.download.DownloadControllerListener;
import org.jdownloader.extensions.extraction.ExtractionStatus;
import org.jdownloader.myjdownloader.client.bindings.interfaces.DownloadsEventsInterface;
import org.jdownloader.plugins.FinalLinkState;

public class DownloadControllerEventPublisher implements EventPublisher, DownloadControllerListener, LocalEventsAPIListener, DownloadControllerEventPublisherInterface, DownloadWatchdogListener {

    private enum BASIC_EVENT {
        REFRESH_STRUCTURE,
        REMOVE_CONTENT,
        REMOVE_PACKAGE,
        REMOVE_LINK,
        ADD_CONTENT,
        ADD_PACKAGE,
        ADD_LINK,
        REFRESH_CONTENT,
        LINK_UPDATE,
        PACKAGE_UPDATE
    }

    private final CopyOnWriteArraySet<RemoteAPIEventsSender> remoteEventSenders      = new CopyOnWriteArraySet<RemoteAPIEventsSender>();
    private static final List<String>                        EVENT_ID_LIST;
    private final CopyOnWriteArraySet<DownloadLink>          linksWithPluginProgress = new CopyOnWriteArraySet<DownloadLink>();

    private final CopyOnWriteArrayList<ChannelCollector>     collectors              = new CopyOnWriteArrayList<ChannelCollector>();

    protected final static List<String>                      INTERVAL_EVENT_ID_LIST  = new ArrayList<String>();
    private ScheduledExecutorService                         executer;
    private final EventsAPI                                  eventsAPI;
    private final AtomicLong                                 backEndChangeID         = new AtomicLong(-1);
    private final AtomicLong                                 contentChangesCounter   = new AtomicLong(-1);
    private final Queue                                      queue                   = new Queue("DownloadControllerEventPublisher") {
                                                                                         public void killQueue() {
                                                                                         };
                                                                                     };
    static {
        EVENT_ID_LIST = new ArrayList<String>();
        for (BASIC_EVENT t : BASIC_EVENT.values()) {
            EVENT_ID_LIST.add(t.name());
        }
        //
        HashMap<String, Object> map = new SimpleMapper().convert(new LinkQueryStorableDummy(), TypeRef.HASHMAP);
        for (Entry<String, Object> es : map.entrySet()) {
            EVENT_ID_LIST.add(BASIC_EVENT.LINK_UPDATE.name() + "." + es.getKey());
        }

        map = new SimpleMapper().convert(new PackageQueryStorableDummy(), TypeRef.HASHMAP);
        for (Entry<String, Object> es : map.entrySet()) {
            EVENT_ID_LIST.add(BASIC_EVENT.PACKAGE_UPDATE.name() + "." + es.getKey());
        }

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
        final boolean wasEmpty = remoteEventSenders.isEmpty();
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
        boolean flush = false;
        if (hasSubscriptionFor(BASIC_EVENT.ADD_CONTENT.name())) {
            fire(BASIC_EVENT.ADD_CONTENT.name(), null, BASIC_EVENT.ADD_CONTENT.name());
            flush = true;
        }
        if (hasSubscriptionFor(BASIC_EVENT.ADD_PACKAGE.name())) {
            final HashMap<String, Object> dls = new HashMap<String, Object>();
            long afterUuid = -1l;
            final PackageController<FilePackage, DownloadLink> controller = pkg.getControlledBy();
            if (controller != null) {
                final boolean readL = controller.readLock();
                try {
                    final int index = controller.indexOf(pkg);
                    if (index > 0) {
                        final FilePackage fp = controller.getPackages().get(index - 1);
                        if (fp != null) {
                            afterUuid = fp.getUniqueID().getID();
                        }
                    }
                } finally {
                    controller.readUnlock(readL);
                }
            }
            dls.put("uuid", pkg.getUniqueID().getID());
            dls.put("afterUuid", afterUuid);
            fire(BASIC_EVENT.ADD_PACKAGE.name(), dls, BASIC_EVENT.ADD_PACKAGE.name() + "." + pkg.getUniqueID().getID());
            flush = true;
        }
        if (flush) {
            flushBuffer();
        }
    }

    @Override
    public void onDownloadControllerStructureRefresh(FilePackage pkg) {
        if (hasControllerChanges() && hasSubscriptionFor(BASIC_EVENT.REFRESH_STRUCTURE.name())) {
            fire(BASIC_EVENT.REFRESH_STRUCTURE.name(), null, BASIC_EVENT.REFRESH_STRUCTURE.name());
            flushBuffer();
        }
    }

    @Override
    public void onDownloadControllerStructureRefresh() {
        if (hasControllerChanges() && hasSubscriptionFor(BASIC_EVENT.REFRESH_STRUCTURE.name())) {
            fire(BASIC_EVENT.REFRESH_STRUCTURE.name(), null, BASIC_EVENT.REFRESH_STRUCTURE.name());
            flushBuffer();
        }
    }

    private final boolean hasControllerChanges() {
        final long newChange = DownloadController.getInstance().getPackageControllerChanges();
        return backEndChangeID.getAndSet(newChange) != newChange;
    }

    private final boolean hasContentChanges() {
        final long newChange = DownloadController.getInstance().getContentChanges();
        return contentChangesCounter.getAndSet(newChange) != newChange;
    }

    @Override
    public void onDownloadControllerStructureRefresh(AbstractNode node, Object param) {
        if (hasControllerChanges() && hasSubscriptionFor(BASIC_EVENT.REFRESH_STRUCTURE.name())) {
            fire(BASIC_EVENT.REFRESH_STRUCTURE.name(), null, BASIC_EVENT.REFRESH_STRUCTURE.name());
            flushBuffer();
        }
    }

    @Override
    public void onDownloadControllerRemovedPackage(FilePackage pkg) {
        boolean flush = false;
        if (hasSubscriptionFor(BASIC_EVENT.REMOVE_CONTENT.name())) {
            fire(BASIC_EVENT.REMOVE_CONTENT.name(), null, null);
            flush = true;
        }
        if (hasSubscriptionFor(BASIC_EVENT.REMOVE_PACKAGE.name())) {
            final HashMap<String, Object> dls = new HashMap<String, Object>();
            dls.put("uuid", pkg.getUniqueID().getID());
            fire(BASIC_EVENT.REMOVE_PACKAGE.name(), dls, BASIC_EVENT.REMOVE_PACKAGE.name() + "." + pkg.getUniqueID().getID());
            flush = true;
        }
        if (flush) {
            flushBuffer();
        }
    }

    @Override
    public void onDownloadControllerRemovedLinklist(List<DownloadLink> list) {
        boolean flush = false;
        if (hasSubscriptionFor(BASIC_EVENT.REMOVE_CONTENT.name())) {
            fire(BASIC_EVENT.REMOVE_CONTENT.name(), null, null);
            flush = true;
        }
        if (hasSubscriptionFor(BASIC_EVENT.REMOVE_LINK.name())) {
            final long[] ret = new long[list.size()];
            int index = 0;
            for (final DownloadLink link : list) {
                ret[index++] = link.getUniqueID().getID();
            }
            final HashMap<String, Object> dls = new HashMap<String, Object>();
            dls.put("uuids", ret);
            fire(BASIC_EVENT.REMOVE_LINK.name(), dls, null);
            flush = true;
        }
        if (flush) {
            flushBuffer();
        }
    }

    private static final String LINK_UPDATE_availability     = BASIC_EVENT.LINK_UPDATE.name() + ".availability";
    private static final String LINK_UPDATE_comment          = BASIC_EVENT.LINK_UPDATE.name() + ".comment";
    private static final String LINK_UPDATE_url              = BASIC_EVENT.LINK_UPDATE.name() + ".url";
    private static final String LINK_UPDATE_bytesTotal       = BASIC_EVENT.LINK_UPDATE.name() + ".bytesTotal";
    private static final String LINK_UPDATE_priority         = BASIC_EVENT.LINK_UPDATE.name() + ".priority";
    private static final String LINK_UPDATE_name             = BASIC_EVENT.LINK_UPDATE.name() + ".name";
    private static final String LINK_UPDATE_extractionStatus = BASIC_EVENT.LINK_UPDATE.name() + ".extractionStatus";
    private static final String LINK_UPDATE_status           = BASIC_EVENT.LINK_UPDATE.name() + ".status";
    private static final String LINK_UPDATE_skipped          = BASIC_EVENT.LINK_UPDATE.name() + ".skipped";
    private static final String LINK_UPDATE_finished         = BASIC_EVENT.LINK_UPDATE.name() + ".finished";
    private static final String LINK_UPDATE_reset            = BASIC_EVENT.LINK_UPDATE.name() + ".reset";
    private static final String PACKAGE_UPDATE_reset         = BASIC_EVENT.PACKAGE_UPDATE.name() + ".reset";
    private static final String LINK_UPDATE_running          = BASIC_EVENT.LINK_UPDATE.name() + ".running";
    private static final String PACKAGE_UPDATE_running       = BASIC_EVENT.PACKAGE_UPDATE.name() + ".running";
    private static final String PACKAGE_UPDATE_status        = BASIC_EVENT.PACKAGE_UPDATE.name() + ".status";
    private static final String LINK_UPDATE_enabled          = BASIC_EVENT.LINK_UPDATE.name() + ".enabled";
    private static final String PACKAGE_UPDATE_enabled       = BASIC_EVENT.PACKAGE_UPDATE.name() + ".enabled";
    private static final String PACKAGE_UPDATE_name          = BASIC_EVENT.PACKAGE_UPDATE.name() + ".name";
    private static final String PACKAGE_UPDATE_priority      = BASIC_EVENT.PACKAGE_UPDATE.name() + ".priority";

    @Override
    public void onDownloadControllerUpdatedData(DownloadLink dl, DownloadLinkProperty property) {
        if (hasListener()) {
            boolean flush = false;
            if (property != null) {
                final FilePackage parent = dl.getParentNode();
                switch (property.getProperty()) {
                case ARCHIVE:
                    break;
                case ARCHIVE_ID:
                    // //archive properties changed;
                    break;
                case AVAILABILITY:
                    if (hasSubscriptionFor(LINK_UPDATE_availability)) {
                        final HashMap<String, Object> dls = new HashMap<String, Object>();
                        dls.put("uuid", dl.getUniqueID().getID());
                        dls.put("availability", property.getValue());
                        fire(LINK_UPDATE_availability, dls, LINK_UPDATE_availability + "." + dl.getUniqueID().getID());
                        flush = true;
                    }
                    break;
                case CHUNKS:
                    break;
                case COMMENT:
                    if (hasSubscriptionFor(LINK_UPDATE_comment)) {
                        final HashMap<String, Object> dls = new HashMap<String, Object>();
                        dls.put("uuid", dl.getUniqueID().getID());
                        dls.put("comment", property.getValue());
                        fire(LINK_UPDATE_comment, dls, LINK_UPDATE_comment + "." + dl.getUniqueID().getID());
                        flush = true;
                    }
                    break;
                case URL_CONTAINER:
                case URL_ORIGIN:
                case URL_REFERRER:
                case URL_CONTENT:
                    if (hasSubscriptionFor(LINK_UPDATE_url)) {
                        final HashMap<String, Object> dls = new HashMap<String, Object>();
                        dls.put("uuid", dl.getUniqueID().getID());
                        dls.put("url", dl.getView().getDisplayUrl());
                        fire(LINK_UPDATE_url, dls, LINK_UPDATE_url + "." + dl.getUniqueID().getID());
                        flush = true;
                    }
                    break;
                case CONDITIONAL_SKIPPED:
                    pushStatus(dl);
                    break;
                case DOWNLOAD_PASSWORD:
                    break;
                case DOWNLOADSIZE:
                case DOWNLOADSIZE_VERIFIED:
                    if (hasSubscriptionFor(LINK_UPDATE_bytesTotal)) {
                        final HashMap<String, Object> dls = new HashMap<String, Object>();
                        dls.put("uuid", dl.getUniqueID().getID());
                        dls.put("bytesTotal", property.getValue());
                        fire(LINK_UPDATE_bytesTotal, dls, LINK_UPDATE_bytesTotal + "." + dl.getUniqueID().getID());
                        flush = true;
                    }
                    break;
                case DOWNLOAD_CONTROLLER:
                    if (hasSubscriptionFor(LINK_UPDATE_running)) {
                        final HashMap<String, Object> dls = new HashMap<String, Object>();
                        dls.put("uuid", dl.getUniqueID().getID());
                        dls.put("running", property.getValue() != null);
                        fire(LINK_UPDATE_running, dls, LINK_UPDATE_running + "." + dl.getUniqueID().getID());
                        flush = true;
                    }
                    if (hasSubscriptionFor(PACKAGE_UPDATE_running)) {
                        final HashMap<String, Object> dls = new HashMap<String, Object>();
                        dls.put("uuid", parent.getUniqueID().getID());
                        dls.put("running", property.getValue() != null || DownloadWatchDog.getInstance().hasRunningDownloads(parent));
                        fire(PACKAGE_UPDATE_running, dls, PACKAGE_UPDATE_running + "." + parent.getUniqueID().getID());
                        flush = true;
                    }
                    break;
                case ENABLED:
                    if (hasSubscriptionFor(LINK_UPDATE_enabled)) {
                        final HashMap<String, Object> dls = new HashMap<String, Object>();
                        dls.put("uuid", dl.getUniqueID().getID());
                        dls.put("enabled", dl.isEnabled());
                        fire(LINK_UPDATE_enabled, dls, LINK_UPDATE_enabled + "." + dl.getUniqueID().getID());
                        flush = true;
                    }
                    if (hasSubscriptionFor(PACKAGE_UPDATE_enabled)) {
                        final HashMap<String, Object> dls = new HashMap<String, Object>();
                        dls.put("uuid", parent.getUniqueID().getID());
                        boolean enabled = dl.isEnabled();
                        if (enabled == false) {
                            final boolean readL = parent.getModifyLock().readLock();
                            try {
                                for (final DownloadLink link : parent.getChildren()) {
                                    if (link.isEnabled()) {
                                        enabled = true;
                                        break;
                                    }
                                }
                            } finally {
                                parent.getModifyLock().readUnlock(readL);
                            }
                        }
                        dls.put("enabled", enabled);
                        fire(PACKAGE_UPDATE_enabled, dls, PACKAGE_UPDATE_enabled + "." + parent.getUniqueID().getID());
                        flush = true;
                    }
                    break;
                case EXTRACTION_STATUS:
                    if (hasSubscriptionFor(LINK_UPDATE_extractionStatus)) {
                        final HashMap<String, Object> dls = new HashMap<String, Object>();
                        dls.put("uuid", dl.getUniqueID().getID());
                        ExtractionStatus es = dl.getExtractionStatus();
                        dls.put("extractionStatus", es == null ? null : es.toString());
                        fire(LINK_UPDATE_extractionStatus, dls, LINK_UPDATE_extractionStatus + "." + dl.getUniqueID().getID());
                        flush = true;
                        pushStatus(dl);
                    }
                    break;
                case FINAL_STATE:
                    if (hasSubscriptionFor(LINK_UPDATE_finished)) {
                        final HashMap<String, Object> dls = new HashMap<String, Object>();
                        dls.put("uuid", dl.getUniqueID().getID());
                        dls.put("finished", (FinalLinkState.CheckFinished(dl.getFinalLinkState())));
                        fire(LINK_UPDATE_finished, dls, LINK_UPDATE_finished + "." + dl.getUniqueID().getID());
                        pushStatus(dl);
                    }
                    break;
                case LINKSTATUS:
                    if (hasSubscriptionFor(LINK_UPDATE_status)) {
                        final HashMap<String, Object> dls = new HashMap<String, Object>();
                        dls.put("uuid", dl.getUniqueID().getID());
                        dls.put("status", property.getValue());
                        fire(LINK_UPDATE_status, dls, LINK_UPDATE_status + "." + dl.getUniqueID().getID());
                        flush = true;
                    }
                    break;
                case MD5:
                    break;
                case NAME:
                    if (hasSubscriptionFor(LINK_UPDATE_name)) {
                        final HashMap<String, Object> dls = new HashMap<String, Object>();
                        dls.put("uuid", dl.getUniqueID().getID());
                        dls.put("name", dl.getView().getDisplayName());
                        fire(LINK_UPDATE_name, dls, LINK_UPDATE_name + "." + dl.getUniqueID().getID());
                        flush = true;
                    }
                    break;
                case PLUGIN_PROGRESS:
                    if (dl.getPluginProgress() == null) {
                        if (linksWithPluginProgress.remove(dl)) {
                            pushDiff(dl);
                            cleanup(dl);
                        }
                    } else {
                        if (linksWithPluginProgress.add(dl)) {
                            queue.add(new QueueAction<Void, RuntimeException>() {

                                @Override
                                protected Void run() throws RuntimeException {
                                    updateExecuter(true);
                                    return null;
                                }
                            });
                        }
                    }
                    break;
                case PRIORITY:
                    if (hasSubscriptionFor(LINK_UPDATE_priority)) {
                        final HashMap<String, Object> dls = new HashMap<String, Object>();
                        dls.put("uuid", dl.getUniqueID().getID());
                        dls.put("priority", org.jdownloader.myjdownloader.client.bindings.PriorityStorable.get(dl.getPriorityEnum().name()));
                        fire(LINK_UPDATE_priority, dls, LINK_UPDATE_priority + "." + dl.getUniqueID().getID());
                        flush = true;
                    }
                    break;
                case RESET:
                    if (hasSubscriptionFor(LINK_UPDATE_reset)) {
                        final HashMap<String, Object> dls = new HashMap<String, Object>();
                        dls.put("uuid", dl.getUniqueID().getID());
                        dls.put("reset", "true");
                        fire(LINK_UPDATE_reset, dls, LINK_UPDATE_reset + "." + dl.getUniqueID().getID());
                        flush = true;
                    }
                    if (hasSubscriptionFor(PACKAGE_UPDATE_reset)) {
                        final HashMap<String, Object> dls = new HashMap<String, Object>();
                        dls.put("uuid", dl.getUniqueID().getID());
                        dls.put("reset", "true");
                        fire(PACKAGE_UPDATE_reset, dls, PACKAGE_UPDATE_reset + "." + parent.getUniqueID().getID());
                        flush = true;
                    }
                    break;
                case RESUMABLE:
                    break;
                case SHA1:
                    break;
                case SHA256:
                    break;
                case SKIPPED:
                    if (hasSubscriptionFor(LINK_UPDATE_skipped)) {
                        final HashMap<String, Object> dls = new HashMap<String, Object>();
                        dls.put("uuid", dl.getUniqueID().getID());
                        dls.put("skipped", property.getValue() != null);
                        if (property.getValue() != null) {
                            dls.put("skipreason", property.getValue().toString());
                        }
                        fire(LINK_UPDATE_skipped, dls, LINK_UPDATE_skipped + "." + dl.getUniqueID().getID());
                        flush = true;
                        pushStatus(dl);
                    }
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
            if (hasContentChanges() && hasSubscriptionFor(BASIC_EVENT.REFRESH_CONTENT.name())) {
                fire(BASIC_EVENT.REFRESH_CONTENT.name(), null, BASIC_EVENT.REFRESH_CONTENT.name());
                flush = true;
            }
            if (flush) {
                flushBuffer();
            }
        }
    }

    /**
     * TODO: optimize!!!!
     *
     * @param dl
     */
    private void pushStatus(DownloadLink dl) {
        if (hasSubscriptionFor(LINK_UPDATE_status)) {
            final HashMap<String, Object> dls = new HashMap<String, Object>();
            dls.put("uuid", dl.getUniqueID().getID());
            final DownloadLinkAPIStorableV2 dlss = RemoteAPIController.getInstance().getDownloadsAPIV2().setStatus(new DownloadLinkAPIStorableV2(), dl, this);
            dls.put("statusIconKey", dlss.getStatusIconKey());
            dls.put("status", dlss.getStatus());
            fire(LINK_UPDATE_status, dls, LINK_UPDATE_status + "." + dl.getUniqueID().getID());
        }
        if (hasSubscriptionFor(PACKAGE_UPDATE_status)) {
            // package
            final FilePackage fp = dl.getFilePackage();
            final FilePackageView fpView = new FilePackageView(fp);
            fpView.aggregate();
            final FilePackageAPIStorableV2 dpss = RemoteAPIController.getInstance().getDownloadsAPIV2().setStatus(new FilePackageAPIStorableV2(), fpView);
            final HashMap<String, Object> dls = new HashMap<String, Object>();
            dls.put("uuid", fp.getUniqueID().getID());
            dls.put("statusIconKey", dpss.getStatusIconKey());
            dls.put("status", dpss.getStatus());
            fire(PACKAGE_UPDATE_status, dls, PACKAGE_UPDATE_status + "." + dl.getFilePackage().getUniqueID().getID());
        }
    }

    private final WeakHashMap<Subscriber, List<EventObject>> buffer = new WeakHashMap<Subscriber, List<EventObject>>(); ;

    private void fire(final String eventID, final Object dls, final String collapseKey) {
        queue.add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                final SimpleEventObject eventObject = new SimpleEventObject(DownloadControllerEventPublisher.this, eventID, dls, collapseKey);
                for (final ChannelCollector collector : collectors) {
                    pushToBuffer(collector, eventObject);
                }
                return null;
            }

        });
    }

    private final boolean hasListener() {
        return collectors.size() > 0;
    }

    private final boolean hasSubscriptionFor(final String event) {
        if (collectors.size() > 0) {
            final String eventID = getPublisherName().concat(".").concat(event);
            for (final ChannelCollector collector : collectors) {
                if (collector.getSubscriber().isSubscribed(eventID)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void fire(BASIC_EVENT eventType, final String eventID, Object dls, UniqueAlltimeID uniqueAlltimeID) {
        if (uniqueAlltimeID != null) {
            fire(eventType.name() + "." + eventID, dls, eventType + "." + eventID + uniqueAlltimeID.getID());
        } else {
            fire(eventType.name() + "." + eventID, dls, null);
        }
    }

    @Override
    public void onDownloadControllerUpdatedData(FilePackage pkg, FilePackageProperty property) {
        if (hasListener()) {
            boolean flush = false;
            if (property != null) {
                switch (property.getProperty()) {
                case COMMENT:
                    if (hasSubscriptionFor(BASIC_EVENT.PACKAGE_UPDATE.name())) {
                        final HashMap<String, Object> dls = new HashMap<String, Object>();
                        dls.put("uuid", pkg.getUniqueID().getID());
                        dls.put("comment", pkg.getComment());
                        fire(BASIC_EVENT.PACKAGE_UPDATE, FilePackageProperty.Property.COMMENT.name(), dls, pkg.getUniqueID());
                        flush = true;
                    }
                    break;
                case FOLDER:
                    break;
                case NAME:
                    if (hasSubscriptionFor(PACKAGE_UPDATE_name)) {
                        final HashMap<String, Object> dls = new HashMap<String, Object>();
                        dls.put("uuid", pkg.getUniqueID().getID());
                        dls.put("name", pkg.getName());
                        fire(PACKAGE_UPDATE_name, dls, PACKAGE_UPDATE_name + "." + pkg.getUniqueID().getID());
                        flush = true;
                    }
                    break;
                case PRIORITY:
                    if (hasSubscriptionFor(PACKAGE_UPDATE_priority)) {
                        final HashMap<String, Object> dls = new HashMap<String, Object>();
                        dls.put("uuid", pkg.getUniqueID().getID());
                        dls.put("priority", org.jdownloader.myjdownloader.client.bindings.PriorityStorable.get(pkg.getPriorityEnum().name()));
                        fire(PACKAGE_UPDATE_priority, dls, PACKAGE_UPDATE_priority + "." + pkg.getUniqueID().getID());
                        flush = true;
                    }
                    break;
                }
            }
            if (hasSubscriptionFor(BASIC_EVENT.REFRESH_CONTENT.name())) {
                fire(BASIC_EVENT.REFRESH_CONTENT.name(), null, BASIC_EVENT.REFRESH_CONTENT.name());
                flush = true;
            }
            if (flush) {
                flushBuffer();
            }
        }
    }

    @Override
    public void onDownloadControllerUpdatedData(DownloadLink downloadlink) {
        if (hasSubscriptionFor(BASIC_EVENT.REFRESH_CONTENT.name())) {
            fire(BASIC_EVENT.REFRESH_CONTENT.name(), null, BASIC_EVENT.REFRESH_CONTENT.name());
            flushBuffer();
        }
    }

    @Override
    public void onDownloadControllerUpdatedData(FilePackage pkg) {
        if (hasSubscriptionFor(BASIC_EVENT.REFRESH_CONTENT.name())) {
            fire(BASIC_EVENT.REFRESH_CONTENT.name(), null, BASIC_EVENT.REFRESH_CONTENT.name());
            flushBuffer();
        }
    }

    @Override
    public void onEventsChannelUpdate(Subscriber subscriber) {
        final ChannelCollector ret = getChannelCollector(subscriber.getSubscriptionID());
        if (ret != null) {
            ret.updateSubscriptions();
            boolean execute = false;
            for (final ChannelCollector collector : collectors) {
                if (collector.hasIntervalSubscriptions()) {
                    execute = true;
                }
            }
            final boolean finalExecute = execute;
            queue.add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    updateExecuter(finalExecute);
                    return null;
                }
            });
        }
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
                            HashSet<DownloadLink> linksToProcess = null;
                            HashSet<FilePackage> packagesToProcess = null;
                            if (true) {
                                for (final ChannelCollector collector : collectors) {
                                    if (collector.hasIntervalSubscriptions()) {
                                        kill = false;
                                        if (System.currentTimeMillis() - collector.getLastPush() >= collector.getInterval()) {
                                            collector.setLastPush(System.currentTimeMillis());
                                            if (linksToProcess == null) {
                                                linksToProcess = new HashSet<DownloadLink>();
                                                for (SingleDownloadController dl : DownloadWatchDog.getInstance().getRunningDownloadLinks()) {
                                                    linksToProcess.add(dl.getDownloadLink());
                                                }
                                                linksToProcess.addAll(linksWithPluginProgress);
                                            }
                                            for (final DownloadLink dl : linksToProcess) {
                                                final HashMap<String, Object> diff = collector.getDiff(dl);
                                                for (final Entry<String, Object> entry : diff.entrySet()) {
                                                    final HashMap<String, Object> dls = new HashMap<String, Object>();
                                                    dls.put("uuid", dl.getUniqueID().getID());
                                                    dls.put(entry.getKey(), entry.getValue());
                                                    final SimpleEventObject eventObject = new SimpleEventObject(DownloadControllerEventPublisher.this, BASIC_EVENT.LINK_UPDATE.name() + "." + entry.getKey(), dls, BASIC_EVENT.LINK_UPDATE.name() + "." + entry.getKey() + "." + dl.getUniqueID().getID());
                                                    final List<Long> publishTo = new ArrayList<Long>();
                                                    pushToBuffer(collector, eventObject);
                                                    events++;
                                                }
                                            }
                                            if (packagesToProcess == null) {
                                                packagesToProcess = new HashSet<FilePackage>();
                                                for (final DownloadLink dl : linksToProcess) {
                                                    final FilePackage p = dl.getParentNode();
                                                    if (p != null) {
                                                        packagesToProcess.add(p);
                                                    }
                                                }
                                            }
                                            for (final FilePackage p : packagesToProcess) {
                                                final HashMap<String, Object> diff = collector.getDiff(p);
                                                for (final Entry<String, Object> entry : diff.entrySet()) {
                                                    final HashMap<String, Object> dls = new HashMap<String, Object>();
                                                    dls.put("uuid", p.getUniqueID().getID());
                                                    dls.put(entry.getKey(), entry.getValue());
                                                    final SimpleEventObject eventObject = new SimpleEventObject(DownloadControllerEventPublisher.this, BASIC_EVENT.PACKAGE_UPDATE.name() + "." + entry.getKey(), dls, BASIC_EVENT.LINK_UPDATE.name() + "." + entry.getKey() + "." + p.getUniqueID().getID());
                                                    pushToBuffer(collector, eventObject);
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
                if (executer != null) {
                    executer.shutdownNow();
                    executer = null;
                }
            }
        }
    }

    protected void flushBuffer() {
        queue.add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                if (buffer.size() > 0) {
                    try {
                        for (final Entry<Subscriber, List<EventObject>> es : buffer.entrySet()) {
                            eventsAPI.push(es.getKey(), es.getValue());
                        }
                    } finally {
                        buffer.clear();
                    }
                }
                return null;
            }
        });
    }

    private final ChannelCollector getChannelCollector(long channelID) {
        if (collectors.size() > 0) {
            for (final ChannelCollector collector : collectors) {
                if (collector.getSubscriber().getSubscriptionID() == channelID) {
                    return collector;
                }
            }
        }
        return null;
    }

    @Override
    public void onEventChannelOpened(Subscriber s) {
        if (s != null) {
            final ChannelCollector ret = getChannelCollector(s.getSubscriptionID());
            if (ret == null) {
                collectors.add(new ChannelCollector(s));
            }
        }
    }

    @Override
    public void onEventChannelClosed(Subscriber s) {
        if (s != null) {
            final ChannelCollector ret = getChannelCollector(s.getSubscriptionID());
            if (ret != null) {
                collectors.remove(ret);
            }
        }
    }

    @Override
    public boolean setStatusEventInterval(long channelID, long interval) {
        final ChannelCollector collector = getChannelCollector(channelID);
        if (collector != null && collector.isAlive()) {
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
    public void onDownloadControllerStart(SingleDownloadController downloadController, final DownloadLinkCandidate candidate) {
        if (hasSubscriptionFor(BASIC_EVENT.LINK_UPDATE.name() + ".running")) {
            final DownloadLink dl = candidate.getLink();
            final HashMap<String, Object> dls = new HashMap<String, Object>();
            dls.put("uuid", dl.getUniqueID().getID());
            dls.put("running", true);
            fire(BASIC_EVENT.LINK_UPDATE.name() + ".running", dls, BASIC_EVENT.LINK_UPDATE.name() + ".running." + dl.getUniqueID().getID());
            flushBuffer();
        }
        if (collectors.size() > 0) {
            queue.add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    for (final ChannelCollector collector : collectors) {
                        if (collector.hasIntervalSubscriptions()) {
                            collector.updateBase(candidate.getLink());
                        }
                    }
                    updateExecuter(true);
                    return null;
                }
            });
        }
    }

    @Override
    public void onDownloadControllerStopped(SingleDownloadController downloadController, DownloadLinkCandidate candidate, DownloadLinkCandidateResult result) {
        final DownloadLink dl = candidate.getLink();
        if (hasSubscriptionFor(BASIC_EVENT.LINK_UPDATE.name() + ".running")) {
            HashMap<String, Object> dls = new HashMap<String, Object>();
            dls.put("uuid", dl.getUniqueID().getID());
            dls.put("running", false);
            fire(BASIC_EVENT.LINK_UPDATE.name() + ".running", dls, BASIC_EVENT.LINK_UPDATE.name() + ".running." + dl.getUniqueID().getID());
        }
        if (collectors.size() > 0) {
            pushDiff(dl);
            cleanup(dl);
            flushBuffer();
        }
    }

    private void cleanup(DownloadLink dl2) {
        if (collectors.size() > 0) {
            queue.add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    final HashSet<DownloadLink> linksToProcess = new HashSet<DownloadLink>();
                    for (SingleDownloadController dl : DownloadWatchDog.getInstance().getRunningDownloadLinks()) {
                        linksToProcess.add(dl.getDownloadLink());
                    }
                    linksToProcess.addAll(linksWithPluginProgress);
                    for (final ChannelCollector collector : collectors) {
                        collector.cleanUp(linksToProcess);
                    }
                    return null;
                }
            });
        }
    }

    private void pushDiff(final DownloadLink dl) {
        if (collectors.size() > 0) {
            final FilePackage parent = dl.getParentNode();
            queue.add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    for (final ChannelCollector collector : collectors) {
                        if (collector.hasIntervalSubscriptions()) {
                            collector.setLastPush(System.currentTimeMillis());
                            HashMap<String, Object> diff = collector.getDiff(dl);
                            for (Entry<String, Object> entry : diff.entrySet()) {
                                final HashMap<String, Object> dls = new HashMap<String, Object>();
                                dls.put("uuid", dl.getUniqueID().getID());
                                dls.put(entry.getKey(), entry.getValue());
                                final EventObject eventObject = new SimpleEventObject(DownloadControllerEventPublisher.this, BASIC_EVENT.LINK_UPDATE.name() + "." + entry.getKey(), dls, BASIC_EVENT.LINK_UPDATE.name() + "." + entry.getKey() + "." + dl.getUniqueID().getID());
                                pushToBuffer(collector, eventObject);
                            }
                            if (parent != null) {
                                diff = collector.getDiff(parent);
                                for (Entry<String, Object> entry : diff.entrySet()) {
                                    final HashMap<String, Object> dls = new HashMap<String, Object>();
                                    dls.put("uuid", parent.getUniqueID().getID());
                                    dls.put(entry.getKey(), entry.getValue());
                                    final SimpleEventObject eventObject = new SimpleEventObject(DownloadControllerEventPublisher.this, BASIC_EVENT.PACKAGE_UPDATE.name() + "." + entry.getKey(), dls, BASIC_EVENT.LINK_UPDATE.name() + "." + entry.getKey() + "." + parent.getUniqueID().getID());
                                    pushToBuffer(collector, eventObject);
                                }
                            }
                        }
                    }
                    return null;
                }
            });
        }
    }

    private void pushToBuffer(final ChannelCollector collector, final EventObject eventObject) {
        if (collector != null) {
            queue.add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    final Object dls = eventObject.getEventdata();
                    if (dls != null && dls instanceof HashMap) {
                        final HashMap<String, Object> copy = new HashMap<String, Object>((HashMap) dls);
                        if (!collector.updateDupeCache(eventObject.getEventid() + "." + copy.remove("uuid"), copy)) {
                            return null;
                        }
                    }
                    List<EventObject> lst = buffer.get(collector.getSubscriber());
                    if (lst == null) {
                        lst = new ArrayList<EventObject>();
                        buffer.put(collector.getSubscriber(), lst);
                    }
                    lst.add(eventObject);
                    return null;
                }
            });
        }
    }

    @Override
    public void onDownloadWatchDogPropertyChange(DownloadWatchDogProperty propertyChange) {
    }

    @Override
    public DownloadListDiffStorable queryLinks(LinkQueryStorable queryParams, int diffID) throws BadParameterException {
        throw new WTFException("Not Implemented");
    }

}
