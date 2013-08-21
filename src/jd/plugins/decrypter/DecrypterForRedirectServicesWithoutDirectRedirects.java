//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "potlocker.net", "mblink.info", "madlink.sk", "zo.mu", "peeplink.in", "lnx.lu", "handsupbr.com", "mkv2.info", "searchonzippy.eu", "sharmota.com", "allsubs.org", "egcenter.com", "komp3.net", "url.bilgiportal.com", "tinymoviez.info", "getunite.com", "hflix.in", "focus.de", "hnzoom.com", "basemp3.ru", "stream2k.eu", "share-films.net", "leechmf.com", "protetorbr.com", "adv.li", "lezlezlez.com", "dwz.cn", "digitaldripped.com", "guardlink.org", "url.cn", "q32.ru", "shrk.biz", "icefilms.info", "mediaboom.org", "vimeo.com", "unlimfiles.com", "adfoc.us", "mrbrownee70.com", "alturl.com", "trancearoundtheworld.com", "egfire.net", "damasgate.com", "freeonsmash.com", "lnk.co", "trackstash.com", "fburls.com", "myurl.in", "h-url.in", "dropbox.com", "filep.info", "grou.ps", "linkexterno.com", "eskimotube.com", "m4u.in",
        "4p5.com", "t.co", "telona.biz", "madmimi.com", "href.hu", "hide.linkleak.org", "migre.me", "degracaemaisgostoso.info", "altervista.org", "agaleradodownload.com", "musicloud.fm", "wowebook.be", "link.songs.pk + songspk.info", "imageto.net", "clubteam.eu", "jforum.uni.cc", "linksole.com", "deurl.me", "yourfileplace.com", "muzgruz.ru", "zero10.net", "aiotool.net", "chip.de/c1_videos", "nbanews.us", "top2tech.com", "umquetenha.org", "oneclickmoviez.com/dwnl/", "1tool.biz", "file4ever.us and catchfile.net", "zero10.net and gamz.us", "official.fm", "hypem.com", "academicearth.org", "skreemr.org", "tm-exchange.com", "adiarimore.com", "mafia.to/download", "newgrounds.com", "accuratefiles.com", "slutdrive.com", "view.stern.de", "fileblip.com", "warcraft.ingame.de", "mixconnect.com", "twiturm.com", "ebooksdownloadfree.com", "freebooksearcher.info", "ubuntuone.com", "mp3.wp.pl",
        "gantrack.com" }, urls = { "http://(www\\.)?potlocker\\.net/[a-z0-9\\-]+/\\d{4}/[a-z0-9]+\\.html", "http://(www\\.)?mblink\\.info/\\?id=\\d+", "http://(www\\.)?(madlink\\.sk|m\\-l\\.sk)/[a-z0-9]+", "http://(www\\.)?zo\\.mu/[A-Za-z0-9]+", "http://(www\\.)?peeplink\\.in/[a-z0-9]+", "http://(www\\.)?(lnx\\.lu|z\\.gs|url\\.fm)/[a-z0-9]+", "http://(www\\.)?handsupbr\\.com/play/\\d+/", "http://(www\\.)?mkv2\\.info/s/[a-z0-9]+", "http://(www\\.)?searchonzippy\\.eu/out\\.php\\?link=\\d+", "http://(www\\.)?sharmota\\.com/movies/\\d+/\\d+", "http://(www\\.)?allsubs\\.org/subs\\-download/[a-z0-9\\-_]+/\\d+/", "http://(www\\.)?egcenter\\.com/\\d+", "http://(www\\.)?komp3\\.net/download/mp3/\\d+/[^<>\"]+\\.html", "http://(www\\.)?url\\.bilgiportal\\.com/[0-9]+", "http://(www\\.)?tinymoviez\\.info/download\\.php\\?link=[A-Za-z0-9]+",
        "http://(www\\.)?getunite\\.com/\\?d=\\d+\\.\\d+\\.\\d+\\.\\d+/[a-z0-9]+/[a-z0-9]+/", "http://(www\\.)?hflix\\.in/[A-Za-z0-9]+", "http://(www\\.)?focus\\.de/[a-zA-Z]+/(videos|internet/[a-zA-Z]+)/[\\w\\-]+\\.html", "http://(www\\.)?hnzoom\\.com/(\\?[A-Za-z0-9]{20}|folder/[a-zA-Z0-9\\-]{11})", "http://(www\\.)?basemp3\\.ru/music\\-view\\-\\d+\\.html", "http://(www\\.)?stream2k\\.eu/video/\\d+", "http://(www\\.)?share\\-films\\.net/redirect\\.php\\?url=[a-z0-9]+", "http://(www\\.)?leechmf\\.com/\\?[A-Za-z0-9]+", "http://(www\\.)?protetorbr\\.com/d\\?id=\\d+", "http://(www\\.)?adv\\.li/[A-Za-z0-9]+", "http://(www\\.)?lezlezlez\\.com/mediaswf\\.php\\?type=vid\\&name=[^<>\"/]+\\.flv", "http://(www\\.)?dwz\\.cn/[A-Za-z0-9]+", "http://(www\\.)?digitaldripped\\.com/(?!/ajax|js/)[^<>\"\\']{2,}", "http://(www\\.)?guardlink\\.org/[A-Za-z0-9]+", "http://url\\.cn/[0-9a-zA-Z]+",
        "http://q32\\.ru/\\d+/c/[A-Za-z0-9\\-_]+", "http://(de\\.)?shrk\\.biz/\\w+", "http://(www\\.)?icefilms\\.info/ip\\.php\\?v=\\d+\\&?", "https?://(www\\.)?vimeo\\.com/(?!(\\d+/|tag|search)).*?/.+", "http://(www\\.)?unlimfiles\\.com/sourceframe/.{2,}", "http://(www\\.)?adfoc\\.us/(serve/\\?id=[a-z0-9]+|(?!serve)[a-z0-9]+)", "http://(www\\.)?mrbrownee70\\.com/\\?id=[A-Za-z0-9]+", "http://[\\w\\.]*?alturl\\.com/[a-z0-9]+", "http://(www\\.)?trancearoundtheworld\\.com/tatw/\\d+", "http://(www\\.)?egfire\\.net/\\d+", "http://(www\\.)?damasgate\\.com/redirector\\.php\\?url=.+", "http://(www\\.)?freeonsmash\\.com/redir/[A-Za-z0-9\\=\\+\\/\\.\\-]+", "http://(www\\.)?lnk\\.co/[A-Za-z0-9]+", "http://(www\\.)?trackstash\\.com/tracks/[a-z0-9\\-]+", "http://(www\\.)?fburls\\.com/\\d+\\-[A-Za-z0-9]+", "http://(www\\.)?protect\\.myurl\\.in/[A-Za-z0-9]+",
        "http://(www\\.)?h\\-url\\.in/[A-Za-z0-9]+", "https://(www\\.)?dropbox\\.com/s/[a-z0-9]+/.+", "http://(www\\.)?filep\\.info/(\\?url=|/)\\d+", "http://(www\\.)?grou\\.ps/[a-z0-9]+/videos/\\d+", "http://(www\\.)?linkexterno\\.com/[A-Za-z0-9]+", "http://(www\\.)?eskimotube\\.com/\\d+\\-.*?\\.html", "http://(www\\.)?m4u\\.in/[a-z0-9]+", "http://(www\\.)?4p5\\.com/[a-z0-9]+", "https?://t\\.co/[a-zA-Z0-9]+", "http://[\\w\\.]*?telona\\.biz/protec?tor.*?\\?.*?//:ptth", "https?://go\\.madmimi\\.com/redirects/[a-zA-Z0-9]+\\?pa=\\d+", "http://href\\.hu/x/[a-zA-Z0-9\\.]+", "http://hide\\.linkleak\\.org/[a-zA-Z0-9\\.]+", "http://[\\w\\.]*?migre\\.me/[a-z0-9A-Z]+", "http://[\\w\\.]*?degracaemaisgostoso\\.(biz|info)/download/\\?url=.*?:ptth", "http://[\\w\\.]*?altervista\\.org/\\?i=[0-9a-zA-Z]+", "http://[\\w\\.]*?agaleradodownload\\.com/download.*?\\?.*?//:ptth",
        "http://[\\w\\.]*?musicloud\\.fm/dl/[A-Za-z0-9]+", "http://(www\\.)?wowebook\\.be/download/\\d+", "http://[\\w\\.]*?(link\\.songs\\.pk/(popsong|song1|bhangra)\\.php\\?songid=|songspk\\.info/ghazals/download/ghazals\\.php\\?id=)[0-9]+", "http://[\\w\\.]*?imageto\\.net/(\\?v=|images/)[0-9a-z]+\\..{2,4}", "http://[\\w\\.]*?clubteam\\.eu/dl\\.php\\?id=\\d\\&c=[a-zA-z0-9=]+", "http://[\\w\\.]*?jforum\\.uni\\.cc/protect/\\?r=[a-z0-9]+", "http://[\\w\\.]*?linksole\\.com/[0-9a-z]+", "http://[\\w\\.]*?deurl\\.me/[0-9A-Z]+", "http://(www\\.)?(yourfileplace|megafilegarden)\\.com/d/\\d+/.+", "http://[\\w\\.]*?muzgruz\\.ru/music/download/\\d+", "http://[\\w\\.]*?zero10\\.net/\\d+", "http://[\\w\\.]*?aiotool\\.net/\\d+", "http://[\\w\\.]*?chip\\.de/c1_videos/.*?-Video_\\d+\\.html", "http://[\\w\\.]*?nbanews\\.us/\\d+", "http://[\\w\\.]*?top2tech\\.com/\\d+",
        "http://(www\\.)?umquetenha\\.org/protecao/resolve\\.php\\?link=.{2,}", "http://(www\\.)?oneclickmoviez\\.com/[a-z]{1,8}/[A-Z0-9\\-_]+/\\d+/\\d+", "http://(www\\.)?1tool\\.biz/\\d+", "http://[\\w\\.]*?(file4ever\\.us|catchfile\\.net)/\\d+", "http://[\\w\\.]*?(zero10\\.net/|gamz\\.us/\\?id=)\\d+", "http://(www\\.)?official\\.fm/track(s)?/\\d+", "http://(www\\.)?hypem\\.com/(track/\\d+/|item/[a-z0-9]+)", "http://[\\w\\.]*?academicearth\\.org/lectures/.{2,}", "http://[\\w\\.]*?skreemr\\.org/link\\.jsp\\?id=[A-Z0-9]+", "http://[\\w\\.]*?tm-exchange\\.com/(get\\.aspx\\?action=trackgbx|\\?action=trackshow)\\&id=\\d+", "http://[\\w\\.]*?adiarimore\\.com/miralink/[a-z0-9]+", "http://[\\w\\.]*?mafia\\.to/download-[a-z0-9]+\\.cfm", "http://[\\w\\.]*?newgrounds\\.com/(portal/view/|audio/listen/)\\d+", "http://(www\\.)?accuratefiles\\.com/fileinfo/[a-z0-9]+",
        "http://(www\\.)?slutdrive\\.com/video/\\d+", "http://(www\\.)?view\\.stern\\.de/de/(picture|original)/.*?-\\d+\\.html", "http://(www\\.)?fileblip\\.com/[a-z0-9]+", "http://(www\\.)?warcraft\\.ingame\\.de/downloads/\\?file=\\d+", "http://(www\\.)?mixconnect\\.com/listen/.*?-mid\\d+", "http://(www\\.)?twiturm\\.com/[a-z0-9]+", "http://(www\\.)?ebooksdownloadfree\\.com/.*?/.*?\\.html", "http://(www\\.)?freebooksearcher\\.info/downloadbook\\.php\\?id=\\d+", "http://(www\\.)?ubuntuone\\.com/p/[A-Za-z0-9]+", "http://[\\w\\.]*?mp3\\.wp\\.pl/(?!ftp)(p/strefa/artysta/\\d+,utwor,\\d+\\.html|\\?tg=[A-Za-z0-9=]+)", "http://(www\\.)?gantrack\\.com/t/l/\\d+/[A-Za-z0-9]+", "http://(www\\.)?mediaboom\\.org/engine/go\\.php\\?url=.+" }, flags = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 })
