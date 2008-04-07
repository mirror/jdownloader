//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://wnu.org/licenses/>.

package jd.plugins;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Vector;

import jd.captcha.pixelgrid.Letter;
import jd.captcha.pixelobject.PixelObject;
import jd.config.Configuration;
import jd.config.Property;
import jd.utils.JDUtilities;

/**
 * Diese Klasse verwaltet Pakete
 * 
 * @author JD-Team
 */
public class FilePackage extends Property implements Serializable {
    /**
     * 
     */
    /**
     * Zählt die instanzierungen durch um eine ID zu erstellen
     */
    private static int           counter          = 0;

    /**
     * Eindeutige PaketID
     */
    private String               id;

    private static final long    serialVersionUID = -8859842964299890820L;

    private String               comment;

    private String               password;

    private String               name;

    private String               downloadDirectory;

    private Vector<DownloadLink> downloadLinks;

    public FilePackage() {
        downloadDirectory = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY);
        counter++;
        id = System.currentTimeMillis() + "_" + counter;
        downloadLinks = new Vector<DownloadLink>();

    }

    /**
     * Diese Methode speichert Paketinformationen ab (falls die Datei noch nicht
     * bereits besteht)
     */

    /**
     * Alles undokumentiert, da selbsterklärend
     */
    public String toString() {
        return id;
    }

    /**
     * 
     * @return Gibt den Kommentar ab den der user im Linkgrabber zu diesem Paket
     *         abgegeben hat
     */
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * 
     * @return Gibt das Archivpasswort zurück das der User für dieses paket
     *         angegeben hat
     */
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * 
     * @return Gibt den Downloadpfad zurück den der user für dieses paket
     *         festgelegt hta
     */
    public String getDownloadDirectory() {
        return downloadDirectory;
    }

    public void setDownloadDirectory(String subFolder) {
        this.downloadDirectory = subFolder;
    }

    /**
     * 
     * @return Gibt nur den namen des Downloadverzeichnisses zurück. ACHTUNG! es
     *         wird nur der Directory-NAME zurückgegeben, nicht der ganze Pfad
     */
    public String getDownloadDirectoryName() {
        if (!hasDownloadDirectory()) return ".";
        return new File(downloadDirectory).getName();
    }

    /**
     * 
     * @return true/false, je nachdem ob ein Passwort festgelegt wurde
     *         (archivpasswort)
     */
    public boolean hasPassword() {
        return password != null && password.length() > 0;
    }

    /**
     * 
     * @return True/false, je nach dem ob ein downloadirectory festgelegt wurde
     */
    public boolean hasDownloadDirectory() {
        return downloadDirectory != null && downloadDirectory.length() > 0;
    }

    /**
     * 
     * @return True/false, je nach dem ob ein Kommentar gespeichert ist
     */
    public boolean hasComment() {
        return comment != null && comment.length() > 0;
    }

    public String getName() {
        if (name == null) return "";
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void add(DownloadLink link) {

        if (!downloadLinks.contains(link)) downloadLinks.add(link);
        link.setFilePackage(this);
    }

    public void add(int index, DownloadLink link) {

        link.setFilePackage(this);
        if (downloadLinks.contains(link)) {
            downloadLinks.remove(link);
        }
        downloadLinks.add(index, link);
        JDUtilities.getLogger().info("");
    }

    public void addAllAt(Vector<DownloadLink> links, int index) {
        for (int i = 0; i < links.size(); i++) {
            add(index + i, links.get(i));
        }

    }

    public void addAll(Vector<DownloadLink> links) {
        for (int i = 0; i < links.size(); i++) {
            add(links.get(i));
        }
    }

    public boolean remove(DownloadLink link) {
        boolean ret = downloadLinks.remove(link);
        if (ret) link.setFilePackage(null);
        return ret;
    }

    public DownloadLink remove(int index) {
        DownloadLink link = downloadLinks.remove(index);
        link.setFilePackage(null);
        return link;
    }

    public boolean contains(DownloadLink link) {
        return downloadLinks.contains(link);
    }

    public int indexOf(DownloadLink link) {

        return downloadLinks.indexOf(link);
    }

    public DownloadLink get(int index) {
        return downloadLinks.get(index);
    }

    public Vector<DownloadLink> getDownloadLinks() {
        return downloadLinks;
    }

    public void setDownloadLinks(Vector<DownloadLink> downloadLinks) {
        this.downloadLinks = new Vector<DownloadLink>(downloadLinks);
    }

    public int size() {

        return downloadLinks.size();
    }

    public long getEstimatedPackageSize() {
        long total = 0;
        int avg = 0;
        int i = 0;
        int current;
        DownloadLink next;
        for (Iterator<DownloadLink> it = downloadLinks.iterator(); it.hasNext();) {
            next = it.next();
            current = (int) next.getDownloadMax();
            if (current <= 0) {
                current = avg;
            }
            avg = avg * i + current / (i + 1);
            total += current;
        }
        return total;
    }

    public long getTotalLoadedPackageBytes() {
        long total = 0;
        for (Iterator<DownloadLink> it = downloadLinks.iterator(); it.hasNext();) {
            total += it.next().getDownloadCurrent();
        }
        return total;
    }

    public void sort(String string) {
        synchronized(downloadLinks){
        Collections.sort(downloadLinks, new Comparator<DownloadLink>() {

            public int compare(DownloadLink a, DownloadLink b) {
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });
        }
        
    }

}
