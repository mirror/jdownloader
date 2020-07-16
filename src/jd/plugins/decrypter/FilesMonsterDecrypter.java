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

import java.util.ArrayList;
import java.util.Iterator;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filesmonster.com" }, urls = { "https?://(?:www\\.)?filesmonster\\.com/(?:download\\.php\\?id=[A-Za-z0-9_-]+(?:\\&wbst=[^\\&]+)?|player/v\\d+/video/[A-Za-z0-9_-]+|dl/[A-Za-z0-9_-]+/free/.+)" })
public class FilesMonsterDecrypter extends PluginForDecrypt {
    public FilesMonsterDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String ADDLINKSACCOUNTDEPENDANT = "ADDLINKSACCOUNTDEPENDANT";
    private static final String TYPE_EMBEDDED            = ".+/player/v3/video/.+";
    private static final String TYPE_MAIN                = "https?://(?:www\\.)?filesmonster\\.com/download\\.php\\?id=([A-Za-z0-9_-]+).*";
    private static final String TYPE_DL_FREE             = "https?://(?:www\\.)?filesmonster\\.com/dl/([A-Za-z0-9_-]+)/free/.+";

    /**
     * TODO: Seems like some urls only have a free download option available if a certain Referer is present e.g.
     * https://board.jdownloader.org/showpost.php?p=343469&postcount=6
     */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br = new Browser();
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String FAILED = null;
        final String referer_url = UrlQuery.parse(param.toString()).get("wbst");
        final boolean onlyAddNeededLinks = SubConfiguration.getConfig(this.getHost()).getBooleanProperty(ADDLINKSACCOUNTDEPENDANT, false);
        boolean addFree = true;
        boolean addPremium = true;
        if (onlyAddNeededLinks) {
            boolean accAvailable = false;
            final ArrayList<Account> accs = new ArrayList<Account>();
            try {
                // multihoster account!
                final ArrayList<Account> mHosterAccs = (ArrayList<Account>) AccountController.getInstance().getMultiHostAccounts("filesmonster.com");
                if (mHosterAccs != null && mHosterAccs.size() != 0) {
                    accs.addAll(mHosterAccs);
                }
            } catch (final Throwable t) {
            }
            // traditional account
            final ArrayList<Account> hoster = AccountController.getInstance().getAllAccounts("filesmonster.com");
            if (hoster != null && hoster.size() != 0) {
                accs.addAll(hoster);
            }
            if (accs != null && accs.size() != 0) {
                Iterator<Account> it = accs.iterator();
                while (it.hasNext()) {
                    Account n = it.next();
                    if (n.isEnabled() && n.isValid()) {
                        accAvailable = true;
                        break;
                    }
                }
            }
            if (accAvailable) {
                addPremium = true;
                addFree = false;
            } else {
                addPremium = false;
                addFree = true;
            }
        }
        br.setReadTimeout(3 * 60 * 1000);
        br.setFollowRedirects(false);
        final String main_id;
        final String parameter;
        if (param.toString().matches(TYPE_EMBEDDED)) {
            main_id = new Regex(param.toString(), "/([^/]+)$").getMatch(0);
            parameter = String.format("https://%s/download.php?id=%s", this.getHost(), main_id);
        } else if (param.toString().matches(TYPE_MAIN)) {
            main_id = new Regex(param.toString(), TYPE_MAIN).getMatch(0);
            /* Removes e.g. unneeded parameters from URL */
            parameter = String.format("https://%s/download.php?id=%s", this.getHost(), main_id);
        } else if (param.toString().matches(TYPE_MAIN)) {
            main_id = new Regex(param.toString(), TYPE_MAIN).getMatch(0);
            parameter = param.toString();
        } else {
            /* TYPE_DL_FREE */
            main_id = new Regex(param.toString(), TYPE_DL_FREE).getMatch(0);
            parameter = param.toString();
        }
        if (main_id == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String protocol = new Regex(parameter, "(https?)://").getMatch(0);
        String browserReferrer = getBrowserReferrer();
        if (browserReferrer != null) {
            // we only need to set the domain.. not the url...
            browserReferrer = new Regex(browserReferrer, "https?://[^/]+/").getMatch(-1);
            br.setCurrentURL(browserReferrer);
        }
        br.getHeaders().put("User-Agent", jd.plugins.hoster.MediafireCom.stringUserAgent());
        jd.plugins.hoster.FilesMonsterCom.prepBR(br);
        if (referer_url != null) {
            logger.info("Accessing URL with referer: " + referer_url);
            br.getPage(parameter + "&wbst=" + referer_url);
        } else {
            logger.info("Accessing URL without referer");
            br.getPage(parameter);
        }
        final String title = jd.plugins.hoster.FilesMonsterCom.getLongTitle(this.br);
        if (jd.plugins.hoster.FilesMonsterCom.isOffline(this.br)) {
            final DownloadLink finalOne = this.createOfflinelink(parameter);
            finalOne.setName(main_id);
            decryptedLinks.add(finalOne);
            return decryptedLinks;
        }
        final String fname = jd.plugins.hoster.FilesMonsterCom.getFileName(br);
        final String fsize = jd.plugins.hoster.FilesMonsterCom.getFileSize(br);
        String[] decryptedStuff = null;
        final String postThat = br.getRegex("\"[^\"]*(/dl/.*?)\"").getMatch(0);
        if (postThat != null) {
            br.postPage(postThat, "");
            final String findOtherLinks = br.getRegex("'(/dl/rft/.*?)\\'").getMatch(0);
            if (findOtherLinks != null) {
                br.getPage(findOtherLinks);
                decryptedStuff = br.getRegex("\\{(.*?)\\}").getColumn(0);
            }
            if (decryptedStuff == null) {
                try {
                    jd.plugins.hoster.FilesMonsterCom.handleErrors(this.br);
                    /* There are no free links available so probably a limit has been reached! */
                    FAILED = "Probably limit reached";
                } catch (final Throwable e) {
                    logger.log(e);
                    /* We know for sure that a limit must be reached */
                    FAILED = "Limit reached";
                }
            }
        } else {
            FAILED = "Limit reached";
        }
        if (br.containsHTML(">You need Premium membership to download files larger than")) {
            FAILED = "There are no free downloadlinks";
        }
        if (addFree) {
            if (FAILED == null) {
                final String theImportantPartOfTheMainLink = jd.plugins.hoster.FilesMonsterCom.getMainLinkID(parameter);
                for (String fileInfo : decryptedStuff) {
                    String filename = new Regex(fileInfo, "\"name\":\"(.*?)\"").getMatch(0);
                    String filesize = new Regex(fileInfo, "\"size\":(\")?(\\d+)").getMatch(1);
                    String filelinkPart = new Regex(fileInfo, "\"dlcode\":\"(.*?)\"").getMatch(0);
                    if (filename == null || filesize == null || filelinkPart == null || filename.length() == 0 || filesize.length() == 0 || filelinkPart.length() == 0) {
                        logger.warning("FilesMonsterDecrypter failed while decrypting link:" + parameter);
                        return null;
                    }
                    String dllink = protocol + "://filesmonsterdecrypted.com/dl/" + theImportantPartOfTheMainLink + "/free/2/" + filelinkPart;
                    final DownloadLink finalOne = createDownloadlink(dllink);
                    // name here can be unicode....
                    finalOne.setFinalFileName(Encoding.htmlDecode(Encoding.unicodeDecode(filename)));
                    finalOne.setDownloadSize(Integer.parseInt(filesize));
                    finalOne.setAvailable(true);
                    // for this to be used in hoster plugin it needs non escaped name. see: getNewTemporaryLink
                    finalOne.setProperty("origfilename", filename);
                    finalOne.setProperty("origsize", filesize);
                    finalOne.setProperty("mainlink", parameter);
                    if (referer_url != null) {
                        finalOne.setProperty("referer_url", referer_url);
                    }
                    if (title != null) {
                        finalOne.setComment(title);
                    }
                    decryptedLinks.add(finalOne);
                }
                if (decryptedStuff == null || decryptedStuff.length == 0) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
            } else {
                logger.info("Failed to get free links because: " + FAILED);
                logger.info("If a limit has been reached, free users have to wait until it is over to be able to add new free downloadurls");
            }
        }
        if (addPremium || FAILED != null) {
            final DownloadLink premiumDownloadURL = createDownloadlink(String.format("https://filesmonsterdecrypted.com/download.php?id=%s", main_id));
            if (fname != null && fsize != null) {
                premiumDownloadURL.setFinalFileName(Encoding.htmlDecode(fname.trim()));
                premiumDownloadURL.setDownloadSize(SizeFormatter.getSize(fsize));
                premiumDownloadURL.setAvailable(true);
            }
            premiumDownloadURL.setProperty("PREMIUMONLY", true);
            if (!StringUtils.isEmpty(title)) {
                premiumDownloadURL.setComment(title);
            }
            decryptedLinks.add(premiumDownloadURL);
        }
        /** All those links belong to the same file so lets make a package. */
        final String fpName;
        if (!StringUtils.isEmpty(fname)) {
            fpName = fname;
        } else {
            /* Fallback */
            fpName = main_id;
        }
        if (decryptedLinks.size() > 1) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}