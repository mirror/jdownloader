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
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "watchfree.com" }, urls = { "https?://(www\\.)?watchfree\\.com/video/.+\\.html" })
public class WatchFreeCom extends PluginForDecrypt {
    public WatchFreeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection() == null || br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("404 Not Found<|Page not found")) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        String iframe = br.getRegex("iframe src=(?:'|\")([^'\"]*?)(?:'|\")").getMatch(0);
        logger.info("iframe: " + iframe);
        if (iframe == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (iframe != null) {
            if (iframe.contains("watchfree")) {
                br.setFollowRedirects(true);
                br.getPage(iframe);
                iframe = br.getRegex("iframe.*?src=(?:'|\")([^'\"]*?)(?:'|\")").getMatch(0);
                logger.info("iframe: " + iframe);
                if (iframe != null && iframe.contains(".spankmasters.com")) {
                    iframe = iframe.replace(".spankmasters.com", "");
                }
            }
            decryptedLinks.add(createDownloadlink(iframe));
        }
        return decryptedLinks;
    }
}