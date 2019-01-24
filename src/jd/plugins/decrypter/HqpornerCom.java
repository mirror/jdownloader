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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hqporner.com" }, urls = { "https?://(?:www\\.)?hqporner\\.com/hdporn/\\d+\\-([^/]+)\\.html" })
public class HqpornerCom extends PornEmbedParser {
    public HqpornerCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    /* Porn_plugin */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        this.br.setFollowRedirects(true);
        /* 2019-01-24: Important! some URLs will just display 404 if Referer is not set! */
        br.getHeaders().put("Referer", "https://" + this.getHost() + "/");
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String url_name = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        String filename = br.getRegex("<h1 class=\"main\\-h1\" style=\"line\\-height: 1em;\">\\s*?([^<>\"]+)</h1>").getMatch(0);
        if (filename == null) {
            /* Fallback */
            filename = url_name;
        }
        decryptedLinks.addAll(findEmbedUrls(filename));
        return decryptedLinks;
    }
}
