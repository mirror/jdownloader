//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd;

import java.util.logging.Logger;
import java.util.regex.Pattern;

import jd.utils.JDUtilities;

public class PluginPattern {
    static private Logger logger = JDUtilities.getLogger();

    static public Pattern decrypterPattern_UCMS_Plugin() {
        StringBuilder Complete_Pattern = new StringBuilder();
        String[] List = { "saugking.net", "oxygen-warez.com", "filefox.in", "alphawarez.us", "pirate-loads.com", "fettrap.com", "omega-music.com", "hardcoremetal.biz", "flashload.org", "twin-warez.com", "oneload.org", "steelwarez.com", "fullstreams.info", "lionwarez.com", "1dl.in", "chrome-database.com", "oneload.org", "youwarez.biz", "saugking.net", "leetpornz.com", "freefiles4u.com", "dark-load.net", "crimeland.de", "get-warez.in", "meinsound.com", "projekt-tempel-news.de.vu", "datensau.org", "musik.am", "spreaded.net", "relfreaks.com", "babevidz.com", "serien24.com", "porn-freaks.net", "xxx-4-free.net", "porn-traffic.net", "chili-warez.net", "game-freaks.net", "isos.at", "your-load.com", "mov-world.net", "xtreme-warez.net", "sceneload.to", "oxygen-warez.com", "epicspeedload.in", "serienfreaks.to", "serienfreaks.in", "warez-load.com", "ddl-scene.com", "mp3king.cinipac-hosting.biz",
                "xwebb.extra.hu/1dl", "wii-reloaded.ath.cx/sites/epic", "wankingking.com", "projekt-tempel-news.org", "porn-ox.in", "music-dome.cc", "sound-load.com", "hoerspiele.to", "jim2008.extra.hu", "ex-yu.extra.hu", "firefiles.in", "gez-load.net", "wrzunlimited.1gb.in", "streamload.in", "toxic.to", "mp3z.to", "sexload.to", "sound-load.com", "sfulc.exofire.net/cms", "fickdiehure.com", "dream-team.bz/cms", "omega-warez.com", "ddl-scene.cc", "xxxstreams.org", "scene-warez.com", "dokuh.tv", "titanload.to", "ddlshock.com", "xtreme-warez.us" };
        for (String Pattern : List) {
            if (Complete_Pattern.length() > 0) {
                Complete_Pattern.append("|");
            }
            Complete_Pattern.append("(http://[\\w\\.]*?" + Pattern.replaceAll("\\.", "\\\\.") + "/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html))");
        }
        logger.finest("UCMS: " + List.length + " Pattern added!");
        return Pattern.compile(Complete_Pattern.toString(), Pattern.CASE_INSENSITIVE);
    }

    static public final Pattern decrypterPattern_Redirecter_Plugin() {
        StringBuilder Complete_Pattern = new StringBuilder();
        String[] List = { "http://[\\w\\.]*?fyad\\.org/[a-zA-Z0-9]+", "http://[\\w\\.]*?is\\.gd/[a-zA-Z0-9]+", "http://[\\w\\.]*?redirect\\.wayaround\\.org/[a-zA-Z0-9]+/(.*)", "http://[\\w\\.]*?rurl\\.org/[a-zA-Z0-9]+", "http://[\\w\\.]*?tinyurl\\.com/[a-zA-Z0-9\\-]+" };
        for (String Pattern : List) {
            if (Complete_Pattern.length() > 0) {
                Complete_Pattern.append("|");
            }
            Complete_Pattern.append(Pattern);
        }
        logger.finest("Redirecter: " + List.length + " Pattern added!");
        return Pattern.compile(Complete_Pattern.toString(), Pattern.CASE_INSENSITIVE);
    }

    static public final Pattern decrypterPattern_AnimeANet_Series = Pattern.compile("http://[\\w\\.]*?animea\\.net/download/[\\d]+/(.*?)\\.html", Pattern.CASE_INSENSITIVE);
    static public final Pattern decrypterPattern_AnimeANet_Episode = Pattern.compile("http://[\\w\\.]*?animea\\.net/download/[\\d]+-[\\d]+/(.*?)\\.html", Pattern.CASE_INSENSITIVE);
    static public final Pattern decrypterPattern_AnimeANet_Plugin = Pattern.compile(decrypterPattern_AnimeANet_Series.pattern() + "|" + decrypterPattern_AnimeANet_Episode.pattern(), Pattern.CASE_INSENSITIVE);

    static public final Pattern decrypterPattern_DDLMusic_Main = Pattern.compile("http://[\\w\\.]*?ddl-music\\.org/index\\.php\\?site=view_download&cat=.+&id=\\d+", Pattern.CASE_INSENSITIVE);
    static public final Pattern decrypterPattern_DDLMusic_Crypt = Pattern.compile("http://[\\w\\.]*?ddl-music\\.org/captcha/ddlm_cr\\d\\.php\\?\\d+\\?\\d+", Pattern.CASE_INSENSITIVE);

    static public final Pattern decrypterPattern_DreiDlAm_1 = Pattern.compile("http://[\\w\\.]*?3dl\\.am/link/[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);
    static public final Pattern decrypterPattern_DreiDlAm_2 = Pattern.compile("http://[\\w\\.]*?3dl\\.am/download/start/[0-9]+/", Pattern.CASE_INSENSITIVE);
    static public final Pattern decrypterPattern_DreiDlAm_3 = Pattern.compile("http://[\\w\\.]*?3dl\\.am/download/[0-9]+/.+\\.html", Pattern.CASE_INSENSITIVE);

    static public final Pattern decrypterPattern_Wordpress = Pattern.compile("http://[\\w\\.]*?(hd-area\\.org/\\d{4}/\\d{2}/\\d{2}/.+|movie-blog\\.org/\\d{4}/\\d{2}/\\d{2}/.+|hoerbuch\\.in/blog\\.php\\?id=[\\d]+|doku\\.cc/\\d{4}/\\d{2}/\\d{2}/.+|xxx-blog\\.org/blog\\.php\\?id=[\\d]+|sky-porn\\.info/blog/\\?p=[\\d]+|best-movies\\.us/\\?p=[\\d]+|game-blog\\.us/game-.+\\.html|pressefreiheit\\.ws/[\\d]+/.+\\.html).*", Pattern.CASE_INSENSITIVE);

}
