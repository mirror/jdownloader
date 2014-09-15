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
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "easybytez.com" }, urls = { "http://(www\\.)?easybytez.com/users/[^<>\"\\?\\&]+" }, flags = { 0 })
public class EasyBytezComFolder extends PluginForDecrypt {

    public EasyBytezComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    private PluginForHost plugin = null;

    private Browser prepBrowser(Browser prepBr) {
        if (plugin == null) {
            plugin = JDUtilities.getPluginForHost("easybytez.com");
            if (plugin == null) {
                throw new IllegalStateException("easybytez.com hoster plugin not found!");
            }
        }
        // set cross browser support
        ((jd.plugins.hoster.EasyBytezCom) plugin).setBrowser(br);
        return ((jd.plugins.hoster.EasyBytezCom) plugin).prepBrowser(br);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br = new Browser();
        prepBrowser(br);
        final String parameter = param.toString().replaceFirst("https?://", ((jd.plugins.hoster.EasyBytezCom) plugin).getProtocol());
        ((jd.plugins.hoster.EasyBytezCom) plugin).getPage(parameter);
        if (((jd.plugins.hoster.EasyBytezCom) plugin).cbr.containsHTML(">\\s*No such user exist\\s*<")) {
            logger.info("Invalid URL! " + parameter);
            return decryptedLinks;
        }
        if (((jd.plugins.hoster.EasyBytezCom) plugin).cbr.containsHTML(">\\s*Guest access not possible!\\s*<")) {
            // we only login when required!
            final boolean logged_in = getLogin();
            if (!logged_in) {
                logger.info("Guest access not possible!");
                logger.info("Login failed or no accounts active/existing -> Continuing without account");
                return decryptedLinks;
            }
            ((jd.plugins.hoster.EasyBytezCom) plugin).getPage(parameter);
        }
        // name isn't needed, other than than text output for fpName. (copy paste xfsfolder)
        String fpName = new Regex(parameter, "(folder/\\d+/|f/[a-z0-9]+/|go/[a-z0-9]+/)[^/]+/(.+)").getMatch(1); // name
        if (fpName == null) {
            fpName = new Regex(parameter, "(folder/\\d+/|f/[a-z0-9]+/|go/[a-z0-9]+/)(.+)").getMatch(1); // id
            if (fpName == null) {
                fpName = new Regex(parameter, "users/[a-z0-9_]+/[^/]+/(.+)").getMatch(0); // name
                if (fpName == null) {
                    fpName = new Regex(parameter, "users/[a-z0-9_]+/(.+)").getMatch(0); // id
                }
            }
        }
        int lastPage = 1;
        final String[] allPages = ((jd.plugins.hoster.EasyBytezCom) plugin).cbr.getRegex("<a href='\\?\\&amp;page=(\\d+)'").getColumn(0);
        if (allPages != null && allPages.length != 0) {
            for (final String aPage : allPages) {
                final int pp = Integer.parseInt(aPage);
                if (pp > lastPage) {
                    lastPage = pp;
                }
            }
        }
        for (int i = 1; i <= lastPage; i++) {
            ((jd.plugins.hoster.EasyBytezCom) plugin).getPage(parameter + "?&page=" + i);
            String[] links = ((jd.plugins.hoster.EasyBytezCom) plugin).cbr.getRegex("class=\"link\"><a href=\"(https?://(www\\.)?easybytez\\.com/[a-z0-9]{12})\"").getColumn(0);
            if (links == null || links.length == 0) {
                links = ((jd.plugins.hoster.EasyBytezCom) plugin).cbr.getRegex("<td><a href=\"(https?://(www\\.)?easybytez\\.com/[a-z0-9]{12})\"").getColumn(0);
            }
            if (links == null || links.length == 0) {
                if (((jd.plugins.hoster.EasyBytezCom) plugin).cbr.containsHTML(">The selected folder contains the following files")) {
                    logger.info("Link offline: " + parameter);
                    return decryptedLinks;
                }
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String singleLink : links) {
                decryptedLinks.add(createDownloadlink(singleLink));
            }
        }
        if (fpName != null) {
            fpName = Encoding.urlDecode(fpName, false);
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
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

    private boolean getLogin() throws Exception {
        final Account aa = AccountController.getInstance().getValidAccount(plugin);
        if (aa != null) {
            try {
                boolean fresh = false;
                Object after = null;
                synchronized (jd.plugins.hoster.EasyBytezCom.ACCLOCK) {
                    Object before = aa.getProperty("cookies", null);
                    after = ((jd.plugins.hoster.EasyBytezCom) plugin).login(aa, false);
                    fresh = before != after;
                    if (fresh) {
                        final String myAccount = "/?op=my_account";
                        if (br.getURL() == null) {
                            br.setFollowRedirects(true);
                            ((jd.plugins.hoster.EasyBytezCom) plugin).getPage(((jd.plugins.hoster.EasyBytezCom) plugin).COOKIE_HOST.replaceFirst("https?://", ((jd.plugins.hoster.EasyBytezCom) plugin).getProtocol()) + myAccount);
                        } else if (!br.getURL().contains(myAccount)) {
                            ((jd.plugins.hoster.EasyBytezCom) plugin).getPage(myAccount);
                        }
                        ((jd.plugins.hoster.EasyBytezCom) plugin).updateAccountInfo(aa, aa.getAccountInfo(), this.br);
                    }
                }
            } catch (final PluginException e) {
                aa.setValid(false);
                logger.info("Account seems to be invalid!");
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}