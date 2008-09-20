package jd;

import java.util.logging.Logger;
import java.util.regex.Pattern;

import jd.utils.JDUtilities;

public class PluginPattern {
    static private Logger logger = JDUtilities.getLogger();

    static public Pattern decrypterPattern_UCMS_Plugin() {
        String Complete_Pattern = "";
        String[] List = { "saugking.net", "oxygen-warez.com", "filefox.in", "alphawarez.us", "pirate-loads.com", "fettrap.com", "omega-music.com", "hardcoremetal.biz", "flashload.org", "twin-warez.com", "oneload.org", "steelwarez.com", "fullstreams.info", "lionwarez.com", "1dl.in", "chrome-database.com", "oneload.org", "youwarez.biz", "saugking.net", "leetpornz.com", "freefiles4u.com", "dark-load.net", "crimeland.de", "get-warez.in", "meinsound.com", "projekt-tempel-news.de.vu", "datensau.org", "musik.am", "spreaded.net", "relfreaks.com", "babevidz.com", "serien24.com", "porn-freaks.net", "xxx-4-free.net", "porn-traffic.net", "chili-warez.net", "game-freaks.net", "isos.at", "your-load.com", "mov-world.net", "xtreme-warez.net", "sceneload.to", "oxygen-warez.com", "epicspeedload.in", "serienfreaks.to", "serienfreaks.in", "warez-load.com", "ddl-scene.com", "mp3king.cinipac-hosting.biz",
                "xwebb.extra.hu/1dl", "wii-reloaded.ath.cx/sites/epic", "wankingking.com", "projekt-tempel-news.org", "porn-ox.in", "music-dome.cc", "sound-load.com", "hoerspiele.to", "jim2008.extra.hu", "ex-yu.extra.hu", "firefiles.in", "gez-load.net", "wrzunlimited.1gb.in", "streamload.in", "toxic.to", "mp3z.to", "sexload.to", "sound-load.com", "sfulc.exofire.net/cms", "fickdiehure.com", "dream-team.bz/cms", "omega-warez.com", "ddl-scene.cc", "xxxstreams.org", "scene-warez.com", "dokuh.tv", "titanload.to" };
        for (String Pattern : List) {
            if (Complete_Pattern.length() > 0) {
                Complete_Pattern += "|";
            }
            Complete_Pattern += "(http://[\\w\\.]*?" + Pattern.replaceAll("\\.", "\\\\.") + "/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html))";
        }
        logger.finest("UCMS: " + List.length + " Pattern added!");
        return Pattern.compile(Complete_Pattern, Pattern.CASE_INSENSITIVE);
    }

    static public final Pattern decrypterPattern_AnimeANet_Series = Pattern.compile("http://[\\w\\.]*?animea\\.net/download/[\\d]+/(.*?)\\.html", Pattern.CASE_INSENSITIVE);
    static public final Pattern decrypterPattern_AnimeANet_Episode = Pattern.compile("http://[\\w\\.]*?animea\\.net/download/[\\d]+-[\\d]+/(.*?)\\.html", Pattern.CASE_INSENSITIVE);
    static public final Pattern decrypterPattern_AnimeANet_Plugin = Pattern.compile(decrypterPattern_AnimeANet_Series.pattern() + "|" + decrypterPattern_AnimeANet_Episode.pattern(), Pattern.CASE_INSENSITIVE);

    static public final Pattern decrypterPattern_DDLMusic_Main = Pattern.compile("http://[\\w\\.]*?ddl-music\\.org/index\\.php\\?site=view_download&cat=.+&id=\\d+", Pattern.CASE_INSENSITIVE);
    static public final Pattern decrypterPattern_DDLMusic_Crypt = Pattern.compile("http://[\\w\\.]*?ddl-music\\.org/captcha/ddlm_cr\\d\\.php\\?\\d+\\?\\d+", Pattern.CASE_INSENSITIVE);

}
