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
import jd.plugins.PluginForDecrypt;

/**
 * @author typek_pb
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, urls = { "http://[\\w\\.]*?avaxhome\\.ws/(ebooks|music|software|video|magazines|games|graphics|misc|hraphile)/.+" }, flags = { 0 }, names = { "avaxhome.ws" })
public class AvxHmeW extends PluginForDecrypt {

    public AvxHmeW(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink cryptedLink, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.clearCookies(getHost());
        br.getPage(cryptedLink.getCryptedUrl());

        // 1.st try: <a href="LINK" target="_blank" rel="nofollow"> but ignore
        // images/self site refs + imdb refs
        String[][] links = br.getRegex("<a href=\"(http(s)?://(?!(www[.]imdb[.]com|avaxhome[.]ws))[\\S&&[^<]]+?)\" target=\"_blank\" rel=\"nofollow\">(?!<img)").getMatches();
        if (null != links && 0 < links.length) {
            for (String[] link : links) {
                if (null != link && 0 < link.length && null != link[0] && 0 < link[0].length()) {
                    decryptedLinks.add(createDownloadlink(link[0]));
                }
            }
        }

        // try also LINK</br>, but ignore self site refs + imdb refs
        links = null;
        links = br.getRegex("(http(s)?://(?!(www[.]imdb[.]com|avaxhome[.]ws))[\\S&&[^<]]+?)<br/>").getMatches();
        if (null != links && 0 < links.length) {
            for (String[] link : links) {
                if (null != link && 0 < link.length && null != link[0] && 0 < link[0].length()) {
                    decryptedLinks.add(createDownloadlink(link[0]));
                }
            }
        }

        return decryptedLinks;
    }
}
