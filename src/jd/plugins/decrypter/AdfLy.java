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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.plugins.components.UserAgents.BrowserName;
import jd.utils.RazStringBuilder;

import org.appwork.utils.net.httpconnection.HTTPConnectionUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class AdfLy extends antiDDoSForDecrypt {
    private static final String[] domains = { "adf.ly", "yobuilder.com", "quamiller.com", "j.gs", "q.gs", "ay.gy", "zo.ee", "babblecase.com", "riffhold.com", "microify.com", "pintient.com", "tinyium.com", "atominik.com", "bluenik.com", "bitigee.com", "atomcurve.com", "picocurl.com", "tinyical.com", "casualient.com", "battleate.com", "mmoity.com", "simizer.com", "dataurbia.com", "viahold.com", "coginator.com", "cogismith.com", "kaitect.com", "yoalizer.com", "kibuilder.com", "kimechanic.com", "quainator.com", "tinyium.com", "pintient.com", "quamiller.com",
        /** <-- full domains & subdomains --> */
        "chathu.apkmania.co", "alien.apkmania.co", "adf.acb.im", "packs.redmusic.pl", "packs2.redmusic.pl", "dl.android-zone.org", "out.unionfansub.com", "sostieni.ilwebmaster21.com", "fuyukai-desu.garuda-raws.net" };

    @Override
    public String[] siteSupportedNames() {
        return domains;
    }

    // DEV NOTES:
    // uids are not transferable between domains.
    // alternative domains do not support https
    public AdfLy(PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * Returns the annotations names array
     *
     * @return
     */
    public static String[] getAnnotationNames() {
        return new String[] { "adf.ly" };
    }

    /**
     * returns the annotation pattern array
     *
     */
    public static String[] getAnnotationUrls() {
        // construct pattern
        final String host = getHostsPattern();
        return new String[] { host + "/[^<>\r\n\t]+" };
    }

    private static String getHostsPattern() {
        final StringBuilder pattern = new StringBuilder();
        for (final String name : domains) {
            pattern.append((pattern.length() > 0 ? "|" : "") + Pattern.quote(name));
        }
        final String hosts = adfPre + "(?:" + pattern.toString() + ")";
        return hosts;
    }

    @Override
    public void init() {
        // first run -1 && revision change == sync.
        if (this.getVersion() > HOSTS_REFERENCE.get()) {
            HOSTS.set(getHostsPattern());
            HOSTS_REFERENCE.set(this.getVersion());
        }
    }

    private static final String            adfPre          = "https?://(?:www\\.)?";
    // builds final String for method calling (no need to edit).
    private static AtomicReference<String> HOSTS           = new AtomicReference<String>(null);
    private static AtomicLong              HOSTS_REFERENCE = new AtomicLong(-1);
    private static final String            INVALIDLINKS    = "/(link-deleted\\.php|index|login|static).+";
    private static Object                  LOCK            = new Object();
    private String                         protocol        = null;

    @Override
    protected boolean useRUA() {
        return true;
    }

    @Override
    protected BrowserName setBrowserName() {
        return BrowserName.Chrome;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("www.", "");
        final String hosts = HOSTS.get();
        if (parameter.matches(hosts + INVALIDLINKS)) {
            logger.info("Invalid: " + parameter);
            return decryptedLinks;
        }
        // poll plugin setting for default protocol
        protocol = getDefaultProtocol();
        if (!parameter.contains("adf.ly/") && protocol.equalsIgnoreCase("https://")) {
            logger.info("sorry, HTTPS option is not aviable for this host '" + Browser.getHost(parameter) + "'");
            protocol = "http://";
        }
        br.setFollowRedirects(false);
        if (parameter.matches(hosts + "/\\d+/.+")) {
            String linkInsideLink = new Regex(parameter, hosts + "/\\d+/(.+)").getMatch(0);
            if (!linkInsideLink.matches("^(?-i)(?:https?|ftp)://.+$")) {
                // we assume they are http
                linkInsideLink = "http://" + linkInsideLink;
            }
            if (!linkInsideLink.matches(hosts + "/.+")) {
                // lets try dns method to verify if the link is actually valid
                // re: https://board.jdownloader.org/showthread.php?p=363462#post363462
                InetAddress[] inetAddress = null;
                try {
                    inetAddress = HTTPConnectionUtils.resolvHostIP(Browser.getHost(linkInsideLink, true), br.getIPVersion());
                    // TODO: this will fail in full proxied environment
                } catch (final UnknownHostException u) {
                }
                if (inetAddress != null && inetAddress.length > 0) {
                    decryptedLinks.add(createDownloadlink(linkInsideLink));
                    return decryptedLinks;
                }
            }
        }
        {
            final String redirect = new Regex(parameter, "/redirecting/(.+)").getMatch(0);
            if (redirect != null) {
                final HashSet<String> results = GenericBase64Decrypter.handleBase64Decode(redirect);
                for (final String result : results) {
                    decryptedLinks.add(createDownloadlink(result));
                }
                if (!decryptedLinks.isEmpty()) {
                    return decryptedLinks;
                }
            }
        }
        boolean skipWait = true;
        String finallink = null;
        for (int i = 0; i <= 2; i++) {
            synchronized (LOCK) {
                getPage(parameter.replaceFirst("https?://", protocol));
            }
            if (true) {
                String redir = br.getRedirectLocation();
                if (redir != null && redir.contains("http://") && isProtocolHTTPS()) {
                    // redirects can happen here to alternative domains, lets return back into the decrypter.
                    decryptedLinks.add(createDownloadlink(redir));
                    return decryptedLinks;
                } else if (redir != null && redir.matches("https?://adf\\.ly/?")) {
                    logger.info("Link offline: " + parameter);
                    return decryptedLinks;
                } else if (br.containsHTML("(<b style=\"font-size: 20px;\">Sorry the page you are looking for does not exist|>404 Not Found<|>Not Found<|>Sorry, but the requested page was not found)") || (br.getRedirectLocation() != null && (br.getRedirectLocation().contains("ink-deleted.php") || br.getRedirectLocation().contains("/suspended")))) {
                    logger.info("Link ink offline: " + parameter);
                    return decryptedLinks;
                } else if (br.containsHTML("Sorry, there has been a problem\\.")) {
                    logger.info("Link ink offline: " + parameter);
                    return decryptedLinks;
                }
            }
            /* javascript vars 20130328 */
            String countdown = getWaittime();
            // they also have secondary zzz variable within 'function adf_counter()', but it's the same.
            String zzz = br.getRegex("var zzz\\s?+=\\s?+'?([\\d,\\.]+|http[^'\"]+)'?;").getMatch(0);
            if (zzz != null && zzz.matches("(https?|ftp)://.+")) {
                finallink = zzz;
            }
            String easyUrl = br.getRegex("var easyUrl\\s?+=\\s?+'?(true|false)'?;").getMatch(0);
            String url = br.getRegex("var url\\s?+=\\s?+'?([^\';]+)'?;").getMatch(0);
            // 201307xx
            String ysmm = br.getRegex("var ysmm = '([^\';]+)'").getMatch(0);
            if (ysmm == null) {
                // 20170419
                ysmm = br.getRegex("ysmm=\"(.*?)\"").getMatch(0);
            }
            if (ysmm != null) {
                String C = "";
                String h = "";
                for (int s = 0; s < ysmm.length(); s++) {
                    if (s % 2 == 0) {
                        C += ysmm.charAt(s);
                    } else {
                        h = ysmm.charAt(s) + h;
                    }
                }
                // new 20170914
                final String[] a = (C + h).split("");
                for (int b = 0; b < a.length; b++) {
                    if (a[b].matches("\\d")) {
                        for (int c = b + 1; c < a.length; c++) {
                            if (a[c].matches("\\d")) {
                                final int d = Integer.parseInt(a[b]) ^ Integer.parseInt(a[c]);
                                if (d < 10) {
                                    a[b] = d + "";
                                }
                                b = c;
                                c = a.length;
                            }
                        }
                    }
                }
                String sec = Encoding.Base64Decode(RazStringBuilder.buildString(a, ""));
                // remove padding, I went with auto padding correction.
                int pcount = sec.indexOf("http");
                if (pcount == -1) {
                    pcount = sec.indexOf("ftp");
                }
                if (pcount > -1) {
                    // this works on the assumption that it's the same offset
                    finallink = sec.substring(pcount, sec.length() - pcount);
                } else {
                    // At this time its 16 chars, prefix and postfix.
                    finallink = sec.substring(16, sec.length() - 16);
                }
            }
            if (finallink == null) {
                finallink = br.getRedirectLocation();
            }
            if (finallink == null) {
                finallink = br.getRegex("\\.attr\\((\"|')href(\"|'), '(.*?)'\\)").getMatch(2);
            }
            if (finallink == null) {
                finallink = br.getRegex("window\\.location = ('|\")(.*?)('|\");").getMatch(1);
                if (finallink != null && finallink.contains("/noscript.php")) {
                    finallink = null;
                }
            }
            if (finallink == null) {
                finallink = br.getRegex("close_bar.*?self\\.location = '(.*?)';").getMatch(0);
            }
            /* 20130130 */
            if (finallink == null && (countdown != null && zzz != null && easyUrl != null)) {
                if (countdown.equalsIgnoreCase("7") && easyUrl.equalsIgnoreCase("false")) {
                    postPage("/shortener/go", "zzz=" + Encoding.urlEncode(zzz));
                    finallink = br.getRegex("(http[^\"']+)").getMatch(0);
                }
            }
            /* old stuff still exists! tested and working as of 20130328 */
            if (finallink != null && (!finallink.startsWith("/") && finallink.matches(hosts + ".+"))) {
                String extendedProtectionPage = br.getRegex("(?:" + hosts + ")?(/go(/|\\.php\\?)[^<>\"']+)").getMatch(0);
                if (extendedProtectionPage == null) {
                    // 201307xx
                    extendedProtectionPage = new Regex(finallink, "(?:" + hosts + ")?(/go(/|\\.php\\?)[^<>\"']+)").getMatch(0);
                    if (extendedProtectionPage != null) {
                        extendedProtectionPage = protocol + "adf.ly" + extendedProtectionPage;
                    } else {
                        break;
                    }
                }
                int wait = 7;
                String waittime = getWaittime();
                // because of possible page action via ajax request, we use old wait time.
                if (waittime == null) {
                    waittime = countdown;
                }
                if (waittime != null && Integer.parseInt(waittime) <= 20) {
                    wait = Integer.parseInt(waittime);
                }
                if (skipWait) {
                    skipWait();
                    // Wait a seconds. Not waiting can cause the skipWait feature to fail
                    sleep(1 * 10001l, param);
                } else {
                    sleep(wait * 1000l, param);
                }
                getPage(extendedProtectionPage);
                String tempLink = br.getRedirectLocation();
                if (tempLink != null) {
                    tempLink = tempLink.replace("www.", "");
                    // Redirected to the same link or blocked...try again
                    if (tempLink.replaceAll("https?://", "").equals(parameter.replaceAll("https?://", "")) || tempLink.contains("adf.ly/locked/") || tempLink.contains("adf.ly/blocked")) {
                        logger.info("Blocked, re-trying with waittime...");
                        skipWait = false;
                        try {
                            br.clearCookies(br.getURL());
                        } catch (final Exception e) {
                        }
                        continue;
                    } else {
                        // We found a link to continue with
                        finallink = tempLink;
                        break;
                    }
                } else {
                    // Everything should have worked correctly, try to get final link
                    finallink = br.getRegex("<META HTTP-EQUIV=\"Refresh\" CONTENT=\"\\d+; URL=(https?://[^<>\"']+)\"").getMatch(0);
                    break;
                }
            } else if (finallink != null && finallink.matches("(https?|ftp)://.+")) {
                break;
            }
        }
        if (finallink != null && finallink.contains("/link-deleted.php")) {
            logger.info(parameter + " has been removed from adf.ly service provider.");
        } else if (finallink != null) {
            decryptedLinks.add(createDownloadlink(finallink.replace("\\", "")));
        } else {
            logger.warning("single regex broken for link: " + parameter);
            logger.info("Adding all available links on page");
            // Use this because they often change the page
            final String[] lol = HTMLParser.getHttpLinks(br.toString(), "");
            for (final String aLink : lol) {
                if (!new Regex(aLink, hosts + "/.+").matches() && !aLink.contains("/javascript/")) {
                    decryptedLinks.add(createDownloadlink(aLink));
                }
            }
        }
        return decryptedLinks;
    }

    private String getWaittime() {
        return br.getRegex("var countdown\\s?+=\\s?+'?(\\d+)'?;").getMatch(0);
    }

    private void skipWait() {
        final Browser brAds = br.cloneBrowser();
        brAds.setConnectTimeout(5 * 1000);
        brAds.setReadTimeout(5 * 1000);
        final String[] skpWaitLinks = { "cdn.adf.ly/css/adfly_1.css", "cdn.adf.ly/js/adfly.js", "cdn.adf.ly/images/logo_fb.png", "cdn.adf.ly/images/skip_ad/en.png", "adf.ly/favicon.ico", "cdn.adf.ly/images/ad_top_bg.png", "adf.ly/omnigy7425325410.swf", "adf.ly/holder.php" };
        for (final String skWaitLink : skpWaitLinks) {
            try {
                openAntiDDoSRequestConnection(brAds, brAds.createGetRequest(protocol + skWaitLink));
            } catch (final Exception e) {
            }
        }
    }

    /**
     * Issue the user with a dialog prompt, ask them to select a default request protocol.<br/>
     * Save users preference in memory for given session. Each Start = new session<br/>
     * <br/>
     * Decrypter Template: Default Request Protocol.
     *
     * @return default request protocol
     * @author raztoki
     */
    private String getDefaultProtocol() {
        return "https://";
    }

    /**
     * returns true when current this.protocol value is https
     *
     */
    private boolean isProtocolHTTPS() {
        if (protocol != null && "https://".equalsIgnoreCase(this.protocol)) {
            return true;
        } else {
            return false;
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.AdfLy_AdfLy;
    }
}