public class DecrypterForRedirectServicesWithoutDirectRedirects extends PluginForDecrypt {

    private final String NEWSREGEX2 = "<div id=\\'prep2\\' dir=\\'ltr\\' ><a  href=\\'(.*?)\\'";

    public DecrypterForRedirectServicesWithoutDirectRedirects(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.setReadTimeout(60 * 1000);
        boolean dh = false;
        boolean offline = false;
        String finallink = null;
        String finalfilename = null;
        if (parameter.contains("vimeo.com")) {
            br.getPage(parameter);
            finallink = br.getRedirectLocation();
            if (finallink != null && !finallink.matches(".*?vimeo.com/\\d+")) {
                finallink = null;
            }
            if (finallink == null) { return decryptedLinks; }
        } else if (parameter.contains("alturl.com") || parameter.contains("protect-ddl.com") || parameter.contains("hide.linkleak.org") || parameter.contains("href.hu") || parameter.contains("madmimi.com") || parameter.contains("wowebook.be")) {
            br.getPage(parameter);
            finallink = br.getRedirectLocation();
            if (finallink == null) {
                finallink = br.getRegex("REFRESH.*?url=(http.*?)\"").getMatch(0);
            }
        }
        /** Some links don't have to be accessed (here) */
        if (!parameter.contains("egcenter.com/") && !parameter.contains("imageto.net/") && !parameter.contains("musicloud.fm/dl") && !parameter.contains("oneclickmoviez.com/") && !parameter.contains("1tool.biz") && !parameter.contains("catchfile.net") && !parameter.contains("file4ever.us") && !parameter.contains("fairtilizer.com/") && !parameter.contains("tm-exchange.com/") && !parameter.contains("fileblip.com/") && !parameter.contains("mixconnect.com/") && !parameter.contains("machines.") && !parameter.contains("ubuntuone.com") && !parameter.contains("dropbox.com/") && !parameter.contains("trancearoundtheworld.com/") && !parameter.contains("dwz.cn/") && !parameter.contains("getunite.com/")) {
            br.getPage(parameter);
        }
        if (parameter.contains("link.songs.pk/") || parameter.contains("songspk.info/ghazals/download/ghazals.php?id=")) {
            finallink = br.getRedirectLocation();
            dh = true;
        } else if (parameter.contains("musicloud.fm/")) {
            final String theId = new Regex(parameter, "musicloud\\.fm/dl/(.+)").getMatch(0);
            if (theId == null) { return null; }
            br.getPage("http://musicloud.fm/dl.php?id=" + theId);
            finallink = br.getRedirectLocation();
            dh = true;
        } else if (parameter.contains("imageto")) {
            if (parameter.contains("imageto.net/images/")) {
                finallink = parameter;
                dh = true;
            } else {
                final String fileid = new Regex(parameter, "imageto\\.net/\\?v=(.+)").getMatch(0);
                finallink = "directhttp://http://imageto.net/images/" + fileid;
            }
        } else if (parameter.contains("clubteam.eu/dl")) {
            finallink = br.getRegex("content=\\'0; url=(.*?)'>").getMatch(0);
        } else if (parameter.contains("jforum.uni.cc/")) {
            finallink = br.getRegex("<frame name=\"page\" src=\"(.*?)\"").getMatch(0);
        } else if (parameter.contains("linksole.com/")) {
            finallink = br.getRegex("linkRefererUrl = '(.*?)';").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("<iframe src=\"(.*?)\"").getMatch(0);
            }
        } else if (parameter.contains("deurl.me/")) {
            finallink = br.getRegex("<i><small>(http://.*?)</small></i>").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("- <a href=\"(http://.*?)\">Click here to visit this").getMatch(0);
            }
        } else if (parameter.contains("yourfileplace.com/") || parameter.contains("megafilegarden.com/")) {
            finallink = parameter.replaceAll("(yourfileplace|megafilegarden)\\.com/d", "turbobit.net");
        } else if (parameter.contains("muzgruz.ru/music/")) {
            finallink = parameter;
            dh = true;
        } else if (parameter.contains("aiotool.net/")) {
            final String id = new Regex(parameter, "aiotool\\.net/(\\d+)").getMatch(0);
            final String accessThis = "http://aiotool.net/3-" + id + ".html";
            br.getPage(accessThis);
            finallink = br.getRedirectLocation();
        } else if (parameter.contains("chip.de/c1_videos")) {
            finallink = br.getRegex("id=\"player\" href=\"(http://.*?\\.flv)\"").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("\"(http://video\\.chip\\.de/\\d+/.*?.flv)\"").getMatch(0);
            }
            dh = true;
        } else if (parameter.contains("nbanews.us/")) {
            br.setFollowRedirects(true);
            final String id = new Regex(parameter, "nbanews\\.us/(\\d+)").getMatch(0);
            br.getPage("http://www.nbanews.us/m1.php?id=" + id);
            finallink = br.getRegex("NewWindow\\('(.*?)'").getMatch(0);
        } else if (parameter.contains("top2tech.com/")) {
            final String id = new Regex(parameter, "top2tech\\.com/(\\d+)").getMatch(0);
            br.getPage("http://top2tech.com/2-" + id);
            finallink = br.getRegex("onclick=\"NewWindow\\('(.*?)','").getMatch(0);
        } else if (parameter.contains("umquetenha.org/protecao/resolve.php?link=")) {
            finallink = br.getRegex("http\\-equiv=\"refresh\" content=\"\\d+; url=(http.*?)\"").getMatch(0);
            if (finallink == null && br.containsHTML("src=\"protecao\\.jpg\"")) offline = true;
        } else if (parameter.contains("oneclickmoviez.com/")) {
            final Regex allMatches = new Regex(parameter, "oneclickmoviez\\.com/[a-z]+/([A-Z0-9\\-_]+)/(\\d+)/(\\d+)");
            final String host = allMatches.getMatch(0);
            final String id1 = allMatches.getMatch(1);
            final String id2 = allMatches.getMatch(2);
            final String getLink = "http://oneclickmoviez.com/wp-content/plugins/link-cloaking-plugin/wplc_redirector.php?post_id=" + id1 + "&link_num=" + id2 + "&cloaked_url=dwnl/" + host + "/" + id1 + "/" + id2;
            br.getPage(getLink);
            finallink = br.getRedirectLocation();
            if (finallink == null) finallink = br.getRegex("<iframe SRC=\"([^<>\"]*?)\"").getMatch(0);
        } else if (parameter.contains("1tool.biz/")) {
            final String id = new Regex(parameter, "1tool\\.biz/(\\d+)").getMatch(0);
            br.getPage("http://1tool.biz/2.php?id=" + id);
            finallink = br.getRegex("onclick=\"NewWindow\\('(.*?)','").getMatch(0);
        } else if (parameter.contains("agaleradodownload.com")) {
            final String url = new Regex(parameter, "download.*?\\?(.+)").getMatch(0);
            if (url != null) {
                final StringBuilder sb = new StringBuilder("");
                sb.append(url);
                sb.reverse();
                finallink = sb.toString();
            }
        } else if (parameter.contains("telona.biz")) {
            final String url = new Regex(parameter, "protec?tor.*?\\?(.+)").getMatch(0);
            if (url != null) {
                final StringBuilder sb = new StringBuilder("");
                sb.append(url);
                sb.reverse();
                finallink = sb.toString();
            }
        } else if (parameter.contains("catchfile.net") || parameter.contains("file4ever.us")) {
            final String damnID = new Regex(parameter, "/(\\d+)$").getMatch(0);
            br.getPage(parameter.replace(damnID, "") + "file.php?id=" + damnID);
            finallink = br.getRegex("<td width=\"70%\">[\t\n\r ]+<a href=\"(.*?)\"").getMatch(0);
        } else if (parameter.contains("gamz.us/")) {
            final String damnID = new Regex(parameter, "(\\d+)$").getMatch(0);
            br.getPage("http://gamz.us/?id=" + damnID + "&d=1");
            finallink = br.getRegex(NEWSREGEX2).getMatch(0);
        } else if (parameter.contains("zero10.net/")) {
            final String damnID = new Regex(parameter, "(\\d+)$").getMatch(0);
            br.postPage("http://zero10.net/link.php?id=" + damnID, "s=1");
            finallink = br.getRegex("onClick=.*?\\('(http:.*?)'").getMatch(0);
        } else if (parameter.contains("official.fm/")) {
            finalfilename = br.getRegex("<meta property=\"og:title\" content=\"(.*?)\"/>").getMatch(0);
            if (finalfilename == null) {
                finalfilename = br.getRegex("<title>(.*?) on Official\\.fm</title>").getMatch(0);
            }
            if (finalfilename != null) {
                finalfilename = Encoding.htmlDecode(finalfilename.trim()) + ".mp3";
            }
            br.setFollowRedirects(true);
            br.getPage(parameter + ".xspf?ll_header=yes");
            finallink = br.getRegex("\"(http://cdn\\.official\\.fm/mp3s/\\d+/\\d+\\.mp3)").getMatch(0);
            dh = true;
        } else if (parameter.contains("hypem.com/")) {
            // Check if a redirect was there before
            if (br.getRedirectLocation() != null) {
                br.getPage(br.getRedirectLocation());
            }
            final String fid = new Regex(br.getURL(), "/item/(.+)").getMatch(0);
            if (fid == null) { return null; }
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.getPage("http://hypem.com/item/" + fid + "?ax=1&ts=");
            final String id0 = br.getRegex("key: \\'(.*?)\\',").getMatch(0);
            if (id0 == null) { return null; }
            final String artist = br.getRegex("artist:\\'(.*?)\\'").getMatch(0);
            final String sngName = br.getRegex("song:\\'(.*?)\\',").getMatch(0);
            if (artist != null && sngName != null) {
                finalfilename = artist.trim() + " - " + sngName.trim().replace("\\'", "'") + ".mp3";
            }
            br.getPage("http://hypem.com/serve/play/" + fid + "/" + id0 + ".mp3");
            finallink = br.getRedirectLocation();
            if (finallink != null) {
                finallink = finallink.replace("Http://", "http://");
            }
            dh = true;
        } else if (parameter.contains("academicearth.org/")) {
            if (!(br.getRedirectLocation() != null && br.getRedirectLocation().contains("users/login"))) {
                if (br.containsHTML(">Looks like the Internet may require a little disciplinary action")) offline = true;
                finallink = br.getRegex("flashVars\\.flvURL = \"(.*?)\"").getMatch(0);
                if (finallink == null) {
                    finallink = br.getRegex("<div><embed src=\"(.*?)\"").getMatch(0);
                }
                if (finallink != null) {
                    if (!finallink.contains("blip.tv") && !finallink.contains("youtube")) { throw new DecrypterException("Found unsupported link in link: " + parameter); }
                    if (finallink.contains("blip.tv/")) {
                        br.getPage(finallink);
                        finallink = br.getRedirectLocation();
                        dh = true;
                    }
                }
            } else {
                throw new DecrypterException("Login required to download link: " + parameter);
            }
        } else if (parameter.contains("skreemr.org/")) {
            finallink = br.getRegex(";soundFile=(http.*?\\.mp3)\\'").getMatch(0);
            if (finallink != null) {
                finallink = Encoding.htmlDecode(finallink);
            }
            dh = true;
        } else if (parameter.contains("tm-exchange.com/")) {
            finallink = "directhttp://" + parameter.replace("?action=trackshow", "get.aspx?action=trackgbx");
        } else if (parameter.contains("adiarimore.com/")) {
            finallink = br.getRegex("<iframe src=\"(.*?)\"").getMatch(0);
        } else if (parameter.contains("altervista.org") || parameter.contains("migre.me")) {
            finallink = br.getRedirectLocation();
        } else if (parameter.contains("mafia.to/download")) {
            br.getPage(parameter.replace("download-", "dl-"));
            finallink = br.getRedirectLocation();
        } else if (parameter.contains("newgrounds.com/")) {
            if (parameter.contains("/audio/listen/")) {
                finallink = "http://www.newgrounds.com/audio/download/" + new Regex(parameter, "(\\d+)$").getMatch(0);
                dh = true;
            } else {
                finallink = br.getRegex("\"src\":[\t\n\r ]+\"(http:[^<>\"]*?)\"").getMatch(0);
                if (finallink != null) finallink = finallink.replace("\\", "");
            }
        } else if (parameter.contains("accuratefiles.com/")) {
            if (br.containsHTML(">File was removed from filehosting")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            String fileID = br.getRegex("\"/go/(\\d+)\"").getMatch(0);
            if (fileID == null) {
                fileID = br.getRegex("name=\"file_id\" value=\"(\\d+)\"").getMatch(0);
            }
            if (fileID == null) { return null; }
            br.postPage("http://www.accuratefiles.com/go/" + fileID, "file_id=" + fileID);
            finallink = br.getRedirectLocation();
            if (finallink == null) finallink = br.getRegex("id=\"fdown\" name=\"fdown\" src=\"(.*?)\"").getMatch(0);
        } else if (parameter.contains("slutdrive.com/video")) {
            if (br.getRedirectLocation() != null) {
                br.setFollowRedirects(true);
                br.getPage(br.getRedirectLocation());
            }
            if ("http://slutdrive.com/".equals(br.getURL())) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            finalfilename = br.getRegex("<span class=\"header\\-text\">([^<>\"]*?)</span>").getMatch(0);
            if (finalfilename != null) finalfilename = Encoding.htmlDecode(finalfilename.trim() + ".flv");
            finallink = br.getRegex("url: \\'(http[^<>\"]*?)\\'").getMatch(0);
            dh = true;
        } else if (parameter.contains("view.stern.de/de/")) {
            br.setFollowRedirects(true);
            br.getPage(parameter.replace("/picture/", "/original/"));
            if (br.containsHTML("/erotikfilter/")) {
                br.postPage(br.getURL(), "savefilter=1&referer=" + Encoding.urlEncode(parameter.replace("/original/", "/picture/")) + "%3Fr%3D1%26g%3Dall");
                br.getPage(parameter.replace("/picture/", "/original/"));
            }
            finallink = br.getRegex("<div class=\"ImgBig\" style=\"width:\\d+px\">[\t\n\r ]+<img src=\"(http://.*?)\"").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("\"(http://view\\.stern\\.de/de/original/([a-z0-9]+/)?\\d+/.*?\\..{3,4})\"").getMatch(0);
            }
            dh = true;
        } else if (parameter.contains("degracaemaisgostoso")) {
            final String tmp = new Regex(parameter, "url=(.+)").getMatch(0);
            if (tmp != null) {
                final StringBuilder sb = new StringBuilder("");
                for (int index = tmp.length() - 1; index >= 0; index--) {
                    sb.append(tmp.charAt(index));
                }
                finallink = sb.toString();
            }
        } else if (parameter.contains("fileblip.com/")) {
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.getHeaders().put("X-Prototype-Version", "1.6.0.3");
            br.postPage("http://fileblip.com/getdata.php", "id=" + new Regex(parameter, "fileblip\\.com/(.+)").getMatch(0));
            finallink = br.getRegex("onload=\"framebreakerbreaker\\(\\)\" src=\"(.*?)\"").getMatch(0);
        } else if (parameter.contains("warcraft.ingame.de/downloads/")) {
            finallink = br.getRegex("\"(http://warcraft\\.ingame\\.de/downloads/\\?rfid=\\d+)\"").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("class=\"download\"><a href=\"(http://.*?)\"").getMatch(0);
            }
            dh = true;
        } else if (parameter.contains("mixconnect.com/")) {
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            final String fid = new Regex(parameter, "mixconnect\\.com/listen/.*?\\-mid(\\d+)").getMatch(0);
            br.getPage("http://www.mixconnect.com/downloadcheck.php?id=" + fid);
            if (br.containsHTML("Unable to resolve the request")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            final String dlHash = br.getRegex("dlhash\":\"([a-z0-9]+)\"").getMatch(0);
            if (dlHash == null) {
                br.getPage(parameter);
                finallink = br.getRegex("mp3:\"http://mixconnect\\.com(http://[^<>\"]*?)\"").getMatch(0);
                dh = true;
            } else {
                br.postPage("http://www.mixconnect.com/createdownload.php?id=" + fid, "dlhash=" + dlHash);
                finallink = br.toString();
                if (!finallink.startsWith("data/") || finallink.length() > 500) { return null; }
                if (finallink.equals("data/zip/.zip")) finallink = null;
                dh = true;
            }
        } else if (parameter.contains("twiturm.com/")) {
            finallink = br.getRegex("<div id=\"player\">[\r\t\n ]+<a href=\"(http://.*?)\">").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("\"(http://s\\d+\\.amazonaws\\.com/twiturm_prod/.*?)\"").getMatch(0);
            }
            dh = true;
            finalfilename = br.getRegex("<title>Twiturm\\.com - (.*?)</title>").getMatch(0);
            if (finalfilename != null) {
                finalfilename += ".mp3";
            }
        } else if (parameter.contains("ebooksdownloadfree.com/")) {
            if (br.containsHTML("the page you requested is not located here<")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            finallink = br.getRegex("<strong>Link:</strong>\\&nbsp; </span><span class=\"linkcat\">[\t\n\r ]+<a style=\"font-size:16px\" href=\"(.*?)\"").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("\"(http://freebooksearcher\\.info/downloadbook\\.php\\?id=\\d+)\"").getMatch(0);
            }
        } else if (parameter.contains("freebooksearcher.info/")) {
            finallink = br.getRegex("<p><a href=\"(.*?)\"").getMatch(0);
        } else if (parameter.contains("ubuntuone.com")) {
            finallink = parameter;
            dh = true;
        } else if (parameter.contains("mp3.wp.pl/")) {
            if (br.getRedirectLocation() != null) {
                br.getPage(br.getRedirectLocation());
            }
            String ID = br.getRegex("name=\"mp3artist\" noresize src=\"http://mp3\\.wp\\.pl//p/strefa/artysta/\\d+,utwor,(\\d+)\\.html\"").getMatch(0);
            if (ID == null) {
                ID = new Regex(parameter, "mp3\\.wp\\.pl/p/strefa/artysta/\\d+,utwor,(\\d+)\\.html").getMatch(0);
            }
            if (ID == null) { return null; }
            br.getPage("http://mp3.wp.pl/i/sciagnij?id=" + ID + "&jakosc=hifi&streaming=0");
            finallink = br.getRedirectLocation();
            dh = true;
        } else if (parameter.contains("4p5.com/") || parameter.contains("m4u.in/")) {
            finallink = br.getRegex("<iframe id=\"frame\" src=\"(.*?)\"").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("<div id=\"removeFrame\">\\&raquo; <a href=\"(.*?)\"").getMatch(0);
            }
        } else if (parameter.contains("eskimotube.com/")) {
            finalfilename = br.getRegex("<TITLE>EskimoTube\\.com \\- Streaming Videos of Unknown \\-([^<>\"]*?)\\- Pornstars And Centerfolds\\.</title>").getMatch(0);
            br.getPage("http://www.eskimotube.com/playlist-live.php?id=" + new Regex(parameter, "eskimotube\\.com/(\\d+)").getMatch(0));
            finallink = br.getRegex("<file>(http://[^<>\"]*?)</file>").getMatch(0);
            if (finalfilename == null) {
                finalfilename = br.getRegex("<TITLE>EskimoTube\\.com - Streaming Videos of (.*?) \\- ").getMatch(0);
            }
            if (finalfilename != null && finallink != null) {
                finalfilename += "." + finallink.substring(finallink.length() - 4, finallink.length());
            }
            dh = true;
        } else if (parameter.contains("linkexterno.com/")) {
            br.postPage(br.getURL(), "text=&image.x=" + new Random().nextInt(100) + "&image.y=" + new Random().nextInt(100));
            finallink = br.getRegex(">window\\.location = \\'(.*?)\\'").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("<iframe scrolling=\"auto\" src=\"(.*?)\"").getMatch(0);
            }
        } else if (parameter.contains("grou.ps/")) {
            finallink = br.getRegex("name=\"movie\" value=\"(http://.*?)\"").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("<embed src=\"(http://.*?)\"").getMatch(0);
            }
            if (finallink == null) {
                finallink = parameter.replace("grou.ps", "decryptedgrou.ps");
            }
        } else if (parameter.contains("filep.info/")) {
            parameter = parameter.replace("filep.info//", "filep.info/?url=");
            br.postPage(parameter, "abod=1");
            finallink = br.getRegex("onclick=\"NewWindow\\(\\'(.*?)\\'").getMatch(0);
        } else if (parameter.contains("dropbox.com/")) {
            finallink = parameter.replace("www.", "").replace("https://", "https://dl.");
            dh = true;
        } else if (parameter.contains("h-url.in/")) {
            br.getPage("http://link.h-url.in/" + new Regex(parameter, "h\\-url\\.in/(.+)").getMatch(0));
            finallink = br.getRegex("frameborder=\"no\" width=\"100%\" src=\"(.*?)\"").getMatch(0);
        } else if (parameter.contains("protect.myurl.in/")) {
            finallink = br.getRegex("<iframe scrolling=\"(yes|no)\" src=\"(.*?)\"").getMatch(1);
        } else if (parameter.contains("fburls.com/")) {
            finallink = br.getRegex("\"refresh\" content=\"\\d+; url=(.*?)\"").getMatch(0);
        } else if (parameter.contains("trackstash.com/")) {
            finallink = br.getRegex("\"media\": \\{\"path\": \"(http://.*?)\"").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("\"(http://(www\\.)?trackstash\\.com/sites/default/files/tracks/.*?)\"").getMatch(0);
            }
            finalfilename = br.getRegex(",\"nid\": \"\\d+\",\"title\": \"(.*?)\"").getMatch(0);
            if (finalfilename == null) {
                finalfilename = br.getRegex("<title>TRACKSTASH\\.COM \\- BETA \\| (.*?)</title>").getMatch(0);
            }
            if (finalfilename != null) {
                finalfilename = finalfilename.trim() + ".mp3";
            }
            if (finallink != null) {
                br.getPage(finallink);
                finallink = br.getRedirectLocation();
            }
            dh = true;
        } else if (parameter.contains("lnk.co/")) {
            finallink = br.getRedirectLocation();
            if (finallink == null) finallink = br.getRegex("window\\.top\\.location = \\'srh\\.php\\?u=(http://[^<>\"]*?)\\'").getMatch(0);
            if (finallink == null) finallink = br.getRegex("style=\\'pointer\\-events: none;\\' id=\\'dest\\' src=\"(http://[^<>\"]*?)\"").getMatch(0);
            if (finallink == null) finallink = br.getRegex("linkurl.*?counter.*?linkurl' href=\"(http://[^<>\"]*?)\"").getMatch(0);
        } else if (parameter.contains("freeonsmash.com/")) {
            if (finallink == null && br.getRedirectLocation() != null) {
                finallink = br.getRedirectLocation();
            }
            if (finallink == null) {
                finallink = br.getRegex("<meta http\\-equiv=\"Refresh\" content=\"\\d+; URL=(.*?)\"").getMatch(0);
                if (finallink == null) {
                    finallink = br.getRegex("<input id=\\'bounce\\-page\\-url\\' type=\\'text\\' value=\\'(.*?)\\'").getMatch(0);
                }
            }
        } else if (parameter.contains("damasgate.com/redirector")) {
            finallink = br.getRegex("align=\"center\"><a href=\"(.*?)\"").getMatch(0);
        } else if (parameter.contains("egfire.net/")) {
            br.getPage("http://www.egfire.net/2-" + new Regex(parameter, "egfire\\.net/(\\d+)").getMatch(0));
            finallink = br.getRegex("<div id=\"continue\" style=\\'display:none\\'><a href=\\'(.*?)\\'").getMatch(0);
        } else if (parameter.contains("trancearoundtheworld.com/")) {
            br.getPage("http://www.trancearoundtheworld.com/player/play.php?id=" + new Regex(parameter, "trancearoundtheworld\\.com/tatw/(\\d+)").getMatch(0));
            finallink = br.getRegex("so\\.addVariable\\(\"url\", \"(.*?)\"").getMatch(0);
            finalfilename = br.getRegex("so\\.addVariable\\(\"name\", \"(.*?)\"").getMatch(0);
            if (finalfilename != null) {
                finalfilename = finalfilename.trim() + ".mp3";
            }
            dh = true;
        } else if (parameter.contains("mrbrownee70.com/?id=")) {
            finallink = br.getRegex("href=\"(.*?)\" target=\"_blank\">Direct Link</a>").getMatch(0);
        } else if (parameter.contains("gantrack.com/")) {
            if (br.getRedirectLocation() != null) {
                br.getPage(br.getRedirectLocation());
                if (br.getRedirectLocation() != null) {
                    br.getPage(br.getRedirectLocation());
                }
            }
            finallink = br.getRegex("http\\-equiv=\"refresh\" content=\"\\d+;URL=(.*?)\">").getMatch(0);
        } else if (parameter.contains("adfoc.us/")) {
            String id = new Regex(parameter, ".us/(.+)").getMatch(0);
            if ("forum".equalsIgnoreCase(id) || "support".equalsIgnoreCase(id) || "self".equalsIgnoreCase(id) || "user".equalsIgnoreCase(id) || "payout".equalsIgnoreCase(id) || "api".equalsIgnoreCase(id) || "js".equalsIgnoreCase(id) || "ajax".equalsIgnoreCase(id) || "faq".equalsIgnoreCase(id) || "1How".equalsIgnoreCase(id)) { return decryptedLinks; }
            br.getPage(parameter);
            String click = br.getRegex("var click_url = \"(https?://(?!adfoc\\.us/)[^\"]+)").getMatch(0);
            if (click == null) {
                click = br.getRegex("(http://adfoc\\.us/serve/click/\\?id=[a-z0-9]+\\&servehash=[a-z0-9]+\\&timestamp=\\d+)").getMatch(0);
                if (click != null) {
                    br.getHeaders().put("Referer", parameter);
                    br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    br.getPage(click);
                    if (br.getRedirectLocation() != null && !br.getRedirectLocation().matches("http://adfoc.us/")) {
                        finallink = br.getRedirectLocation();
                    }
                }
            } else {
                finallink = click;
            }
        } else if (parameter.contains("unlimfiles.com/")) {
            finallink = br.getRegex("<iframe src=\"(.*?)\"").getMatch(0);
            if (br.containsHTML("<iframe src=\"\">")) offline = true;
        } else if (parameter.contains("mediaboom.org/")) {
            finallink = Encoding.Base64Decode(new Regex(Encoding.htmlDecode(parameter), "mediaboom\\.org/engine/go\\.php\\?url=(.+)").getMatch(0));
        } else if (parameter.contains("gabber.od.ua/")) {
            finallink = br.getRegex("Download link:<br><br><br><a href=\\'([^<>\"\\']+)\\'").getMatch(0);
        } else if (parameter.contains("icefilms.info/")) {
            String nextUrl = br.getRegex("value=\'<iframe src=\"(http.*?)\"").getMatch(0);
            if (nextUrl == null) {
                nextUrl = br.getRegex("<iframe id=\"videoframe\" src=\"(.*?)\"").getMatch(0);
                if (nextUrl == null) { return null; }
                nextUrl = !nextUrl.startsWith("http") ? "http://www.icefilms.info" + nextUrl : nextUrl;
            }
            br.getPage(nextUrl);
            final String id = br.getRegex("onclick=\'go\\((\\d+)").getMatch(0);
            final String sec = br.getRegex("f\\.lastChild\\.value=\"(\\w+)\"").getMatch(0);
            final String t = br.getRegex("&t=(\\d+)\"").getMatch(0);
            final int s = (int) (1 + Math.random() * 20);
            final int m = (int) (1 + Math.random() * 250);
            String url = br.getRegex("\"POST\",\"(.*?)\"").getMatch(0);
            if (id == null || sec == null || t == null || url == null) { return null; }
            url = url.startsWith("http") ? url : "http://www.icefilms.info" + url;
            br.postPage(url, "id=" + id + "&s=" + String.valueOf(s) + "&iqs=&url=&m=-" + String.valueOf(m) + "&cap=&sec=" + sec + "&t=" + t);
            finallink = Encoding.htmlDecode(br.getRegex("url=(.*?)$").getMatch(0));
        } else if (parameter.contains("q32.ru/")) {
            final Form dlForm = br.getForm(0);
            if (dlForm != null) {
                br.submitForm(dlForm);
                finallink = br.getRegex("http\\-equiv=\"Refresh\" content=\"\\d+; URL=(.*?)\"").getMatch(0);
            }
        } else if (parameter.contains("url.cn/")) {
            br.setFollowRedirects(true);
            br.getPage(parameter);
            finallink = br.getRegex("window\\.location=\"(.*?)\"").getMatch(0);
        } else if (parameter.contains("guardlink.org/")) {
            finallink = br.getRegex("<iframe src=\"([^<>\"\\']+)\"").getMatch(0);
            if (finallink != null) {
                finallink = Encoding.deepHtmlDecode(finallink).replace("&#114", "r");
            }
        } else if (parameter.contains("digitaldripped.com/")) {
            if (br.containsHTML("http\\-equiv=\"refresh\">") || br.containsHTML(">404 Not Found<")) offline = true;
            finallink = br.getRegex("<a class=\"download\\-btn\" target=\"[^<>\"\\']+\" href=\"(http://[^<>\"\\']+)\"").getMatch(0);
        } else if (parameter.contains("dwz.cn/")) {
            // It's a normal redirector but needs special handling, maybe
            // because of browser bug
            br.setFollowRedirects(true);
            br.getPage(parameter);
            if (!br.getURL().contains("dwz.cn/")) {
                finallink = br.getURL();
            }
        } else if (parameter.contains("lezlezlez.com/")) {
            finallink = parameter.replace("lezlezlez.com/mediaswf.php?type=vid&name=", "lezlezlez.com/xmoov.php?file=vidz/") + "&start=true";
            dh = true;
            finalfilename = new Regex(parameter, "\\?type=vid\\&name=(.+)").getMatch(0);
        } else if (parameter.contains("adv.li/")) {
            finallink = br.getRedirectLocation();
            if (finallink == null) finallink = br.getRegex("_url=\\'(https?://[^<>\"]*?)\\'").getMatch(0);
        } else if (parameter.contains("protetorbr.com/")) {
            if (br.getRedirectLocation() != null) {
                br.getPage(br.getRedirectLocation());
            }
            final Map<String, List<String>> rH = br.getRequest().getResponseHeaders();
            final Set<String> keys = rH.keySet();
            for (final String s : keys) {
                if (!"refresh".equalsIgnoreCase(s)) {
                    continue;
                }
                for (final String ss : rH.get(s)) {
                    finallink = new Regex(ss, "URL=(.*?)$").getMatch(0);
                }
            }
        } else if (parameter.contains("leechmf.com/") || parameter.contains("hnzoom.com/")) {
            if (parameter.matches(".+hnzoom\\.com/folder/[a-zA-Z0-9\\-]{11}")) {
                String uid = new Regex(parameter, "/folder/([a-zA-Z0-9\\-]{11})").getMatch(0);
                br.getPage(parameter);
                String[] links = br.getRegex("href=\"([^\"]+/out/" + uid + "/\\d+)").getColumn(0);
                if (links != null && links.length != 0) {
                    for (String link : links) {
                        Browser br2 = br.cloneBrowser();
                        br2.getPage(link);
                        // jd2 supports within refresh
                        String fl = br2.getRedirectLocation();
                        if (fl == null) fl = br2.getRegex("url=([^'\"\n\t]+);?").getMatch(0);
                        if (fl != null) {
                            decryptedLinks.add(createDownloadlink(fl));
                        }
                    }
                }
                if (links == null || links.length == 0 || decryptedLinks.isEmpty()) {
                    logger.warning("Possible plugin error! Please confirm link within your web browser. If broken please report to JDownloader Development Team : " + parameter);
                    return null;
                }
                return decryptedLinks;
            } else {
                // 'protection service' to final mediafire direct link
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.postPage(new Regex(parameter, "(https?://[^/]+)").getMatch(0) + "/get.php?do=getlink", "url=" + new Regex(parameter, "https?://[^/]+/\\?(.+)").getMatch(0) + "&pass=");
                if (br.containsHTML("\"status\":2") && br.containsHTML("\"link\":null")) {
                    // requires password
                    for (int i = 0; i <= 3; i++) {
                        final String pass = Plugin.getUserInput("Password Required", param);
                        if (pass == null) {
                            return decryptedLinks;
                        } else {
                            br.postPage(new Regex(parameter, "(https?://[^/]+)").getMatch(0) + "/get.php?do=getlink", "url=" + new Regex(parameter, "https?://[^/]+/\\?(.+)").getMatch(0) + "&pass=" + pass);
                        }
                        if (br.containsHTML("\"status\":2") && br.containsHTML("\"link\":null")) {
                            continue;
                        } else {
                            break;
                        }
                    }
                }
                if (br.containsHTML("\"status\":4")) {
                    logger.info("Link offline: " + parameter);
                    return decryptedLinks;
                }
                finallink = br.getRegex("\"link\":\"(http:[^<>\"]*?)\"").getMatch(0);
                if (finallink != null) {
                    finallink = finallink.replace("\\", "");
                    final String mediafireID = new Regex(finallink, "http://[^/]*?/[^/]*?/([^/]*?)/").getMatch(0);
                    if (mediafireID != null) {
                        decryptedLinks.add(createDownloadlink("http://www.mediafire.com/?" + mediafireID));
                        return decryptedLinks;
                    }
                }
            }
        } else if (parameter.contains("share-films.net/")) {
            finallink = br.getRegex("<meta http\\-equiv=\"refresh\" content=\"\\d+; url=([^<>\"]*?)\"").getMatch(0);
        } else if (parameter.contains("stream2k.eu/")) {
            finallink = br.getRegex("proxy\\.swf\\&amp;proxy\\.link=(http://[^<>\"]*?)\\&amp;skin=").getMatch(0);
        } else if (parameter.contains("basemp3.ru/")) {
            finallink = "http://basemp3.ru/download.php?id=" + new Regex(parameter, "(\\d+)\\.html$").getMatch(0);
            finalfilename = br.getRegex("<title>([^<>\"]*?) скачать бесплатно mp3").getMatch(0);
            if (finalfilename == null) {
                finalfilename = br.getRegex("href=\"download\\.php\\?id=\\d+\" title=\"([^<>\"]*?) скачать бесплатно mp3\"").getMatch(0);
            }
            if (finalfilename != null) {
                finalfilename = Encoding.htmlDecode(finalfilename.trim()) + ".mp3";
            }
            dh = true;
        } else if (parameter.contains("focus.de/")) {
            finallink = br.getRegex("video player \\-\\-.*?href=\"(http.*?videos\\.focus\\.de.*?)\" style=").getMatch(0);
            finallink = Encoding.htmlDecode(finallink);
            finalfilename = br.getRegex("customizeHeadlinesType\\(\'(.*?)\',").getMatch(0);
            if (finalfilename != null) {
                finalfilename = Encoding.htmlDecode(finalfilename.trim()) + ".mp4";
            }
            dh = true;
        } else if (parameter.contains("hflix.in/")) {
            finallink = br.getRedirectLocation();
            if (finallink == null) {
                finallink = br.getRegex("<a id=\"yourls\\-once\" href=\"(http://[^<>\"]*?)\"").getMatch(0);
            }
        } else if (parameter.contains("getunite.com/")) {
            finallink = "http://mediafire.com/?" + new Regex(parameter, "getunite\\.com/\\?d=\\d+\\.\\d+\\.\\d+\\.\\d+/[a-z0-9]+/([a-z0-9]+)/").getMatch(0);
        } else if (parameter.contains("tinymoviez.info/")) {
            finallink = br.getRegex("ReplaceContentInContainer\\(containerID,\\'<a href=\"(http://[^<>\"]*?)\"").getMatch(0);
        } else if (parameter.contains("url.bilgiportal.com")) {
            finallink = br.getRegex("<frame id=\"main\" name=\"main\" marginwidth=\"0\" marginheight=\"0\" src=\"(http[^<>\"]*?)\"").getMatch(0);
        } else if (parameter.contains("komp3.net/")) {
            br.getPage(parameter.replace("/download/mp3/", "/download/mp3/da/"));
            finallink = br.getRedirectLocation();
        } else if (parameter.contains("shrk.biz/")) {
            finallink = br.getRegex("<meta http\\-equiv=\"refresh\" content=\"\\d+; URL=(https?://[^<>\"]*?)\"").getMatch(0);
        } else if (parameter.contains("egcenter.com/")) {
            String next = new Regex(parameter, "/(\\d+)").getMatch(0);
            br.getPage(parameter.replace(next, "2-" + next));
            finallink = br.getRegex("<div id=\"continue\" style=\'display:none\'><a href=\'(http://[^\']+)").getMatch(0);
        } else if (parameter.contains("allsubs.org/")) {
            finallink = "directhttp://" + parameter.replace("/subs-download/", "//subs-download2/") + "/";
        } else if (parameter.contains("sharmota.com/")) {
            br.setFollowRedirects(true);
            br.getPage(parameter);
            finalfilename = br.getRegex("<h3><b>(.*?)</b></h3>").getMatch(0);
            if (finalfilename != null) finalfilename = finalfilename.replaceAll("[\r\n]+", "") + ".flv";
            finallink = "http://videos.sharmota.com/flv888/" + new Regex(parameter, "movies/\\d+/(\\d+)").getMatch(0) + ".flv";
        } else if (parameter.matches("https?://t\\.co/.*")) {
            if (br.containsHTML("<h1>Sorry, that page doesn’t exist|<p>Thanks for noticing\\&mdash;we\\'re going")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            finallink = br.getRedirectLocation();
            if (finallink == null) finallink = br.getRegex("http\\-equiv=\"refresh\" content=\"\\d+;URL=(http[^<>\"]*?)\"").getMatch(0);
        } else if (parameter.contains("searchonzippy.eu/")) {
            finallink = br.getRegex("setTimeout\\(\"document\\.location\\.href=\\'(http[^<>\"]*?)\\'\"").getMatch(0);
        } else if (parameter.contains("mkv2.info/")) {
            finallink = br.getRegex("<iframe id=\"frame\" src=\"(http[^<>\"]*?)\"").getMatch(0);
        } else if (parameter.contains("handsupbr.com/")) {
            br.postPage(parameter, "dl=1");
            finallink = br.getRegex("Click on Download below to start the download</div>.*? <a href=\"(http[^<>\"]*?)\"").getMatch(0);
        } else if (parameter.contains("lnx.lu/") || parameter.contains("z.gs/") || parameter.contains("url.fm/")) {
            if (parameter.matches("http://(www\\.)?(lnx\\.lu|z\\.gs|url\\.fm)/(dmca|privacy|terms|advertising|contact|rates|faq)")) {
                logger.info("Link invalid: " + parameter);
                return decryptedLinks;
            }
            if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
            if (br.containsHTML("No htmlCode read")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            if (br.containsHTML("id=\"shrinkfield\">")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            if (br.containsHTML("<title>Index of")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            finallink = br.getRegex("\"(/\\?click=[^<>\"/]*?)\"").getMatch(0);
            if (finallink != null) {
                br.getPage("http://lnx.lu" + finallink);
                if (br.containsHTML("No htmlCode read")) {
                    logger.info("Link offline: " + parameter);
                    return decryptedLinks;
                }
                finallink = br.getRedirectLocation();
            }
        } else if (parameter.contains("peeplink.in/")) {
            finallink = br.getRegex("<article>(.*?)</article>").getMatch(0);
            if (finallink != null) {
                final String[] finallinks = HTMLParser.getHttpLinks(finallink, "");
                if (finallinks != null && finallinks.length != 0) {
                    for (final String aLink : finallinks)
                        decryptedLinks.add(createDownloadlink(aLink));
                    return decryptedLinks;
                } else {
                    finallink = null;
                }
            }
        } else if (parameter.contains("zo.mu/")) {
            if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
            int seconds = 5;
            String secondssite = br.getRegex("You will be redirected in <strong id=\"sec\">(\\d+)</strong> seconds</div>").getMatch(0);
            if (secondssite == null) secondssite = br.getRegex("zomu\\.redirect\\.show\\(\\'(\\d+)\\',").getMatch(0);
            if (secondssite != null) seconds = Integer.parseInt(secondssite);
            sleep(seconds * 1001l, param);
            br.getPage("http://zo.mu/redirector/process?link=" + new Regex(parameter, "([A-Za-z0-9]+)$").getMatch(0));
            finallink = br.getRedirectLocation();
            if (finallink != null) {
                if (finallink.contains("zo.mu/")) br.getPage(finallink);
                finallink = br.getRedirectLocation();
            }
        } else if (parameter.contains("madlink.sk/") || parameter.contains("m-l.sk/")) {
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.postPage("http://madlink.sk/ajax/check_redirect.php", "link=" + new Regex(parameter, "([a-z0-9]+)$").getMatch(0));
            finallink = br.toString();
            if (finallink == null || finallink.length() > 500 || !finallink.startsWith("http")) finallink = null;
        } else if (parameter.contains("demo.ovh.net")) {
            final String fileURL = param.getCryptedUrl();
            final int languagePosition = fileURL.indexOf("/", 7);
            final String language = fileURL.substring(languagePosition + 1, languagePosition + 3);
            final String hosterURL = fileURL.substring(0, fileURL.indexOf("/" + language + "/"));
            final String fileID = fileURL.replace(hosterURL + "/" + language + "/", "").replace("/", "");
            finallink = hosterURL + br.getRegex("<a href=\"(/download/.*)\"><img class=\"fdd\" src=\"/images/download.gif\" alt=\"download\"><span name=\"filename\">").getMatch(0);
            finalfilename = finallink.replace(hosterURL + "/download/", "").replace(fileID + "/", "");
        } else if (parameter.contains("mblink.info/")) {
            finallink = br.getRegex("http\\-equiv=\"refresh\" content=\"\\d+; URL=(http[^<>\"]*?)\"").getMatch(0);
        } else if (parameter.contains("potlocker.net/")) {
            finallink = br.getRegex("<IFRAME SRC=\"(http[^<>\"]*?)\" FRAMEBORDER=0").getMatch(0);
        }
        if (offline) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (finallink == null) {
            logger.info("DecrypterForRedirectServicesWithoutDirectRedirects says \"Out of date\" for link: " + parameter);
            return null;
        }
        if (dh) {
            finallink = "directhttp://" + finallink;
        }
        final DownloadLink dl = createDownloadlink(finallink);
        if (finalfilename != null) {
            dl.setFinalFileName(finalfilename);
        }
        decryptedLinks.add(dl);

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}