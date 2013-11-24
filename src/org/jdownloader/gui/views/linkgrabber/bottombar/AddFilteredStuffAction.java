package org.jdownloader.gui.views.linkgrabber.bottombar;

import java.awt.event.ActionEvent;

import javax.swing.JComponent;

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
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.bottombar.SelfComponentFactoryInterface;
import org.jdownloader.gui.views.downloads.bottombar.SelfLayoutInterface;

public class AddFilteredStuffAction extends CustomizableAppAction implements ActionContext, SelfComponentFactoryInterface, SelfLayoutInterface {

    private boolean onlyVisibleIfThereIsFilteredStuff = true;

    public AddFilteredStuffAction() {
        setIconKey("filter");

    }

    @Override
    public void requestUpdate(Object requestor) {
        super.requestUpdate(requestor);

    }

    @Customizer(name = "Only Visible if there is filtered stuff")
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
                java.util.List<CrawledLink> filteredStuff = LinkCollector.getInstance().getFilteredStuff(true);
                LinkCollector.getInstance().addCrawlerJob(filteredStuff);
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
                        setText(_GUI._.RestoreFilteredLinksAction_(size));
                        setVisible(true);
                    }
                };
            } else {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        setText(_GUI._.RestoreFilteredLinksAction_(0));
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

    }

    @Override
    public JComponent createComponent() {
        Button ret = new Button(this);
        ret.setEnabled(true);

        return ret;
    }

}
