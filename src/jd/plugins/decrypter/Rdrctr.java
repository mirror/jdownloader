//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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
package jd.plugins.decrypter;

import java.net.SocketTimeoutException;
import java.util.ArrayList;

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.components.SiteType.SiteTemplate;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3,

names = { "lan.wf", "upx.nz", "engt.co", "camshowdownloads.com", "hungryleech.com", "soo.gd", "sht.io", "afl.to", "noref.co", "led.wf", "vk.cc", "somosmovies.com", "smv.tv", "ligman.me", "feedproxy.google.com", "cur.bz", "awe.sm", "pip.bz", "ovh.to", "po.st", "share-films.net", "fburls.com", "shrk.biz", "go.madmimi.com", "alturl.com", "ssh.yt", "rapidsky.net", "yhoo.it", "proton.biz", "smarturl.it", "westernlink.co", "sharingdb.com", "ift.tt", "urlm.in", "cutmy.name", "tinyw.in", "baidu.com", "relink.ws", "clck.ru", "youtu.be", "readthis.ca", "redirects.ca", "goshrink.com", "clickthru.ca", "atu.ca", "easyurl.net", "redirect.wayaround.org", "rurl.org", "tinyurl.com", "smarturl.eu", "rln.me", "sp2.ro", "ow.ly", "bit.ly", "maticulo.us", "trib.al", "ab.co", "vb.ly", "ponyurl.com", "budurl.com", "tra.kz", "tiny.pl", "linkth.at", "moourl.com", "ri.ms", "gelen.org", "pettyurl.com", "9mmo.com",
        "decenturl.com", "shor7.com", "f5y.info", "starturl.com", "2s8.org", "kno.li", "wapurl.co.uk", "0845.com", "piurl.com", "twurl.nl", "shortlinks.co.uk", "rubyurl.com", "u.nu", "hex.io", "short.redirect.am", "2li.ru", "goo.gl", "u.to", "up.ht", "romsite.net", "ymlp.com", "lowlifeinc.us2.list-manage.com", "metalwarez.com", "shorlink.net", "chilp.it", "dai.ly", "db.tt", "artofx.org", "ewe.h1.ru" },

