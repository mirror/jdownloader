package jd.controlling;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadLinkCandidate;
import jd.controlling.downloadcontroller.DownloadLinkCandidateResult;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.DownloadWatchDogProperty;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.downloadcontroller.event.DownloadWatchdogListener;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorCrawler;
import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcollector.LinkCollectorListener;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLinkProperty;
import jd.plugins.FilePackage;
import jd.plugins.FilePackageProperty;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.utils.Application;
import org.appwork.utils.logging2.LogInterface;
import org.jdownloader.controlling.download.DownloadControllerListener;
import org.jdownloader.settings.DelayWriteMode;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;

public class DelayWriteController {

    private static final DelayWriteController INSTANCE = new DelayWriteController();

    private DelayWriteController() {
    }

    public static final DelayWriteController getInstance() {
        return INSTANCE;
    }

    private final AtomicBoolean              initialized                = new AtomicBoolean(false);
    private final DelayedRunnable            flushWrites                = new DelayedRunnable(10000l, 120000l) {
                                                                            @Override
                                                                            public String getID() {
                                                                                return "DelayWriteController";
                                                                            }

                                                                            @Override
                                                                            public void delayedrun() {
                                                                                StorageHandler.flushWrites();
                                                                            }

                                                                        };

    private final DownloadWatchdogListener   downloadWatchDogListener   = new DownloadWatchdogListener() {

                                                                            @Override
                                                                            public void onDownloadWatchdogStateIsStopping() {
                                                                                flushWrites.resetAndStart();
                                                                            }

                                                                            @Override
                                                                            public void onDownloadWatchdogStateIsStopped() {
                                                                                flushWrites.resetAndStart();
                                                                            }

                                                                            @Override
                                                                            public void onDownloadWatchdogStateIsRunning() {
                                                                                flushWrites.resetAndStart();
                                                                            }

                                                                            @Override
                                                                            public void onDownloadWatchdogStateIsPause() {
                                                                                flushWrites.resetAndStart();
                                                                            }

                                                                            @Override
                                                                            public void onDownloadWatchdogStateIsIdle() {
                                                                                flushWrites.resetAndStart();
                                                                            }

                                                                            @Override
                                                                            public void onDownloadWatchdogDataUpdate() {
                                                                                flushWrites.resetAndStart();
                                                                            }

                                                                            @Override
                                                                            public void onDownloadWatchDogPropertyChange(DownloadWatchDogProperty propertyChange) {
                                                                                flushWrites.resetAndStart();
                                                                            }

                                                                            @Override
                                                                            public void onDownloadControllerStopped(SingleDownloadController downloadController, DownloadLinkCandidate candidate, DownloadLinkCandidateResult result) {
                                                                                flushWrites.resetAndStart();
                                                                            }

                                                                            @Override
                                                                            public void onDownloadControllerStart(SingleDownloadController downloadController, DownloadLinkCandidate candidate) {
                                                                                flushWrites.resetAndStart();
                                                                            }
                                                                        };

    private final LinkCollectorListener      linkCollectorListener      = new LinkCollectorListener() {

                                                                            @Override
                                                                            public void onLinkCrawlerStopped(LinkCollectorCrawler crawler) {
                                                                                flushWrites.resetAndStart();
                                                                            }

                                                                            @Override
                                                                            public void onLinkCrawlerStarted(LinkCollectorCrawler crawler) {
                                                                                flushWrites.resetAndStart();
                                                                            }

                                                                            @Override
                                                                            public void onLinkCrawlerNewJob(LinkCollectingJob job) {
                                                                                flushWrites.resetAndStart();
                                                                            }

                                                                            @Override
                                                                            public void onLinkCrawlerAdded(LinkCollectorCrawler crawler) {
                                                                                flushWrites.resetAndStart();
                                                                            }

                                                                            @Override
                                                                            public void onLinkCollectorStructureRefresh(LinkCollectorEvent event) {
                                                                                flushWrites.resetAndStart();
                                                                            }

                                                                            @Override
                                                                            public void onLinkCollectorLinkAdded(LinkCollectorEvent event, CrawledLink link) {
                                                                                flushWrites.resetAndStart();
                                                                            }

                                                                            @Override
                                                                            public void onLinkCollectorFilteredLinksEmpty(LinkCollectorEvent event) {
                                                                                flushWrites.resetAndStart();
                                                                            }

                                                                            @Override
                                                                            public void onLinkCollectorFilteredLinksAvailable(LinkCollectorEvent event) {
                                                                                flushWrites.resetAndStart();
                                                                            }

                                                                            @Override
                                                                            public void onLinkCollectorDupeAdded(LinkCollectorEvent event, CrawledLink link) {
                                                                                flushWrites.resetAndStart();
                                                                            }

                                                                            @Override
                                                                            public void onLinkCollectorDataRefresh(LinkCollectorEvent event) {
                                                                                flushWrites.resetAndStart();
                                                                            }

                                                                            @Override
                                                                            public void onLinkCollectorContentRemoved(LinkCollectorEvent event) {
                                                                                flushWrites.resetAndStart();
                                                                            }

                                                                            @Override
                                                                            public void onLinkCollectorContentAdded(LinkCollectorEvent event) {
                                                                                flushWrites.resetAndStart();
                                                                            }

                                                                            @Override
                                                                            public void onLinkCollectorAbort(LinkCollectorEvent event) {
                                                                                flushWrites.resetAndStart();
                                                                            }

                                                                            @Override
                                                                            public void onLinkCrawlerFinished() {
                                                                                flushWrites.resetAndStart();
                                                                            }
                                                                        };

