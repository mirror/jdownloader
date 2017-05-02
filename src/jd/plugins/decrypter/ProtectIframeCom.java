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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "protect-iframe.com" }, urls = { "https?://(?:www\\.)?protect\\-iframe\\.com/embed\\-[a-z0-9]+" })
public class ProtectIframeCom extends PluginForDecrypt {

    public ProtectIframeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String fid = new Regex(parameter, "([a-z0-9]+)$").getMatch(0);
        br.getPage(parameter);
        // br.getPage("http://www.iframe-secure.com/embed/iframe.php?u=" + fid);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }

        String finallink = null;
        final String cryptedScripts[] = this.br.getRegex("p\\}\\((.*?)\\.split\\('\\|'\\)").getColumn(0);
        if (cryptedScripts != null && cryptedScripts.length != 0) {
            for (String crypted : cryptedScripts) {
                finallink = decodeDownloadLink(crypted);
                if (finallink != null) {
                    break;
                }
            }
        }

        if (finallink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));

        return decryptedLinks;
    }

    private String decodeDownloadLink(final String s) {
        String decoded = null;

        try {
            Regex params = new Regex(s, "\\'(.*?[^\\\\])\\',(\\d+),(\\d+),\\'(.*?)\\'");

            String p = params.getMatch(0).replaceAll("\\\\", "");
            int a = Integer.parseInt(params.getMatch(1));
            int c = Integer.parseInt(params.getMatch(2));
            String[] k = params.getMatch(3).split("\\|");

            while (c != 0) {
                c--;
                if (k[c].length() != 0) {
                    p = p.replaceAll("\\b" + Integer.toString(c, a) + "\\b", k[c]);
                }
            }

            decoded = p;
        } catch (Exception e) {
        }

        String finallink = null;
        if (decoded != null) {
            /* Open regex is possible because in the unpacked JS there are usually only 1 links */
            finallink = new Regex(decoded, "(?:\"|\\')(https?://[^<>\"\\']*?\\.(avi|flv|mkv|mp4))(?:\"|\\')").getMatch(0);
            if (finallink == null) {
                /* Maybe rtmp */
                finallink = new Regex(decoded, "(?:\"|\\')(rtmp://[^<>\"\\']*?mp4:[^<>\"\\']+)(?:\"|\\')").getMatch(0);
            }
        } else {
            final String olid = new Regex(s, "\\|([A-Za-z0-9]+)\\|location").getMatch(0);
            if (olid != null) {
                finallink = "https://openload.co/embed/" + olid;
            }
        }
        return finallink;
    }

}
