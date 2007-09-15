package jd.plugins;

import java.io.File;
import java.io.Serializable;

import jd.JDUtilities;

/**
 * Diese Klasse verwaltet Pakete
 * @author coalado
 */
public class FilePackage implements Serializable{
    /**
     * 
     */
    private static int counter=0;
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
  public String toString(){
      return id;
  }
public String getComment() {
    return comment;
}
public void setComment(String comment) {
    this.comment = comment;
}
public String getPassword() {
    return password;
}
public void setPassword(String password) {
    this.password = password;
}
public String getDownloadDirectory() {
    return downloadDirectory;
}
public void setDownloadDirectory(String subFolder) {
    this.downloadDirectory = subFolder;
}
public String getDownloadDirectoryName(){
    if(!hasDownloadDirectory())return ".";
    return new File(downloadDirectory).getName();
}
public boolean hasPassword(){
    return password!=null && password.length()>0;
}
public boolean hasDownloadDirectory(){
    return downloadDirectory!=null && downloadDirectory.length()>0;
}

public boolean hasComment(){
    return comment!=null && comment.length()>0;
}

}
