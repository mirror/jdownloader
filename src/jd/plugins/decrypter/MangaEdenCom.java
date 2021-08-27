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

import java.text.DecimalFormat;
import java.util.ArrayList;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mangaeden.com" }, urls = { "https?://(www\\.)?mangaeden\\.com/(?:[a-z]{2}/)?[a-z0-9\\-]+/[a-z0-9\\-]+/\\d+(?:\\.\\d+)?/1/" })
public class MangaEdenCom extends antiDDoSForDecrypt {
    public MangaEdenCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /** 2019-02-10: Too many requests --> error 503 */
    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final ArrayList<String> pageURLs = new ArrayList<String>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(new int[] { 503 });
        /* Login whenever possible as some content can only be accessed via account. */
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        if (account != null) {
            final PluginForHost plg = this.getNewPluginForHostInstance(this.getHost());
            ((jd.plugins.hoster.MangaedenCom) plg).login(account, false);
        }
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("404 NOT FOUND")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 503) {
            logger.info("Too many requests - try again later");
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        } else if (br.containsHTML("(?i)Isn't Out\\!\\s*<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getURL().matches("(?i)https?://[^/]+/[^/]+/login/?$")) {
            /* Account required to access content. */
            throw new AccountRequiredException();
        }
        final String thisLinkpart = new Regex(br.getURL(), "mangaeden\\.com(/.*?)1/$").getMatch(0);
        String fpName = br.getRegex("<title>\\s*([^<>\"]*?)(?:\\s*-\\s*page \\d+)?\\s*-\\s*(?:Read Manga Online Free|Manga Eden)").getMatch(0);
        final String[] pages = br.getRegex("<option[^>]+value=\"(" + thisLinkpart + "\\d+/)\"").getColumn(0);
        if (pages == null || pages.length == 0 || fpName == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        fpName = Encoding.htmlDecode(fpName.trim()).replace("\n", "");
        for (final String currentPage : pages) {
            if (!pageURLs.contains(currentPage)) {
                pageURLs.add(currentPage);
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        // decrypt all pages
        final DecimalFormat df = new DecimalFormat(pageURLs.size() < 100 ? "00" : "000");
        int counter = 1;
        for (final String currentPage : pageURLs) {
            logger.info("Working on item " + counter + "/" + pageURLs.size());
            if (!br.getURL().endsWith(currentPage)) {
                getPage(currentPage);
            }
            final String decryptedlink = getSingleLink();
            final DownloadLink dd = createDownloadlink("directhttp://" + decryptedlink);
            dd.setAvailable(true);
            dd.setFinalFileName(fpName + "_" + df.format(counter) + getFileNameExtensionFromString(decryptedlink, ".jpg"));
            fp.add(dd);
            decryptedLinks.add(dd);
            distribute(dd);
            counter++;
            if (isAbort()) {
                break;
            }
        }
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    private String getSingleLink() {
        String finallink = br.getRegex("<img[^>]+id=\"mainImg\"[^>]+src=\"((?:https?:)?//[^<>\"]*?)\"").getMatch(0);
        if (finallink == null) {
            finallink = br.getRegex("\"((?:https?:)?//(?:www\\.)?cdn\\.mangaeden\\.com/mangasimg/[^<>\"]*?)\"").getMatch(0);
        }
        if (finallink == null) {
            return null;
        }
        finallink = Request.getLocation(finallink, br.getRequest());
        return finallink;
    }

    public boolean hasCaptcha(final CryptedLink link, final jd.plugins.Account acc) {
        return false;
    }
}