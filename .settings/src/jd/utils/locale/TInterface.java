package jd.utils.locale;

import java.io.File;
import java.util.HashMap;

public class TInterface {

    private HashMap<String, ConvertEntry> map;
    private File                          path;
    private String                        name;

    public File getPath() {
        return path;
    }

    public HashMap<String, ConvertEntry> getMap() {
        return map;
    }

    public TInterface(File trans) {
        map = new HashMap<String, ConvertEntry>();
        this.path = trans;
        name = trans.getParentFile().getName();
        name = name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    public File getTranslationFile() {
        return new File(path, name + "Translation.java");
    }

    public File getShortFile() {
        return new File(path, "T.java");
    }

    public String getClassName() {
        return name;
    }

}
