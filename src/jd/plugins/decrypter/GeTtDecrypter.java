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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ge.tt" }, urls = { "http://(www\\.)?ge\\.tt/(?!developers|press|tools|notifications|blog|about|javascript|button|contact|terms|api)#?[A-Za-z0-9]+(/v/\\d+)?" }, flags = { 0 })
public class GeTtDecrypter extends PluginForDecrypt {

    public GeTtDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        String parameter = param.toString().replace("#", "");

        br.getPage(parameter);

        if (br.containsHTML("Page not found|The page you were looking for was not found|Files removed|These files have been removed by the owner")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }

        String singleFile = new Regex(parameter, "/v/(\\d+)").getMatch(0);
        String fid = new Regex(parameter, "ge\\.tt/([A-Za-z0-9]+)(/v/\\d+)?").getMatch(0);
        br.getPage("http://open.ge.tt/1/shares/" + fid);

        if (br.containsHTML("\"error\":\"share not found\"") || br.containsHTML(">404 Not Found<")) {
            final DownloadLink dlink = createDownloadlink("http://open.ge.tt/1/files/" + fid + "/0/blob");
            dlink.setAvailable(false);
            decryptedLinks.add(dlink);
            return decryptedLinks;
        }

        for (String id : br.getRegex("\"fileid\":\"(\\d+)\"").getColumn(0)) {
            if (singleFile != null) id = singleFile;
            decryptedLinks.add(createDownloadlink("http://open.ge.tt/1/files/" + fid + "/" + id + "/blob"));
            if (singleFile != null) break;
        }

        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            if (br.getHttpConnection().getResponseCode() == 200) {
                logger.info("ge.tt: Share is empty! Link: " + parameter);
                return decryptedLinks;
            }
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

}