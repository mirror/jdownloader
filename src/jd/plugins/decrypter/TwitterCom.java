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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "twitter.com" }, urls = { "https?://(www\\.)?twitter\\.com/[A-Za-z0-9_\\-]+/media" }, flags = { 0 })
public class TwitterCom extends PluginForDecrypt {

    public TwitterCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("http://", "https://");
        br.getPage(parameter);
        if (br.getRequest().getHttpConnection().getResponseCode() == 404) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        final String user = new Regex(parameter, "twitter\\.com/([A-Za-z0-9_\\-]+)/").getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(user);
        int reloadNumber = 1;
        String maxid = br.getRegex("data\\-max\\-id=\"(\\d+)\"").getMatch(0);
        do {
            try {
                if (this.isAbort()) {
                    logger.info("Decryption aborted at reload " + reloadNumber);
                    return decryptedLinks;
                }
            } catch (final Throwable e) {
                // Not available in 0.9.581
            }
            logger.info("Decrypting reloadnumber " + reloadNumber + ", found " + decryptedLinks.size() + " links till now");
            if (reloadNumber > 1) {
                maxid = br.getRegex("\"max_id\":\"(\\d+)").getMatch(0);
            }
            if (maxid == null) {
                logger.info("Either there is nothing to decrypt or the decrypter is broken: " + parameter);
                return decryptedLinks;
            }
            int addedlinks_all = 0;
            final String[] embedurl_regexes = new String[] { "\"(https?://(www\\.)?(youtu\\.be/|youtube\\.com/embed/)[A-Za-z0-9\\-_]+)\"", "data\\-expanded\\-url=\"(https?://vine\\.co/v/[A-Za-z0-9]+)\"" };
            for (final String regex : embedurl_regexes) {
                final String[] embed_links = br.getRegex(regex).getColumn(0);
                if (embed_links != null && embed_links.length != 0) {
                    for (final String single_embed_ink : embed_links) {
                        final DownloadLink dl = createDownloadlink(single_embed_ink);
                        dl._setFilePackage(fp);
                        try {
                            distribute(dl);
                        } catch (final Throwable e) {
                            // Not available in 0.9.581
                        }
                        decryptedLinks.add(dl);
                        addedlinks_all++;
                    }
                }
            }

            final String[] directlink_regexes = new String[] { "data\\-url=\\&quot;(https?://[a-z0-9]+\\.twimg\\.com/[^<>\"]*?\\.(jpg|png|gif):large)\\&", "data\\-url=\"(https?://[a-z0-9]+\\.twimg\\.com/[^<>\"]*?)\"" };
            for (final String regex : directlink_regexes) {
                final String[] piclinks = br.getRegex(regex).getColumn(0);
                if (piclinks != null && piclinks.length != 0) {
                    for (final String singleLink : piclinks) {
                        final DownloadLink dl = createDownloadlink("directhttp://" + Encoding.htmlDecode(singleLink.trim()));
                        dl._setFilePackage(fp);
                        dl.setAvailable(true);
                        try {
                            distribute(dl);
                        } catch (final Throwable e) {
                            // Not available in 0.9.581
                        }
                        decryptedLinks.add(dl);
                        addedlinks_all++;
                    }
                }
            }
            if (addedlinks_all == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            br.getPage("https://twitter.com/i/profiles/show/" + user + "/media_timeline?include_available_features=1&include_entities=1&max_id=" + maxid);
            br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
            reloadNumber++;
        } while (br.containsHTML("\"has_more_items\":true"));

        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

}
