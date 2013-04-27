package org.jdownloader.extensions.jdanywhere.api.interfaces;

import java.util.List;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.annotations.ApiSessionRequired;
import org.jdownloader.extensions.jdanywhere.api.storable.FilePackageStorable;

@ApiNamespace("jdanywhere/filePackage")
@ApiSessionRequired
public interface IFilePackageApi extends RemoteAPIInterface {

    public abstract List<FilePackageStorable> list();

    public abstract List<FilePackageStorable> listRanges(int startWith, int maxResults);

    public abstract String getIDFromLinkID(long ID);

    public abstract boolean remove(long ID);

    // Sets the enabled flag of a downloadPackage
    // used in iPhone-App
    public abstract boolean setEnabled(long ID, boolean enabled);

    public boolean priority(long ID, int priority);

    public boolean reset(long ID);

    public abstract FilePackageStorable getFilePackage(long ID);

    public boolean forceDownload(long ID);

}