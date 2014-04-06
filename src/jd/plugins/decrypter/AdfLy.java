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
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

import org.jdownloader.logging.LogController;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "adf.ly" }, urls = { "https?://(www\\.)?(adf\\.ly|j\\.gs|q\\.gs|adclicks\\.pw|ay\\.gy|(dl|david)\\.nhachot\\.info|chathu\\.apkmania\\.co|alien\\.apkmania\\.co|n\\.shareme\\.in)/[^<>\r\n\t]+" }, flags = { 0 })
@SuppressWarnings("deprecation")
public class AdfLy extends PluginForDecrypt {

    // DEV NOTES:
    // uids are not transferable between domains.
    // alternative domains do not support https

    public AdfLy(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String HOSTS        = "https?://(www\\.)?(adf\\.ly|j\\.gs|q\\.gs|adclicks\\.pw|ay\\.gy|dl\\.nhachot\\.info|chathu\\.apkmania\\.co|alien\\.apkmania\\.co|n\\.shareme\\.in)";
    private static final String INVALIDLINKS = "/(link-deleted\\.php|index|login|static).+";
    private static Object       LOCK         = new Object();

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("www.", "");
        if (parameter.matches(HOSTS + INVALIDLINKS)) {
            logger.info("adf.ly link invalid: " + parameter);
            return decryptedLinks;
        }

        for (final String link : Adfly().decryptThis(parameter)) {
            decryptedLinks.add(createDownloadlink(link));
        }

        return decryptedLinks;
    }

    public static class adfly {
        private final Browser br            = new Browser();
        private String        protocol      = null;
        private final boolean supportsHTTPS = true;
        private Logger        logger        = LogController.getInstance().getLogger(this.getClass().getName());
        private String        hosts         = HOSTS;

