package jd.captcha.easy;

import java.io.File;
import java.util.ArrayList;

public  class EasyFile
{
    public File file = null;
    private static final long serialVersionUID = 1L;
    public EasyFile(File file) {
        this.file=file;
    }
    public boolean existsScriptJas()
    {
        return new File(file, "script.jas").exists();
    }
    public EasyFile() {
    }
    public EasyFile(String file)
    {
        this(new File(file));
    }
    @Override
    public String toString() {
        return file.getName();
    }
    public EasyFile[] listFiles() {
        File[] files = file.listFiles();
       ArrayList<EasyFile> ret = new ArrayList<EasyFile>();
        for (int i = 0; i < files.length; i++) {
           EasyFile ef = new EasyFile(files[i]);
           if(ef.existsScriptJas())
               ret.add(ef);
            
        }
        return ret.toArray(new EasyFile[] {});
    }
}