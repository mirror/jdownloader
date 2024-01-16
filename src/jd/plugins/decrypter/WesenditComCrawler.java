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
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.WesenditCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class WesenditComCrawler extends PluginForDecrypt {
    public WesenditComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        prepBR(br);
        return br;
    }

    public static Browser prepBR(final Browser br) {
        br.setFollowRedirects(true);
        br.getHeaders().put("Accept", "application/json, text/plain, */*");
        br.getHeaders().put("Content-Type", "application/json");
        br.getHeaders().put("Origin", "https://www.wesendit.com");
        br.getHeaders().put("Referer", "https://www.wesendit.com/");
        return br;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "wesendit.com", "w-si.link" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(dl/)?([A-Za-z0-9]+)(/([A-Za-z0-9]+))?");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String folderID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(1);
        final String recipientID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(3);
        br.postPageRaw("https://api-prod.wesendit.com/web2/api/files/transfers/public/details", "{\"publicId\":\"" + folderID + "\"}");
        if (br.getHttpConnection().getResponseCode() == 404) {
            /*
             * E.g. {"timestamp":"REDACTED","status":404,"code":"NOT_FOUND_ERROR","path":
             * "/api/files/transfers/public/details","details":{"message":"Transfer not found with publicId = REDACTED and status = ACTIVE"
             * }}
             */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(entries.get("name").toString());
        final PluginForHost plg = this.getNewPluginForHostInstance(this.getHost());
        final List<Map<String, Object>> files = (List<Map<String, Object>>) entries.get("files");
        for (final Map<String, Object> file : files) {
            final DownloadLink link = this.createDownloadlink("");
            link.setDefaultPlugin(plg);
            link.setFinalFileName(file.get("name").toString());
            link.setVerifiedFileSize(((Number) file.get("size")).longValue());
            link.setAvailable(true);
            link._setFilePackage(fp);
            link.setProperty(WesenditCom.PROPERTY_FOLDER_ID, entries.get("publicId"));
            link.setProperty(WesenditCom.PROPERTY_FILE_ID, file.get("id"));
            /* RecipientID is no always given */
            if (recipientID != null) {
                link.setProperty(WesenditCom.PROPERTY_RECIPIENT_ID, recipientID);
            }
            ret.add(link);
        }
        return ret;
    }
}
