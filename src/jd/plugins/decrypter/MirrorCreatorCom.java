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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mirrorcreator.com" }, urls = { "http://[\\w\\.]*?mirrorcreator\\.com/files/[0-9A-Z]{8}" }, flags = { 0 })
public class MirrorCreatorCom extends PluginForDecrypt {

    public MirrorCreatorCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        // Tohse links need a "/" at the end to be valid
        if (!param.getCryptedUrl().endsWith("/")) param.setCryptedUrl(param.getCryptedUrl().toString() + "/");
        String parameter = param.toString();
        String host = new Regex(parameter, "(.+)/files").getMatch(0);
        String id = new Regex(parameter, "files/([0-9A-Z]+)").getMatch(0);
        // This should never happen but in case a dev changes the plugin without
        // much testing he ll see the error later!
        if (host == null || id == null) {
            logger.warning("A critical error happened! Please inform the support.");
            return null;
        }
        parameter = host + "/status.php?uid=" + id;
        br.getPage(parameter);
        /* Error handling */
        if (!br.containsHTML("<img src=") && !br.containsHTML("<td class=\"host\">")) {
            logger.info("The following link should be offline: " + parameter);
            throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        }
        String[] redirectLinks = br.getRegex("(/redirect/[0-9A-Z]+/[a-z0-9]+)").getColumn(0);
        if (redirectLinks == null || redirectLinks.length == 0) redirectLinks = br.getRegex("><a href=(.*?)target=").getColumn(0);
        if (redirectLinks == null || redirectLinks.length == 0) return null;
        progress.setRange(redirectLinks.length);
        String nicelookinghost = host.replaceAll("(www\\.|http://|/)", "");
        logger.info("Found " + redirectLinks.length + " " + nicelookinghost + " links to decrypt...");
        for (String singlelink : redirectLinks) {
            String dllink = null;
            // Handling for links that need to be regexed or that need to be get
            // by redirect
            if (singlelink.contains("/redirect/")) {
                br.getPage(host + singlelink);
                dllink = br.getRedirectLocation();
                if (dllink == null) dllink = br.getRegex("<frame name=\"main\" src=\"(.*?)\">").getMatch(0);
            } else {
                // Handling for already regexed final-links
                dllink = singlelink;
            }
            if (dllink == null) {
                logger.warning("Decrypter for this link is broken: " + parameter);
                return null;
            }
            if (dllink.equals("")) logger.info("Found one broken link!");
            decryptedLinks.add(createDownloadlink(dllink));
            progress.increase(1);
        }

        return decryptedLinks;
    }

}