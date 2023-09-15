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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Map;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "fakku.net" }, urls = { "https?://(?:www\\.)?fakku\\.net/[a-z0-9\\-_]+/[a-z0-9\\-_]+/read" })
public class FakkuNet extends antiDDoSForDecrypt {
    public FakkuNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY, LazyPlugin.FEATURE.XXX };
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        /* Forced HTTPS */
        final String contenturl = param.getCryptedUrl().replaceFirst("(?i)http://", "https://");
        final Regex urlinfo = new Regex(param.getCryptedUrl(), "https?://[^/]+/([^/]+)/([^/]+)");
        final String contentGenre = urlinfo.getMatch(0);
        final String url_title = urlinfo.getMatch(1);
        if (contentGenre == null || url_title == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        if (account != null) {
            final PluginForHost plugin = this.getNewPluginForHostInstance(this.getHost());
            ((jd.plugins.hoster.FakkuNet) plugin).login(br, account, false);
        }
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(410);
        getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 410) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("id=\"error\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final DecimalFormat df = new DecimalFormat("000");
        int counter = 1;
        String fpName = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        final String json_array = br.getRegex("window\\.params\\.thumbs = (\\[.*?\\]);").getMatch(0);
        String main_part = br.getRegex("('|\")((?:https?:)?//t\\.fakku\\.net/images/[^<>\"]+/images/)\\1").getMatch(1);
        if (json_array == null && main_part == null) {
            /* Looks like account is required to view this. */
            if (account == null) {
                throw new AccountRequiredException();
            }
            /* Handling for subscription URLs */
            getPage("https://books." + this.getHost() + "/" + contentGenre + "/" + url_title + "/read");
            final Map<String, Object> entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            if (fpName == null) {
                fpName = (String) JavaScriptEngineFactory.walkJson(entries, "content/content_name");
            }
            long content_pages = JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(entries, "content/content_pages"), 0);
            final Map<String, Object> pageMap = (Map<String, Object>) entries.get("pages");
            if (fpName == null || content_pages == 0 || pageMap == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (counter = 1; counter <= content_pages; counter++) {
                final Map<String, Object> imgInfo = (Map<String, Object>) pageMap.get(Long.toString(counter));
                final String directlink = (String) imgInfo.get("image");
                final DownloadLink dl = createDownloadlink(directlink);
                final String final_filename = fpName + " - " + df.format(counter) + ".jpg";
                dl.setFinalFileName(final_filename);
                dl.setAvailable(true);
                dl.setContentUrl(contenturl);
                dl.setProperty("mainlink", "https://www.fakku.net/manga/" + url_title + "/read");
                dl.setProperty("decrypterfilename", final_filename);
                ret.add(dl);
            }
        } else {
            if (fpName == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            main_part = Request.getLocation(main_part, br.getRequest());
            fpName = Encoding.htmlDecode(fpName).trim();
            final String allThumbs[] = PluginJSonUtils.getJsonResultsFromArray(json_array);
            if (allThumbs.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (String thumb : allThumbs) {
                thumb = PluginJSonUtils.unescape(thumb);
                final String thumb_number = new Regex(thumb, "/thumbs/(\\d+)\\.thumb\\.jpg").getMatch(0);
                final DownloadLink dl = createDownloadlink("directhttp://" + main_part + thumb_number + ".jpg");
                dl.setFinalFileName(fpName + " - " + df.format(counter) + ".jpg");
                dl.setAvailable(true);
                ret.add(dl);
                counter++;
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName).trim());
            fp.addLinks(ret);
        }
        return ret;
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}