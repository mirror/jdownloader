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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hightail.com" }, urls = { "https?://(?:www\\.)?(?:yousendit|hightail)\\.com/download/[A-Za-z0-9\\-_]+|https?://spaces\\.hightail\\.com/receive/[A-Za-z0-9]+/.+" })
public class HighTailComDecrypter extends PluginForDecrypt {
    @SuppressWarnings("deprecation")
    public HighTailComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static Browser prepBR(final Browser br) {
        br.setAllowedResponseCodes(new int[] { 500 });
        return br;
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        prepBR(this.br);
        final String linkpart = new Regex(parameter, "(/download/.+)").getMatch(0);
        final String linkpart_encoded = Encoding.Base64Encode(linkpart);
        final String sessionId = getSessionID(this.br);
        if (StringUtils.isEmpty(sessionId)) {
            return null;
        }
        final String spaceID;
        if (parameter.matches(".+/download/.+")) {
            /* Old URLs --> We have to find the spaceID */
            br.getPage("/api/v1/link/" + linkpart_encoded);
            if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 500) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            spaceID = PluginJSonUtils.getJson(this.br, "spaceUrl");
        } else {
            /* New URLs --> spaceID is given inside URL. */
            spaceID = new Regex(parameter, "/receive/([A-Za-z0-9]+)").getMatch(0);
        }
        if (StringUtils.isEmpty(spaceID)) {
            return null;
        }
        // br.getHeaders().put("Accept", "application/json, text/plain, */*");
        // br.getHeaders().put("X-Correlation-Id", "gp7n6k11lymls:1fugwrm16otk1n");
        br.getPage("/api/v1/spaces/url/" + spaceID + "?status=SEND");
        final String errorMessage = PluginJSonUtils.getJson(this.br, "errorMessage");
        if (!StringUtils.isEmpty(errorMessage)) {
            /* 2017-05-04: E.g. {"errorMessage":"SPACE_EXPIRED"} */
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String spaceIDLonger = PluginJSonUtils.getJson(this.br, "id");
        if (StringUtils.isEmpty(spaceIDLonger)) {
            return null;
        }
        br.getPage("https://api.spaces.hightail.com/api/v1/files/" + spaceIDLonger + "?cacheBuster=" + System.currentTimeMillis() + "&depth=1&limit=10000&offset=0&sort=custom");
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("children");
        for (final Object fo : ressourcelist) {
            entries = (LinkedHashMap<String, Object>) fo;
            final String fileID = (String) entries.get("fileId");
            final String versionId = (String) entries.get("versionId");
            if (StringUtils.isEmpty(fileID) || StringUtils.isEmpty(versionId)) {
                return null;
            }
            final boolean isDirectory = ((Boolean) entries.get("isDirectory")).booleanValue();
            if (isDirectory) {
                final String folderlink_new = "https://de.hightail.com/sharedFolder?phi_action=app/orchestrateSharedFolder&id=" + spaceID + "&folderid=" + fileID;
                final DownloadLink dl = createDownloadlink(folderlink_new);
                decryptedLinks.add(dl);
            } else {
                final DownloadLink dl = createDownloadlink("http://yousenditdecrypted.com/download/" + System.currentTimeMillis() + new Random().nextInt(100000));
                final String filename = (String) entries.get("name");
                final long filesize = JavaScriptEngineFactory.toLong(entries.get("size"), -1);
                if (StringUtils.isEmpty(filename) || filesize == -1) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                dl.setFinalFileName(filename);
                dl.setDownloadSize(filesize);
                dl.setLinkID(fileID + spaceIDLonger + versionId);
                dl.setProperty("directname", filename);
                dl.setProperty("directsize", filesize);
                dl.setProperty("spaceid", spaceIDLonger);
                dl.setProperty("fileid", fileID);
                dl.setProperty("versionid", versionId);
                dl.setContentUrl(parameter);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        }
        return decryptedLinks;
    }

    public static String getSessionID(final Browser br) throws IOException {
        br.getPage("https://api.spaces.hightail.com/api/v1/auth/sessionInfo?cacheBuster=" + System.currentTimeMillis());
        final String sessionId = PluginJSonUtils.getJson(br, "sessionId");
        if (!StringUtils.isEmpty(sessionId)) {
            br.setCookie(".spaces.hightail.com", "sessionId", sessionId);
        }
        return sessionId;
    }
}
