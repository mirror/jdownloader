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
import jd.plugins.hoster.DeviantArtCom;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "deviantart.com" }, urls = { "https?://[\\w\\.\\-]*?deviantart\\.com/(art/[\\w\\-]+|((gallery|favourites)/\\d+(\\?offset=\\d+)?|(gallery|favourites)/(\\?offset=\\d+)?))" }, flags = { 0 })
public class DevArtCm extends PluginForDecrypt {

    private boolean loggedIn = false;

    public DevArtCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * @author raztoki
     */

    /* This plugin grabs range of content depending on parameter. 
            profile.devart.com/gallery/uid*
            profile.devart.com/favorites/uid*
            profile.devart.com/gallery/*
            profile.devart.com/favorites/*
              * = ?offset=\\d+
                
                All of the above formats should support spanning pages, but when parameter contains '?offset=x' it will not span. 
               
            profilename.deviantart.com/art/uid/  == grabs the 'download image' (best quality available).

       I've created the plugin this way to allow users to grab as little or as much, content as they wish. Hopefully this wont create any issues.
     */

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
        // for stable compliance (getHost() will only return domain.tld and not
        // subdomain(s).domain.tld)
        String host = new Regex(br.getURL(), "(https?://.*?deviantart\\.com)/").getMatch(0);

        // only non /art/ requires packagename
        if (parameter.contains("/gallery/") || parameter.contains("/favourites/")) {
            // find and set username
            String username = br.getRegex("<h1>\\*<a class=\"u\" href=\"" + host + "\">(.*?)</a></h1>").getMatch(0);
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
                fp.addLinks(decryptedLinks);
            }
        }
        // art links just grab final link
        else if (parameter.contains("/art/")) {
            parseArtPage(decryptedLinks, parameter);
        }
        return decryptedLinks;
    }

    private void parseArtPage(ArrayList<DownloadLink> ret, String parameter) throws Exception {
        boolean ratedContent = false;
        String fileName = null;
        String[][] dllinks = null;
        // login if required, else don't
        if (br.containsHTML(">Mature Content Filter</a>")) {
            String artPageURL = br.getURL();
            login();
            br.getPage(artPageURL);
        }
        // define page as mature content
        if (br.containsHTML(">Mature Content</span>") && !br.containsHTML(">Mature Content Filter<")) {
            ratedContent = true;
        }
        String dllink = br.getRegex("id=\"download\\-button\" href=\"(https?://[\\w\\.\\-]*?deviantart.com/download/\\d+/.*?)\"").getMatch(0);
        if (dllink == null) {
            // fail over, when download isn't present
            fileName = br.getRegex("<title>(.+) on deviantART</title>").getMatch(0);
            if (fileName == null) fileName = br.getRegex("<meta name=\"title\" content=\"(.+) on deviantART\" />").getMatch(0);
            // find the largest Image
            String imgL = br.getRegex("(<img  name=\"gmi\\-ResViewSizer_fullimg\" .+ class=\"fullview smshadow\">)").getMatch(0);
            // regular image
            String imgR = br.getRegex("(<img  name=\"gmi\\-ResViewSizer_img\" .+ class=\"smshadow\" >)").getMatch(0);
            if (imgL != null)
                dllinks = new Regex(imgL, "src=\"([^\"]+(\\.[a-z]+))").getMatches();
            else
                dllinks = new Regex(imgR, "src=\"([^\"]+(\\.[a-z]+))").getMatches();
            if (dllinks == null) {
                logger.warning("Possible Plugin error, with finding download image: " + parameter);
                return;
            }
        }
        // dllink[]
        if (dllink != null) {
            DownloadLink dl = createDownloadlink("DEVART://" + dllink);
            dl.setProperty("ratedContent", ratedContent);
            ret.add(dl);
        }
        // dllinks[][]
        else if (dllinks != null && fileName != null) {
            DownloadLink dl = createDownloadlink("DEVART://" + dllinks[0][0]);
            dl.setFinalFileName(Encoding.htmlDecode(fileName + dllinks[0][1]).trim());
            dl.setProperty("ratedContent", ratedContent);
            ret.add(dl);
        } else {
            logger.warning("Error within parseArtPage");
            return;
        }
    }

    private void parsePage(ArrayList<DownloadLink> ret, String host, String parameter) throws Exception {
        String grab = br.getRegex("<div class=\"folderview\\-art\">(.*?)</div><div class=\"pagination\\-wrapper full\">").getMatch(0);
        String[] artlinks = new Regex(grab, "<a class=\"thumb([\\s\\w]+)?\" href=\"(https?://[\\w\\.\\-]*?deviantart\\.com/art/[\\w\\-]+)\"").getColumn(1);
        String nextPage = br.getRegex("<div class=\"pagination\\-wrapper full\">.*?<li class=\"next\"><a class=\"away\" href=\"(/(gallery|favourites)/(\\d+)?\\?offset=\\d+)\">Next</a>").getMatch(0);
        if (artlinks == null || artlinks.length == 0) {
            logger.warning("Possible Plugin error, with finding /art/ links: " + parameter);
            return;
        }
        if (artlinks != null && artlinks.length != 0) {
            for (String al : artlinks) {
                br.getPage(al);
                parseArtPage(ret, parameter);
            }
        }
        if (nextPage != null && !parameter.contains("?offset=")) {
            br.getPage(host + nextPage);
            parsePage(ret, host, parameter);
        }
    }

    private void login() throws Exception {
        boolean isNew = false;
        PluginForHost DeviantArtPlugin = JDUtilities.getPluginForHost("deviantart.com");
        Account aa = AccountController.getInstance().getValidAccount(DeviantArtPlugin);
        if (aa == null) {
            isNew = true;
            String username = UserIO.getInstance().requestInputDialog("Enter Loginname for deviantart.com :");
            if (username == null) throw new DecrypterException(JDL.L("plugins.decrypt.deviantart.nousername", "Username not entered!"));
            String password = UserIO.getInstance().requestInputDialog("Enter password for deviantart.com :");
            if (password == null) throw new DecrypterException(JDL.L("plugins.decrypt.deviantart.nopassword", "Password not entered!"));
            aa = new Account(username, password);
        }
        try {
            ((DeviantArtCom) DeviantArtPlugin).login(aa, this.br);
        } catch (final PluginException e) {
            aa.setEnabled(false);
            aa.setValid(false);
            throw new DecrypterException(JDL.L("plugins.decrypt.deviantart.invalidaccount", "Account is invalid!"));
        }
        if (isNew == true) AccountController.getInstance().addAccount(DeviantArtPlugin, aa);
    }

}