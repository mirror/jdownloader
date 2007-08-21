package jd;

import java.io.File;
import java.io.FileFilter;

/**
 * Als FileFilter akzeptiert diese Klasse alle .jar Dateien
 *
 * @author astaldo
 */
public class JDFileFilter extends  javax.swing.filechooser.FileFilter implements FileFilter{
    private String name      = null;
    private String extension = null;
    private boolean acceptDirectories = true;
    
    public JDFileFilter(String name, String extension, boolean acceptDirectories){
        this.name              = name;
        this.extension         = extension;
        this.acceptDirectories = acceptDirectories;
    }
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
        if(f.isDirectory() && acceptDirectories==false)
            return false;
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
