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
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class DropdenCom extends PluginForDecrypt {
    public DropdenCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(401);
        return br;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "dropden.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([a-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String folderID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        br.getHeaders().put("Referer", param.getCryptedUrl());
        int attempts = 0;
        String passCode = null;
        do {
            br.getPage("https://" + this.getHost() + "/" + folderID + ".json");
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (attempts >= 4) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            } else if (br.getHttpConnection().getResponseCode() == 401) {
                if (passCode != null) {
                    logger.info("User entered invalid password: " + passCode);
                } else {
                    logger.info("Password required");
                }
                passCode = getUserInput("Password?", param);
                br.getHeaders().put("X-Download-Pass", passCode);
                attempts++;
                continue;
            } else {
                break;
            }
        } while (!this.isAbort());
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final List<Map<String, Object>> items = (List<Map<String, Object>>) entries.get("items");
        final FilePackage fp = FilePackage.getInstance();
        fp.setPackageKey("dropdencom://folder/" + folderID);
        for (final Map<String, Object> item : items) {
            final Map<String, Object> metadata = (Map<String, Object>) item.get("metadata");
            final String url = br.getURL(item.get("url").toString()).toExternalForm();
            final DownloadLink link = this.createDownloadlink(DirectHTTP.createURLForThisPlugin(url));
            link.setFinalFileName(metadata.get("name").toString());
            link.setVerifiedFileSize(((Number) item.get("size")).longValue());
            link.setAvailable(true);
            link._setFilePackage(fp);
            if (passCode != null) {
                link.setDownloadPassword(passCode);
            }
            ret.add(link);
        }
        return ret;
    }
}
