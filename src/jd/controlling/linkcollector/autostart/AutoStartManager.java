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
import org.jdownloader.extensions.extraction.BooleanStatus;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.linkgrabber.contextmenu.ConfirmLinksContextAction;
import org.jdownloader.myjdownloader.client.json.AvailableLinkState;
import org.jdownloader.settings.staticreferences.CFG_LINKGRABBER;

public class AutoStartManager implements GenericConfigEventListener<Boolean> {

    private DelayedRunnable             delayer;
    private boolean                     globalAutoStart;
    private boolean                     globalAutoConfirm;
    private long                        lastReset;
    private AutoStartManagerEventSender eventSender;
    private int                         waittime;
    private long                        lastStarted;

    public AutoStartManagerEventSender getEventSender() {
        return eventSender;
    }

    public AutoStartManager() {
        eventSender = new AutoStartManagerEventSender();
        globalAutoStart = CFG_LINKGRABBER.LINKGRABBER_AUTO_START_ENABLED.isEnabled();
        globalAutoConfirm = CFG_LINKGRABBER.LINKGRABBER_AUTO_CONFIRM_ENABLED.isEnabled();
        CFG_LINKGRABBER.LINKGRABBER_AUTO_START_ENABLED.getEventSender().addListener(this, true);
        CFG_LINKGRABBER.LINKGRABBER_AUTO_CONFIRM_ENABLED.getEventSender().addListener(this, true);
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
                        eventSender.fireEvent(new AutoStartManagerEvent(this, AutoStartManagerEvent.Type.RUN));
                        boolean autoConfirm = globalAutoConfirm;
                        boolean autoStart = globalAutoStart;
                        java.util.List<AbstractNode> list = new ArrayList<AbstractNode>();

                        SelectionInfo<CrawledPackage, CrawledLink> sel = new SelectionInfo<CrawledPackage, CrawledLink>(null, LinkCollector.getInstance().getPackages(), CFG_LINKGRABBER.CFG.isAutoStartConfirmSidebarFilterEnabled());

                        for (CrawledLink l : sel.getChildren()) {
                            if (l.getLinkState() == AvailableLinkState.OFFLINE) continue;

                            if (l.isAutoConfirmEnabled() || autoConfirm) {
                                list.add(l);
                                if (l.isAutoStartEnabled()) autoStart = true;
                            }
                        }

                        ConfirmLinksContextAction.confirmSelection(new SelectionInfo<CrawledPackage, CrawledLink>(null, list, false), autoStart, false, false, null, BooleanStatus.UNSET);

                        // lastReset = -1;

                        eventSender.fireEvent(new AutoStartManagerEvent(this, AutoStartManagerEvent.Type.DONE));
                        return null;
                    }
                });
            }
        };
    }

    public void onLinkAdded(CrawledLink link) {

        if (!globalAutoStart && !globalAutoConfirm && !link.isAutoConfirmEnabled() && !link.isAutoStartEnabled()) return;

        if (!delayer.isDelayerActive()) {
            lastStarted = System.currentTimeMillis();
        }
        delayer.resetAndStart();

        lastReset = System.currentTimeMillis();
        eventSender.fireEvent(new AutoStartManagerEvent(this, AutoStartManagerEvent.Type.RESET));

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
        delayer.stop();

        eventSender.fireEvent(new AutoStartManagerEvent(this, AutoStartManagerEvent.Type.DONE));

    }
}
