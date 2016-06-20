package org.jdownloader.gui.views.linkgrabber.bottombar;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JComponent;

import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorCrawler;
import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcollector.LinkCollectorListener;
import jd.controlling.linkcrawler.CrawledLink;

import org.appwork.swing.components.ExtButton;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.bottombar.SelfComponentFactoryInterface;
import org.jdownloader.gui.views.downloads.bottombar.SelfLayoutInterface;
import org.jdownloader.translate._JDT;

public class AddFilteredStuffAction extends CustomizableAppAction implements ActionContext, SelfComponentFactoryInterface, SelfLayoutInterface {

    private boolean onlyVisibleIfThereIsFilteredStuff = true;

    public AddFilteredStuffAction() {
        setIconKey(IconKey.ICON_FILTER);

    }

    @Override
    public void requestUpdate(Object requestor) {
        super.requestUpdate(requestor);

    }

    public static String getTranslationForOnlyVisibleIfThereIsFilteredStuff() {
        return _JDT.T.AddFilteredStuffAction_getTranslationForOnlyVisibleIfThereIsFilteredStuff();
    }

    @Customizer(link = "#getTranslationForOnlyVisibleIfThereIsFilteredStuff")
    public boolean isOnlyVisibleIfThereIsFilteredStuff() {
        return onlyVisibleIfThereIsFilteredStuff;
    }

    public void setOnlyVisibleIfThereIsFilteredStuff(boolean onlyVisibleIfThereIsFilteredStuff) {
        this.onlyVisibleIfThereIsFilteredStuff = onlyVisibleIfThereIsFilteredStuff;

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        LinkCollector.getInstance().getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                final List<CrawledLink> filteredStuff = LinkCollector.getInstance().getFilteredStuff(true);
                final HashMap<LinkCollectingJob, List<CrawledLink>> jobs = new HashMap<LinkCollectingJob, List<CrawledLink>>();
                if (filteredStuff != null && filteredStuff.size() > 0) {
                    for (final CrawledLink link : filteredStuff) {
                        final LinkCollectingJob job = link.getSourceJob();
                        List<CrawledLink> list = jobs.get(job);
                        if (list == null) {
                            list = new ArrayList<CrawledLink>();
                            jobs.put(job, list);
                        }
                        list.add(link);
                    }
                    for (Entry<LinkCollectingJob, List<CrawledLink>> entry : jobs.entrySet()) {
                        LinkCollector.getInstance().addCrawlerJob(entry.getValue(), entry.getKey());
                    }
                }
                return null;
            }
        });
    }

    @Override
    public String createConstraints() {
        return "height 24!,aligny top,hidemode 3";
    }

    public class Button extends ExtButton implements LinkCollectorListener {

        public Button(AddFilteredStuffAction addFilteredStuffAction) {
            super(addFilteredStuffAction);
            LinkCollector.getInstance().getEventsender().addListener(this, true);
            setFilteredAvailable(LinkCollector.getInstance().getfilteredStuffSize());
        }

        private void setFilteredAvailable(final int size) {

            if (size > 0) {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        setText(_GUI.T.RestoreFilteredLinksAction_(size));
                        setVisible(true);
                    }
                };
            } else {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        setText(_GUI.T.RestoreFilteredLinksAction_(0));
                        setVisible(!isOnlyVisibleIfThereIsFilteredStuff());
                    }
                };
            }
        }

        @Override
        public void onLinkCollectorAbort(LinkCollectorEvent event) {
        }

        @Override
        public void onLinkCollectorFilteredLinksAvailable(LinkCollectorEvent event) {
            setFilteredAvailable(LinkCollector.getInstance().getfilteredStuffSize());
        }

        @Override
        public void onLinkCollectorFilteredLinksEmpty(LinkCollectorEvent event) {
            setFilteredAvailable(0);
        }

        @Override
        public void onLinkCollectorDataRefresh(LinkCollectorEvent event) {
        }

        @Override
        public void onLinkCollectorStructureRefresh(LinkCollectorEvent event) {
        }

        @Override
        public void onLinkCollectorContentRemoved(LinkCollectorEvent event) {
        }

        @Override
        public void onLinkCollectorContentAdded(LinkCollectorEvent event) {
        }

        @Override
        public void onLinkCollectorLinkAdded(LinkCollectorEvent event, CrawledLink parameter) {
        }

        @Override
        public void onLinkCollectorDupeAdded(LinkCollectorEvent event, CrawledLink parameter) {
        }

        @Override
        public void onLinkCrawlerAdded(LinkCollectorCrawler parameter) {
        }

        @Override
        public void onLinkCrawlerStarted(LinkCollectorCrawler parameter) {
        }

        @Override
        public void onLinkCrawlerStopped(LinkCollectorCrawler parameter) {
        }

        @Override
        public void onLinkCrawlerNewJob(LinkCollectingJob job) {
        }

        @Override
        public void onLinkCrawlerFinished() {
        }

    }

    @Override
    public JComponent createComponent() {
        Button ret = new Button(this);
        ret.setEnabled(true);

        return ret;
    }

}
