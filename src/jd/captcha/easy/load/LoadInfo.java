package jd.captcha.easy.load;

public class LoadInfo {
    boolean followLinks = false;

    public String link;
    public int menge = 100;
    public boolean directLoad = true;

    public LoadInfo(String link, int menge) {
        this.link = link;
        this.menge = menge;
    }
}