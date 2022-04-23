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
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "one.akwam.cc" }, urls = { "https?://one.akwam.cc/(?!download)[^/]+/(?:episode/)?\\d+/.*" })
public class AkwamCc extends PluginForDecrypt {
    public AkwamCc(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String page = br.getPage(param.getCryptedUrl());
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(new Regex(page, "<title>(.*)</title>").getMatch(0).replace(" | اكوام", ""));
        String[][] links;
        if (page.contains("series-episodes") || page.contains("show-episodes")) {
            String bulkHtml = new Regex(page, "(id=\"(?:series|show)-episodes\"[\\s\\S]+widget-4)").getMatch(0);
            links = new Regex(bulkHtml, "<a href=\"(https://one.akwam.cc/[^\"]+)\"").getMatches();
        } else {
            links = new Regex(page, "<a href=\"([^\"]+)\"[^>]+link-download").getMatches();
        }
        for (final String[] link : links) {
            String finalLink = link[0];
            if (!page.contains("series-episodes") && !page.contains("show-episodes")) {
                finalLink = "https://" + br.getHost(true) + "/download/" + new Regex(link[0], "link/(\\d+)").getMatch(0) + "/" + new Regex(param.getCryptedUrl(), "https?://one.akwam.cc/[^/]+/(?:episode/)?(\\d+)/.*").getMatch(0);
            }
            final DownloadLink dl = createDownloadlink(finalLink);
            dl._setFilePackage(fp);
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }
}
