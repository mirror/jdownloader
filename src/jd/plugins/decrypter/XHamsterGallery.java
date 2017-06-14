//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "xhamster.com" }, urls = { "https?://(www\\.)?((de|es|ru|fr|it|jp|pt|nl|pl)\\.)?xhamster\\.com/photos/(gallery/[0-9A-Za-z_\\-/]+(\\.html)?|view/[0-9A-Za-z_\\-/]+(\\.html)?)" })
public class XHamsterGallery extends PluginForDecrypt {
    public XHamsterGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("www.", "");
        /* Force english language */
        final String replace_string = new Regex(parameter, "(https?://(www\\.)?((de|es|ru|fr|it|jp|pt|nl|pl)\\.)?xhamster\\.com/)").getMatch(0);
        parameter = parameter.replace(replace_string, "https://xhamster.com/");
        br.addAllowedResponseCodes(410);
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        // Login if possible
        getUserLogin(false);
        br.setFollowRedirects(true);
        br.getPage(parameter);
        /* Error handling */
        if (br.getHttpConnection().getResponseCode() == 410 || br.containsHTML("Sorry, no photos found|error\">Gallery not found<|>Page Not Found<")) {
            decryptedLinks.add(createOfflinelink(parameter, "Content Offline"));
            return decryptedLinks;
        }
        if (br.containsHTML(">This gallery is visible for")) {
            logger.info("This gallery is only visible for specified users, account needed: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML(">This gallery needs password<")) {
            boolean failed = true;
            for (int i = 1; i <= 3; i++) {
                String passCode = getUserInput("Password?", param);
                br.postPage(br.getURL(), "password=" + Encoding.urlEncode(passCode));
                if (br.containsHTML(">This gallery needs password<")) {
                    continue;
                }
                failed = false;
                break;
            }
            if (failed) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
        }
        if (new Regex(br.getURL(), "/gallery/[0-9]+/[0-9]+").matches()) { // Single picture
            DownloadLink dl = createDownloadlink("directhttp://" + br.getRegex("class='slideImg'\\s+src='([^']+)").getMatch(0));
            dl.setAvailable(true);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        final String urlWithoutPageParameter = this.br.getURL();
        String fpname = br.getRegex("<title>(.*?) \\- \\d+ (Pics|Bilder) \\- xHamster\\.com</title>").getMatch(0);
        if (fpname == null) {
            fpname = br.getRegex("<title>(.*?)\\s*>\\s*").getMatch(0);
        }
        int pageMax = 1;
        final String[] pagesTemp = br.getRegex("\\?page=(\\d+)").getColumn(0);
        if (pagesTemp != null && pagesTemp.length != 0) {
            int pageTmp = 1;
            for (final String aPage_str : pagesTemp) {
                pageTmp = Integer.parseInt(aPage_str);
                if (pageTmp > pageMax) {
                    pageMax = pageTmp;
                }
            }
        }
        for (int pageCurrent = 1; pageCurrent <= pageMax; pageCurrent++) {
            if (this.isAbort()) {
                logger.info("Decryption aborted by user");
                return decryptedLinks;
            }
            if (pageCurrent > 1) {
                br.getPage(urlWithoutPageParameter + "?page=" + pageCurrent);
            }
            String allLinks = br.getRegex("class='iListing'>(.*?)id='galleryInfoBox'>").getMatch(0);
            if (allLinks == null) {
                allLinks = br.getRegex("id='imgSized'(.*?)gid='\\d+").getMatch(0);
            }
            // 'http://ept.xhcdn.com/000/027/563/101_160.jpg'
            final String[][] thumbNails = new Regex(allLinks, "(\"|')(https?://(?:ept|upt|ep\\d+)\\.xhcdn\\.com/\\d+/\\d+/\\d+/\\d+_(?:160|1000)\\.(je?pg|gif|png))\\1").getMatches();
            if (thumbNails == null || thumbNails.length == 0) {
                logger.warning("Decrypter failed on page " + pageCurrent);
                return null;
            }
            for (final String[] thumbNail : thumbNails) {
                DownloadLink dl = createDownloadlink("directhttp://http://ep.xhamster.com/" + new Regex(thumbNail[1], ".+\\.xhcdn\\.com/(\\d+/\\d+/\\d+/\\d+_)(160|1000)\\." + thumbNail[2]).getMatch(0) + "1000." + thumbNail[2]);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        }
        if (fpname != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpname.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /**
     * JD2 CODE: DO NOIT USE OVERRIDE FÒR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    private boolean getUserLogin(final boolean force) throws Exception {
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost("xhamster.com");
        final Account aa = AccountController.getInstance().getValidAccount(hostPlugin);
        if (aa == null) {
            logger.warning("There is no account available, stopping...");
            return false;
        }
        try {
            ((jd.plugins.hoster.XHamsterCom) hostPlugin).setBrowser(br);
            ((jd.plugins.hoster.XHamsterCom) hostPlugin).login(aa, force);
        } catch (final PluginException e) {
            aa.setValid(false);
            return false;
        }
        return true;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}