        /**
         * Allows decrypter or hoster plugins to call this method
         **/
        public ArrayList<String> decryptThis(String parameter) throws Exception {
            ArrayList<String> decryptedLinks = new ArrayList<String>();
            // imported protocol choice
            protocol = new Regex(parameter, "(https?://)").getMatch(0);
            // poll plugin setting for default protocol, if not set ask the user.
            protocol = getDefaultProtocol();
            if (!parameter.contains("adf.ly/") && protocol.equalsIgnoreCase("https://")) {
                logger.info("sorry, HTTPS option is not aviable for this host '" + new Regex(parameter, "://([^/]+)").getMatch(0) + "'");
                protocol = "http://";
            }
            br.setFollowRedirects(false);
            br.setReadTimeout(3 * 60 * 1000);

            if (parameter.matches(hosts + "/\\d+/(http|ftp).+")) {
                String linkInsideLink = new Regex(parameter, hosts + "/\\d+/(.+)").getMatch(2);
                if (!linkInsideLink.matches(hosts + "/.+")) {
                    decryptedLinks.add(linkInsideLink);
                    return decryptedLinks;
                } else {
                    parameter = linkInsideLink.replace("www.", "");
                }
            }
            br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/32.0.1700.76");
            boolean skipWait = true;
            String finallink = null;
            for (int i = 0; i <= 2; i++) {
                synchronized (LOCK) {
                    br.getPage(parameter.replaceFirst("https?://", protocol));
                }
                if (true) {
                    String redir = br.getRedirectLocation();
                    if (redir != null && redir.contains("http://") && isProtocolHTTPS()) {
                        // redirects can happen here to alternative domains, lets return back into the decrypter.
                        decryptedLinks.add(redir);
                        return decryptedLinks;
                    } else if (redir != null && redir.matches("https?://adf\\.ly/?")) {
                        logger.info("adf.ly link offline: " + parameter);
                        return decryptedLinks;
                    } else if (br.containsHTML("(<b style=\"font-size: 20px;\">Sorry the page you are looking for does not exist|>404 Not Found<|>Not Found<|>Sorry, but the requested page was not found)") || (br.getRedirectLocation() != null && (br.getRedirectLocation().contains("ink-deleted.php") || br.getRedirectLocation().contains("/suspended")))) {
                        logger.info("adf.ly link offline: " + parameter);
                        return decryptedLinks;
                    } else if (br.containsHTML("Sorry, there has been a problem\\.")) {
                        logger.info("adf.ly link offline: " + parameter);
                        return decryptedLinks;
                    }
                }
                /* javascript vars 20130328 */
                String countdown = getWaittime();
                // they also have secondary zzz variable within 'function adf_counter()', but it's the same.
                String zzz = br.getRegex("var zzz\\s?+=\\s?+'?([\\d,\\.]+|http[^'\"]+)'?;").getMatch(0);
                if (zzz != null && zzz.matches("(https?|ftp)://.+")) finallink = zzz;
                String easyUrl = br.getRegex("var easyUrl\\s?+=\\s?+'?(true|false)'?;").getMatch(0);
                String url = br.getRegex("var url\\s?+=\\s?+'?([^\';]+)'?;").getMatch(0);
                // 201307xx
                String ysmm = br.getRegex("var ysmm = '([^\';]+)'").getMatch(0);
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
                    String sec = Encoding.Base64Decode(C + h);
                    finallink = sec.substring(sec.length() - (sec.length() - 2));
                }
                if (finallink == null) {
                    finallink = br.getRedirectLocation();
                }
                if (finallink == null) {
                    finallink = br.getRegex("\\.attr\\((\"|')href(\"|'), '(.*?)'\\)").getMatch(2);
                }
                if (finallink == null) {
                    finallink = br.getRegex("window\\.location = ('|\")(.*?)('|\");").getMatch(1);
                    if (finallink != null && finallink.contains("/noscript.php")) finallink = null;
                }
                if (finallink == null) {
                    finallink = br.getRegex("close_bar.*?self\\.location = '(.*?)';").getMatch(0);
                }
                /* 20130130 */
                if (finallink == null && (countdown != null && zzz != null && easyUrl != null)) {
                    if (countdown.equalsIgnoreCase("7") && easyUrl.equalsIgnoreCase("false")) {
                        br.postPage("/shortener/go", "zzz=" + Encoding.urlEncode(zzz));
                        finallink = br.getRegex("(http[^\"']+)").getMatch(0);
                    }
                }
                /* old stuff still exists! tested and working as of 20130328 */
                if (finallink != null && (!finallink.startsWith("/") && finallink.matches(hosts + ".+"))) {
                    String extendedProtectionPage = br.getRegex("(" + hosts + ")?(/go(/|\\.php\\?)[^<>\"']+)").getMatch(3);
                    if (extendedProtectionPage == null) {
                        // 201307xx
                        extendedProtectionPage = new Regex(finallink, "(" + hosts + ")?(/go(/|\\.php\\?)[^<>\"']+)").getMatch(3);
                        if (extendedProtectionPage != null)
                            extendedProtectionPage = protocol + "adf.ly" + extendedProtectionPage;
                        else
                            break;
                    }
                    int wait = 7;
                    String waittime = getWaittime();
                    // because of possible page action via ajax request, we use old wait time.
                    if (waittime == null) waittime = countdown;
                    if (waittime != null && Integer.parseInt(waittime) <= 20) wait = Integer.parseInt(waittime);
                    if (skipWait) {
                        skipWait();
                        // Wait a seconds. Not waiting can cause the skipWait feature to fail
                        Thread.sleep(1 * 10001l);
                    } else {
                        Thread.sleep(wait * 1000l);
                    }
                    br.getPage(extendedProtectionPage);
                    String tempLink = br.getRedirectLocation();
                    if (tempLink != null) {
                        tempLink = tempLink.replace("www.", "");
                        // Redirected to the same link or blocked...try again
                        if (tempLink.replaceAll("https?://", "").equals(parameter.replaceAll("https?://", "")) || tempLink.contains("adf.ly/locked/") || tempLink.contains("adf.ly/blocked")) {
                            logger.info("Blocked, re-trying with waittime...");
                            skipWait = false;
                            try {
                                br.clearCookies("http://adf.ly/");
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
                } else if (finallink != null && finallink.matches("(https?|ftp)://.+")) break;
            }
            if (finallink != null && finallink.contains("/link-deleted.php")) {
                logger.info(parameter + " has been removed from adf.ly service provider.");
            } else if (finallink != null) {
                decryptedLinks.add(finallink.replace("\\", ""));
            } else {
                logger.warning("adf.ly single regex broken for link: " + parameter);
                logger.info("Adding all available links on page");
                // Use this because they often change the page
                final String[] lol = HTMLParser.getHttpLinks(br.toString(), "");
                for (final String aLink : lol) {
                    if (!new Regex(aLink, hosts + "/.+").matches() && !aLink.contains("/javascript/")) {
                        decryptedLinks.add(aLink);
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
                    brAds.openGetConnection(protocol + skWaitLink);
                } catch (final Exception e) {
                }
            }
        }

        public void addHost(final String h) {
            if (h == null) return;
            final String domain = new Regex(h, "https?://([^/]+)/").getMatch(0);
            // only add it if it's not already inside hosts pattern
            if (!hosts.replace("\\.", ".").contains(domain)) {
                hosts = hosts.substring(0, hosts.length() - 1) + "|" + domain.replace(".", "\\.") + ")";
            }
        }

        private static AtomicReference<String> defaultProtocol = new AtomicReference<String>(null);

        /**
         * Issue the user with a dialog prompt, ask them to select a default request protocol.<br/>
         * Save users preference in memory for given session. Each Start = new session<br/>
         * <br/>
         * Decrypter Template: Default Request Protocol.
         * 
         * @return default request protocol
         * @author raztoki
         * */
        private String getDefaultProtocol() {
            if (!supportsHTTPS) {
                defaultProtocol.set("http://");
            } else {
                SubConfiguration config = null;
                synchronized (LOCK) {
                    try {
                        config = SubConfiguration.getConfig("adf.ly", false);
                        if (isJD2()) {
                            defaultProtocol.set(config.getStringProperty("savedDefaultProtocol", null));
                            if (defaultProtocol.get() != null) return defaultProtocol.get();
                        }
                        if (defaultProtocol.get() == null) {
                            String lng = System.getProperty("user.language");
                            String message = null;
                            String title = null;
                            if ("de".equalsIgnoreCase(lng)) {
                                title = "Wähle bitte Dein Standard Request Protokoll aus.";
                                message = "Dies ist eine einmalige Auswahl. Einmal gespeichert, nutzt der JDownloader Dein\r\ngewähltes Standard Protokoll auch für alle zukünftigen Verbindungen zu adf.ly.";
                            } else {
                                title = "Please select your default Request Protocol.";
                                message = "This is a once off choice. Once saved, JDownloader will reuse\r\n your default Protocol for all future requests to adf.ly.";
                            }
                            String[] select = new String[] { "http (insecure)", "https (secure)" };
                            int userSelect = UserIO.getInstance().requestComboDialog(0, JDL.L("plugins.decrypter.adfly.SelectDefaultProtocolTitle", title), JDL.L("plugins.decrypter.adfly.SelectDefaultProtocolMessage", message), select, 0, null, null, null, null);
                            if (userSelect != -1) {
                                defaultProtocol.set(userSelect == 0 ? "http://" : "https://");
                                if (isJD2()) {
                                    config.setProperty("savedDefaultProtocol", defaultProtocol.get());
                                    config.save();
                                }
                            } else {
                                // 'cancelled/closed/time outed' dialog, returns import protocol.
                                return protocol;
                            }
                        }
                    } catch (final Throwable e) {
                    }
                }
            }
            return defaultProtocol.get();
        }

        private boolean isJD2() {
            if (System.getProperty("jd.revision.jdownloaderrevision") != null)
                return true;
            else
                return false;
        }

        /**
         * returns true when current this.protocol value is https
         * 
         */
        private boolean isProtocolHTTPS() {
            if ("https://".equalsIgnoreCase(this.protocol))
                return true;
            else
                return false;
        }

    }

    public adfly Adfly() {
        return new adfly();
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}