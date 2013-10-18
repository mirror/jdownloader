package org.jdownloader.gui.views.linkgrabber.bottombar;

import java.awt.event.ActionEvent;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorCrawler;
import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcollector.LinkCollectorListener;
import jd.controlling.linkcrawler.CrawledLink;

import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.actions.AppAction;
import org.jdownloader.actions.CachableInterface;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.translate._GUI;

public class AddFilteredStuffAction extends AppAction implements CachableInterface, LinkCollectorListener {

    private boolean onlyVisibleIfThereIsFilteredStuff = true;

    public AddFilteredStuffAction() {
        setIconKey("filter");

        LinkCollector.getInstance().getEventsender().addListener(this, true);
        setFilteredAvailable(LinkCollector.getInstance().getfilteredStuffSize());
    }

    @Customizer(name = "Only Visible if there is filtered stuff")
    public boolean isOnlyVisibleIfThereIsFilteredStuff() {
        return onlyVisibleIfThereIsFilteredStuff;
    }

    public void setOnlyVisibleIfThereIsFilteredStuff(boolean onlyVisibleIfThereIsFilteredStuff) {
        this.onlyVisibleIfThereIsFilteredStuff = onlyVisibleIfThereIsFilteredStuff;

        new EDTRunner() {

            @Override
            protected void runInEDT() {
                setFilteredAvailable(LinkCollector.getInstance().getfilteredStuffSize());
            }
        };

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

    private void setFilteredAvailable(final int size) {

        if (size > 0) {
            new EDTRunner() {

                @Override
                protected void runInEDT() {
                    setName(_GUI._.RestoreFilteredLinksAction_(size));
                    setVisible(true);
                }
            };
        } else {
            new EDTRunner() {

                @Override
                protected void runInEDT() {
                    setName(_GUI._.RestoreFilteredLinksAction_(0));
                    setVisible(!isOnlyVisibleIfThereIsFilteredStuff());
                }
            };
        }
    }

    @Override
    public void setData(String data) {
        setFilteredAvailable(LinkCollector.getInstance().getfilteredStuffSize());
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
