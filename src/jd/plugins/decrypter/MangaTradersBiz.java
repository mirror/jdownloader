//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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

@DecrypterPlugin(revision = "$Revision: 26023 $", interfaceVersion = 3, names = { "mangatraders.biz" }, urls = { "https?://(?:www\\.)?mangatraders\\.(biz|org)/series/\\w+" })
public class MangaTradersBiz extends PluginForDecrypt {

    private PluginForHost plugin = null;

    private Browser prepBrowser(final Browser prepBr) {
        if (plugin == null) {
            plugin = JDUtilities.getPluginForHost("mangatraders.biz");
            if (plugin == null) {
                throw new IllegalStateException("mangatraders hoster plugin not found!");
            }
        }
        // set cross browser support
        ((jd.plugins.hoster.MangaTradersBiz) plugin).setBrowser(br);
        return prepBr;
    }

    private void getPage(final String page) throws Exception {
        if (plugin == null) {
            plugin = JDUtilities.getPluginForHost("mangatraders.biz");
            if (plugin == null) {
                throw new IllegalStateException("mangatraders hoster plugin not found!");
            }
        }
        // set cross browser support
        ((jd.plugins.hoster.MangaTradersBiz) plugin).getPage(page);
    }

    public MangaTradersBiz(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replaceAll("http(s?)://[^/]+(/.+)", "http$1://" + this.getHost() + "$2");
        prepBrowser(br);
        // pages now are just links to download files. We need to be logged in to get this information.
        if (!login()) {
            decryptedLinks.add(createOfflinelink(parameter, "In order to use this website you need an Account!"));
            return decryptedLinks;
        }
        br.setFollowRedirects(true);

        getPage(parameter);
        if (isOffline(this.br)) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }

        final String seriesNameUrl = new Regex(parameter, "series/(.+)").getMatch(0);
        String fpName = br.getRegex("class=\"SeriesName\">([^<>]+)<").getMatch(0);
        if (fpName == null) {
            /* Fallback */
            fpName = seriesNameUrl;
        }
        FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);

        // logger.info(br.toString());
        final String[][] linkinfos = br.getRegex("linkValue=\"([A-Za-z0-9]+)\">\\s*<[^<>]+>([^<>]+)</span>").getMatches();

        if (linkinfos == null || linkinfos.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        if (linkinfos != null && linkinfos.length != 0) {
            for (final String linkinfo[] : linkinfos) {
                final DownloadLink dlink = createDownloadlink("http://" + this.getHost() + "/downloadlink/" + linkinfo[0]);
                dlink.setProperty("mainlink", parameter);
                dlink.setName(linkinfo[1]);
                dlink.setAvailable(true);
                decryptedLinks.add(dlink);
            }
        }

        if (decryptedLinks.size() > 0) {
            fp.addLinks(decryptedLinks);
            return decryptedLinks;
        } else {
            return null;
        }
    }

    private final boolean login() throws Exception {
        final Account aa = AccountController.getInstance().getValidAccount(plugin);
        if (aa != null) {
            try {
                synchronized (jd.plugins.hoster.MangaTradersBiz.ACCLOCK) {
                    ((jd.plugins.hoster.MangaTradersBiz) plugin).login(aa, false);
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

    public static boolean isOffline(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 404;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}