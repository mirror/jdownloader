package jd.plugins;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Vector;

import jd.config.Property;
import jd.utils.JDUtilities;

/**
 * Diese Klasse verwaltet Pakete
 * 
 * @author coalado
 */
public class FilePackage extends Property implements Serializable {
    /**
     * 
     */
    /**
     * Zählt die instanzierungen durch um eine ID zu erstellen
     */
    private static int        counter          = 0;
    /**
     * Eindeutige PaketID
     */
    private String            id;
    private static final long serialVersionUID = -8859842964299890820L;
    private String            comment;
    private String            password;
    private String            name;
    private String            downloadDirectory;
    private Vector<DownloadLink> downloadLinks;

    public FilePackage() {
        downloadDirectory = JDUtilities.getConfiguration().getDownloadDirectory();
        counter++;
        id = System.currentTimeMillis() + "_" + counter;

    }
    /**
     * Diese Methode speichert Paketinformationen ab (falls die Datei noch nicht bereits besteht)
     */
    public void saveInfoFile(){
        String filename;
        if (getName() != null && !getName().equals(""))
            filename = getName();
        else
            filename = getId();
        filename = getDownloadDirectory()+"/"+filename+".info";
        try {
            File infoFile = new File(filename);
            if(!infoFile.exists()){
                
                infoFile.createNewFile();
                DataOutputStream dos = new DataOutputStream(new FileOutputStream(infoFile));
                
                dos.writeBytes("Passwort für die folgenden Archive:"+ getPassword()+"\n\n");
                
                Iterator<DownloadLink> iterator = getDownloadLinks().iterator();
                while(iterator.hasNext()){
                    dos.writeBytes(iterator.next().getName()+"\n");
                }
                dos.close();
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
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
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getId() {
        return id;
    }
    public Vector<DownloadLink> getDownloadLinks() {
        return downloadLinks;
    }
    public void setDownloadLinks(Vector<DownloadLink> downloadLinks) {
        this.downloadLinks = downloadLinks;
    }

}
