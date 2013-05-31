package org.jdownloader.extensions.jdanywhere.api.interfaces;

import java.util.List;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.annotations.ApiSessionRequired;
import org.jdownloader.extensions.jdanywhere.api.storable.DownloadLinkInfoStorable;
import org.jdownloader.extensions.jdanywhere.api.storable.DownloadLinkStorable;

@ApiNamespace("jdanywhere/downloadLink")
@ApiSessionRequired
public interface IDownloadLinkApi extends RemoteAPIInterface {

    public abstract List<DownloadLinkStorable> list(long ID);

    public abstract byte[] listcompressed(long ID);

    public abstract boolean remove(List<Long> linkIds);

    // Sets the enabled flag of a downloadlink
    // used in iPhone-App
    public abstract boolean setEnabled(List<Long> linkIds, boolean enabled);

    public abstract boolean reset(List<Long> linkIds);

    public abstract DownloadLinkStorable getDownloadLink(long ID);

    public abstract boolean setPriority(final List<Long> linkIds, int priority);

    public abstract boolean forceDownload(final List<Long> linkIds);

    public abstract DownloadLinkInfoStorable getInformation(long ID);

}