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
import jd.http.ext.BasicBrowserEnviroment;
import jd.http.ext.ExtBrowser;
import jd.http.ext.ExtBrowserException;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "komp3.net", "url.bilgiportal.com", "tinymoviez.info", "getunite.com", "hflix.in", "focus.de", "hnzoom.com", "basemp3.ru", "stream2k.eu", "share-films.net", "leechmf.com", "protetorbr.com", "primemusic.ru", "adv.li", "lezlezlez.com", "dwz.cn", "online.nolife-tv.com", "digitaldripped.com", "guardlink.org", "url.cn", "q32.ru", "shrk.biz", "icefilms.info", "gabber.od.ua", "mediaboom.org", "vimeo.com", "unlimfiles.com", "crypt2.be", "adfoc.us", "mrbrownee70.com", "alturl.com", "trancearoundtheworld.com", "egfire.net", "damasgate.com", "freeonsmash.com", "lnk.co", "trackstash.com", "fburls.com", "myurl.in", "h-url.in", "dropbox.com", "1-star.net", "protect-ddl.com", "filep.info", "grou.ps", "linkexterno.com", "eskimotube.com", "m4u.in", "4p5.com", "t.co", "telona.biz", "madmimi.com", "href.hu",
        "hide.linkleak.org", "migre.me", "degracaemaisgostoso.info", "altervista.org", "agaleradodownload.com", "musicloud.fm", "wowebook.com", "link.songs.pk + songspk.info", "imageto.net", "clubteam.eu", "jforum.uni.cc", "linksole.com", "deurl.me", "yourfileplace.com", "muzgruz.ru", "zero10.net", "aiotool.net", "chip.de/c1_videos", "multiprotect.info", "nbanews.us", "top2tech.com", "umquetenha.org", "oneclickmoviez.com/dwnl/", "1tool.biz", "trailerzone.info", "imagetwist.com", "file4ever.us and catchfile.net", "zero10.net and gamz.us", "official.fm", "hypem.com", "academicearth.org", "skreemr.org", "tm-exchange.com", "adiarimore.com", "mafia.to/download", "bogatube.com", "newgrounds.com", "accuratefiles.com", "slutdrive.com", "view.stern.de", "fileblip.com", "warcraft.ingame.de", "mixconnect.com", "twiturm.com", "ebooksdownloadfree.com", "freebooksearcher.info", "ubuntuone.com",
        "mp3.wp.pl", "gantrack.com" }, urls = { "http://(www\\.)?komp3\\.net/download/mp3/\\d+/[^<>\"]+\\.html", "http://(www\\.)?url\\.bilgiportal\\.com/[0-9]+", "http://(www\\.)?tinymoviez\\.info/download\\.php\\?link=[A-Za-z0-9]+", "http://(www\\.)?getunite\\.com/\\?d=\\d+\\.\\d+\\.\\d+\\.\\d+/[a-z0-9]+/[a-z0-9]+/", "http://(www\\.)?hflix\\.in/[A-Za-z0-9]+", "http://(www\\.)?focus\\.de/[a-zA-Z]+/(videos|internet/[a-zA-Z]+)/[\\w\\-]+\\.html", "http://(www\\.)?hnzoom.com/\\?[A-Za-z0-9]{20}", "http://(www\\.)?basemp3\\.ru/music\\-view\\-\\d+\\.html", "http://(www\\.)?stream2k\\.eu/video/\\d+", "http://(www\\.)?share\\-films\\.net/redirect\\.php\\?url=[a-z0-9]+", "http://(www\\.)?leechmf\\.com/\\?[A-Za-z0-9]+", "http://(www\\.)?protetorbr\\.com/d\\?id=\\d+", "http://(www\\.)?primemusic\\.ru/Media\\-page\\-\\d+\\.html", "http://(www\\.)?adv\\.li/[A-Za-z0-9]+",
        "http://(www\\.)?lezlezlez\\.com/mediaswf\\.php\\?type=vid\\&name=[^<>\"/]+\\.flv", "http://(www\\.)?dwz\\.cn/[A-Za-z0-9]+", "http://(www\\.)?online\\.nolife\\-tv\\.com/index\\.php\\?id=\\d+", "http://(www\\.)?digitaldripped\\.com/[^<>\"\\']+", "http://(www\\.)?guardlink\\.org/[A-Za-z0-9]+", "http://url\\.cn/[0-9a-zA-Z]+", "http://q32\\.ru/\\d+/c/[A-Za-z0-9\\-_]+", "http://(de\\.)?shrk\\.biz/\\w+", "http://(www\\.)?icefilms\\.info/ip\\.php\\?v=\\d+\\&?", "http://(www\\.)?gabber\\.od\\.ua/g/\\?[^/<>\"]+", "http://(www\\.)?vimeo\\.com/(?!(\\d+/|tag|search)).*?/.+", "http://(www\\.)?unlimfiles\\.com/sourceframe/.+", "http://(www\\.)?crypt2\\.be/file/[a-z0-9]+", "http://(www\\.)?adfoc\\.us/(serve/\\?id=[a-z0-9]+|[a-z0-9]+)", "http://(www\\.)?mrbrownee70\\.com/\\?id=[A-Za-z0-9]+", "http://[\\w\\.]*?alturl\\.com/[a-z0-9]+", "http://(www\\.)?trancearoundtheworld\\.com/tatw/\\d+",
        "http://(www\\.)?egfire\\.net/\\d+", "http://(www\\.)?damasgate\\.com/redirector\\.php\\?url=.+", "http://(www\\.)?freeonsmash\\.com/redir/[A-Za-z0-9\\=\\+\\/\\.\\-]+", "http://(www\\.)?lnk\\.co/[A-Za-z0-9]+", "http://(www\\.)?trackstash\\.com/tracks/[a-z0-9\\-]+", "http://(www\\.)?fburls\\.com/\\d+\\-[A-Za-z0-9]+", "http://(www\\.)?protect\\.myurl\\.in/[A-Za-z0-9]+", "http://(www\\.)?h\\-url\\.in/[A-Za-z0-9]+", "https://(www\\.)?dropbox\\.com/s/[a-z0-9]+/.+", "http://(www\\.)?(1lien\\.com/(download/)?\\d+/|1\\-star\\.net/\\d+/\\d+/|stardima\\.com(/anime)?/download/\\d+/)", "http://(www\\.)?protect-ddl\\.com/[a-fA-F0-9]+", "http://(www\\.)?filep\\.info/(\\?url=|/)\\d+", "http://(www\\.)?grou\\.ps/[a-z0-9]+/videos/\\d+", "http://(www\\.)?linkexterno\\.com/[A-Za-z0-9]+", "http://(www\\.)?eskimotube\\.com/\\d+\\-.*?\\.html", "http://(www\\.)?m4u\\.in/[a-z0-9]+",
        "http://(www\\.)?4p5\\.com/[a-z0-9]+", "http://t\\.co/[a-zA-Z0-9]+", "http://[\\w\\.]*?telona\\.biz/protec?tor.*?\\?.*?//:ptth", "http://go\\.madmimi\\.com/redirects/[a-zA-Z0-9]+\\?pa=\\d+", "http://href\\.hu/x/[a-zA-Z0-9\\.]+", "http://hide\\.linkleak\\.org/[a-zA-Z0-9\\.]+", "http://[\\w\\.]*?migre\\.me/[a-z0-9A-Z]+", "http://[\\w\\.]*?degracaemaisgostoso\\.(biz|info)/download/\\?url=.*?:ptth", "http://[\\w\\.]*?altervista\\.org/\\?i=[0-9a-zA-Z]+", "http://[\\w\\.]*?agaleradodownload\\.com/download.*?\\?.*?//:ptth", "http://[\\w\\.]*?musicloud\\.fm/dl/[A-Za-z0-9]+", "http://[\\w\\.]*?wowebook\\.com/(e-|non-e-)book/.*?/.*?\\.html", "http://[\\w\\.]*?(link\\.songs\\.pk/(popsong|song1|bhangra)\\.php\\?songid=|songspk\\.info/ghazals/download/ghazals\\.php\\?id=)[0-9]+", "http://[\\w\\.]*?imageto\\.net/(\\?v=|images/)[0-9a-z]+\\..{2,4}",
        "http://[\\w\\.]*?clubteam\\.eu/dl\\.php\\?id=\\d\\&c=[a-zA-z0-9=]+", "http://[\\w\\.]*?jforum\\.uni\\.cc/protect/\\?r=[a-z0-9]+", "http://[\\w\\.]*?linksole\\.com/[0-9a-z]+", "http://[\\w\\.]*?deurl\\.me/[0-9A-Z]+", "http://(www\\.)?(yourfileplace|megafilegarden)\\.com/d/\\d+/.+", "http://[\\w\\.]*?muzgruz\\.ru/music/download/\\d+", "http://[\\w\\.]*?zero10\\.net/\\d+", "http://[\\w\\.]*?aiotool\\.net/\\d+", "http://[\\w\\.]*?chip\\.de/c1_videos/.*?-Video_\\d+\\.html", "http://(www\\.)?multiprotect\\.info/\\d+", "http://[\\w\\.]*?nbanews\\.us/\\d+", "http://[\\w\\.]*?top2tech\\.com/\\d+", "http://(www\\.)?umquetenha\\.org/protecao/resolve\\.php\\?link=.+", "http://[\\w\\.]*?oneclickmoviez\\.com/dwnl/.*?/\\d+/\\d+", "http://[\\w\\.]*?1tool\\.biz/\\d+", "http://(www\\.)?trailerzone\\.info/(protect|wait(2)?)\\.(php\\?|html)(key=|u=|#:{1,})[a-zA-Z0-9=+/]+",
        "http://[\\w\\.]*?imagetwist\\.com/[a-z0-9]{12}", "http://[\\w\\.]*?(file4ever\\.us|catchfile\\.net)/\\d+", "http://[\\w\\.]*?(zero10\\.net/|gamz\\.us/\\?id=)\\d+", "http://(www\\.)?official\\.fm/track(s)?/\\d+", "http://(www\\.)?hypem\\.com/(track/\\d+/|item/[a-z0-9]+)", "http://[\\w\\.]*?academicearth\\.org/lectures/.+", "http://[\\w\\.]*?skreemr\\.org/link\\.jsp\\?id=[A-Z0-9]+", "http://[\\w\\.]*?tm-exchange\\.com/(get\\.aspx\\?action=trackgbx|\\?action=trackshow)\\&id=\\d+", "http://[\\w\\.]*?adiarimore\\.com/miralink/[a-z0-9]+", "http://[\\w\\.]*?mafia\\.to/download-[a-z0-9]+\\.cfm", "http://[\\w\\.]*?bogatube\\.com/tube/\\d+/\\d+/.*?\\.php", "http://[\\w\\.]*?newgrounds\\.com/(portal/view/|audio/listen/)\\d+", "http://(www\\.)?accuratefiles\\.com/fileinfo/[a-z0-9]+", "http://(www\\.)?slutdrive\\.com/video-\\d+\\.html",
        "http://(www\\.)?view\\.stern\\.de/de/(picture|original)/.*?-\\d+\\.html", "http://(www\\.)?fileblip\\.com/[a-z0-9]+", "http://(www\\.)?warcraft\\.ingame\\.de/downloads/\\?file=\\d+", "http://(www\\.)?mixconnect\\.com/listen/.*?-mid\\d+", "http://(www\\.)?twiturm\\.com/[a-z0-9]+", "http://(www\\.)?ebooksdownloadfree\\.com/.*?/.*?\\.html", "http://(www\\.)?freebooksearcher\\.info/downloadbook\\.php\\?id=\\d+", "http://(www\\.)?ubuntuone\\.com/p/[A-Za-z0-9]+", "http://[\\w\\.]*?mp3\\.wp\\.pl/(?!ftp)(p/strefa/artysta/\\d+,utwor,\\d+\\.html|\\?tg=[A-Za-z0-9=]+)", "http://(www\\.)?gantrack\\.com/t/l/\\d+/[A-Za-z0-9]+", "http://(www\\.)?mediaboom\\.org/engine/go\\.php\\?url=.+" }, flags = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 })
