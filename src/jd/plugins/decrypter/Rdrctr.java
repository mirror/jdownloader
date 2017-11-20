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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.Base64;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "8trx.com", "eepurl.com", "4shared.com", "youtubz.link", "securely.link", "gfxxtra.com", "upx.nz", "engt.co", "hungryleech.com", "soo.gd", "afl.to", "noref.co", "vk.cc", "somosmovies.com", "smv.tv", "ligman.me", "feedproxy.google.com", "cur.bz", "pip.bz", "ovh.to", "po.st", "share-films.net", "fburls.com", "go.madmimi.com", "alturl.com", "rapidsky.net", "yhoo.it", "smarturl.it", "sharingdb.com", "ift.tt", "urlm.in", "cutmy.name", "tinyw.in", "baidu.com", "clck.ru", "youtu.be", "redirects.ca", "clickthru.ca", "atu.ca", "easyurl.net", "smarturl.eu", "ow.ly", "maticulo.us", "trib.al", "ab.co", "budurl.com", "tiny.pl", "linkth.at", "moourl.com", "ri.ms", "gelen.org", "pettyurl.com", "decenturl.com", "f5y.info", "starturl.com", "wapurl.co.uk", "short.redirect.am", "2li.ru", "goo.gl", "u.to", "up.ht",
        "ymlp.com", "lowlifeinc.us2.list-manage.com", "chilp.it", "dai.ly", "db.tt", "artofx.org", "bit.ly" }, urls = { "https?://(www\\.)?8trx\\.com/[A-Za-z0-9]+", "https?://(?:www\\.)?eepurl\\.com/[A-Za-z0-9]+", "https://(?:www\\.)?4shared.com/s/f[a-zA-Z0-9]+", "https?://(?:www\\.)?youtubz\\.link/[A-Za-z0-9]+", "https?://(?:www\\.)?securely\\.link/[A-Za-z0-9]+", "http?://(?:www\\.)?(?:gfxxtra\\.com|gftxra\\.net)/[a-f0-9]{32}/[^\\s]+\\.html", "https?://(?:www\\.)?upx\\.nz/[a-zA-Z0-9]+", "https?://(?:www\\.)?engt\\.co/[a-zA-Z0-9]+", "https?://(?:www\\.)?hungryleech\\.com/redir/[a-zA-Z0-9]+(?:/[\\w\\-]+)?", "https?://soo\\.gd/.+", "https?://(?:\\w+\\.)?afl\\.to/[a-z0-9A-Z]{7}", "https?://noref\\.co/\\?id=\\d+", "https?://(?:www\\.)?vk\\.cc/[A-Za-z0-9]+", "https?://(?:www\\.)?somosmovies\\.com/link/[A-Za-z0-9=]+/?", "https?://(?:www\\.)?smv\\.tv/link/[A-Za-z0-9=]+/?",
        "http://(?:www\\.)?ligman\\.me/[A-Za-z0-9]+", "https?://feedproxy\\.google\\.com/~r/.+", "http://cur\\.bz/[A-Za-z0-9]+", "https?://(www\\.)?pip\\.bz/[A-Za-z0-9\\-]+", "http://(www\\.)?ovh\\.to/[A-Za-z0-9]+", "http://(www\\.)?po\\.st/[A-Za-z0-9]+", "http://(www\\.)?share-films\\.net/redirect\\.php\\?url=[a-z0-9]+", "http://(www\\.)?fburls\\.com/\\d+\\-[A-Za-z0-9]+", "https?://go\\.madmimi\\.com/redirects/[a-zA-Z0-9]+\\?pa=\\d+", "http://[\\w\\.]*?alturl\\.com/[a-z0-9]+", "http://(www\\.)?rapidsky\\.net/[A-Za-z0-9]+", "https?://(www\\.)?yhoo\\.it/[\\w]+", "http://[\\w\\.]*smarturl\\.it/[A-Za-z0-9]+", "http://[\\w\\.]*sharingdb\\.com/[a-z0-9]+", "http://(www\\.)?ift\\.tt/[a-zA-Z0-9]+", "https?://(www\\.)?urlm\\.in/[a-zA-Z0-9]+", "http://(www\\.)?cutmy\\.name/(?!#|.+\\.php)(u/[a-f0-9]{6,8}|[a-z0-9]+)", "https?://(www\\.)?tinyw\\.in/[a-zA-Z0-9]+",
        "http://(www\\.)?baidu\\.com/link\\?url=[A-Za-z0-9\\-_]+", "http://(www\\.)?clck\\.ru/(d/[A-Za-z0-9\\-_]+|[A-Za-z0-9\\-_]+)", "https?://[\\w\\.]*youtu\\.be/[a-z_A-Z0-9\\-]+(?:\\?\\S+)?", "http://[\\w\\.]*redirects\\.ca(/[a-zA-Z0-9]+)?", "http://[\\w\\.]*clickthru\\.ca(/[a-zA-Z0-9]+)?", "http://[\\w\\.]*atu\\.ca(/[a-zA-Z0-9]+)?", "http://[\\w\\.]*easyurl\\.net(/[a-zA-Z0-9]+)?", "http://[\\w\\.]*smarturl\\.eu/\\?[a-zA-Z0-9]+", "http://[\\w\\.]*ow\\.ly/[\\w]+", "http://(www\\.)?maticulo\\.us/[\\w]+", "http://(www\\.)?trib\\.al/[\\w]+", "http://(www\\.)?ab\\.co/[\\w]+", "http://[\\w\\.]*budurl\\.com/[a-zA-Z0-9]+", "http://[\\w\\.]*tiny\\.pl/[\\w]+", "http://[\\w\\.]*linkth\\.at/[a-z]+", "http://[\\w\\.]*moourl\\.com/[a-zA-Z0-9]+", "http://[\\w\\.]*ri\\.ms/[a-z0-9]+", "http://[\\w\\.]*gelen\\.org/[0-9]+", "http://[\\w\\.]*pettyurl\\.com/[0-9a-z]+",
        "http://[\\w\\.]*\\.decenturl\\.com/[a-z0-9.-]+", "http://[\\w\\.]*f5y\\.info/[a-zA-Z-]+", "http://[\\w\\.]*starturl\\.com/[a-z_]+", "http://[\\w\\.]*wapurl\\.co\\.uk/\\?[0-9A-Z]+", "http://short\\.redirect\\.am/\\?[\\w]+", "http://[\\w\\.]*2li\\.ru//[a-z0-9]+", "https?://[\\w\\.]*goo\\.gl/(?:photos/)?[A-Za-z0-9]+", "http://(www\\.)?u\\.to/[A-Za-z0-9\\-]+", "http://(www\\.)?up\\.ht/[A-Za-z0-9]+", "http://(www\\.)?t\\.ymlp\\d+\\.com/[a-z0-9]+/click\\.php", "http://(www\\.)?lowlifeinc\\.us2\\.list-manage\\.com/track/click\\?u=[a-z0-9]+\\&id=[a-z0-9]+\\&e=[a-z0-9]+", "https?://chilp\\.it/\\??\\w{6}", "http://(www\\.)?dai\\.ly/[A-Za-z0-9]+", "http://(www\\.)?db\\.tt/[A-Za-z0-9]+", "http://(www\\.)?artofx\\.org/(links/)?[a-z0-9\\=/+]+", "https?://(?:www\\.)?bit\\.ly/[\\w]+" })
public class Rdrctr extends antiDDoSForDecrypt {
    public Rdrctr(PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * 2017-08-02: Do NOT add any workarounds to this code. This is only for websites which redirect to other websites via simple redirect,
     * NOTHING ELSE!!
     */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        try {
            if ("artofx.org".equals(getHost())) {
                final String base64 = new Regex(parameter, "(aHR0c.+)").getMatch(0);
                if (base64 != null) {
                    final String url = Base64.decodeToString(base64);
                    if (url != null) {
                        decryptedLinks.add(createDownloadlink(url));
                        return decryptedLinks;
                    }
                }
                br.setCurrentURL("http://artofx.org");
            }
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
                }
            }
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

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}