urls = { "https?://(?:www\\.)?lan\\.wf/[a-zA-Z0-9]+", "https?://(?:www\\.)?upx\\.nz/[a-zA-Z0-9]+", "https?://(?:www\\.)?engt\\.co/[a-zA-Z0-9]+", "https?://(?:www\\.)?camshowdownloads\\.com/dl/.+", "https?://(?:www\\.)?hungryleech\\.com/redir/[a-zA-Z0-9]+(?:/[\\w\\-]+)?", "https?://soo\\.gd/.+", "https?://(?:\\w+\\.)?sht\\.io/[a-z0-9]{4,}", "https?://(?:\\w+\\.)?afl\\.to/[a-z0-9A-Z]{7}", "https?://noref\\.co/\\?id=\\d+", "https?://(www\\.)?led\\.wf/[a-zA-Z0-9]+", "https?://(?:www\\.)?vk\\.cc/[A-Za-z0-9]+", "https?://(?:www\\.)?somosmovies\\.com/link/[A-Za-z0-9=]+/?", "https?://(?:www\\.)?smv\\.tv/link/[A-Za-z0-9=]+/?", "http://(?:www\\.)?ligman\\.me/[A-Za-z0-9]+", "https?://feedproxy\\.google\\.com/~r/.+", "http://cur\\.bz/[A-Za-z0-9]+", "http://(www\\.)?awe\\.sm/[A-Za-z0-9]+", "http://(www\\.)?pip\\.bz/[A-Za-z0-9\\-]+", "http://(www\\.)?ovh\\.to/[A-Za-z0-9]+",
        "http://(www\\.)?po\\.st/[A-Za-z0-9]+", "http://(www\\.)?share-films\\.net/redirect\\.php\\?url=[a-z0-9]+", "http://(www\\.)?fburls\\.com/\\d+\\-[A-Za-z0-9]+", "http://(de\\.)?shrk\\.biz/\\w+", "https?://go\\.madmimi\\.com/redirects/[a-zA-Z0-9]+\\?pa=\\d+", "http://[\\w\\.]*?alturl\\.com/[a-z0-9]+", "http://(?:www\\.)?ssh\\.yt/[a-zA-Z0-9]+", "http://(www\\.)?rapidsky\\.net/[A-Za-z0-9]+", "https?://(www\\.)?yhoo\\.it/[\\w]+", "https?://(\\w\\.)?proton\\.biz/aralara\\.php\\?link=[a-z0-9]+", "http://[\\w\\.]*smarturl\\.it/[A-Za-z0-9]+", "http://[\\w\\.]*westernlink\\.co/l/[A-Za-z0-9]+", "http://[\\w\\.]*sharingdb\\.com/[a-z0-9]+", "http://(www\\.)?ift\\.tt/[a-zA-Z0-9]+", "https?://(www\\.)?urlm\\.in/[a-zA-Z0-9]+", "http://(www\\.)?cutmy\\.name/(?!#|.+\\.php)(u/[a-f0-9]{6,8}|[a-z0-9]+)", "https?://(www\\.)?tinyw\\.in/[a-zA-Z0-9]+",
        "http://(www\\.)?baidu\\.com/link\\?url=[A-Za-z0-9\\-_]+", "http://(www\\.)?relink\\.ws/[A-Za-z0-9]+", "http://(www\\.)?clck\\.ru/(d/[A-Za-z0-9\\-_]+|[A-Za-z0-9\\-_]+)", "https?://[\\w\\.]*youtu\\.be/[a-z_A-Z0-9\\-]+", "http://[\\w\\.]*readthis\\.ca(/[a-zA-Z0-9]+)?", "http://[\\w\\.]*redirects\\.ca(/[a-zA-Z0-9]+)?", "http://[\\w\\.]*goshrink\\.com(/[a-zA-Z0-9]+)?", "http://[\\w\\.]*clickthru\\.ca(/[a-zA-Z0-9]+)?", "http://[\\w\\.]*atu\\.ca(/[a-zA-Z0-9]+)?", "http://[\\w\\.]*easyurl\\.net(/[a-zA-Z0-9]+)?", "http://[\\w\\.]*redirect\\.wayaround\\.org/[a-zA-Z0-9]+/(.*)", "http://[\\w\\.]*rurl\\.org(/[a-zA-Z0-9]+)?", "http://(www\\.)?tinyurl\\.com/(?!favicon|preview)[a-z0-9]+(/[a-z0-9]+)?", "http://[\\w\\.]*smarturl\\.eu/\\?[a-zA-Z0-9]+", "http://[\\w\\.]*rln\\.me/[0-9a-zA-Z]+", "http://[\\w\\.]*sp2\\.ro/[0-9a-zA-Z]+", "http://[\\w\\.]*ow\\.ly/[\\w]+",
        "http://(www\\.)?bit\\.ly/[\\w]+", "http://(www\\.)?maticulo\\.us/[\\w]+", "http://(www\\.)?trib\\.al/[\\w]+", "http://(www\\.)?ab\\.co/[\\w]+", "http://[\\w\\.]*vb\\.ly/[\\w]+", "http://[\\w\\.]*ponyurl\\.com/[\\w]+", "http://[\\w\\.]*budurl\\.com/[a-zA-Z0-9]+", "http://[\\w\\.]*tra\\.kz/[\\w_\\-,()*:]+", "http://[\\w\\.]*tiny\\.pl/[\\w]+", "http://[\\w\\.]*linkth\\.at/[a-z]+", "http://[\\w\\.]*moourl\\.com/[a-zA-Z0-9]+", "http://[\\w\\.]*ri\\.ms/[a-z0-9]+", "http://[\\w\\.]*gelen\\.org/[0-9]+", "http://[\\w\\.]*pettyurl\\.com/[0-9a-z]+", "http://[\\w\\.]*9mmo\\.com/[a-z0-9]+", "http://[\\w\\.]*\\.decenturl\\.com/[a-z0-9.-]+", "http://[\\w\\.]*shor7\\.com/\\?[A-Z]+", "http://[\\w\\.]*f5y\\.info/[a-zA-Z-]+", "http://[\\w\\.]*starturl\\.com/[a-z_]+", "http://[\\w\\.]*2s8\\.org/[0-9a-z]+", "http://[\\w\\.]*kno\\.li/[0-9a-z]+", "http://[\\w\\.]*wapurl\\.co\\.uk/\\?[0-9A-Z]+",
        "http://[\\w\\.]*0845\\.com/[a-zA-Z0-9]+", "http://[\\w\\.]*piurl\\.com/[a-z0-9]+", "http://[\\w\\.]*twurl\\.nl/[a-z0-9]+", "http://[\\w\\.]*shortlinks\\.co\\.uk/[0-9a-z]+", "http://[\\w\\.]*rubyurl\\.com/[0-9a-zA-Z]+", "http://[\\w\\.]*u\\.nu/[0-9a-z]+", "http://(www\\.)?hex\\.io/(?!contact|user|javascript|api)[\\w]+", "http://short\\.redirect\\.am/\\?[\\w]+", "http://[\\w\\.]*2li\\.ru//[a-z0-9]+", "https?://[\\w\\.]*goo\\.gl/[A-Za-z0-9]+", "http://(www\\.)?u\\.to/[A-Za-z0-9\\-]+", "http://(www\\.)?up\\.ht/[A-Za-z0-9]+", "http://(www\\.)?romsite\\.net/download\\.php\\?id=\\d+\\&s=[a-z0-9]{1,4}\\&f=[a-z0-9]{1,5}", "http://(www\\.)?t\\.ymlp\\d+\\.com/[a-z0-9]+/click\\.php", "http://(www\\.)?lowlifeinc\\.us2\\.list-manage\\.com/track/click\\?u=[a-z0-9]+\\&id=[a-z0-9]+\\&e=[a-z0-9]+", "http://(www\\.)?metalwarez\\.com/link/\\d+", "http://(www\\.)?shorlink\\.net/s/[A-Za-z0-9]+",
        "https?://chilp\\.it/\\??\\w{6}", "http://(www\\.)?dai\\.ly/[A-Za-z0-9]+", "http://(www\\.)?db\\.tt/[A-Za-z0-9]+", "http://(www\\.)?artofx\\.org/[a-z0-9]+", "http://(www\\.)?ewe\\.h1\\.ru/[A-Za-z0-9\\-_]+/[^<>\"/]+" }, flags = { 0 })
