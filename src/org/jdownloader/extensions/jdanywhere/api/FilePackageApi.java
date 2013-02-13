package org.jdownloader.extensions.jdanywhere.api;

import java.util.ArrayList;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.extensions.jdanywhere.api.interfaces.IFilePackageApi;
import org.jdownloader.extensions.jdanywhere.api.storable.FilePackageStorable;

public class FilePackageApi implements IFilePackageApi {

    /*
     * (non-Javadoc)
     * 
     * @see org.jdownloader.extensions.jdanywhere.api.IFilePackageApi#list()
     */
    @Override
    public List<FilePackageStorable> list() {
        DownloadController dlc = DownloadController.getInstance();
        boolean b = dlc.readLock();
        try {
            java.util.List<FilePackageStorable> ret = new ArrayList<FilePackageStorable>(dlc.size());
            for (FilePackage fpkg : dlc.getPackages()) {
                @SuppressWarnings("unused")
                FilePackageStorable pkg;
                ret.add(pkg = new FilePackageStorable(fpkg));
            }
            return ret;
        } finally {
            dlc.readUnlock(b);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jdownloader.extensions.jdanywhere.api.IFilePackageApi#getIDFromLinkID(long)
     */
    @Override
    public String getIDFromLinkID(long ID) {
        DownloadLink dl = Helper.getDownloadLinkFromID(ID);
        FilePackage fpk = dl.getFilePackage();
        return fpk.getUniqueID().toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jdownloader.extensions.jdanywhere.api.IFilePackageApi#removeDownloadPackage(long)
     */
    @Override
    public boolean removeDownloadPackage(long ID) {
        FilePackage fpkg = Helper.getFilePackageFromID(ID);
        if (fpkg != null) {
            DownloadController.getInstance().removePackage(fpkg);
        }
        return true;
    }

    // Sets the enabled flag of a downloadPackage
    // used in iPhone-App
    /*
     * (non-Javadoc)
     * 
     * @see org.jdownloader.extensions.jdanywhere.api.IFilePackageApi#setEnabled(long, boolean)
     */
    @Override
    public boolean setEnabled(long ID, boolean enabled) {
        DownloadController dlc = DownloadController.getInstance();
        boolean b = dlc.readLock();
        long id = Long.valueOf(ID);
        try {
            for (FilePackage fpkg : dlc.getPackages()) {
                if (fpkg.getUniqueID().getID() == id) synchronized (fpkg) {
                    for (DownloadLink link : fpkg.getChildren()) {
                        link.setEnabled(enabled);
                    }
                    return true;
                }
            }
        } finally {
            dlc.readUnlock(b);
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jdownloader.extensions.jdanywhere.api.IFilePackageApi#getFilePackage(long)
     */
    @Override
    public FilePackageStorable getFilePackage(long ID) {
        FilePackage fpkg = Helper.getFilePackageFromID(ID);
        return new FilePackageStorable(fpkg);
    }
}
