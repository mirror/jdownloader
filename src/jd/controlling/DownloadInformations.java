//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.controlling;

/**
 * Contains all needful information about the downloads
 * 
 * @author botzi
 * 
 */
public class DownloadInformations {
    private long totalDownloadSize = 0;
    private long currentDownloadSize = 0;
    private int packages = 0;
    private int downloadLinks = 0;
    private int disabledDownloads = 0;
    private int runningDownloads = 0;
    private int finishedDownloads = 0;
    private int duplicateDownloads = 0;

    protected void reset() {
        totalDownloadSize = 0;
        currentDownloadSize = 0;
        packages = 0;
        downloadLinks = 0;
        disabledDownloads = 0;
        runningDownloads = 0;
        finishedDownloads = 0;
        duplicateDownloads = 0;
    }

    protected void addTotalDownloadSize(final long size) {
        totalDownloadSize += size;
    }

    protected void addCurrentDownloadSize(final long size) {
        currentDownloadSize += size;
    }

    protected void addPackages(final int size) {
        packages += size;
    }

    protected void addDownloadLinks(final int size) {
        downloadLinks += size;
    }

    protected void addDisabledDownloads(final int size) {
        disabledDownloads += size;
    }

    protected void addRunningDownloads(final int size) {
        runningDownloads += size;
    }

    protected void addFinishedDownloads(final int size) {
        finishedDownloads += size;
    }

    protected void addDuplicateDownloads(final int size) {
        duplicateDownloads += size;
    }

    /**
     * Returns the total downloadsize
     */
    public long getTotalDownloadSize() {
        return totalDownloadSize;
    }

    /**
     * Returns the current downloaded size
     */
    public long getCurrentDownloadSize() {
        return currentDownloadSize;
    }

    /**
     * Returns the count of all packages
     */
    public int getPackagesCount() {
        return packages;
    }

    /**
     * Returns the count of all downloads
     */
    public int getDownloadCount() {
        return downloadLinks;
    }

    /**
     * Returns the count of all disabled downloads
     */
    public int getDisabledDownloads() {
        return disabledDownloads;
    }

    /**
     * Returns the count of all running downloads
     */
    public int getRunningDownloads() {
        return runningDownloads;
    }

    /**
     * Returns the count of all finished downloads
     */
    public int getFinishedDownloads() {
        return finishedDownloads;
    }

    /**
     * Returns the count of all duplicate downloads
     */
    public int getDuplicateDownloads() {
        return duplicateDownloads;
    }

    public long getETA() {
        long etanum = 0;
        if (DownloadWatchDog.getInstance().getConnectionManager().getIncommingBandwidthUsage() > 1024) etanum = (getTotalDownloadSize() - getCurrentDownloadSize()) / DownloadWatchDog.getInstance().getConnectionManager().getIncommingBandwidthUsage();
        return etanum;
    }

    public double getPercent() {
        return Math.round((getCurrentDownloadSize() * 10000.0) / getTotalDownloadSize()) / 100.0;
    }

}
