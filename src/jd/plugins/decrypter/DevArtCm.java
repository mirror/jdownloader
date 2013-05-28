//    jDownloader - Downloadmanager
//    Copyright (C) 2011  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "deviantart.com" }, urls = { "https?://[\\w\\.\\-]*?deviantart\\.com/((gallery|favourites)/\\d+(\\?offset=\\d+)?|(gallery|favourites)/(\\?offset=\\d+)?)" }, flags = { 0 })
public class DevArtCm extends PluginForDecrypt {

    public DevArtCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * @author raztoki
     */

    // This plugin grabs range of content depending on parameter.
    // profile.devart.com/gallery/uid*
    // profile.devart.com/favorites/uid*
    // profile.devart.com/gallery/*
    // profile.devart.com/favorites/*
    // * = ?offset=\\d+
    //
    // All of the above formats should support spanning pages, but when parameter contains '?offset=x' it will not span.
    //
    // profilename.deviantart.com/art/uid/ == grabs the 'download image' (best quality available).
    //
    // I've created the plugin this way to allow users to grab as little or as much, content as they wish. Hopefully this wont create any
    // issues.

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.setCookiesExclusive(true);
        br.getPage(parameter);
        if (br.containsHTML("The page you were looking for doesn\\'t exist\\.")) {
            logger.warning("Invalid URL: " + parameter);
            return decryptedLinks;
        }
        // for stable compliance (getHost() will only return domain.tld and not subdomain(s).domain.tld)
        String host = new Regex(br.getURL(), "(https?://.*?deviantart\\.com)/").getMatch(0);

        // only non /art/ requires packagename
        if (parameter.contains("/gallery/") || parameter.contains("/favourites/")) {
            // find and set username
            String username = br.getRegex("name=\"username\" value=\"([^<>\"]*?)\"").getMatch(0);
            // find and set page type
            String pagetype = "";
            if (parameter.contains("/favourites/")) pagetype = "Favourites";
            if (parameter.contains("/gallery/")) pagetype = "Gallery";
            // find and set pagename
            String pagename = br.getRegex("<span class=\"folder\\-title\">(.*?)</span>").getMatch(0);
            // set packagename
            String fpName = "";
            if ((username != null) && (pagetype != null) && (pagename != null))
                fpName = username + " - " + pagetype + " - " + pagename;
            else if ((username != null) && (pagename != null))
                fpName = username + " - " + pagename;
            else if ((username != null) && (pagetype != null))
                fpName = username + " - " + pagetype;
            else if ((pagetype != null) && (pagename != null)) fpName = pagetype + " - " + pagename;

            // now we find and crawl!
            parsePage(decryptedLinks, host, parameter);

            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName);
                fp.setProperty("ALLOW_MERGE", true);
                fp.addLinks(decryptedLinks);
            }
        }
        return decryptedLinks;
    }

    private void parsePage(ArrayList<DownloadLink> ret, String host, String parameter) throws Exception {
        String grab = br.getRegex("<smoothie q=(.*?)class=\"folderview\\-bottom\"></div>").getMatch(0);
        String[] artlinks = new Regex(grab, "<a class=\"thumb([\\s\\w]+)?\" href=\"(https?://[\\w\\.\\-]*?deviantart\\.com/art/[\\w\\-]+)\"").getColumn(1);
        String nextPage = br.getRegex("href=\"(/(gallery|favourites)/(\\d+)?\\?offset=\\d+)\">Next</a>").getMatch(0);
        if (artlinks == null || artlinks.length == 0) {
            logger.warning("Possible Plugin error, with finding /art/ links: " + parameter);
            return;
        }
        if (artlinks != null && artlinks.length != 0) {
            for (final String al : artlinks) {
                ret.add(createDownloadlink(al));
            }
        }
        if (nextPage != null && !parameter.contains("?offset=")) {
            br.getPage(host + nextPage);
            parsePage(ret, host, parameter);
        }
    }

    private static final Object LOCK = new Object();

    private boolean login() throws Exception {
        boolean isNew = false;
        PluginForHost DeviantArtPlugin = JDUtilities.getPluginForHost("deviantart.com");
        Account aa = AccountController.getInstance().getValidAccount(DeviantArtPlugin);
        if (aa == null) {
            // lets prevent multiple dialogs because of threading
            synchronized (LOCK) {
                isNew = true;
                String username = UserIO.getInstance().requestInputDialog("Enter Loginname for deviantart.com :");
                if (username == null) return false;
                String password = UserIO.getInstance().requestInputDialog("Enter password for deviantart.com :");
                if (password == null) return false;
                aa = new Account(username, password);
            }
        }
        try {
            ((jd.plugins.hoster.DeviantArtCom) DeviantArtPlugin).login(aa, this.br, false);
        } catch (final PluginException e) {
            aa.setEnabled(false);
            aa.setValid(false);
            logger.info("Account is invalid!");
            return false;
        }
        if (isNew == true) AccountController.getInstance().addAccount(DeviantArtPlugin, aa);
        return true;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}