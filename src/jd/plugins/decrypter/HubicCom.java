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
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hubic.com" }, urls = { "(https?://(?:www\\.)?hubic\\.com/home/pub/\\?ruid=([a-zA-Z0-9_/\\+\\=\\-%]+)|https?://[a-z0-9\\-_]+\\.hubic\\.ovh\\.net/.+)" })
public class HubicCom extends PluginForDecrypt {
    public HubicCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        jd.plugins.hoster.HubicCom.prepBR(this.br);
        String ruid_b64 = null;
        if (parameter.matches("https?://[a-z0-9\\-_]+\\.hubic\\.ovh\\.net/.+")) {
            final URLConnectionAdapter con = br.openGetConnection(parameter);
            if (con.isOK() && con.isContentDisposition() && !StringUtils.containsIgnoreCase(con.getContentType(), "json")) {
                con.disconnect();
                final DownloadLink dl = this.createDownloadlink(parameter.replaceAll("hubic.ovh.net/", "hubicdecrypted.ovh.net/"));
                final String name = getFileNameFromHeader(con);
                if (name != null) {
                    dl.setFinalFileName(name);
                }
                final long size = con.getLongContentLength();
                if (size > 0) {
                    dl.setVerifiedFileSize(size);
                }
                dl.setAvailable(true);
                decryptedLinks.add(dl);
                return decryptedLinks;
            } else {
                br.getPage(parameter);
            }
        } else {
            ruid_b64 = Encoding.htmlDecode(new Regex(parameter, this.getSupportedLinks()).getMatch(0));
            br.getPage(parameter);
            jd.plugins.hoster.HubicCom.prepBRAjax(this.br);
            accessRUID(this.br, ruid_b64);
        }
        if (isOffline(this.br, null)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        if (isOffline(this.br, entries)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final ArrayList<Object> ressourcelist = getList(entries);
        for (final Object fileo : ressourcelist) {
            entries = (LinkedHashMap<String, Object>) fileo;
            final String hash = (String) entries.get("hash");
            final String name = (String) entries.get("name");
            final String url = (String) entries.get("url");
            final long filesize = JavaScriptEngineFactory.toLong(entries.get("size"), 0);
            if (StringUtils.isEmpty(name) || StringUtils.isEmpty(url) || filesize == 0) {
                continue;
            }
            final DownloadLink dl = this.createDownloadlink(url.replaceAll("hubic.ovh.net/", "hubicdecrypted.ovh.net/"));
            dl.setProperty("ruid", ruid_b64);
            dl.setProperty("hash", hash);
            dl.setFinalFileName(name);
            dl.setDownloadSize(filesize);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }
        String fpName = br.getRegex("null(.+)null").getMatch(0);
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    public static void accessRUID(final Browser br, final String ruid_b64) throws IOException {
        br.postPage("https://hubic.com/home/pub/get/", "ruid=" + Encoding.urlEncode(ruid_b64) + "&path=%2F");
    }

    public static boolean isOffline(final Browser br, final LinkedHashMap<String, Object> entries) {
        final boolean is_offline = br.getHttpConnection().getResponseCode() == 401 || br.getHttpConnection().getResponseCode() == 405 || (entries != null && entries.get("error") != null);
        return is_offline;
    }

    public static ArrayList<Object> getList(LinkedHashMap<String, Object> entries) throws Exception {
        if (entries.containsKey("answer")) {
            entries = (LinkedHashMap<String, Object>) entries.get("answer");
        }
        if (entries.get("list") instanceof Map) {
            final Object ret = ((Map<String, Object>) entries.get("list")).get("/");
            if (ret instanceof List) {
                return (ArrayList<Object>) ret;
            } else if (ret instanceof Map) {
                return new ArrayList<Object>(((Map<String, Object>) ret).values());
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else {
            return (ArrayList<Object>) entries.get("list");
        }
    }
}
