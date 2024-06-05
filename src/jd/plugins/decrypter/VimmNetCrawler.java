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
import java.util.List;
import java.util.Map;

import org.appwork.storage.TypeRef;

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
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.VimmNet;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class VimmNetCrawler extends PluginForDecrypt {
    public VimmNetCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "vimm.net" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/vault/(?:\\?p=play\\&id=)?(\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        final String contentid = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        final String contenturl = "https://" + this.getHost() + "/vault/" + contentid;
        br.getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String jsonarrayStr = br.getRegex("var allMedia = (\\[.*?\\]);").getMatch(0);
        if (jsonarrayStr == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final List<Map<String, Object>> resourcelist = (List<Map<String, Object>>) restoreFromString(jsonarrayStr, TypeRef.OBJECT);
        final String downloadformatsText = br.getRegex("<select[^<]*id=\"dl_format\"[^<]*>(.*?)</select>").getMatch(0);
        if (downloadformatsText == null) {
            logger.info("Looks like there are no alternative formats available for this item");
        }
        final String[][] downloadformatsOptions = downloadformatsText != null ? new Regex(downloadformatsText, "<option[^<]*value=\"(\\d+)\"[^>]*>([^<]+)</option>").getMatches() : null;
        for (final Map<String, Object> resource : resourcelist) {
            final String mediaID = resource.get("ID").toString();
            final String preSetFilename = resource.get("GoodTitle").toString();
            final DownloadLink link = this.createDownloadlink(contenturl + "#?media_id=" + mediaID);
            link.setProperty(VimmNet.PROPERTY_MEDIA_ID, mediaID);
            link.setProperty(VimmNet.PROPERTY_PRE_GIVEN_FILENAME, preSetFilename);
            final String filesizeZippedStr = resource.get("Zipped").toString();
            final Object filesizeAltZippedStr = resource.get("AltZipped");
            if (filesizeZippedStr != null && filesizeZippedStr.matches("\\d+")) {
                link.setDownloadSize(Long.parseLong(filesizeZippedStr) * 1024);
            }
            /**
             * There are file-hashes available but it looks like those are for the files inside the .zip archives so we can't make use of
             * them. </br>
             * See fields GoodHash, GoodMd5, GoodSha1
             */
            if (filesizeAltZippedStr != null && resourcelist.size() == 1 && downloadformatsOptions != null && downloadformatsOptions.length >= 2) {
                /* Alternative version is available */
                /* Add format properties to first format */
                link.setProperty(VimmNet.PROPERTY_FORMAT_ID, downloadformatsOptions[0][0]);
                link.setProperty(VimmNet.PROPERTY_FORMAT, Encoding.htmlDecode(downloadformatsOptions[0][1]).trim());
                /* Add downloadlink for 2nd format */
                final DownloadLink link2 = this.createDownloadlink(contenturl + "#?media_id=" + mediaID);
                link2.setProperty(VimmNet.PROPERTY_MEDIA_ID, mediaID);
                link2.setProperty(VimmNet.PROPERTY_PRE_GIVEN_FILENAME, preSetFilename);
                link2.setProperty(VimmNet.PROPERTY_FORMAT_ID, downloadformatsOptions[1][0]);
                link2.setProperty(VimmNet.PROPERTY_FORMAT, Encoding.htmlDecode(downloadformatsOptions[1][1]).trim());
                link2.setDownloadSize(Long.parseLong(filesizeAltZippedStr.toString()) * 1024);
                ret.add(link2);
            }
            ret.add(link);
        }
        /* Set some additional properties which we want to have on all of our results. */
        for (final DownloadLink result : ret) {
            result.setAvailable(true);
            VimmNet.setFilename(result);
        }
        final String lastFilename = ret.get(ret.size() - 1).getStringProperty(VimmNet.PROPERTY_PRE_GIVEN_FILENAME);
        String title = br.getRegex("<title>([^<]+)</title>").getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        if (title != null) {
            title = Encoding.htmlOnlyDecode(title).trim();
            title = title.replaceFirst("(?i)^Download\\s*", "");
            fp.setName(title);
        } else if (lastFilename != null) {
            /* Fallback 1 */
            fp.setName(lastFilename);
        } else {
            /* Fallback 2 */
            fp.setName(contentid);
        }
        /* We want all results to go into this package. */
        fp.addLinks(ret);
        return ret;
    }
}
