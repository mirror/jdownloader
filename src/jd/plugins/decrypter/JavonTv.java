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
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "javon.tv" }, urls = { "http://(www\\.)?javon\\.tv/watch/[a-z0-9\\-]+/video/\\d+/[a-z0-9\\-]+\\.html" }, flags = { 0 })
public class JavonTv extends PluginForDecrypt {

    public JavonTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        br.setFollowRedirects(true);
        if (br.getHttpConnection().getResponseCode() == 404) {
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setFinalFileName(new Regex(parameter, "https?://[^<>\"/]+/(.+)").getMatch(0));
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        final String redirect = br.getRedirectLocation();
        if (redirect != null && !redirect.contains("javon.tv/")) {
            decryptedLinks.add(createDownloadlink(redirect));
            return decryptedLinks;
        } else if (redirect != null) {
            br.getPage(redirect);
        }
        final String iframe = br.getRegex("(<iframe width.*?</iframe>)").getMatch(0);
        if (iframe == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final String finallink = new Regex(iframe, "src=\"(https?://[^<>\"]*?)\"").getMatch(0);
        if (finallink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));

        return decryptedLinks;
    }

}
