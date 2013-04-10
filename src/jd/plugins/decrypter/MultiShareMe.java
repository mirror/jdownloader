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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "multishare.me" }, urls = { "http://(www\\.)?multishare\\.me/files/get/.+" }, flags = { 0 })
public class MultiShareMe extends PluginForDecrypt {

    public MultiShareMe(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = param.toString();
        String host = new Regex(parameter, "(.+)/(files|dl)").getMatch(0);
        br.getPage(parameter);
        /* Error handling */
        if (br.containsHTML("(File Link Error<|Your file could not be found\\. Please check the download link\\.<)")) {
            logger.info("The following link should be offline: " + parameter);
            throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        }
        String[] redirectLinks = br.getRegex("(/(redirect|rd)/[0-9A-Z]+/[a-z0-9]+)").getColumn(0);
        if (redirectLinks == null || redirectLinks.length == 0) redirectLinks = br.getRegex("><a href=(.*?)target=").getColumn(0);
        if (redirectLinks == null || redirectLinks.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        String nicelookinghost = host.replaceAll("(www\\.|http://|/)", "");
        logger.info("Found " + redirectLinks.length + " " + nicelookinghost + " links to decrypt...");
        for (String singlelink : redirectLinks) {
            String dllink = null;
            // Handling for links that need to be regexed or that need to be get
            // by redirect
            if (singlelink.contains("/redirect/") || singlelink.contains("/rd/")) {
                br.getPage(host + "/files" + singlelink);
                dllink = br.getRedirectLocation();
                if (dllink == null) {
                    dllink = br.getRegex("<frame name=\"main\" src=\"(.*?)\">").getMatch(0);
                    if (dllink == null) dllink = br.getRegex("http-equiv=\"refresh\" content=\"\\d+; URL=(.*?)\"").getMatch(0);
                }
            } else {
                // Handling for already regexed final-links
                dllink = singlelink;
            }
            if (dllink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            if (dllink.matches("")) logger.info("Found one broken link!");
            decryptedLinks.add(createDownloadlink(dllink));
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}