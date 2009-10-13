package jd.plugins.optional.langfileeditor;

public class KeyInfo implements Comparable<KeyInfo> {

    private final String key;

    private String source = "";

    private String language = "";

    private String enValue;

    public KeyInfo(String key, String source, String language, String enValue) {
        this.key = key;
        this.setSource(source);
        this.enValue = enValue;
        this.setLanguage(language);
    }

    public String getKey() {
        return this.key;
    }

    public String getLanguage() {
        return this.language;
    }

    public String getSource() {
        return this.source;
    }

    public void setLanguage(String language) {
        if (language != null) this.language = language;
    }

    public void setSource(String source) {
        if (source != null) this.source = source;
    }

    public boolean isMissing() {
        return this.getLanguage().equals("");
    }

    public boolean isOld() {
        return this.getSource().equals("");
    }

    public int compareTo(KeyInfo o) {
        return this.getKey().compareToIgnoreCase(o.getKey());
    }

    @Override
    public String toString() {
        return this.getKey() + " = " + this.getLanguage();
    }

    /**
     * Returns the english value
     * 
     * @return
     */
    public String getEnglish() {
        return enValue;
    }

}