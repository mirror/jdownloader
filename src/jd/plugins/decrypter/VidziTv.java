//jDownloader - Downloadmanager
//Copyright (C) 2015  JD-Team support@jdownloader.org
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

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

/**
 *
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision: 20458 $", interfaceVersion = 2, names = { "vidzi.tv" }, urls = { "https?://(www\\.)?vidzi\\.tv/((vid)?embed\\-)?[a-z0-9]{12}" }, flags = { 0 })
public class VidziTv extends antiDDoSForDecrypt {

    public VidziTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        // add video link
        decryptedLinks.add(createDownloadlink(parameter.replace("vidzi.tv", "vidzidecrypted.tv")));
        // default is to only want video, so we can can return decrypted fast without the need to page get
        if (!this.getPluginConfig().getBooleanProperty(jd.plugins.hoster.VidziTv.DownloadSubTitles, jd.plugins.hoster.VidziTv.DownloadSubTitles_default)) {
            return decryptedLinks;
        }
        final PluginForHost plugin = JDUtilities.getPluginForHost("vidzi.tv");
        ((jd.plugins.hoster.VidziTv) plugin).setBrowser(br);
        ((jd.plugins.hoster.VidziTv) plugin).getPage(parameter);
        // title
        final String[] fileInfo = new String[3];
        ((jd.plugins.hoster.VidziTv) plugin).scanInfo(fileInfo);
        if (inValidate(fileInfo[0])) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // lets correct fileInfo[0]
        fileInfo[0] = fileInfo[0].trim();
        // files
        final String[] files = getJsonResultsFromArray(getJsonArray("tracks"));
        if (files != null) {
            for (final String file : files) {
                if (StringUtils.containsIgnoreCase(getJson(file, "kind"), "subtitles")) {
                    final String link = getJson(file, "file");
                    final DownloadLink dl = createDownloadlink("directhttp://" + link);
                    dl.setFinalFileName(fileInfo[0] + link.substring(link.lastIndexOf(".")));
                    decryptedLinks.add(dl);
                }
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fileInfo[0]));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}