package org.jdownloader.extensions.jdanywhere.api.interfaces;

import java.util.List;

import org.appwork.remoteapi.ApiNamespace;
import org.appwork.remoteapi.ApiSessionRequired;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.jdownloader.extensions.jdanywhere.api.storable.FilePackageStorable;

@ApiNamespace("jdanywhere/filePackage")
@ApiSessionRequired
public interface IFilePackageApi extends RemoteAPIInterface {

    public abstract List<FilePackageStorable> list();

    public abstract String getIDFromLinkID(long ID);

    public abstract boolean remove(long ID);

    // Sets the enabled flag of a downloadPackage
    // used in iPhone-App
    public abstract boolean setEnabled(long ID, boolean enabled);

    public abstract FilePackageStorable getFilePackage(long ID);

}