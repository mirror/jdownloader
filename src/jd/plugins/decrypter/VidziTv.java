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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.JDUtilities;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

/**
 *
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vidzi.tv" }, urls = { "https?://(www\\.)?vidzi\\.(tv|cc|si)/((vid)?embed\\-)?[a-z0-9]{12}" })
public class VidziTv extends antiDDoSForDecrypt {
    public VidziTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace(".cc", ".si").replace(".tv", ".si");
        // add video link
        decryptedLinks.add(createDownloadlink(parameter.replace("vidzi.si", "vidzidecrypted.tv")));
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
        String subtitlesource = null;
        final String cryptedScripts[] = br.getRegex("p\\}\\((.*?)\\.split\\('\\|'\\)").getColumn(0);
        if (cryptedScripts != null && cryptedScripts.length != 0) {
            for (String crypted : cryptedScripts) {
                subtitlesource = getSubtitleSource(crypted);
                if (subtitlesource != null) {
                    break;
                }
            }
        }
        // files
        final String[] files = PluginJSonUtils.getJsonResultsFromArray(PluginJSonUtils.getJsonArray(subtitlesource, "tracks"));
        if (files != null) {
            for (final String file : files) {
                if (StringUtils.containsIgnoreCase(PluginJSonUtils.getJsonValue(file, "kind"), "subtitles")) {
                    final String link = PluginJSonUtils.getJsonValue(file, "file");
                    if (link != null) {
                        final DownloadLink dl = createDownloadlink("directhttp://" + link);
                        dl.setFinalFileName(fileInfo[0] + link.substring(link.lastIndexOf(".")));
                        decryptedLinks.add(dl);
                    }
                }
            }
        }
        // only add packagename when one isn't already set. otherwise you get multiple packages just because this provider has its own
        // decrypter plugin.
        if (this.getCurrentLink().getSourceLink().getDesiredPackageInfo() == null || this.getCurrentLink().getSourceLink().getDesiredPackageInfo().getName() == null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fileInfo[0]));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private String getSubtitleSource(final String s) {
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
        return decoded;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}