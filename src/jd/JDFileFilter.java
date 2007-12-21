package jd;

import java.io.File;
import java.io.FileFilter;

/**
 * Mit dieser Klasse kann man sowohl bestimmte Dateien aus einem Verzeichnis
 * auflisten als auch einen FileFilter in einem JDFileChooser nutzen
 * 
 * @author astaldo
 */
public class JDFileFilter extends javax.swing.filechooser.FileFilter implements FileFilter {
    /**
     * Name der Datei ohne Extension
     */
    private String   name              = null;

    /**
     * Extension der Datei (mit Punkt)
     */
    private String[] extension         = null;
    private String extensions=null;

    /**
     * Sollen Verzeichnisse akzeptiert werden?
     */
    private boolean  acceptDirectories = true;

    /**
     * Erstellt einen neuen JDFileFilter
     * 
     * @param name Name der Datei ohne Extension
     * @param extension Extension der Datei (mit Punkt)
     * @param acceptDirectories Sollen Verzeichnisse akzeptiert werden?
     */
    public JDFileFilter(String name, String extension, boolean acceptDirectories) {
        this.name = name;
        if(extension!=null){
            
        this.extension = extension.split("\\|");

        extensions=extension;
        }
        this.acceptDirectories = acceptDirectories;
    }

    /**
     * Liefert ein FileObjekt zurück, daß aus dem Name und der Extension
     * zusammengesetzt wird
     * 
     * @return Ein FileObjekt
     */
    public File getFile() {
        StringBuffer filename = new StringBuffer();
        if (name != null)
            filename.append(name);
        else
            filename.append("*");
        if (extension != null)
            filename.append(extensions);
        else
            filename.append(".*");
        return new File(filename.toString());
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.filechooser.FileFilter#getDescription()
     */
    /**
     * Gibt die Filefilter beschreibung zurück
     */
    @Override
    public String getDescription() {
        return "Containerfiles";
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.filechooser.FileFilter#accept(java.io.File)
     */
    public boolean accept(File f) {
        if (f.isDirectory()) return acceptDirectories;
        if (extension != null) {
            for (int i = 0; i < extension.length; i++) {
                 if (f.getName().toLowerCase().endsWith(extension[i].toLowerCase())) return true;
            }
            return false;
        }
        if (name != null) {
            if (!f.getName().startsWith(name)) return false;
        }
        return true;
    }
}
