//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mirrorcreator.com" }, urls = { "http://(www\\.)?(mirrorcreator\\.com/files|mir\\.cr)/[0-9A-Z]{8}" }, flags = { 0 })
public class MirrorCreatorCom extends PluginForDecrypt {

    private String userAgent = null;

    public MirrorCreatorCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (userAgent == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            userAgent = jd.plugins.hoster.MediafireCom.stringUserAgent();
        }
        br.getHeaders().put("User-Agent", userAgent);
        br.setFollowRedirects(false);
        String parameter = param.toString().replace("mir.cr/", "mirrorcreator.com/files/").replaceAll("(http://|www\\.)", "");
        // Those links need a "/" at the end to be valid
        if (!parameter.endsWith("/")) parameter += "/";
        parameter = "http://www." + parameter;
        param.setCryptedUrl(parameter);

        String uid = new Regex(parameter, "/([A-Z0-9]{8})/").getMatch(0);

        br.getPage(parameter);
        /* Error handling */
        if (br.containsHTML("(>Unfortunately, the link you have clicked is not available|>Error \\- Link disabled or is invalid|>Links Unavailable as the File Belongs to Suspended Account\\. <)")) {
            logger.info("The following link should be offline: " + param.toString());
            return decryptedLinks;
        }

        // they comment in fakes, so we will just try them all!
        String[] links = br.getRegex("(/[^<>\"/]*?=[a-z0-9]{25,32})\"").getColumn(0);
        if (links == null || links.length == 0) {
            links = br.getRegex("\"(/[^\"]+uid=" + uid + "[^\"]+)\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("A critical error happened! Please inform the support. : " + param.toString());
                return null;
            }
        }
        for (String link : links) {
            Browser br2 = br.cloneBrowser();
            br2.getPage(link);
            String[] redirectLinks = br2.getRegex("(/[^/]+/" + uid + "/[^\"]+)").getColumn(0);
            if (redirectLinks == null || redirectLinks.length == 0) {
                redirectLinks = br2.getRegex("><a href=(.*?)target=").getColumn(0);
                if (redirectLinks == null || redirectLinks.length == 0) {
                    logger.warning("Scanning for redirectLinks, possible change in html, continuing...");
                    continue;
                }
            }
            if (redirectLinks == null || redirectLinks.length == 0) {
                logger.warning("Excausted redirect search, Please report issue to JDownloader Development team. : " + param.toString());
                return null;
            }
            logger.info("Found " + redirectLinks.length + " " + this.getHost() + " links to decrypt...");
            for (String singlelink : redirectLinks) {
                singlelink = singlelink.replace("\"", "").replace(" ", "");
                br2 = br.cloneBrowser();
                String dllink = null;
                // Handling for links that need to be regexed or that need to be get by redirect
                if (singlelink.matches("/[^/]+/.*?" + uid + ".*?/.*?")) {
                    br2.getPage(singlelink.trim());
                    dllink = br2.getRedirectLocation();
                    if (dllink == null) {
                        dllink = br2.getRegex("Please <a href=(\"|\\')?(http.*?)(\"|\\')? ").getMatch(1);
                        if (dllink == null) dllink = br2.getRegex("redirecturl\">(https?://[^<>]+)</div>").getMatch(0);
                    }
                } else {
                    // Handling for already regexed final-links
                    dllink = singlelink;
                }
                if (dllink == null || dllink.equals("")) {
                    // Continue away, randomised pages can cause failures.
                    logger.warning("Possible plugin error: " + param.toString());
                    logger.warning("Continuing...");
                    continue;
                }
                decryptedLinks.add(createDownloadlink(dllink));
            }
        }
        logger.warning("Task Complete! : " + param.toString());
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}