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
import java.util.LinkedHashMap;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "telequebec.tv" }, urls = { "https?://(?:[A-Za-z0-9]+\\.)?telequebec\\.tv/media/(\\d+)/[a-z0-9\\-]+/[a-z0-9\\-]+" })
public class TelequebecTv extends PluginForDecrypt {
    public TelequebecTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String mediaID = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        br.getPage("https://mnmedias.api.telequebec.tv/api/v2/player/" + mediaID);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        // final boolean canPlay = ((Boolean) entries.get("canPlay")).booleanValue();
        // if (!canPlay) {
        // logger.info("Possibly GEO-blocked");
        // }
        final String status = (String) entries.get("status");
        if (!"Available".equalsIgnoreCase(status)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String title = (String) entries.get("fileName");
        if (StringUtils.isEmpty(title)) {
            title = mediaID;
        }
        br.getPage("https://mnmedias.api.telequebec.tv/m3u8/" + mediaID + ".m3u8");
        // final List<HlsContainer> allHlsContainers = HlsContainer.getHlsQualities(br);
        // for (final HlsContainer hlscontainer : allHlsContainers) {
        // if (!hlscontainer.isVideo()) {
        // /* Skip audio containers here as we (sometimes) have separate mp3 URLs for this host. */
        // continue;
        // }
        // final String filename = title + "_" + hlscontainer.getStandardFilename();
        // final DownloadLink dl = this.createDownloadlink(hlscontainer.getDownloadurl());
        // dl.setFinalFileName(filename);
        // // dl.setAvailable(true);
        // decryptedLinks.add(dl);
        // }
        final ArrayList<DownloadLink> ret = GenericM3u8Decrypter.parseM3U8(this, param.getCryptedUrl(), br, param.getCryptedUrl(), null, null, title);
        String fpName = title;
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return ret;
    }
}
