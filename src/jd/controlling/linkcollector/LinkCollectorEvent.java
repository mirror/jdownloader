package jd.controlling.linkcollector;

import org.appwork.utils.event.SimpleEvent;
import org.appwork.utils.event.queue.Queue.QueuePriority;

public class LinkCollectorEvent extends SimpleEvent<LinkCollector, Object, LinkCollectorEvent.TYPE> {

    private QueuePriority prio = QueuePriority.NORM;

    public QueuePriority getPrio() {
        return prio;
    }

    public void setPrio(QueuePriority prio) {
        this.prio = prio;
    }

    public LinkCollectorEvent(LinkCollector caller, TYPE type, Object[] parameters, QueuePriority prio) {
        super(caller, type, parameters);
        this.prio = prio;
    }

    public LinkCollectorEvent(LinkCollector caller, TYPE type, Object parameter, QueuePriority prio) {
        super(caller, type, new Object[] { parameter });
        this.prio = prio;
    }

    public LinkCollectorEvent(LinkCollector caller, TYPE type, QueuePriority prio) {
        super(caller, type);
        this.prio = prio;
    }

    public LinkCollectorEvent(LinkCollector caller, TYPE type) {
        this(caller, type, QueuePriority.NORM);
    }

    public static enum TYPE {
        /* new filtered stuff is available */
        FILTERED_AVAILABLE,
        /* all filtered stuff is gone */
        FILTERED_EMPTY,
        /* only refresh the content data */
        REFRESH_DATA,
        /* refresh content structure */
        REFRESH_STRUCTURE,
        /* content got removed */
        REMOVE_CONTENT,
        ADD_CONTENT,
        REFRESH_CONTENT,
        /* request collector abort */
        ABORT,
        /**
         * New link has been added and grouped.<br>
         * Parameter[0]: (CrawledLink) added Link
         */
        ADDED_LINK,
        /**
         * Dupe link has been added. This event is just to inform<br>
         * Parameter[0]: (CrawledLink) added Link
         */
        DUPE_LINK

    }

}