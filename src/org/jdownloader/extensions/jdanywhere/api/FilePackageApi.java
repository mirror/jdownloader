package org.jdownloader.extensions.jdanywhere.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.storage.JSonStorage;
import org.jdownloader.extensions.jdanywhere.api.interfaces.IFilePackageApi;
import org.jdownloader.extensions.jdanywhere.api.storable.FilePackageInfoStorable;
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
                ret.add(new FilePackageStorable(fpkg));
            }
            return ret;
        } finally {
            dlc.readUnlock(b);
        }

    }

    @Override
    public byte[] listcompressed() {
        DownloadController dlc = DownloadController.getInstance();
        boolean b = dlc.readLock();
        try {
            java.util.List<FilePackageStorable> ret = new ArrayList<FilePackageStorable>(dlc.size());
            for (FilePackage fpkg : dlc.getPackages()) {
                ret.add(new FilePackageStorable(fpkg));
            }
            String returnValue = JSonStorage.toString(ret);
            return Helper.compress(returnValue);
            // return ret;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            dlc.readUnlock(b);
        }
        return null;
    }

    @Override
    public List<FilePackageStorable> listRanges(int startWith, int maxResults) {
        DownloadController dlc = DownloadController.getInstance();
        boolean b = dlc.readLock();
        try {
            java.util.List<FilePackageStorable> ret = new ArrayList<FilePackageStorable>(dlc.size());
            if (startWith > dlc.size() - 1) return ret;
            if (startWith < 0) startWith = 0;
            if (maxResults < 0) maxResults = dlc.size();

            for (int i = startWith; i < Math.min(startWith + maxResults, dlc.size()); i++) {
                FilePackage fpkg = dlc.getPackages().get(i);
                ret.add(new FilePackageStorable(fpkg));
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
    public boolean remove(long ID) {
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
        FilePackage fpkg = Helper.getFilePackageFromID(ID);
        for (DownloadLink link : fpkg.getChildren()) {
            link.setEnabled(enabled);
        }
        return true;
    }

    public boolean priority(long ID, int priority) {
        FilePackage fpkg = Helper.getFilePackageFromID(ID);
        if (fpkg != null) {
            for (DownloadLink link : fpkg.getChildren()) {
                link.setPriority(priority);
            }
            return true;
        } else
            return false;
    }

    @Override
    public boolean reset(long ID) {
        try {
            FilePackage fpkg = Helper.getFilePackageFromID(ID);
            for (DownloadLink link : fpkg.getChildren()) {
                link.reset();
            }
            return true;
        } finally {
        }
    }

    public boolean forceDownload(long ID) {
        try {
            FilePackage fpkg = Helper.getFilePackageFromID(ID);
            if (fpkg != null) {
                DownloadWatchDog dwd = DownloadWatchDog.getInstance();
                List<DownloadLink> sdl = fpkg.getChildren();
                dwd.forceDownload(sdl);
                return true;
            }
        } finally {
        }
        return false;
    }

    @Override
    public FilePackageInfoStorable getInformation(long ID) {
        FilePackage fpkg = Helper.getFilePackageFromID(ID);
        return new FilePackageInfoStorable(fpkg);
    }

    @Override
    public FilePackageStorable getFilePackage(long ID) {
        FilePackage fpkg = Helper.getFilePackageFromID(ID);
        return new FilePackageStorable(fpkg);
    }
}
