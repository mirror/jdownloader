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
import java.util.HashSet;
import java.util.Set;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pornhub.com" }, urls = { "https?://(?:www\\.|[a-z]{2}\\.)?pornhub(?:premium)?\\.com/album/\\d+" })
public class PornHubComGallery extends PluginForDecrypt {
    public PornHubComGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        parameter = parameter.replaceAll("https://", "http://");
        parameter = parameter.replaceAll("^http://(www\\.)?([a-z]{2}\\.)?", "https://www.");
        br.setFollowRedirects(true);
        Account account = AccountController.getInstance().getValidAccount(getHost());
        if (account != null) {
            try {
                jd.plugins.hoster.PornHubCom.login(this, br, account, false);
            } catch (PluginException e) {
                logger.info("Login failure");
                handleAccountException(account, e);
                account = null;
            }
        }
        if (account != null && AccountType.PREMIUM.equals(account.getType())) {
            parameter = parameter.replace("pornhub.com", "pornhubpremium.com");
        } else {
            parameter = parameter.replace("pornhubpremium.com", "pornhub.com");
        }
        jd.plugins.hoster.PornHubCom.getFirstPageWithAccount(this, account, br, parameter);
        final boolean privateImage = br.containsHTML(jd.plugins.hoster.PornHubCom.html_privateimage);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String fpName = br.getRegex("class=\"photoAlbumTitleV2\">\\s*([^<>\"]*?)\\s*<").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("title>\\s*([^<>\"]*?)\\s*</title>").getMatch(0);
        }
        if (fpName == null) {
            fpName = "pornhub.com album " + new Regex(parameter, "(\\d+)$").getMatch(0);
        }
        final Set<String> pages = new HashSet<String>();
        while (true) {
            final String[] links = br.getRegex("\"/photo/(\\d+)\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String singleID : links) {
                final DownloadLink dl = createDownloadlink("https://www.pornhub.com/photo/" + singleID);
                if (privateImage) {
                    dl.setProperty("private", Boolean.TRUE);
                }
                if (br.containsHTML(singleID + "\">\\s*<div[^>]*class=\"gifStamp\"")) {
                    /* gif images --> .mp4 videos without sound */
                    dl.setName(singleID + ".mp4");
                } else {
                    dl.setName(singleID + ".jpg");
                }
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
            final String nextPage = br.getRegex("\"page_next\"\\s*>\\s*<a\\s*href\\s*=\\s*\"(.*?)\"").getMatch(0);
            if (nextPage == null || !pages.add(nextPage)) {
                break;
            } else {
                jd.plugins.hoster.PornHubCom.getPage(br, nextPage);
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}
