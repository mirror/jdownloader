//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.RandomUserAgent;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sexuria.com" }, urls = { "http://(www\\.)?sexuria\\.com/(v1/)?Pornos_Kostenlos_.+?_(\\d+)\\.html|http://(www\\.)?sexuria\\.com/(v1/)?dl_links_\\d+_\\d+\\.html|http://(www\\.)?sexuria\\.com/out\\.php\\?id=([0-9]+)\\&part=[0-9]+\\&link=[0-9]+" }, flags = { 0 })
public class Sxrcm extends PluginForDecrypt {

    private static final Pattern PATTEREN_SUPPORTED_MAIN    = Pattern.compile("http://(www\\.)?sexuria\\.com/(v1/)?Pornos_Kostenlos_.+?_(\\d+)\\.html", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_SUPPORTED_CRYPT    = Pattern.compile("http://(www\\.)?sexuria\\.com/(v1/)?dl_links_\\d+_(\\d+)\\.html", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_SUPPORTED_REDIRECT = Pattern.compile("http://(www\\.)?sexuria\\.com/out\\.php\\?id=([0-9]+)\\&part=[0-9]+\\&link=[0-9]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_PASSWORD           = Pattern.compile("<strong>Passwort: </strong></div></td>.*?bgcolor=\"#EFEFEF\">(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern PATTERN_DL_LINK_PAGE       = Pattern.compile("\"(dl_links_\\d+_\\d+\\.html)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_REDIRECT_LINKS     = Pattern.compile("value=\"(http://sexuria\\.com/out\\.php\\?id=\\d+\\&part=\\d+\\&link=\\d+)\" readonly", Pattern.CASE_INSENSITIVE);
    private static Object        LOCK                       = new Object();

    public Sxrcm(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        String downloadId;
        String password = null;
        br.setFollowRedirects(false);
        synchronized (LOCK) {
            if (new Regex(parameter, PATTEREN_SUPPORTED_MAIN).matches()) {
                br.getPage(parameter);
                String links[] = br.getRegex(PATTERN_DL_LINK_PAGE).getColumn(0);
                for (String link : links) {
                    decryptedLinks.add(createDownloadlink("http://sexuria.com/v1/" + link));
                }
                return decryptedLinks;
            } else if (new Regex(parameter, PATTERN_SUPPORTED_CRYPT).matches()) {
                downloadId = new Regex(parameter, PATTERN_SUPPORTED_CRYPT).getMatch(2);
                br.getPage("http://sexuria.com/v1/Pornos_Kostenlos_info_" + downloadId + ".html");
                password = br.getRegex(PATTERN_PASSWORD).getMatch(0);
                ArrayList<String> pwList = null;
                if (password != null) {
                    pwList = new ArrayList<String>(Arrays.asList(new String[] { password.trim() }));
                }
                Thread.sleep(1000);
                br.getPage(parameter);
                final String links[] = br.getRegex(PATTERN_REDIRECT_LINKS).getColumn(0);
                if (links == null || links.length == 0) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                for (String link : links) {
                    try {
                        if (this.isAbort()) {
                            logger.info("Decryption aborted by user: " + parameter);
                            return decryptedLinks;
                        }
                    } catch (final Throwable e) {
                        // Not available in old 0.9.581 Stable
                    }
                    link = link.replace("http://sexuria.com/", "http://www.sexuria.com/");
                    Thread.sleep(1000);
                    br.getPage(link);
                    final String finallink = br.getRedirectLocation();
                    if (finallink == null || finallink.contains("sexuria.com/")) {
                        logger.warning("Decrypter broken for link: " + parameter);
                        return null;
                    }
                    final DownloadLink dlLink = createDownloadlink(finallink);
                    if (pwList != null) dlLink.setSourcePluginPasswordList(pwList);
                    decryptedLinks.add(dlLink);
                    try {
                        distribute(dlLink);
                    } catch (final Throwable e) {
                        // Not available in old 0.9.581 Stable
                    }
                }
                return decryptedLinks;
            } else if (new Regex(parameter, PATTERN_SUPPORTED_REDIRECT).matches()) {
                String id = new Regex(parameter, PATTERN_SUPPORTED_REDIRECT).getMatch(0);
                decryptedLinks.add(createDownloadlink("http://sexuria.com/Pornos_Kostenlos_liebe_" + id + ".html"));
                return decryptedLinks;
            }
            return null;
        }
    }

    // @Override

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}