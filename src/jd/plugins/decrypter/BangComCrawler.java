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
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.BangCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class BangComCrawler extends PluginForDecrypt {
    public BangComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "bang.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/video/([A-Za-z0-9]+)/([a-z0-9\\-]+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            logger.info("This plugin is still under development!");
            return ret;
        }
        br.setFollowRedirects(true);
        final BangCom plg = (BangCom) this.getNewPluginForHostInstance(this.getHost());
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        if (account != null) {
            plg.login(account, false);
        }
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        Map<String, Object> videoObject = null;
        final String[] jsSnippets = br.getRegex("<script type=\"application/ld\\+json\">(.*?)</script>").getColumn(0);
        for (final String jsSnippet : jsSnippets) {
            final Map<String, Object> entries = restoreFromString(jsSnippet, TypeRef.MAP);
            final String type = (String) entries.get("@type");
            if (StringUtils.equalsIgnoreCase(type, "VideoObject")) {
                videoObject = entries;
                break;
            }
        }
        if (videoObject == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String contentID = videoObject.get("@id").toString();
        final String title = videoObject.get("name").toString();
        final String thumbnailUrl = videoObject.get("thumbnailUrl").toString();
        final String previewURL = videoObject.get("contentUrl").toString();
        final String description = (String) videoObject.get("description");
        final String photosAsZipURL = br.getRegex("\"(https?://photos\\.[^/]+/\\.zip[^\"]+)\"").getMatch(0);
        if (thumbnailUrl != null) {
            final DownloadLink thumb = new DownloadLink(plg, null, this.getHost(), thumbnailUrl, true);
            thumb.setProperty(BangCom.PROPERTY_QUALITY_IDENTIFIER, "THUMBNAIL");
            ret.add(thumb);
        }
        if (previewURL != null) {
            final DownloadLink preview = new DownloadLink(plg, null, this.getHost(), previewURL, true);
            preview.setProperty(BangCom.PROPERTY_QUALITY_IDENTIFIER, "PREVIEW");
            ret.add(preview);
        }
        if (photosAsZipURL != null) {
            final DownloadLink zip = new DownloadLink(plg, null, this.getHost(), photosAsZipURL, true);
            zip.setProperty(BangCom.PROPERTY_QUALITY_IDENTIFIER, "ZIP");
            ret.add(zip);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        if (!StringUtils.isEmpty(description)) {
            fp.setComment(description);
        }
        for (final DownloadLink result : ret) {
            result.setProperty(BangCom.PROPERTY_CONTENT_ID, contentID);
            result.setAvailable(true);
        }
        fp.addLinks(ret);
        return ret;
    }
}
