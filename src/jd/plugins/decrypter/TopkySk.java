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
 * Only direct links are suported + those we have plugins for. => for example
 * mtv doesn't work.
 * 
 * following have been tested: youtube.com, img.zoznam.sk
 * 
 * @author butkovip
 * 
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, urls = { "http://[\\w\\.]*?topky\\.sk/cl/[0-9]+/[0-9]+/VIDEO-[-a-zA-Z0-9]+" }, flags = { 0 }, names = { "topky.sk" })
public class TopkySk extends PluginForDecrypt {

    public TopkySk(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink cryptedLink, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.clearCookies(getHost());
        br.getPage(cryptedLink.getCryptedUrl());

        // extract img.zoznam.sk like vids
        String[][] links = br.getRegex("fo.addVariable[(]\"file\", \"(.*?)\"[)]").getMatches();
        if (null != links && 0 < links.length) {
            for (String[] link : links) {
                if (null != link && 1 == link.length && null != link[0] && 0 < link[0].length()) {
                    decryptedLinks.add(createDownloadlink(link[0]));
                }
            }
        }

        // extract youtube links
        links = br.getRegex("<PARAM NAME=\"movie\" VALUE=\"http://www.youtube.com/v/(.*?)&").getMatches();
        if (null != links && 0 < links.length) {
            for (String[] link : links) {
                if (null != link && 1 == link.length && null != link[0] && 0 < link[0].length()) {
                    decryptedLinks.add(createDownloadlink("http://www.youtube.com/watch?v=" + link[0] + "&feature=player_embedded"));
                }
            }
        }

        return decryptedLinks;
    }
}
