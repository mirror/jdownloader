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

import javax.script.ScriptEngine;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fbjav.net" }, urls = { "https?://(www\\.)?fbjav\\.(?:net|com)/\\w+-\\d+[^/]*" })
public class FbjavNet extends antiDDoSForDecrypt {
    public FbjavNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String parameter = param.getCryptedUrl();
        br.setFollowRedirects(true);
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String fpName = null;
        fpName = br.getRegex("<title>\\s*([^<]+)\\s*").getMatch(0);
        if (fpName != null) {
            fpName = Encoding.htmlDecode(fpName).trim();
        }
        final String thumbnailurl = br.getRegex("\"thumbnailUrl\"\\s*:\\s*\"(https?://[^\"]+)").getMatch(0);
        if (thumbnailurl != null) {
            final DownloadLink thumbnail = this.createDownloadlink(DirectHTTP.createURLForThisPlugin(thumbnailurl));
            thumbnail.setAvailable(true);
            ret.add(thumbnail);
        }
        String[][] videoSources = br.getRegex("eid\\s*=\\s*[\"']*(\\d+)[\"']*\\s+dtl\\s*=\\s*[\"']*(\\w+)[\"']*").getMatches();
        if (videoSources != null && videoSources.length > 0) {
            final ScriptEngine engine = JavaScriptEngineFactory.getScriptEngineManager(null).getEngineByName("javascript");
            final String decodeJS = getDecryptJS(br);
            engine.eval(decodeJS);
            for (String[] videoSource : videoSources) {
                String eid = videoSource[0];
                String dtl = videoSource[1];
                try {
                    engine.eval("var res = link_decode(\"" + dtl + "\");");
                    String result = engine.get("res").toString();
                    final DownloadLink dl = createDownloadlink(result);
                    /* Workaround as their own cdn-API does not return any filenames! */
                    if (result.contains("imfb.xyz") && fpName != null) {
                        dl.setFinalFileName(fpName + ".mp4");
                    }
                    ret.add(dl);
                } catch (Exception e) {
                    getLogger().log(e);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, null, e);
                }
            }
        }
        if (ret.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        }
        if (StringUtils.isNotEmpty(fpName)) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName).trim());
            fp.addLinks(ret);
        }
        return ret;
    }

    private String getDecryptJS(Browser br) throws Exception {
        final StringBuilder str = new StringBuilder();
        final Browser brc = br.cloneBrowser();
        brc.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        // final String scriptURL = "https://static.fbjav.com/wp-content/themes/fbjav/assets/js/custom28919.js";
        /* 2021-03-15 */
        final String scriptURL = "https://fbjav.com/wp-content/themes/fbjav/assets/js/custom.min.js?v11020";
        getPage(brc, scriptURL);
        str.append(brc.getRegex("(function\\s*reverse\\s*\\(\\s*t\\s*\\)\\s*\\{\\s*return[^\\}]+\\s*\\})").getMatch(0));
        str.append("\r\n");
        str.append(brc.getRegex("(function\\s*strtr[^\\{]+\\{.*?)function link_decode").getMatch(0));
        str.append("\r\n");
        str.append(brc.getRegex("(function\\s*link_decode\\(t\\)\\s*\\{[^\\}]+\\})").getMatch(0));
        /* Finally add base64 decode function */
        str.append("\r\n");
        str.append("function atob (f){var g={},b=65,d=0,a,c=0,h,e='',k=String.fromCharCode,l=f.length;for(a='';91>b;)a+=k(b++);a+=a.toLowerCase()+'0123456789+/';for(b=0;64>b;b++)g[a.charAt(b)]=b;for(a=0;a<l;a++)for(b=g[f.charAt(a)],d=(d<<6)+b,c+=6;8<=c;)((h=d>>>(c-=8)&255)||a<l-2)&&(e+=k(h));return e};");
        final String result = str.toString().trim().replace("window.location.host", "\"" + brc.getHost() + "\"");
        if (StringUtils.isEmpty(result)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return result;
    }
}