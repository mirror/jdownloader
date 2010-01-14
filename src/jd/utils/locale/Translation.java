package jd.utils.locale;

public class Translation {

    private String original;
    private String destLanguage;
    private String sourceLanguage;
    private String translated;

    public String getOriginal() {
        return original;
    }

    public void setOriginal(String original) {
        this.original = original;
    }

    public String getDestLanguage() {
        return destLanguage;
    }

    public void setDestLanguage(String destLanguage) {
        this.destLanguage = destLanguage;
    }

    public String getSourceLanguage() {
        return sourceLanguage;
    }

    public void setSourceLanguage(String sourceLanguage) {
        this.sourceLanguage = sourceLanguage;
    }

    public String getTranslated() {
        return translated;
    }

    public void setTranslated(String translated) {
        this.translated = translated;
    }

    public Translation(String msg, String to) {
        this.original=msg;
        this.destLanguage=to;
    }

  

}
