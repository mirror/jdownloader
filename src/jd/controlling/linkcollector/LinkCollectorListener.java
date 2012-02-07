package jd.controlling.linkcollector;

import java.util.EventListener;

public interface LinkCollectorListener extends EventListener {
    /**
     * abort requested
     * 
     * @param event
     */
    void onLinkCollectorAbort(LinkCollectorEvent event);

    /**
     * new filtered stuff is available
     * 
     * @param event
     */
    void onLinkCollectorFilteredLinksAvailable(LinkCollectorEvent event);

    /**
     * all filtered stuff is gone
     * 
     * @param event
     */
    void onLinkCollectorFilteredLinksEmpty(LinkCollectorEvent event);

    /**
     * only refresh the content data
     * 
     * @param event
     */
    void onLinkCollectorDataRefresh(LinkCollectorEvent event);

    /**
     * refresh content structure
     * 
     * @param event
     */
    void onLinkCollectorStructureRefresh(LinkCollectorEvent event);

    /**
     * content got removed
     * 
     * @param event
     */
    void onLinkCollectorLinksRemoved(LinkCollectorEvent event);

}