//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "multiupload.com" }, urls = { "http://(www\\.)?multiupload\\.(com|nl)/([A-Z0-9]{2}_[A-Z0-9]+|[0-9A-Z]+)" }, flags = { 0 })
public class MltpldCm extends PluginForDecrypt {

    public MltpldCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.getRedirectLocation() != null) {
            /* domain redirection */
            br.getPage(br.getRedirectLocation());
        }
        if (br.containsHTML(">Unfortunately, the link you have clicked is not available")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML(">UNKNOWN ERROR<")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (!parameter.contains("_")) {
            final String[] redirectLinks = br.getRegex(Pattern.compile("id=\"url_\\d+\"><a href=\"(http://(www\\.)?multiupload\\.(com|nl)/.*?)\"")).getColumn(0);
            if (redirectLinks == null || redirectLinks.length == 0) {
                logger.warning("redirectLinks list is null...");
                return null;
            }
            for (final String redirectLink : redirectLinks) {
                br.getPage(redirectLink);
                String finallink = br.getRedirectLocation();
                if (finallink == null) {
                    continue;
                }
                if (finallink.contains("mediafire")) finallink = finallink.replace("mediafire.com?", "mediafire.com/?");
                decryptedLinks.add(createDownloadlink(finallink));
            }
        } else {
            String finallink = br.getRedirectLocation();
            if (finallink == null) return null;
            decryptedLinks.add(createDownloadlink(finallink));
        }

        return decryptedLinks;
    }
}
