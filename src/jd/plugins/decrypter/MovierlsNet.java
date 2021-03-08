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
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "movierls.net" }, urls = { "https?://dl\\.movierls\\.net/[a-z0-9]+" })
public class MovierlsNet extends PluginForDecrypt {
    public MovierlsNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String[] links = br.getRegex("(/downloadl/[^<>\"\\']+)").getColumn(0);
        if (links.length > 0) {
            final Browser brc = br.cloneBrowser();
            brc.setFollowRedirects(false);
            for (final String singleLink : links) {
                String finallink = singleLink;
                int counter = 0;
                do {
                    brc.getPage(finallink);
                    finallink = brc.getRedirectLocation();
                    counter++;
                } while (!this.isAbort() && counter <= 3 && finallink != null && (finallink.contains(this.getHost() + "/") || finallink.startsWith("/")));
                if (finallink == null || finallink.contains(this.getHost() + "/")) {
                    if (finallink != null) {
                        logger.info("Skipping URL: " + finallink);
                    }
                    continue;
                }
                decryptedLinks.add(createDownloadlink(finallink));
            }
        } else {
            logger.info("Failed to find any redirectURLs -> Adding plain URLs");
            final String[] urls = HTMLParser.getHttpLinks(br.toString(), br.getURL());
            for (final String url : urls) {
                if (!this.canHandle(url)) {
                    decryptedLinks.add(this.createDownloadlink(url));
                }
            }
        }
        return decryptedLinks;
    }
}
