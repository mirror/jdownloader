package jd.controlling.linkcollector.autostart;

import java.util.ArrayList;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.controlling.Priority;
import org.jdownloader.extensions.extraction.BooleanStatus;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.linkgrabber.contextmenu.ConfirmLinksContextAction;
import org.jdownloader.gui.views.linkgrabber.contextmenu.ConfirmLinksContextAction.OnOfflineLinksAction;
import org.jdownloader.myjdownloader.client.json.AvailableLinkState;
import org.jdownloader.settings.staticreferences.CFG_LINKGRABBER;

public class AutoStartManager implements GenericConfigEventListener<Boolean> {

    private final DelayedRunnable             delayer;
    private volatile boolean                  globalAutoStart;
    private volatile boolean                  globalAutoConfirm;
    private long                              lastReset;
    private final AutoStartManagerEventSender eventSender;
    private final int                         waittime;
    private long                              lastStarted;

    public AutoStartManagerEventSender getEventSender() {
        return eventSender;
    }

    public AutoStartManager() {

        eventSender = new AutoStartManagerEventSender();
        CFG_LINKGRABBER.LINKGRABBER_AUTO_START_ENABLED.getEventSender().addListener(this, true);
        CFG_LINKGRABBER.LINKGRABBER_AUTO_CONFIRM_ENABLED.getEventSender().addListener(this, true);
        globalAutoStart = CFG_LINKGRABBER.LINKGRABBER_AUTO_START_ENABLED.isEnabled();
        globalAutoConfirm = CFG_LINKGRABBER.LINKGRABBER_AUTO_CONFIRM_ENABLED.isEnabled();
        waittime = CFG_LINKGRABBER.CFG.getAutoConfirmDelay();
        delayer = new DelayedRunnable(waittime, -1) {

            @Override
            public String getID() {
                return "AutoConfirmButton";
            }

            @Override
            public void delayedrun() {
                LinkCollector.getInstance().getQueue().add(new QueueAction<Void, RuntimeException>() {
                    @Override
                    protected Void run() throws RuntimeException {
                        if (eventSender.hasListener()) {
                            eventSender.fireEvent(new AutoStartManagerEvent(this, AutoStartManagerEvent.Type.RUN));
                        }
                        boolean autoConfirm = globalAutoConfirm;
                        boolean autoStart = globalAutoStart;

                        switch (CFG_LINKGRABBER.CFG.getAutoConfirmManagerAutoStart()) {

                        case DISABLED:
                            autoStart = false;
                            break;
                        case ENABLED:
                            autoStart = true;
                            break;

                        }
                        java.util.List<AbstractNode> list = new ArrayList<AbstractNode>();

                        SelectionInfo<CrawledPackage, CrawledLink> sel = new SelectionInfo<CrawledPackage, CrawledLink>(null, LinkCollector.getInstance().getPackages(), CFG_LINKGRABBER.CFG.isAutoStartConfirmSidebarFilterEnabled());

                        for (CrawledLink l : sel.getChildren()) {
                            if (l.getLinkState() == AvailableLinkState.OFFLINE) {
                                continue;
                            }

                            if (l.isAutoConfirmEnabled() || autoConfirm) {
                                list.add(l);
                                if (l.isAutoStartEnabled()) {
                                    autoStart = true;
                                }
                            }
                        }
                        OnOfflineLinksAction onOfflineHandler = CFG_LINKGRABBER.CFG.getDefaultOnAddedOfflineLinksAction();
                        if (onOfflineHandler == OnOfflineLinksAction.GLOBAL) {
                            onOfflineHandler = CFG_LINKGRABBER.CFG.getDefaultOnAddedOfflineLinksAction();
                        }
                        Priority priority = CFG_LINKGRABBER.CFG.getAutoConfirmManagerPiority();
                        if (!CFG_LINKGRABBER.CFG.isAutoConfirmManagerAssignPriorityEnabled()) {
                            priority = null;
                        }
                        if (list.size() > 0) {
                            ConfirmLinksContextAction.confirmSelection(new SelectionInfo<CrawledPackage, CrawledLink>(null, list, false), autoStart, CFG_LINKGRABBER.CFG.isAutoConfirmManagerClearListAfterConfirm(), CFG_LINKGRABBER.CFG.isAutoSwitchToDownloadTableOnConfirmDefaultEnabled(), priority, CFG_LINKGRABBER.CFG.isAutoConfirmManagerForceDownloads() ? BooleanStatus.TRUE : BooleanStatus.FALSE, onOfflineHandler);
                        }

                        // lastReset = -1;
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
        if (globalAutoStart || globalAutoConfirm || link.isAutoConfirmEnabled() || link.isAutoStartEnabled()) {
            if (!delayer.isDelayerActive()) {
                lastStarted = System.currentTimeMillis();
            }
            delayer.resetAndStart();
            lastReset = System.currentTimeMillis();
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
        return (int) (lastReset + waittime - lastStarted);
    }

    public int getValue() {
        return (int) (System.currentTimeMillis() - lastStarted);
    }

    public void interrupt() {
        if (delayer.stop() && eventSender.hasListener()) {
            eventSender.fireEvent(new AutoStartManagerEvent(this, AutoStartManagerEvent.Type.DONE));
        }
    }
}
