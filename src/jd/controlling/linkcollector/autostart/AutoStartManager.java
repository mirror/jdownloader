package jd.controlling.linkcollector.autostart;

import java.util.ArrayList;
import java.util.List;

import jd.controlling.linkcollector.LinkCollectingInformation;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollector.MoveLinksMode;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.Application;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.EDTHelper;
import org.jdownloader.controlling.Priority;
import org.jdownloader.extensions.extraction.BooleanStatus;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.contextmenu.ConfirmLinksContextAction;
import org.jdownloader.gui.views.linkgrabber.contextmenu.ConfirmLinksContextAction.OnDupesLinksAction;
import org.jdownloader.gui.views.linkgrabber.contextmenu.ConfirmLinksContextAction.OnOfflineLinksAction;
import org.jdownloader.myjdownloader.client.json.AvailableLinkState;
import org.jdownloader.settings.staticreferences.CFG_LINKGRABBER;

public class AutoStartManager implements GenericConfigEventListener<Boolean> {
    private final DelayedRunnable             delayer;
    private volatile boolean                  globalAutoStart;
    private volatile boolean                  globalAutoConfirm;
    private final AutoStartManagerEventSender eventSender;

    public AutoStartManagerEventSender getEventSender() {
        return eventSender;
    }

    public AutoStartManager() {
        eventSender = new AutoStartManagerEventSender();
        CFG_LINKGRABBER.LINKGRABBER_AUTO_START_ENABLED.getEventSender().addListener(this, true);
        CFG_LINKGRABBER.LINKGRABBER_AUTO_CONFIRM_ENABLED.getEventSender().addListener(this, true);
        globalAutoStart = CFG_LINKGRABBER.LINKGRABBER_AUTO_START_ENABLED.isEnabled();
        globalAutoConfirm = CFG_LINKGRABBER.LINKGRABBER_AUTO_CONFIRM_ENABLED.isEnabled();
        final int minDelay = Math.max(1, CFG_LINKGRABBER.CFG.getAutoConfirmDelay());
        int maxDelay = CFG_LINKGRABBER.CFG.getAutoConfirmMaxDelay();
        if (maxDelay <= 0) {
            maxDelay = -1;
        } else if (maxDelay < minDelay) {
            maxDelay = minDelay;
        }
        delayer = new DelayedRunnable(minDelay, maxDelay) {
            @Override
            public String getID() {
                return "AutoConfirmButton";
            }

            @Override
            public void delayedrun() {
                final SelectionInfo<CrawledPackage, CrawledLink> selectionInfo;
                if (!Application.isHeadless() && CFG_LINKGRABBER.CFG.isAutoStartConfirmSidebarFilterEnabled()) {
                    /* dirty workaround */
                    selectionInfo = new EDTHelper<SelectionInfo<CrawledPackage, CrawledLink>>() {
                        @Override
                        public SelectionInfo<CrawledPackage, CrawledLink> edtRun() {
                            LinkGrabberTable.getInstance().getModel().fireStructureChange(true);
                            return LinkGrabberTable.getInstance().getSelectionInfo(false, true);
                        }
                    }.getReturnValue();
                } else {
                    selectionInfo = LinkCollector.getInstance().getSelectionInfo();
                }
                LinkCollector.getInstance().getQueue().add(new QueueAction<Void, RuntimeException>() {
                    @Override
                    protected Void run() throws RuntimeException {
                        if (eventSender.hasListener()) {
                            eventSender.fireEvent(new AutoStartManagerEvent(this, AutoStartManagerEvent.Type.RUN));
                        }
                        final boolean autoConfirm = globalAutoConfirm;
                        final boolean autoStart;
                        switch (CFG_LINKGRABBER.CFG.getAutoConfirmManagerAutoStart()) {
                        case DISABLED:
                            autoStart = false;
                            break;
                        case ENABLED:
                            autoStart = true;
                            break;
                        case AUTO:
                        default:
                            autoStart = globalAutoStart;
                            break;
                        }
                        final List<AbstractNode> list = new ArrayList<AbstractNode>(selectionInfo.getChildren().size());
                        boolean createNewSelection = false;
                        for (final CrawledLink child : selectionInfo.getChildren()) {
                            if (child.getLinkState() == AvailableLinkState.OFFLINE) {
                                createNewSelection = true;
                                continue;
                            } else {
                                if (autoConfirm || child.isAutoConfirmEnabled()) {
                                    list.add(child);
                                } else {
                                    createNewSelection = true;
                                }
                            }
                        }
                        if (list.size() > 0) {
                            final OnOfflineLinksAction onOfflineHandler = CFG_LINKGRABBER.CFG.getDefaultOnAddedOfflineLinksAction();
                            final OnDupesLinksAction onDupesHandler = CFG_LINKGRABBER.CFG.getDefaultOnAddedDupesLinksAction();
                            final Priority priority;
                            if (!CFG_LINKGRABBER.CFG.isAutoConfirmManagerAssignPriorityEnabled()) {
                                priority = null;
                            } else {
                                priority = CFG_LINKGRABBER.CFG.getAutoConfirmManagerPiority();
                            }
                            final SelectionInfo<CrawledPackage, CrawledLink> si;
                            if (createNewSelection) {
                                si = new SelectionInfo<CrawledPackage, CrawledLink>(null, list);
                            } else {
                                si = selectionInfo;
                            }
                            ConfirmLinksContextAction.confirmSelection(MoveLinksMode.AUTO, si, autoStart, CFG_LINKGRABBER.CFG.isAutoConfirmManagerClearListAfterConfirm(), CFG_LINKGRABBER.CFG.isAutoSwitchToDownloadTableOnConfirmDefaultEnabled(), priority, BooleanStatus.convert(CFG_LINKGRABBER.CFG.isAutoConfirmManagerForceDownloads()), onOfflineHandler, onDupesHandler);
                        }
                        if (delayer.isDelayerActive() == false && eventSender.hasListener()) {
                            eventSender.fireEvent(new AutoStartManagerEvent(this, AutoStartManagerEvent.Type.DONE));
                        }
                        return null;
                    }
                });
            }
        };
    }

    public void onLinkAdded(CrawledLink link) {
        if (globalAutoStart || globalAutoConfirm || link.isAutoConfirmEnabled() || link.isAutoStartEnabled() || link.isForcedAutoStartEnabled()) {
            final LinkCollectingInformation collectingInfo = link.getCollectingInfo();
            if (collectingInfo != null && collectingInfo.getLinkCrawler().isCollecting() && delayer.getMaximumDelay() == -1) {
                return;
            }
            delayer.resetAndStart();
            if (eventSender.hasListener()) {
                eventSender.fireEvent(new AutoStartManagerEvent(this, AutoStartManagerEvent.Type.RESET));
            }
        }
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        globalAutoStart = CFG_LINKGRABBER.LINKGRABBER_AUTO_START_ENABLED.isEnabled();
        globalAutoConfirm = CFG_LINKGRABBER.LINKGRABBER_AUTO_CONFIRM_ENABLED.isEnabled();
    }

    public int getMaximum() {
        return (int) (delayer.getMinimumDelay());
    }

    public int getValue() {
        return (int) (delayer.getEstimatedNextRun());
    }

    public boolean isRunning() {
        return delayer != null && delayer.isDelayerActive();
    }

    public void interrupt() {
        if (delayer.stop() && eventSender.hasListener()) {
            eventSender.fireEvent(new AutoStartManagerEvent(this, AutoStartManagerEvent.Type.DONE));
        }
    }
}
