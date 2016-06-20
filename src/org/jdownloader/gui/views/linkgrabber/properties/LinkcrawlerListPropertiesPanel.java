package org.jdownloader.gui.views.linkgrabber.properties;

import java.awt.event.ActionEvent;

import javax.swing.JPopupMenu;

import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorCrawler;
import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcollector.LinkCollectorListener;
import jd.controlling.linkcrawler.CrawledLink;

import org.jdownloader.gui.components.CheckboxMenuItem;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.properties.AbstractNodeProperties;
import org.jdownloader.gui.views.downloads.properties.AbstractNodePropertiesPanel;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public abstract class LinkcrawlerListPropertiesPanel<E extends AbstractNodeProperties> extends AbstractNodePropertiesPanel<E> implements LinkCollectorListener {

    public LinkcrawlerListPropertiesPanel() {
        super();
        CFG_GUI.LINK_PROPERTIES_PANEL_SAVE_TO_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.LINK_PROPERTIES_PANEL_FILENAME_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.LINK_PROPERTIES_PANEL_PACKAGENAME_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.LINK_PROPERTIES_PANEL_DOWNLOAD_FROM_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.LINK_PROPERTIES_PANEL_DOWNLOAD_PASSWORD_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.LINK_PROPERTIES_PANEL_CHECKSUM_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.LINK_PROPERTIES_PANEL_COMMENT_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.LINK_PROPERTIES_PANEL_ARCHIVEPASSWORD_VISIBLE.getEventSender().addListener(this, true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        refresh();
    }

    public void fillPopup(JPopupMenu pu) {
        pu.add(new CheckboxMenuItem(_GUI.T.LinkgrabberPropertiesHeader_packagename(), CFG_GUI.LINK_PROPERTIES_PANEL_PACKAGENAME_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI.T.LinkgrabberPropertiesHeader_filename(), CFG_GUI.LINK_PROPERTIES_PANEL_FILENAME_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI.T.LinkgrabberPropertiesHeader_saveto(), CFG_GUI.LINK_PROPERTIES_PANEL_SAVE_TO_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI.T.LinkgrabberPropertiesHeader_downloadfrom(), CFG_GUI.LINK_PROPERTIES_PANEL_DOWNLOAD_FROM_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI.T.LinkgrabberPropertiesHeader_downloadpassword(), CFG_GUI.LINK_PROPERTIES_PANEL_DOWNLOAD_PASSWORD_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI.T.LinkgrabberPropertiesHeader_checksum(), CFG_GUI.LINK_PROPERTIES_PANEL_CHECKSUM_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI.T.LinkgrabberPropertiesHeader_comment_and_priority(), CFG_GUI.LINK_PROPERTIES_PANEL_COMMENT_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI.T.LinkgrabberPropertiesHeader_archiveline(), CFG_GUI.LINK_PROPERTIES_PANEL_ARCHIVEPASSWORD_VISIBLE));
    }

    @Override
    protected boolean isArchiveLineEnabled() {
        return CFG_GUI.LINK_PROPERTIES_PANEL_ARCHIVEPASSWORD_VISIBLE.isEnabled();
    }

    @Override
    protected boolean isCheckSumEnabled() {
        return CFG_GUI.LINK_PROPERTIES_PANEL_CHECKSUM_VISIBLE.isEnabled();
    }

    @Override
    protected boolean isCommentAndPriorityEnabled() {
        return CFG_GUI.LINK_PROPERTIES_PANEL_COMMENT_VISIBLE.isEnabled();
    }

    @Override
    protected boolean isDownloadFromEnabled() {
        return CFG_GUI.LINK_PROPERTIES_PANEL_DOWNLOAD_FROM_VISIBLE.isEnabled();
    }

    @Override
    protected boolean isDownloadPasswordEnabled() {
        return CFG_GUI.LINK_PROPERTIES_PANEL_DOWNLOAD_PASSWORD_VISIBLE.isEnabled();
    }

    @Override
    protected boolean isFileNameEnabled() {
        return CFG_GUI.LINK_PROPERTIES_PANEL_FILENAME_VISIBLE.isEnabled();
    }

    @Override
    protected boolean isPackagenameEnabled() {
        return CFG_GUI.LINK_PROPERTIES_PANEL_PACKAGENAME_VISIBLE.isEnabled();
    }

    @Override
    protected boolean isSaveToEnabled() {
        return CFG_GUI.LINK_PROPERTIES_PANEL_SAVE_TO_VISIBLE.isEnabled();
    }

    @Override
    public void onLinkCollectorAbort(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorContentAdded(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorContentRemoved(LinkCollectorEvent event) {
    }

    @Override
    public abstract void onLinkCollectorDataRefresh(LinkCollectorEvent event);

    @Override
    public void onLinkCollectorDupeAdded(LinkCollectorEvent event, CrawledLink parameter) {
    }

    @Override
    public void onLinkCollectorFilteredLinksAvailable(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorFilteredLinksEmpty(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorLinkAdded(LinkCollectorEvent event, CrawledLink parameter) {
    }

    @Override
    public void onLinkCollectorStructureRefresh(LinkCollectorEvent event) {
        refresh();
    }

    @Override
    public void onLinkCrawlerFinished() {
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
    protected void onHidden() {
        try {
            super.onHidden();
        } finally {
            LinkCollector.getInstance().getEventsender().removeListener(this);
        }
    }

    @Override
    protected void onShowing() {
        try {
            super.onShowing();
        } finally {
            LinkCollector.getInstance().getEventsender().addListener(this, true);
        }
    }

    @Override
    public void onLinkCrawlerNewJob(LinkCollectingJob job) {
    }
}