    private final DownloadControllerListener downloadControllerListener = new DownloadControllerListener() {

                                                                            @Override
                                                                            public void onDownloadControllerUpdatedData(FilePackage pkg) {
                                                                                flushWrites.resetAndStart();
                                                                            }

                                                                            @Override
                                                                            public void onDownloadControllerUpdatedData(DownloadLink downloadlink) {
                                                                                flushWrites.resetAndStart();
                                                                            }

                                                                            @Override
                                                                            public void onDownloadControllerUpdatedData(FilePackage pkg, FilePackageProperty property) {
                                                                                flushWrites.resetAndStart();
                                                                            }

                                                                            @Override
                                                                            public void onDownloadControllerUpdatedData(DownloadLink downloadlink, DownloadLinkProperty property) {
                                                                                flushWrites.resetAndStart();
                                                                            }

                                                                            @Override
                                                                            public void onDownloadControllerStructureRefresh(AbstractNode node, Object param) {
                                                                                flushWrites.resetAndStart();
                                                                            }

                                                                            @Override
                                                                            public void onDownloadControllerStructureRefresh() {
                                                                                flushWrites.resetAndStart();
                                                                            }

                                                                            @Override
                                                                            public void onDownloadControllerStructureRefresh(FilePackage pkg) {
                                                                                flushWrites.resetAndStart();
                                                                            }

                                                                            @Override
                                                                            public void onDownloadControllerRemovedPackage(FilePackage pkg) {
                                                                                flushWrites.resetAndStart();
                                                                            }

                                                                            @Override
                                                                            public void onDownloadControllerRemovedLinklist(List<DownloadLink> list) {
                                                                                flushWrites.resetAndStart();
                                                                            }

                                                                            @Override
                                                                            public void onDownloadControllerAddedPackage(FilePackage pkg) {
                                                                                flushWrites.resetAndStart();
                                                                            }
                                                                        };

    public void init() {
        if (initialized.compareAndSet(false, true)) {
            final LogInterface logger = org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger();
            final DelayWriteMode mode = CFG_GENERAL.CFG.getDelayWriteMode();
            logger.info("DelayWriteMode: " + mode);
            if ((DelayWriteMode.AUTO.equals(mode) && Application.isHeadless())) {
                logger.info("DelayedWrites auto enabled because JDownloader is running in headless mode!");
                setEnabled(true);
            } else if (DelayWriteMode.ON.equals(mode)) {
                logger.info("DelayedWrites enabled!");
                setEnabled(true);
            } else {
                logger.info("DelayedWrites disabled!");
            }
        }
    }

    private void setEnabled(final boolean enabled) {
        if (enabled) {
            DownloadController.getInstance().getEventSender().addListener(downloadControllerListener);
            DownloadWatchDog.getInstance().getEventSender().addListener(downloadWatchDogListener);
            LinkCollector.getInstance().getEventsender().addListener(linkCollectorListener);
        } else {
            DownloadController.getInstance().getEventSender().removeListener(downloadControllerListener);
            DownloadWatchDog.getInstance().getEventSender().removeListener(downloadWatchDogListener);
            LinkCollector.getInstance().getEventsender().removeListener(linkCollectorListener);
        }
        StorageHandler.setDelayedWritesEnabled(enabled);
    }

}
