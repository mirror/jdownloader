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

import jd.utils.JDUtilities;

public class PluginPattern {
    static private Logger logger = JDUtilities.getLogger();

    static public String decrypterPattern_UCMS_Plugin() {
        StringBuilder completePattern = new StringBuilder();
        String[] list = { "saugking.net", "oxygen-warez.com", "filefox.in", "alphawarez.us", "pirate-loads.com", "fettrap.com", "omega-music.com", "hardcoremetal.biz", "flashload.org", "twin-warez.com", "oneload.org", "steelwarez.com", "fullstreams.info", "lionwarez.com", "1dl.in", "chrome-database.com", "oneload.org", "youwarez.biz", "saugking.net", "leetpornz.com", "freefiles4u.com", "dark-load.net", "crimeland.de", "get-warez.in", "meinsound.com", "projekt-tempel-news.de.vu", "datensau.org", "musik.am", "spreaded.net", "relfreaks.com", "babevidz.com", "serien24.com", "porn-freaks.net", "xxx-4-free.net", "porn-traffic.net", "chili-warez.net", "game-freaks.net", "isos.at", "your-load.com", "mov-world.net", "xtreme-warez.net", "sceneload.to", "oxygen-warez.com", "epicspeedload.in", "serienfreaks.to", "serienfreaks.in", "warez-load.com", "ddl-scene.com", "mp3king.cinipac-hosting.biz",
                "xwebb.extra.hu/1dl", "wii-reloaded.ath.cx/sites/epic", "wankingking.com", "projekt-tempel-news.org", "porn-ox.in", "music-dome.cc", "sound-load.com", "hoerspiele.to", "jim2008.extra.hu", "ex-yu.extra.hu", "firefiles.in", "gez-load.net", "wrzunlimited.1gb.in", "streamload.in", "toxic.to", "mp3z.to", "sexload.to", "sound-load.com", "sfulc.exofire.net/cms", "fickdiehure.com", "dream-team.bz/cms", "omega-warez.com", "ddl-scene.cc", "xxxstreams.org", "scene-warez.com", "dokuh.tv", "titanload.to", "ddlshock.com", "xtreme-warez.us", "crunkwarez.com", "serienking.in", "stream.szenepic.us", "gate-warez.com" };
        for (String pattern : list) {
            if (completePattern.length() > 0) {
                completePattern.append("|");
            }
            completePattern.append("(http://[\\w\\.]*?" + pattern.replaceAll("\\.", "\\\\.") + "/(\\?id=.+|[\\?]*?/.*?\\.html|category/.*?/.*?\\.html|download/.*?/.*?\\.html))");
        }
        logger.finest("UCMS: " + list.length + " Pattern added!");
        return completePattern.toString();
    }

    static public String decrypterPattern_Wordpress_Plugin() {
        StringBuilder completePattern = new StringBuilder();
        completePattern.append("http://[\\w\\.]*?(");
        completePattern.append("(game-blog\\.us/game-.+\\.html)");
        completePattern.append("|(pressefreiheit\\.ws/[\\d]+/.+\\.html)");
        completePattern.append("|(zeitungsjunge\\.info/.*?/.*?/.*?/)");
        String[] listType1 = { "hd-area.org", "movie-blog.org", "doku.cc", "sound-blog.org" };
        for (String pattern : listType1) {
            completePattern.append("|(" + pattern.replaceAll("\\.", "\\\\.") + "/\\d{4}/\\d{2}/\\d{2}/.+)");
        }
        String[] listType2 = { "hoerbuch.in", "xxx-blog.org" };
        for (String pattern : listType2) {
            completePattern.append("|(" + pattern.replaceAll("\\.", "\\\\.") + "/blog\\.php\\?id=[\\d]+)");
        }
        String[] listType3 = { "sky-porn.info/blog", "best-movies.us" };
        for (String pattern : listType3) {
            completePattern.append("|(" + pattern.replaceAll("\\.", "\\\\.") + "/\\?p=[\\d]+)");
        }
        completePattern.append(")");
        logger.finest("Wordpress: " + (1 + 1 + listType1.length + listType2.length + listType3.length) + " Pattern added!");
        return completePattern.toString();
    }

    static public final String decrypterPattern_Redirecter_Plugin() {
        StringBuilder completePattern = new StringBuilder();
        String[] list = { "http://[\\w\\.]*?fyad\\.org/[a-zA-Z0-9]+", "http://[\\w\\.]*?is\\.gd/[a-zA-Z0-9]+", "http://[\\w\\.]*?redirect\\.wayaround\\.org/[a-zA-Z0-9]+/(.*)", "http://[\\w\\.]*?rurl\\.org/[a-zA-Z0-9]+", "http://[\\w\\.]*?tinyurl\\.com/[a-zA-Z0-9\\-]+", "http://[\\w\\.]*?smarturl\\.eu/\\?[a-zA-Z0-9]+" };
        for (String pattern : list) {
            if (completePattern.length() > 0) {
                completePattern.append("|");
            }
            completePattern.append(pattern);
        }
        logger.finest("Redirecter: " + list.length + " Pattern added!");
        return completePattern.toString();
    }

    static public final String decrypterPattern_AnimeANet_Series = "http://[\\w\\.]*?animea\\.net/download/[\\d]+/(.*?)\\.html";
    static public final String decrypterPattern_AnimeANet_Episode = "http://[\\w\\.]*?animea\\.net/download/[\\d]+-[\\d]+/(.*?)\\.html";
    static public final String decrypterPattern_AnimeANet_Plugin = decrypterPattern_AnimeANet_Series + "|" + decrypterPattern_AnimeANet_Episode;

    static public final String decrypterPattern_DDLMusic_Main = "http://[\\w\\.]*?ddl-music\\.org/index\\.php\\?site=view_download&cat=.+&id=\\d+";
    static public final String decrypterPattern_DDLMusic_Crypt = "http://[\\w\\.]*?ddl-music\\.org/captcha/ddlm_cr\\d\\.php\\?\\d+\\?\\d+";
    static public final String decrypterPattern_DDLMusic_Plugin = decrypterPattern_DDLMusic_Main + "|" + decrypterPattern_DDLMusic_Crypt;

    static public final String decrypterPattern_DreiDlAm_1 = "http://[\\w\\.]*?3dl\\.am/link/[a-zA-Z0-9]+";
    static public final String decrypterPattern_DreiDlAm_2 = "http://[\\w\\.]*?3dl\\.am/download/start/[0-9]+/";
    static public final String decrypterPattern_DreiDlAm_3 = "http://[\\w\\.]*?3dl\\.am/download/[0-9]+/.+\\.html";
    static public final String decrypterPattern_DreiDlAm_4 = "http://[\\w\\.]*?3dl\\.am/index\\.php\\?action=detailansicht&file_id=[0-9]+";
    static public final String decrypterPattern_DreiDlAm_Plugin = decrypterPattern_DreiDlAm_1 + "|" + decrypterPattern_DreiDlAm_2 + "|" + decrypterPattern_DreiDlAm_3 + "|" + decrypterPattern_DreiDlAm_4;

}
