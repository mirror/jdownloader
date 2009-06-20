package jd.plugins.optional.langfileeditor;

public class LngEntry {

    private String key;
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    private String value;

    public LngEntry(String key, String value) {
        this.key = key;
        this.value = value;
    }
    public String toString(){
        return key+" = "+value;
    }

}
