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
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "gamestar.de" }, urls = { "https?://(www\\.)?gamestar\\.de/videos/.+" })
public class GmstrD extends PluginForDecrypt {

    public GmstrD(PluginWrapper wrapper) {
        super(wrapper);
        Browser.setRequestIntervalLimitGlobal(getHost(), 250);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(param.getCryptedUrl());
        String videoURLs[] = br.getRegex("'file'\\s*:\\s*'(/videos/media.*?)'").getColumn(0);
        if (videoURLs == null || videoURLs.length == 0) {
            videoURLs = br.getRegex("\"file\"\\s*:\\s*\"(/videos/media.*?)\"").getColumn(0);
        }
        if (videoURLs != null) {
            final String title = br.getRegex("og:title\"\\s*content=\"(.*?)\"").getMatch(0);
            for (final String videoURL : videoURLs) {
                br.setFollowRedirects(false);
                br.getPage(videoURL);
                final String url = br.getRedirectLocation();
                if (url != null) {
                    decryptedLinks.add(createDownloadlink(url));
                } else {
                    final String contentURL = br.getRegex("content url=\"(https?://.*?)\"\\s*type=\"video").getMatch(0);
                    if (contentURL != null) {
                        decryptedLinks.add(createDownloadlink("directhttp://" + contentURL));
                    }
                }
            }
            if (title != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(title);
                fp.addLinks(decryptedLinks);
            }
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}