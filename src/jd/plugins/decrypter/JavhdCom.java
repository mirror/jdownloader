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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.utils.Regex;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "javhd.com" }, urls = { "https?://(?:www\\.)?javhd\\.com/[a-z]{2}/id/(\\d+)/([a-z0-9\\-]+)" })
public class JavhdCom extends PluginForDecrypt {
    public JavhdCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String fpName = br.getRegex("<title>([^<>\"]+)</title>").getMatch(0);
        if (fpName == null) {
            /* Fallback */
            fpName = new Regex(parameter, this.getSupportedLinks()).getMatch(1);
        }
        final String picsJson = br.getRegex(":images=\"([^\"]+)").getMatch(0);
        if (picsJson != null) {
            final List<Map<String, Object>> pics = (List<Map<String, Object>>) JavaScriptEngineFactory.jsonToJavaObject(Encoding.htmlDecode(picsJson));
            for (final Map<String, Object> pic : pics) {
                final String url = (String) pic.get("src");
                final DownloadLink dl = createDownloadlink(url);
                String filename = fpName;
                final String name_url = getFileNameFromURL(new URL(url));
                if (name_url != null) {
                    filename += "_" + name_url;
                } else {
                    /* Assume fileextension */
                    filename += ".jpg";
                }
                dl.setAvailable(true);
                dl.setFinalFileName(filename);
                decryptedLinks.add(dl);
            }
        }
        /* Add main URL for hosterplugin to download video */
        decryptedLinks.add(this.createDownloadlink(parameter));
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }
}
