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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3,

names = { "feedproxy.google.com", "cur.bz", "awe.sm", "pip.bz", "ovh.to", "po.st", "share-films.net", "fburls.com", "umquetenha.org", "mblink.info", "t.co", "shrk.biz", "go.madmimi.com", "alturl.com", "click.tf", "ssh.yt", "rapidsky.net", "yhoo.it", "proton.biz", "otrkeyfinder.com", "smarturl.it", "westernlink.co", "sharingdb.com", "ift.tt", "doujin.us", "share.wf", "urlm.in", "cutmy.name", "tinyw.in", "baidu.com", "sharedpartners.com", "relink.ws", "clck.ru", "youtu.be", "readthis.ca", "redirects.ca", "goshrink.com", "clickthru.ca", "atu.ca", "easyurl.net", "redirect.wayaround.org", "rurl.org", "tinyurl.com", "smarturl.eu", "rln.me", "sp2.ro", "ow.ly", "bit.ly", "maticulo.us", "trib.al", "ab.co", "vb.ly", "ponyurl.com", "budurl.com", "yep.it", "urlite.com", "urlcini.com", "tra.kz", "tiny.pl", "linkth.at", "urlpass.com", "moourl.com", "thnlnk.com", "ri.ms", "gelen.org", "pettyurl.com",
        "9mmo.com", "bmi-group.99k.org", "decenturl.com", "shor7.com", "f5y.info", "starturl.com", "2s8.org", "kno.li", "elurl.com", "wapurl.co.uk", "micurl.com", "urlcantik.com", "0845.com", "piurl.com", "tren.tk", "twurl.nl", "shortlinks.co.uk", "rubyurl.com", "u.nu", "hex.io", "short.redirect.am", "2li.ru", "goo.gl", "u.to", "up.ht", "romsite.net", "ymlp.com", "lowlifeinc.us2.list-manage.com", "metalwarez.com", "shorlink.net", "likejav.com", "chilp.it", "sflk.in", "dai.ly", "db.tt", "0url.in", "artofx.org", "ewe.h1.ru" },

        urls = { "https?://feedproxy\\.google\\.com/~r/.+", "http://cur\\.bz/[A-Za-z0-9]+", "http://(www\\.)?awe\\.sm/[A-Za-z0-9]+", "http://(www\\.)?pip\\.bz/[A-Za-z0-9\\-]+", "http://(www\\.)?ovh\\.to/[A-Za-z0-9]+", "http://(www\\.)?po\\.st/[A-Za-z0-9]+", "http://(www\\.)?share-films\\.net/redirect\\.php\\?url=[a-z0-9]+", "http://(www\\.)?fburls\\.com/\\d+\\-[A-Za-z0-9]+", "http://(www\\.)?umquetenha\\.org/protecao/resolve\\.php\\?link=.{2,}", "http://(www\\.)?mblink\\.info/\\?id=\\d+", "https?://t\\.co/[a-zA-Z0-9]+", "http://(de\\.)?shrk\\.biz/\\w+", "https?://go\\.madmimi\\.com/redirects/[a-zA-Z0-9]+\\?pa=\\d+", "http://[\\w\\.]*?alturl\\.com/[a-z0-9]+", "http://click\\.tf/[a-zA-Z0-9]{8}", "http://(?:www\\.)?ssh\\.yt/[a-zA-Z0-9]+", "http://(www\\.)?rapidsky\\.net/[A-Za-z0-9]+", "https?://(www\\.)?yhoo\\.it/[\\w]+", "https?://(\\w\\.)?proton\\.biz/aralara\\.php\\?link=[a-z0-9]+",
        "http://(www\\.)?otrkeyfinder\\.com/index\\.php\\?page=goto_mirror\\&otrkey=[^<>\"/]+\\.otrkey\\&search=[a-f0-9]+\\&mirror=\\d+", "http://[\\w\\.]*smarturl\\.it/[A-Za-z0-9]+", "http://[\\w\\.]*westernlink\\.co/l/[A-Za-z0-9]+", "http://[\\w\\.]*sharingdb\\.com/[a-z0-9]+", "http://(www\\.)?ift\\.tt/[a-zA-Z0-9]+", "https?://(www\\.)?doujin\\.us/link/\\d+/\\d+", "http://(www\\.)?share\\.wf/[A-Za-z0-9]+", "https?://(www\\.)?urlm\\.in/[a-zA-Z0-9]+", "http://(www\\.)?cutmy\\.name/(?!#|.+\\.php)(u/[a-f0-9]{6,8}|[a-z0-9]+)", "https?://(www\\.)?tinyw\\.in/[a-zA-Z0-9]+", "http://(www\\.)?baidu\\.com/link\\?url=[A-Za-z0-9\\-_]+", "http://(www\\.)?sharedpartners\\.com/in/\\d+/\\d+", "http://(www\\.)?relink\\.ws/[A-Za-z0-9]+", "http://(www\\.)?clck\\.ru/(d/[A-Za-z0-9\\-_]+|[A-Za-z0-9\\-_]+)", "https?://[\\w\\.]*youtu\\.be/[a-z_A-Z0-9\\-]+", "http://[\\w\\.]*readthis\\.ca(/[a-zA-Z0-9]+)?",
        "http://[\\w\\.]*redirects\\.ca(/[a-zA-Z0-9]+)?", "http://[\\w\\.]*goshrink\\.com(/[a-zA-Z0-9]+)?", "http://[\\w\\.]*clickthru\\.ca(/[a-zA-Z0-9]+)?", "http://[\\w\\.]*atu\\.ca(/[a-zA-Z0-9]+)?", "http://[\\w\\.]*easyurl\\.net(/[a-zA-Z0-9]+)?", "http://[\\w\\.]*redirect\\.wayaround\\.org/[a-zA-Z0-9]+/(.*)", "http://[\\w\\.]*rurl\\.org(/[a-zA-Z0-9]+)?", "http://(www\\.)?tinyurl\\.com/(?!favicon|preview)[a-z0-9]+(/[a-z0-9]+)?", "http://[\\w\\.]*smarturl\\.eu/\\?[a-zA-Z0-9]+", "http://[\\w\\.]*rln\\.me/[0-9a-zA-Z]+", "http://[\\w\\.]*sp2\\.ro/[0-9a-zA-Z]+", "http://[\\w\\.]*ow\\.ly/[\\w]+", "http://(www\\.)?bit\\.ly/[\\w]+", "http://(www\\.)?maticulo\\.us/[\\w]+", "http://(www\\.)?trib\\.al/[\\w]+", "http://(www\\.)?ab\\.co/[\\w]+", "http://[\\w\\.]*vb\\.ly/[\\w]+", "http://[\\w\\.]*ponyurl\\.com/[\\w]+", "http://[\\w\\.]*budurl\\.com/[a-zA-Z0-9]+",
        "http://(www\\.)?yep\\.it/(?!preview|stat|favicon|css|js|apipage|go|yepsrc|contacts|about|cloud|icon|rssfeed)[A-Za-z0-9]+", "http://[\\w\\.]*urlite\\.com/[\\d]+", "http://[\\w\\.]*urlcini\\.com/[\\w]+", "http://[\\w\\.]*tra\\.kz/[\\w_\\-,()*:]+", "http://[\\w\\.]*tiny\\.pl/[\\w]+", "http://[\\w\\.]*linkth\\.at/[a-z]+", "http://[\\w\\.]*urlpass\\.com/[0-9a-z]+", "http://[\\w\\.]*moourl\\.com/[a-zA-Z0-9]+", "http://[\\w\\.]*thnlnk\\.com/[a-zA-Z0-9]+/[a-zA-Z0-9]+/[0-9]+", "http://[\\w\\.]*ri\\.ms/[a-z0-9]+", "http://[\\w\\.]*gelen\\.org/[0-9]+", "http://[\\w\\.]*pettyurl\\.com/[0-9a-z]+", "http://[\\w\\.]*9mmo\\.com/[a-z0-9]+", "http://[\\w\\.]*bmi-group\\.99k\\.org/go\\.php\\?id=[0-9]+", "http://[\\w\\.]*\\.decenturl\\.com/[a-z0-9.-]+", "http://[\\w\\.]*shor7\\.com/\\?[A-Z]+", "http://[\\w\\.]*f5y\\.info/[a-zA-Z-]+", "http://[\\w\\.]*starturl\\.com/[a-z_]+",
        "http://[\\w\\.]*2s8\\.org/[0-9a-z]+", "http://[\\w\\.]*kno\\.li/[0-9a-z]+", "http://[\\w\\.]*elurl\\.com/[0-9a-z]+", "http://[\\w\\.]*wapurl\\.co\\.uk/\\?[0-9A-Z]+", "http://[\\w\\.]*micurl\\.com/[a-zA-Z]+", "http://[\\w\\.]*urlcantik\\.com/[a-z0-9]+", "http://[\\w\\.]*0845\\.com/[a-zA-Z0-9]+", "http://[\\w\\.]*piurl\\.com/[a-z0-9]+", "http://[\\w\\.]*tren\\.tk/[a-z0-9]+", "http://[\\w\\.]*twurl\\.nl/[a-z0-9]+", "http://[\\w\\.]*shortlinks\\.co\\.uk/[0-9a-z]+", "http://[\\w\\.]*rubyurl\\.com/[0-9a-zA-Z]+", "http://[\\w\\.]*u\\.nu/[0-9a-z]+", "http://(www\\.)?hex\\.io/(?!contact|user|javascript|api)[\\w]+", "http://short\\.redirect\\.am/\\?[\\w]+", "http://[\\w\\.]*2li\\.ru//[a-z0-9]+", "https?://[\\w\\.]*goo\\.gl/[A-Za-z0-9]+", "http://(www\\.)?u\\.to/[A-Za-z0-9\\-]+", "http://(www\\.)?up\\.ht/[A-Za-z0-9]+",
        "http://(www\\.)?romsite\\.net/download\\.php\\?id=\\d+\\&s=[a-z0-9]{1,4}\\&f=[a-z0-9]{1,5}", "http://(www\\.)?t\\.ymlp\\d+\\.com/[a-z0-9]+/click\\.php", "http://(www\\.)?lowlifeinc\\.us2\\.list-manage\\.com/track/click\\?u=[a-z0-9]+\\&id=[a-z0-9]+\\&e=[a-z0-9]+", "http://(www\\.)?metalwarez\\.com/link/\\d+", "http://(www\\.)?shorlink\\.net/s/[A-Za-z0-9]+", "http://(www\\.)?likejav\\.com/file/[a-z0-9]+", "https?://chilp\\.it/\\??\\w{6}", "http://(www\\.)?sflk\\.in/\\w{11}", "http://(www\\.)?dai\\.ly/[A-Za-z0-9]+", "http://(www\\.)?db\\.tt/[A-Za-z0-9]+", "http://(www\\.)?0url\\.in/[A-Za-z0-9]+/", "http://(www\\.)?artofx\\.org/[a-z0-9]+", "http://(www\\.)?ewe\\.h1\\.ru/[A-Za-z0-9\\-_]+/[^<>\"/]+" }, flags = { 0 })
public class Rdrctr extends antiDDoSForDecrypt {

    public Rdrctr(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
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
                        } else if (parameter.contains("yep.it") && br.containsHTML(">Put your URL here<")) {
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
                        } else if (parameter.contains("umquetenha.org/protecao/resolve.php?link=") && br.containsHTML("src=\"protecao\\.jpg\"")) {
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
        // when empty it's not always an error, specially when we use lazy regex!
        if (declink != null) {
            decryptedLinks.add(createDownloadlink(declink));
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}