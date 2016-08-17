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
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hqtubexxx.com" }, urls = { "http://(www\\.)?hqtubexxx\\.com/[a-z0-9\\-]+\\-\\d+\\.html" }) 
public class HqTubeXxxCom extends PornEmbedParser {

    public HqTubeXxxCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    // This is a site which shows embedded videos of other sites so we may have
    // to add regexes/handlings here
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        try {
            br.getPage(parameter);
        } catch (final Exception e) {
            logger.info("Received server error for link: " + parameter);
            decryptedLinks.add(createOfflinelink(parameter, "Server Error"));
            return decryptedLinks;
        }
        String filename = br.getRegex("<h1 class=\"name\">([^<>\"\\']+)</h1>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>([^<>\"\\']+) \\-    </title>").getMatch(0);
        }
        decryptedLinks.addAll(findEmbedUrls(filename));
        if (decryptedLinks.isEmpty()) {
            return null;
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}