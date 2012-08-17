package jd.http.ext;

import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.http.Request;

import org.appwork.utils.logging.Log;

public class AdBlocker implements AdBlockerInterface {
    private static final AdBlocker INSTANCE = new AdBlocker();

    public static AdBlocker getInstance() {
        return INSTANCE;
    }

    private java.util.List<Pattern> blackList;
    private java.util.List<Pattern> whiteList;

    private AdBlocker() {
        this.blackList = new ArrayList<Pattern>();
        // Google & Adsense
        blackList.add(Pattern.compile(".*\\.doubleclick\\..*", Pattern.CASE_INSENSITIVE));
        blackList.add(Pattern.compile(".*partner\\.googleadservices\\..*", Pattern.CASE_INSENSITIVE));
        blackList.add(Pattern.compile(".*\\.googlesyndication\\..*", Pattern.CASE_INSENSITIVE));
        blackList.add(Pattern.compile(".*\\.google\\..*", Pattern.CASE_INSENSITIVE));
        blackList.add(Pattern.compile(".*harrenmedianetwork.*", Pattern.CASE_INSENSITIVE));
        blackList.add(Pattern.compile(".*rubiconproject.*", Pattern.CASE_INSENSITIVE));
        blackList.add(Pattern.compile(".*scorecardresearch.*", Pattern.CASE_INSENSITIVE));

        // addthis
        blackList.add(Pattern.compile(".*\\.addthis\\.com.*", Pattern.CASE_INSENSITIVE));

        this.whiteList = new ArrayList<Pattern>();
        // google login
        whiteList.add(Pattern.compile("https://www.google.com/accounts/ServiceLogin.*"));

    }

    public boolean doBlockRequest(Request request) {
        String url = request.getUrl();
        for (Pattern p : whiteList) {
            if (p.matcher(url).matches()) {
                //
                Log.L.info("ADWhitelist: " + url);
                return false;
                //
            }
        }
        for (Pattern p : blackList) {
            if (p.matcher(url).matches()) {
                //
                Log.L.info("Adblocked: " + url);
                return true;
                //
            }
        }
        return false;
    }

    public String prepareScript(String text, String source) {
        // TODO Auto-generated method stub
        if (source != null) { return text; }
        String rez = text.replaceAll("(\\s*G._google[^;]*.)", "\r\n/*BlockedByAdblocker $1*/");
        rez = rez.replaceAll("(\\s*google_ad[^;]*.)", "\r\n/*BlockedByAdblocker $1*/");
        rez = rez.replace("<!--", "");
        rez = rez.replace("-->", "");
        if (!rez.equals(text)) {
            System.out.println(rez);
        }
        return rez;
    }
}
