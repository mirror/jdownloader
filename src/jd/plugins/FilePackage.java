package jd.plugins;

import java.io.File;
import java.io.Serializable;

import jd.JDUtilities;
import jd.config.Property;

/**
 * Diese Klasse verwaltet Pakete
 * @author coalado
 */
public class FilePackage extends Property implements Serializable{
    /**
     * 
     */
    /**
     * Zählt die instanzierungen durch um eine ID zu erstellen
     */
    private static int counter=0;
    /**
     * Eindeutige PaketID
     */
    private String id;
    private static final long serialVersionUID = -8859842964299890820L;
    private String comment;
    private String password;
    private String downloadDirectory;
   
 
  public FilePackage(){
      downloadDirectory=JDUtilities.getConfiguration().getDownloadDirectory();
      counter++;
      id=System.currentTimeMillis()+"_"+counter;
      
  }
  /**
   * Alles undokumentiert, da selbsterklärend
   */
  
  
  public String toString(){
      return id;
  }
  /**
   * 
   * @return Gibt den Kommentar ab den der user im Linkgrabber zu diesem Paket abgegeben hat
   */
public String getComment() {
    return comment;
}
public void setComment(String comment) {
    this.comment = comment;
}
/**
 * 
 * @return Gibt das Archivpasswort zurück das der User für dieses paket angegeben hat
 */
public String getPassword() {
    return password;
}
public void setPassword(String password) {
    this.password = password;
}
/**
 * 
 * @return Gibt den Downloadpfad zurück den der user für dieses paket festgelegt hta
 */
public String getDownloadDirectory() {
    return downloadDirectory;
}
public void setDownloadDirectory(String subFolder) {
    this.downloadDirectory = subFolder;
}
/**
 *
 * @return Gibt nur den namen des Downloadverzeichnisses zurück.
 * ACHTUNG! es wird nur der Directory-NAME zurückgegeben, nicht der ganze Pfad
 */
public String getDownloadDirectoryName(){
    if(!hasDownloadDirectory())return ".";
    return new File(downloadDirectory).getName();
}

/**
 * 
 * @return true/false, je nachdem ob ein Passwort festgelegt wurde (archivpasswort)
 */
public boolean hasPassword(){
    return password!=null && password.length()>0;
}
/**
 * 
 * @return True/false, je nach dem ob ein downloadirectory festgelegt wurde
 */
public boolean hasDownloadDirectory(){
    return downloadDirectory!=null && downloadDirectory.length()>0;
}
/**
 * 
 * @return True/false, je nach dem ob ein Kommentar gespeichert ist
 */
public boolean hasComment(){
    return comment!=null && comment.length()>0;
}

}