public class DecrypterForRedirectServicesWithoutDirectRedirects extends PluginForDecrypt {

    private static final String NEWSREGEX2 = "<div id=\\'prep2\\' dir=\\'ltr\\' ><a  href=\\'(.*?)\\'";

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
        String finallink = null;
        String finalfilename = null;
        if (parameter.contains("vimeo.com")) {
            br.getPage(parameter);
            finallink = br.getRedirectLocation();
            if (finallink != null && !finallink.matches(".*?vimeo.com/\\d+")) {
                finallink = null;
            }
            if (finallink == null) { return decryptedLinks; }
        } else if (parameter.contains("alturl.com") || parameter.contains("protect-ddl.com") || parameter.contains("hide.linkleak.org") || parameter.contains("href.hu") || parameter.contains("madmimi.com") || parameter.contains("http://t.co")) {
            br.getPage(parameter);
            finallink = br.getRedirectLocation();
            if (finallink == null) {
                finallink = br.getRegex("REFRESH.*?url=(http.*?)\"").getMatch(0);
            }
        }
        /** Some links don't have to be accessed (here) */
        if (!parameter.contains("imageto.net/") && !parameter.contains("musicloud.fm/dl") && !parameter.contains("oneclickmoviez.com/dwnl/") && !parameter.contains("1tool.biz") && !parameter.contains("catchfile.net") && !parameter.contains("file4ever.us") && !parameter.contains("trailerzone.info/") && !parameter.contains("fairtilizer.com/") && !parameter.contains("tm-exchange.com/") && !parameter.contains("fileblip.com/") && !parameter.contains("mixconnect.com/") && !parameter.contains("machines.") && !parameter.contains("ubuntuone.com") && !parameter.contains("dropbox.com/") && !parameter.contains("trancearoundtheworld.com/") && !parameter.contains("dwz.cn/") && !parameter.contains("getunite.com/")) {
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
        } else if (parameter.contains("wowebook.com")) {
            final String redirectLink = br.getRegex("\"(http://www\\.wowebook\\.com/download.*?)\"").getMatch(0);
            if (redirectLink == null) { return null; }
            br.getPage(redirectLink);
            finallink = br.getRedirectLocation();
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
        } else if (parameter.contains("multiprotect.info/")) {
            br.setFollowRedirects(true);
            final String id = new Regex(parameter, "multiprotect\\.info/(\\d+)").getMatch(0);
            br.getPage("http://multiprotect.info/?id=" + id + "&d=1");
            finallink = br.getRegex("<div id=\\'prep2\\' ><a  href=(\\'|\")(.*?)(\\'|\")").getMatch(1);
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
        } else if (parameter.contains("oneclickmoviez.com/dwnl/")) {
            final Regex allMatches = new Regex(parameter, "oneclickmoviez\\.com/dwnl/(.*?)/(\\d+)/(\\d+)");
            final String host = allMatches.getMatch(0);
            final String id1 = allMatches.getMatch(1);
            final String id2 = allMatches.getMatch(2);
            final String getLink = "http://oneclickmoviez.com/wp-content/plugins/link-cloaking-plugin/wplc_redirector.php?post_id=" + id1 + "&link_num=" + id2 + "&cloaked_url=dwnl/" + host + "/" + id1 + "/" + id2;
            br.getPage(getLink);
            finallink = br.getRedirectLocation();
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
        } else if (parameter.contains("trailerzone.info/")) {
            final String cipher = new Regex(parameter, "#:{1,}(.*?)$").getMatch(-1);
            if (cipher == null) { return null; }
            final long timeStamp = System.currentTimeMillis();
            br.postPage("http://trailerzone.info/go.html" + cipher, "timestamp=" + timeStamp + "&link=&redir=Weiter+zum+Download");
            try {
                final ExtBrowser eb = new ExtBrowser();
                eb.setBrowserEnviroment(new BasicBrowserEnviroment(new String[] { ".*" }, new String[] { ".*trailerzone.info.*" }) {
                    @Override
                    public boolean isAutoProcessSubFrames() {
                        return false;
                    }
                });
                eb.eval(br);
                finallink = eb.getRegex("><iframe src=\"(.*?)\"").getMatch(0);
                if (!finallink.matches("http://.*?/.*?")) {
                    finallink = null;
                }
            } catch (final ExtBrowserException e) {
            }
        } else if (parameter.contains("imagetwist.com/")) {
            finallink = br.getRegex("\"(http://img\\d+\\.imagetwist\\.com/i/\\d+/.*?)\"").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("<p><img src=\"(http://.*?)\"").getMatch(0);
            }
            dh = true;
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
        } else if (parameter.contains("bogatube.com/")) {
            // Those site only contains videos of xvideos.com, just find the id
            // and make the link
            final String videoID = br.getRegex("id_video=(\\d+)\"").getMatch(0);
            if (videoID != null) {
                finallink = "http://xvideos.com/video" + videoID;
            }
        } else if (parameter.contains("newgrounds.com/")) {
            if (parameter.contains("/audio/listen/")) {
                finallink = br.getRegex("\\(\"filename=(.*?)\\&length").getMatch(0);
            } else {
                finallink = br.getRegex("var fw = new FlashWriter\\(\"(http.*?\\.swf)\"").getMatch(0);
            }
        } else if (parameter.contains("accuratefiles.com/")) {
            String fileID = br.getRegex("\"/go/(\\d+)\"").getMatch(0);
            if (fileID == null) {
                fileID = br.getRegex("name=\"file_id\" value=\"(\\d+)\"").getMatch(0);
            }
            if (fileID == null) { return null; }
            br.postPage("http://www.accuratefiles.com/go/" + fileID, "file_id=" + fileID);
            finallink = br.getRegex("id=\"fdown\" name=\"fdown\" src=\"(.*?)\"").getMatch(0);
        } else if (parameter.contains("slutdrive.com/video-")) {
            finallink = br.getRegex("allowscriptaccess=\"always\" flashvars=\"file=(http://.*?\\.flv)\\&image=").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("(http://fs\\d+\\.slutdrive\\.com/videos/\\d+\\.flv)").getMatch(0);
            }
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
            final String fid = new Regex(parameter, "mixconnect\\.com/listen/.*?-mid(\\d+)").getMatch(0);
            br.getPage("http://www.mixconnect.com/downloadcheck.php?id=" + fid);
            final String dlHash = br.getRegex("dlhash\":\"([a-z0-9]+)\"").getMatch(0);
            if (dlHash == null) { return null; }
            br.postPage("http://www.mixconnect.com/createdownload.php?id=" + fid, "dlhash=" + dlHash);
            finallink = br.toString();
            if (!finallink.startsWith("data/") || finallink.length() > 500) { return null; }
            if (finallink.equals("data/zip/.zip")) { throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore.")); }
            finallink = "directhttp://http://mixconnect.com/" + finallink;
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
            finallink = br.getRegex("<font face=arial size=2 color=black><b><a href=(http://.*?)>").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("overlayId: \\'play\\' \\},[\t\n\r ]+\\{ url: \\'/movie\\.php\\?movie=(.*?)\\'").getMatch(0);
                if (finallink != null) {
                    finallink = Encoding.Base64Decode(finallink);
                }
            }
            finalfilename = br.getRegex("<b><font color=green size=\\+1>(.*?)</font></b><br>").getMatch(0);
            String name2 = br.getRegex("Scene Info: <b><font color=brown>(.*?)</font></b><br>").getMatch(0);
            if (name2 == null) {
                name2 = br.getRegex("<TITLE>EskimoTube\\.com - Streaming Videos of .*? \\- (.*?) \\- Pornstars And Centerfolds\\.</title>").getMatch(0);
            }
            if (finalfilename == null) {
                finalfilename = br.getRegex("<TITLE>EskimoTube\\.com - Streaming Videos of (.*?) \\- ").getMatch(0);
            }
            if (finalfilename != null && finallink != null && name2 != null) {
                finalfilename += " - " + name2 + finallink.substring(finallink.length() - 4, finallink.length());
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
        } else if (parameter.contains("stardima.com/") || parameter.contains("1lien.com/") || parameter.contains("1-star.net/")) {
            if (br.getRedirectLocation() != null) {
                br.getPage(br.getRedirectLocation());
            }
            final Regex numbers = new Regex(br.getURL(), "1\\-star.net/(\\d+)/(\\d+)");
            final String numbr1 = numbers.getMatch(0);
            final String numbr2 = numbers.getMatch(1);
            if (numbr1 == null || numbr2 == null) { return null; }
            br.getPage("http://1-star.net/" + numbr1 + "/2.php?" + numbr2);
            finallink = br.getRegex("onclick=\"NewWindow\\(\\'(http[^<>\"\\']+)\\'").getMatch(0);
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
            br.getPage(parameter);
            String click = br.getRegex("(http://adfoc\\.us/serve/click/\\?id=[a-z0-9]+\\&servehash=[a-z0-9]+\\&timestamp=\\d+)").getMatch(0);
            if (click == null) {
                click = br.getRegex("var click_url = \"([^\"]+)").getMatch(0);
            }
            if (click != null) {
                br.getHeaders().put("Referer", parameter);
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.getPage(click);
                if (br.getRedirectLocation() != null && !br.getRedirectLocation().matches("http://adfoc.us/")) {
                    finallink = br.getRedirectLocation();
                }
            }
        } else if (parameter.contains("crypt2.be/")) {
            finallink = br.getRegex("id=\"iframe\" src=\"([^\"\\'<>]+)\"").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("name=\"movie\" value=\"([^\"\\'<>]+)\"").getMatch(0);
            }
            if (finallink == null) {
                finallink = br.getRedirectLocation();
            }
        } else if (parameter.contains("unlimfiles.com/")) {
            finallink = br.getRegex("<iframe src=\"(.*?)\"").getMatch(0);
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
            finallink = br.getRegex("<a class=\"download\\-btn\" target=\"[^<>\"\\']+\" href=\"(http://[^<>`\"\\']+)\"").getMatch(0);
        } else if (parameter.contains("online.nolife-tv.com/")) {
            br.setFollowRedirects(true);
            if (br.getRedirectLocation() != null) {
                br.getPage(br.getRedirectLocation());
            }
            finallink = br.getRegex("class=\"underline\">sur un autre service</span>\\.<br />[\t\n\r ]+<a href=\"(http://[^<>\"\\']+)\"").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("<div id=\"screen_non_abo\">[\t\n\r ]+<a href=\"(http://[^<>\"\\']+)\"").getMatch(0);
            }
            if (finallink == null) {
                finallink = parameter.replace("online.nolife-tv.com/", "online.nolife-tvdecrypted.com/");
            }
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
            finallink = br.getRegex("_url=\\'(http://[^<>\"]*?)\\'").getMatch(0);
        } else if (parameter.contains("primemusic.ru/")) {
            finalfilename = br.getRegex("<h2>Скачать ([^<>\"]*?)\\.mp3</h2>").getMatch(0);
            if (finalfilename == null) {
                finalfilename = br.getRegex("<div class=\"caption\">[\t\n\r ]+<h1>([^<>\"]*?) скачать песню</h1>").getMatch(0);
            }
            if (finalfilename != null) {
                finalfilename = Encoding.htmlDecode(finalfilename.trim()) + ".mp3";
            }
            br.getPage(parameter.replace("/Media-page-", "/Media-download-"));
            finallink = br.getRegex("<a class=\"download\" href=(http://[^<>\"]*?\\.mp3)\"").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("\"(http://mp3\\.primemusic\\.ru/dl\\d+/[^<>\"]*?)\"").getMatch(0);
            }
            dh = true;
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
            finallink = br.getRegex("<a id=\"yourls\\-once\" href=\"(http://[^<>\"]*?)\"").getMatch(0);
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
}