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

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sketchfab.com" }, urls = { "https?://(?:www\\.)?sketchfab\\.com/(3d-models/[a-z0-9\\-]+\\-[a-f0-9]{32}|models/[a-f0-9]{32}/embed)" })
public class Sketchfab extends PluginForDecrypt {
    public Sketchfab(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_NORMAL = "https?://(?:www\\.)?sketchfab\\.com/3d-models/([a-z0-9\\-]+)\\-([a-f0-9]{32})";
    private static final String TYPE_EMBED  = "https?://(?:www\\.)?sketchfab\\.com/models/([a-f0-9]{32})/embed";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final String modelHash;
        if (param.getCryptedUrl().matches(TYPE_EMBED)) {
            modelHash = new Regex(param.getCryptedUrl(), TYPE_EMBED).getMatch(0);
        } else {
            modelHash = new Regex(param.getCryptedUrl(), TYPE_NORMAL).getMatch(1);
        }
        br.getPage("https://" + this.getHost() + "/models/" + modelHash + "/embed?autostart=1&internal=1&tracking=0&ui_ar=0&ui_infos=0&ui_snapshots=1&ui_stop=0&ui_theatre=1&ui_watermark=0");
        String fpName = br.getRegex("class=\"model-name__label\"[^>]*>([^<]+)</").getMatch(0);
        if (StringUtils.isEmpty(fpName)) {
            /* Fallback */
            fpName = modelHash;
        }
        final String archiveLink = br.getRegex("(http[^#;]+ile\\.osgjs\\.gz)").getMatch(0);
        if (archiveLink != null && archiveLink.length() > 0) {
            /* TODO: Check if this is still needed */
            String decodedLink = br.getURL(Encoding.htmlDecode(archiveLink)).toString();
            DownloadLink dl1 = createDownloadlink(decodedLink);
            decodedLink = br.getURL(Encoding.htmlDecode(archiveLink)).toString().replace("file.osgjs.gz", "model_file.bin.gz");
            DownloadLink dl2 = createDownloadlink(decodedLink);
            if (StringUtils.isNotEmpty(fpName)) {
                dl1.setFinalFileName(fpName + "_file.osgjs");
                dl2.setFinalFileName(fpName + "_model_file.bin");
            }
            ret.add(dl1);
            ret.add(dl2);
        }
        final String binzExtension = ".binz";
        String configData = br.getRegex("<div[^>]+id\\s*=\\s*\"js-dom-data-prefetched-data\"[^>]*><!--([^<]*)--></div>").getMatch(0);
        if (StringUtils.isNotEmpty(configData)) {
            configData = Encoding.htmlDecode(configData);
            int stringEnd = configData.indexOf("--></div>");
            if (stringEnd > 0) {
                configData = configData.substring(0, stringEnd);
            }
            final String[] links = HTMLParser.getHttpLinks(configData, null);
            if (links != null && links.length > 0) {
                for (String link : links) {
                    link = Encoding.htmlDecode(link);
                    if (!this.canHandle(link) && (link.contains(modelHash) || link.endsWith(".bin.gz") || link.endsWith(binzExtension))) {
                        ret.add(createDownloadlink(DirectHTTP.createURLForThisPlugin(link)));
                    }
                }
            }
        }
        // /* Search for .binz URLs */
        // final String[] links = HTMLParser.getHttpLinks(br.getRequest().getHtmlCode(), null);
        // if (links != null && links.length > 0) {
        // for (String link : links) {
        // if (!this.canHandle(link) && link.endsWith(".binz")) {
        // ret.add(createDownloadlink(DirectHTTP.createURLForThisPlugin(link)));
        // }
        // }
        // }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        fp.addLinks(ret);
        return ret;
    }
}