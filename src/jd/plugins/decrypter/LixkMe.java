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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "lixk.me" }, urls = { "http://(www\\.)?lixk\\.me/[A-Za-z0-9]+" }, flags = { 0 })
public class LixkMe extends PluginForDecrypt {

    public LixkMe(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.getURL().equals("http://lixk.me/")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String[] postIDs = br.getRegex("name=\"pg_goto\" value=\"(\\d+)\"").getColumn(0);
        boolean letsTry = false;
        // Just try if no links found. This way we can skip captcha, password
        // and additional stuff
        if (postIDs == null || postIDs.length == 0) letsTry = true;
        parameter = parameter.replace("www.", "");
        int maxLinks = 50;
        if (!letsTry) maxLinks = postIDs.length - 1;
        if (!letsTry) progress.setRange(postIDs.length);
        for (int i = 0; i <= maxLinks; i++) {
            br.postPage(parameter, "pg_goto=" + Integer.toString(i));
            String finallink = br.getRedirectLocation();
            if (finallink == null) finallink = br.getRegex("window\\.location = \"(.*?)\"").getMatch(0);
            if (finallink == null && decryptedLinks.size() == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            if (finallink == null) {
                logger.info("End probably reached, stopping...");
                break;
            }
            decryptedLinks.add(createDownloadlink(finallink));
            progress.increase(1);
        }
        return decryptedLinks;
    }
}
