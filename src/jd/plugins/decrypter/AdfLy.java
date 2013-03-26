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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "adf.ly" }, urls = { "http://(www\\.)?(adf\\.ly|9\\.bb|j\\.gs|q\\.gs|urlm\\.in)/(?!link\\-deleted\\.php)[^<>\"/#]+" }, flags = { 0 })
public class AdfLy extends PluginForDecrypt {

    public AdfLy(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static Object LOCK = new Object();

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String finallink = null;
        final String parameter = param.toString().replace("www.", "");
        br.setFollowRedirects(false);
        br.setReadTimeout(3 * 60 * 1000);
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:17.0) Gecko/20100101 Firefox/17.0");
        boolean skipWait = true;
        for (int i = 0; i <= 2; i++) {
            synchronized (LOCK) {
                br.getPage(parameter);
                if (parameter.contains("9.bb/") && br.getRedirectLocation() != null) {
                    br.getPage(br.getRedirectLocation());
                }
            }
            if (br.containsHTML("(<b style=\"font\\-size: 20px;\">Sorry the page you are looking for does not exist|>404 Not Found<|>Not Found<|>Sorry, but the requested page was not found)")) {
                logger.info("adf.ly link offline: " + parameter);
                return decryptedLinks;
            }
            if (br.containsHTML("Sorry, there has been a problem\\.")) {
                logger.info("adf.ly link offline: " + parameter);
                return decryptedLinks;
            }
            finallink = br.getRedirectLocation();
            if (finallink == null) {
                finallink = br.getRegex("\\.attr\\((\"|\\')href(\"|\\'), \\'(.*?)\\'\\)").getMatch(2);
            }
            if (finallink == null) {
                finallink = br.getRegex("window\\.location = ('|\")(.*?)('|\");").getMatch(1);
                if (finallink.contains("/noscript.php")) finallink = null;
            }
            if (finallink == null) {
                finallink = br.getRegex("close_bar.*?self\\.location = \\'(.*?)\\';").getMatch(0);
            }

            String extendedProtectionPage = br.getRegex("\\'(https?://adf\\.ly/go(/|\\.php\\?)[^<>\"\\']*?)\\'").getMatch(0);
            if (extendedProtectionPage == null) {
                extendedProtectionPage = br.getRegex("var url = \\'(/go(/|\\.php\\?)[^<>\"\\']*?)\\'").getMatch(0);
                if (extendedProtectionPage != null) {
                    extendedProtectionPage = "http://adf.ly" + extendedProtectionPage;
                }
            }
            if (extendedProtectionPage == null && finallink == null) break;
            if (extendedProtectionPage != null) {
                int wait = 7;
                String waittime = getWaittime();
                if (waittime != null && Integer.parseInt(waittime) <= 20) wait = Integer.parseInt(waittime);
                if (skipWait) {
                    skipWait();
                    // Wait a seconds. Not waiting can cause the skipWait
                    // feature to fail
                    sleep(1 * 10001l, param);
                } else {
                    sleep(wait * 1000l, param);
                }
                br.getPage(extendedProtectionPage);
                String tempLink = br.getRedirectLocation();
                if (tempLink != null) {
                    tempLink = tempLink.replace("www.", "");
                    // Redirected to the same link or blocked...try again
                    if (tempLink.equals(parameter) || tempLink.contains("adf.ly/locked/") || tempLink.contains("adf.ly/blocked")) {
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
                    // Everything should have worked correctly, try to get final
                    // link
                    finallink = br.getRegex("<META HTTP\\-EQUIV=\"Refresh\" CONTENT=\"\\d+; URL=(http://[^<>\"\\']+)\"").getMatch(0);
                    break;
                }
            }
        }
        /* 20130130 */
        if (finallink == null) {
            String query = br.getRegex("var zzz\\s?=\\s?\'([^\']+)\'").getMatch(0);
            String path = br.getRegex("url\\s?:\\s?\'([^\']+)\'").getMatch(0);
            if (path != null && query != null) {
                br.getPage("http://adf.ly" + path + "?zzz=" + Encoding.urlEncode(query));
                finallink = br.getRegex("zzz\":\"([^\"]+)\"").getMatch(0);
                if (finallink != null) finallink = finallink.replace("\\", "");
            } else if (query != null) finallink = query;
        }
        if (finallink != null) {
            decryptedLinks.add(createDownloadlink(finallink));
        } else {
            logger.warning("adf.ly single regex broken for link: " + parameter);
            logger.info("Adding all available links on page");
            // Use this because they often change the page
            final String[] lol = HTMLParser.getHttpLinks(br.toString(), "");
            for (final String aLink : lol) {
                if (!new Regex(aLink, "http://(www\\.)?(adf\\.ly|9\\.bb|j\\.gs|q\\.gs|urlm\\.in)/.+").matches() && !aLink.contains("/javascript/")) {
                    decryptedLinks.add(createDownloadlink(aLink));
                }
            }
        }

        return decryptedLinks;
    }

    private void skipWait() {
        final Browser brAds = br.cloneBrowser();
        brAds.setConnectTimeout(5 * 1000);
        brAds.setReadTimeout(5 * 1000);
        final String[] skpWaitLinks = { "http://cdn.adf.ly/css/adfly_1.css", "http://cdn.adf.ly/js/adfly.js", "http://cdn.adf.ly/images/logo_fb.png", "http://cdn.adf.ly/images/skip_ad/en.png", "http://adf.ly/favicon.ico", "http://adf.ly/favicon.ico", "http://cdn.adf.ly/images/ad_top_bg.png", "http://adf.ly/omnigy7425325410.swf" };
        for (final String skWaitLink : skpWaitLinks) {
            try {
                brAds.openGetConnection(skWaitLink);
            } catch (final Exception e) {
            }
        }
    }

    private String getWaittime() {
        return br.getRegex("var countdown = (\\d+);").getMatch(0);
    }

}
