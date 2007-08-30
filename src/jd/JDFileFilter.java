package jd;

import java.io.File;
import java.io.FileFilter;

/**
 * Mit dieser Klasse kann man sowohl bestimmte Dateien aus einem Verzeichnis auflisten als auch
 * einen FileFilter in einem JFileChooser nutzen
 *
 * @author astaldo
 */
public class JDFileFilter extends  javax.swing.filechooser.FileFilter implements FileFilter{
    /**
     * Name der Datei ohne Extension
     */
    private String name      = null;
    /**
     * Extension der Datei (mit Punkt)
     */
    private String extension = null;
    /**
     * Sollen Verzeichnisse akzeptiert werden?
     */
    private boolean acceptDirectories = true;
    /**
     * Erstellt einen neuen JDFileFilter
     * 
     * @param name Name der Datei ohne Extension 
     * @param extension Extension der Datei (mit Punkt)
     * @param acceptDirectories Sollen Verzeichnisse akzeptiert werden?
     */
    public JDFileFilter(String name, String extension, boolean acceptDirectories){
        this.name              = name;
        this.extension         = extension;
        this.acceptDirectories = acceptDirectories;
    }
    /**
     * Liefert ein FileObjekt zurück, daß aus dem Name und der Extension zusammengesetzt wird
     * 
     * @return Ein FileObjekt
     */
    public File getFile(){
        StringBuffer filename = new StringBuffer();
        if(name != null)
            filename.append(name);
        else
            filename.append("*");
        if(extension!=null)
            filename.append(extension);
        else
            filename.append(".*");
        return new File(filename.toString());
    }
    
    @Override
    public String getDescription() {
        return getFile().toString();
    }

    public boolean accept(File f) {
        if(f.isDirectory())
            return acceptDirectories;
        if(extension != null){
            if(!f.getName().endsWith(extension))
                return false;
        }
        if(name != null){
            if(!f.getName().startsWith(name))
                return false;
        }
        return true;
    }
}