public class Rdrctr extends antiDDoSForDecrypt {

    public Rdrctr(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        // Workaround for preview.tinyurl.com Links
        parameter = parameter.replaceFirst("preview\\.tinyurl\\.com", "tinyurl\\.com");
        // Workaround for 0856.com links
        if (parameter.contains("0845.com")) {
            parameter = parameter + "/";
        }
        // Workaround for ponyurl.com Links
        parameter = parameter.replace("ponyurl.com/", "ponyurl.com/forward.php?");
        try {
            getPage(parameter);
        } catch (final SocketTimeoutException e) {
            logger.info("Link offline (server offline?): " + parameter);
            return decryptedLinks;
        }
        String declink = br.getRedirectLocation();
        if (declink == null) {
            declink = br.getRegex("<iframe frameborder=\"0\"\\s*src=\"(.*?)\"").getMatch(0);
            if (declink == null) {
                declink = br.getRegex("<meta http-equiv=(\"|')refresh\\1\\s*content=(\"|')\\d+;\\s*url=(https?://[^<>\"]+)\\2[^>]*").getMatch(2);
                if (declink == null) {
                    declink = br.getRegex("<script type=(\"|')text/javascript\\1>\\s*window\\.location\\.href=(\"|')(https?.*?)\\2;</script>").getMatch(2);
                    if (declink == null) {
                        boolean offline = false;
                        if (br.getRequest().getHttpConnection().getResponseCode() == 403 || br.getRequest().getHttpConnection().getResponseCode() == 404) {
                            offline = true;
                        } else if (parameter.contains("goo.gl/") && br.containsHTML(">404: Page not found|this URL has been disabled")) {
                            offline = true;
                        } else if (parameter.contains("tinyurl.com/") & (br.containsHTML("tinyurl.com/errorb\\.php\\?") || br.containsHTML(">Error: TinyURL redirects to a TinyURL|>The URL you followed redirects back to a TinyURL") || br.containsHTML(">Error: Unable to find site\\'s URL to redirect to|>Please check that the URL entered is correct"))) {
                            offline = true;
                        } else if (parameter.contains("bit.ly/") && br.containsHTML(">Something's wrong here|>Uh oh, bitly couldn't find a link for the|Page Not Found")) {
                            offline = true;
                        } else if (parameter.toLowerCase().contains("hex.io/") && declink == null) {
                            declink = br.getRegex("<div class=\"third grid\">[\t\n\r ]+<a href=\"(http[^<>\"]+)\"").getMatch(0);
                            if (declink != null) {
                                decryptedLinks.add(createDownloadlink(declink));
                                return decryptedLinks;
                            }
                            if (br.containsHTML(">404 Page Not Found|bannedsextapes\\.com/|\"error\":\"Please enter a valid URL") || br.getRequest().getHttpConnection().getResponseCode() == 404) {
                                offline = true;
                            }
                        } else if (parameter.matches("https?://t\\.co/.*") && br.containsHTML("<h1>Sorry, that page doesn’t exist|<p>Thanks for noticing\\&mdash;we\\'re going")) {
                            offline = true;
                        }
                        if (offline) {
                            logger.info("Link offline or invalid: " + parameter);
                            return decryptedLinks;
                        }
                    }
                }
            }
        }
        if ("sht.io".equals(this.getHost()) && !inValidate(declink)) {
            // link inside a link
            declink = new Regex(declink, ".+((?:ftp|https?)://.+)$").getMatch(0);
        }
        // bit.ly (hosts many short link domains via dns alias), they have protection/warnings
        if (StringUtils.isNotEmpty(declink) && new Regex(declink, "https?://bitly\\.com/a/warning\\?hash=[a-z0-9]+.+").matches()) {
            final String newlink = new Regex(declink, "&url=(.+)(?!&)?").getMatch(0);
            if (StringUtils.isNotEmpty(newlink)) {
                declink = Encoding.urlDecode(newlink, false);
            }
        }
        // when empty it's not always an error, specially when we use lazy regex!
        if (declink != null) {
            decryptedLinks.add(createDownloadlink(declink));
        }
        return decryptedLinks;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.GeneralRedirectorDecrypter;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}