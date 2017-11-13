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
import jd.http.Cookies;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "freedisc.pl" }, urls = { "https?://(www\\.)?freedisc\\.pl/[A-Za-z0-9_\\-]+,d\\-\\d+([A-Za-z0-9_,\\-]+)?" })
public class FreeDiscPlFolder extends PluginForDecrypt {
    public FreeDiscPlFolder(PluginWrapper wrapper) {
        super(wrapper);
        try {
            Browser.setRequestIntervalLimitGlobal("freedisc.pl", 1000, 20, 60000);
        } catch (final Throwable e) {
        }
    }

    private static final String TYPE_FOLDER    = "https?://(www\\.)?freedisc\\.pl/[A-Za-z0-9\\-_]+,d-\\d+";
    protected static Cookies    botSafeCookies = new Cookies();

    private Browser prepBR(final Browser br) {
        jd.plugins.hoster.FreeDiscPl.prepBRStatic(br);
        synchronized (botSafeCookies) {
            if (!botSafeCookies.isEmpty()) {
                br.setCookies(this.getHost(), botSafeCookies);
            }
        }
        return br;
    }

    /* 2017-01-06: Bot-block captchas. */
    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        prepBR(this.br);
        getPage(parameter);
        if (this.br.getHttpConnection().getResponseCode() == 410) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String fpName = br.getRegex(">([^>]+)</h1>").getMatch(0);
        // final String[] entries = br.getRegex("div class=\"dir-item\"><div.*?</div></div></div>").getColumn(-1);
        final String[] entries = br.getRegex("div\\s*class=\"dir-item\">[^~]*?</div>\\s*</div>").getColumn(-1);
        // final String fileEntry = "class=('|\"|)[\\w -]+\\1><a href=\"(/[^<>\"]*?,f\\-[^<>\"]*?)\"[^>]*>(.*?)</a>";
        final String fileEntry = "class=('|\"|)[\\w -]+\\1>\\s*<a\\s*href=\"(/[^<>\"]*?,f-[^<>\"]*?)\"[^>]*>\\s*(.*?)</a>";
        final String folderEntry = "class=('|\"|)[\\w -]+\\1><a href=\"(/?[A-Za-z0-9\\-_]+,d\\-\\d+[^<>\"]*?)\"";
        if (entries != null && entries.length > 0) {
            for (final String e : entries) {
                final String folder = new Regex(e, folderEntry).getMatch(1);
                if (folder != null) {
                    // decryptedLinks.add(createDownloadlink(Request.getLocation(folder, br.getRequest()))); // Too much!
                    continue;
                }
                final String link = new Regex(e, fileEntry).getMatch(1);
                final String filename = new Regex(e, fileEntry).getMatch(2);
                final String filesize = new Regex(e, "info\">Rozmiar :(.*?)<").getMatch(0);
                final DownloadLink dl = createDownloadlink(Request.getLocation(link, br.getRequest()));
                dl.setName(filename);
                dl.setDownloadSize(SizeFormatter.getSize(filesize.replace("Bajty", "Bytes")));
                dl.setAvailableStatus(AvailableStatus.TRUE);
                decryptedLinks.add(dl);
            }
        } else {
            // fail over
            final String[] links = br.getRegex(fileEntry).getColumn(0);
            final String[] folders = br.getRegex(folderEntry).getColumn(1);
            if ((links == null || links.length == 0) && (folders == null || folders.length == 0) && br.containsHTML("class=\"directoryText previousDirLinkFS\"")) {
                decryptedLinks.add(createOfflinelink(parameter));
                return decryptedLinks;
            }
            if (links != null && links.length > 0) {
                for (String singleLink : links) {
                    singleLink = Request.getLocation(singleLink, br.getRequest());
                    decryptedLinks.add(createDownloadlink(singleLink));
                }
            }
            if (folders != null && folders.length > 0) {
                for (final String singleLink : folders) {
                    // decryptedLinks.add(createDownloadlink("https://freedisc.pl" + singleLink)); Too much!
                }
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private void getPage(final String url) throws Exception {
        this.br.getPage(url);
        handleAntiBot(br);
    }

    private void handleAntiBot(final Browser br) throws Exception {
        if (isBotBlocked()) {
            /* Process anti-bot captcha */
            logger.info("Spam protection detected");
            final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
            br.postPage(br.getURL(), "g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response));
            if (isBotBlocked()) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Anti-Bot block", 5 * 60 * 1000l);
            }
            // save the session!
            synchronized (botSafeCookies) {
                botSafeCookies = br.getCookies(this.getHost());
            }
        }
    }

    private boolean isBotBlocked() {
        return jd.plugins.hoster.FreeDiscPl.isBotBlocked(this.br);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}