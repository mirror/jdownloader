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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xhamster.com" }, urls = { "http://(www\\.)?xhamster\\.com/photos/gallery/[0-9]+/.*?\\.html" }, flags = { 0 })
public class XHamsterGallery extends PluginForDecrypt {

    public XHamsterGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> allPages = new ArrayList<String>();
        allPages.add("1");
        final String parameter = param.toString().replace("www.", "");
        final String parameterWihoutHtml = parameter.replace(".html", "");
        // Login if possible
        getUserLogin(false);
        br.getPage(parameter);
        /* Error handling */
        if (br.containsHTML("Sorry, no photos found|>Gallery not found<")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML(">This gallery is visible for")) {
            logger.info("This gallery is only visible for specified users, account needed: " + parameter);
            return decryptedLinks;
        }

        if (br.containsHTML(">This gallery needs password<")) {
            for (int i = 1; i <= 3; i++) {
                String passCode = getUserInput("Password?", param);
                br.postPage(br.getURL(), "password=" + Encoding.urlEncode(passCode));
                if (br.containsHTML(">This gallery needs password<")) continue;
                break;
            }
            if (br.containsHTML(">This gallery needs password<")) throw new DecrypterException(DecrypterException.PASSWORD);
        }

        String fpname = br.getRegex("<title>(.*?) \\- \\d+ Pics \\- xHamster\\.com</title>").getMatch(0);
        String[] pagesTemp = br.getRegex("\\'" + parameterWihoutHtml + "-\\d+\\.html\\'>(\\d+)</a>").getColumn(0);
        if (pagesTemp != null && pagesTemp.length != 0) {
            for (String aPage : pagesTemp)
                if (!allPages.contains(aPage)) allPages.add(aPage);
        }
        for (String currentPage : allPages) {
            final String thePage = parameterWihoutHtml + "-" + currentPage + ".html";
            br.getPage(thePage);
            final String allLinks = br.getRegex("class=\\'iListing\\'>(.*?)id=\\'galleryInfoBox\\'>").getMatch(0);
            if (allLinks == null) {
                logger.warning("Decrypter failed on page " + currentPage + " for link: " + thePage);
                return null;
            }
            final String[] thumbNails = new Regex(allLinks, "(\"|\\')(http://p\\d+\\.xhamster\\.com/\\d+/\\d+/\\d+/\\d+_160\\.jpg)(\"|\\')").getColumn(1);
            if (thumbNails == null || thumbNails.length == 0) {
                logger.warning("Decrypter failed on page " + currentPage + " for link: " + thePage);
                return null;
            }
            for (String thumbNail : thumbNails) {
                DownloadLink dl = createDownloadlink("directhttp://http://up.xhamster.com/" + new Regex(thumbNail, "http://p\\d+\\.xhamster\\.com/(\\d+/\\d+/\\d+/\\d+_)160\\.jpg").getMatch(0) + "1000.jpg");
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        }
        if (fpname != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpname.trim());
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

    private boolean getUserLogin(final boolean force) throws Exception {
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost("xhamster.com");
        final Account aa = AccountController.getInstance().getValidAccount(hostPlugin);
        if (aa == null) {
            logger.warning("There is no account available, stopping...");
            return false;
        }
        try {
            ((jd.plugins.hoster.XHamsterCom) hostPlugin).login(this.br, aa, force);
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