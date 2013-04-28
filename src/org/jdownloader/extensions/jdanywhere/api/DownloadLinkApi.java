package org.jdownloader.extensions.jdanywhere.api;

import java.util.ArrayList;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.api.downloads.DownloadsAPIImpl;
import org.jdownloader.extensions.jdanywhere.api.interfaces.IDownloadLinkApi;
import org.jdownloader.extensions.jdanywhere.api.storable.DownloadLinkInfoStorable;
import org.jdownloader.extensions.jdanywhere.api.storable.DownloadLinkStorable;

public class DownloadLinkApi implements IDownloadLinkApi {

    DownloadsAPIImpl dlAPI = new DownloadsAPIImpl();

    /*
     * (non-Javadoc)
     * 
     * @see org.jdownloader.extensions.jdanywhere.api.IDownloadLink#list(long)
     */
    @Override
    public List<DownloadLinkStorable> list(long ID) {
        DownloadController dlc = DownloadController.getInstance();
        for (FilePackage fpkg : dlc.getPackages()) {
            if (fpkg.getUniqueID().getID() == ID) {
                synchronized (fpkg) {
                    List<DownloadLinkStorable> links = new ArrayList<DownloadLinkStorable>(fpkg.size());
                    for (DownloadLink link : fpkg.getChildren()) {
                        links.add(new DownloadLinkStorable(link));
                    }
                    return links;
                }
            }
        }
        return null;
    }

    @Override
    public boolean remove(final List<Long> linkIds) {
        return dlAPI.removeLinks(linkIds);
    }

    // Sets the enabled flag of a downloadlink
    // used in iPhone-App
    @Override
    public boolean setEnabled(final List<Long> linkIds, boolean enabled) {
        if (enabled) {
            return dlAPI.enableLinks(linkIds);
        } else {
            return dlAPI.disableLinks(linkIds);
        }
    }

    @Override
    public boolean reset(final List<Long> linkIds) {
        List<DownloadLink> list = Helper.getFilteredDownloadLinks(linkIds);
        if (list != null && !list.isEmpty()) {
            for (DownloadLink dl : list) {
                dl.reset();
            }
        }
        return true;
    }

    @Override
    public DownloadLinkInfoStorable getInformation(long ID) {
        DownloadLink link = Helper.getDownloadLinkFromID(ID);
        return new DownloadLinkInfoStorable(link);
    }

    @Override
    public DownloadLinkStorable getDownloadLink(long ID) {
        DownloadLink link = Helper.getDownloadLinkFromID(ID);
        return new DownloadLinkStorable(link);
    }

    @Override
    public boolean setPriority(final List<Long> linkIds, int priority) {
        List<DownloadLink> list = Helper.getFilteredDownloadLinks(linkIds);
        if (list != null && !list.isEmpty()) {
            for (DownloadLink dl : list) {
                dl.setPriority(priority);
            }
        }
        return true;
    }

    @Override
    public boolean forceDownload(final List<Long> linkIds) {
        return dlAPI.forceDownload(linkIds);
    }

}
