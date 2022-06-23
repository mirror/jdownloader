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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Request;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "vsco.co" }, urls = { "https?://(?:[^/]+\\.vsco\\.co/grid/\\d+|(?:www\\.)?vsco\\.co/[\\w-]+/grid/\\d+|(?:www\\.)?vsco\\.co/[\\w-]+)" })
public class VscoCo extends PluginForDecrypt {
    public VscoCo(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String PROPERTY_USERNAME      = "user";
    private static final String PROPERTY_DATE          = "date";
    private static final String PROPERTY_DATE_CAPTURED = "date_captured";

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String username = new Regex(param.getCryptedUrl(), "https?://([^/]+)\\.vsco\\.co/").getMatch(0);
        if (username == null) {
            username = new Regex(param.getCryptedUrl(), "vsco\\.co/([\\w-]+)").getMatch(0);
        }
        br.setCurrentURL("https://" + this.getHost() + "/" + username + "/images/1");
        br.getPage("https://vsco.co/content/Static/userinfo");
        // final String json = br.getRegex("define\\((.*?)\\)").getMatch(0);
        // final Map<String, Object> userInfo = JavaScriptEngineFactory.jsonToJavaMap(json);
        final String cookie_vs = br.getCookie(this.getHost(), "vs");
        br.getPage("/ajxp/" + cookie_vs + "/2.0/sites?subdomain=" + username);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String siteid = PluginJSonUtils.getJsonValue(br, "id");
        if (cookie_vs == null || siteid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        long amount_total = 0;
        /* More than 500 possible */
        int max_count_per_page = 500;
        int page = 1;
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(username);
        do {
            final Browser ajax = br.cloneBrowser();
            ajax.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
            ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            ajax.getPage("/ajxp/" + cookie_vs + "/2.0/medias?site_id=" + siteid + "&page=" + page + "&size=" + max_count_per_page);
            final Map<String, Object> entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(ajax.toString());
            if (page == 1 || page > 1) {
                amount_total = JavaScriptEngineFactory.toLong(entries.get("total"), 0);
                if (page == 1 && amount_total == 0) {
                    logger.info("User owns zero media content!");
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else if (page > 1 && amount_total == 0) {
                    return decryptedLinks;
                }
            }
            final List<Object> medias = (List) entries.get("media");
            final SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd");
            for (final Object resource : medias) {
                final Map<String, Object> media = (Map<String, Object>) resource;
                final String fid = (String) media.get("_id");
                if (fid == null) {
                    return null;
                }
                final String medialink = (String) media.get("permalink");
                final Boolean isVideo = (Boolean) media.get("is_video");
                String url_content = null;
                if (Boolean.TRUE.equals(isVideo)) {
                    url_content = (String) media.get("video_url");
                } else {
                    url_content = (String) media.get("responsive_url");
                }
                if (StringUtils.isEmpty(url_content)) {
                    /* Skip invalid items */
                    continue;
                }
                if (!(url_content.startsWith("http") || url_content.startsWith("//"))) {
                    url_content = Request.getLocation("//" + url_content, br.getRequest());
                }
                final String description = (String) media.get("description");
                final String filename = username + "_" + fid + getFileNameExtensionFromString(url_content, Boolean.TRUE.equals(isVideo) ? ".mp4" : ".jpg");
                final DownloadLink dl = this.createDownloadlink("directhttp://" + url_content);
                dl.setContentUrl(medialink);
                dl.setName(filename);
                dl.setAvailable(true);
                if (!StringUtils.isEmpty(description)) {
                    dl.setComment(description);
                }
                /* Set some Packagizer properties */
                dl.setProperty(PROPERTY_USERNAME, username);
                dl.setProperty(PROPERTY_DATE, sd.format(new Date(((Number) media.get("upload_date")).longValue())));
                dl.setProperty(PROPERTY_DATE_CAPTURED, sd.format(new Date(((Number) media.get("capture_date_ms")).longValue())));
                decryptedLinks.add(dl);
                fp.add(dl);
                distribute(dl);
            }
            if (medias.size() < max_count_per_page) {
                /* Fail safe */
                break;
            } else if (this.isAbort()) {
                logger.info("Decryption aborted by user");
                break;
            }
            page++;
        } while (decryptedLinks.size() < amount_total);
        if (decryptedLinks.size() == 0 && !this.isAbort()) {
            return null;
        }
        return decryptedLinks;
    }
}
