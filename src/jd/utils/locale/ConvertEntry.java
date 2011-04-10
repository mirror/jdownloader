package jd.utils.locale;

import java.io.File;
import java.util.ArrayList;

public class ConvertEntry {

    private String key;

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    private ArrayList<File> files;
    private String          value;

    public ConvertEntry(String e, String value) {
        key = e;
        this.value = value;
        files = new ArrayList<File>();
    }

    public ArrayList<File> getFiles() {
        return files;
    }

    public void addFile(File file) {
        files.remove(file);
        files.add(file);
    }

}
