package org.jdownloader.extensions.jdanywhere.api.interfaces;

import java.util.List;

import org.appwork.remoteapi.ApiNamespace;
import org.appwork.remoteapi.ApiSessionRequired;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.jdownloader.extensions.jdanywhere.api.storable.DownloadLinkStorable;

@ApiNamespace("jdanywhere/downloadLink")
@ApiSessionRequired
public interface IDownloadLinkApi extends RemoteAPIInterface {

    public abstract List<DownloadLinkStorable> list(long ID);

    public abstract boolean remove(List<Long> linkIds);

    // Sets the enabled flag of a downloadlink
    // used in iPhone-App
    public abstract boolean setEnabled(List<Long> linkIds, boolean enabled);

    public abstract boolean reset(List<Long> linkIds);

    public abstract DownloadLinkStorable getDownloadLink(long ID);

}