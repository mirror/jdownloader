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
import java.util.HashSet;

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
import jd.plugins.PluginForDecrypt;
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
        String title = br.getRegex("<title>\\s*([^<]+)\\s*").getMatch(0);
        if (title != null) {
            title = Encoding.htmlDecode(title).trim();
        }
        final String thumbnailurl = br.getRegex("\"thumbnailUrl\"\\s*:\\s*\"(https?://[^\"]+)").getMatch(0);
        if (thumbnailurl != null) {
            final DownloadLink thumbnail = this.createDownloadlink(DirectHTTP.createURLForThisPlugin(thumbnailurl));
            thumbnail.setAvailable(true);
            ret.add(thumbnail);
        }
        // final String[][] videoSources = br.getRegex("eid\\s*=\\s*[\"']*(\\d+)[\"']*\\s+dtl\\s*=\\s*[\"']*(\\w+)[\"']*").getMatches();
        final String[][] videoSources = br.getRegex("dtl\\s*=(?:'|\")?(\\w+)").getMatches();
        if (videoSources != null && videoSources.length > 0) {
            final HashSet<String> dupes = new HashSet<String>();
            PluginForDecrypt specialCrawlerplugin = this.getNewPluginForDecryptInstance(ImcontentMe.getPluginDomains().get(0)[0]);
            final ScriptEngine engine = JavaScriptEngineFactory.getScriptEngineManager(null).getEngineByName("javascript");
            final String decodeJS = getDecryptJS(br.cloneBrowser());
            engine.eval(decodeJS);
            for (String[] videoSource : videoSources) {
                // String eid = videoSource[0];
                String dtl = videoSource[0];
                if (!dupes.add(dtl)) {
                    /* Skip dupe */
                    continue;
                }
                try {
                    engine.eval("var res = link_decode(\"" + dtl + "\");");
                    final String resultURL = engine.get("res").toString();
                    logger.info("resultURL: " + resultURL);
                    final DownloadLink dl = createDownloadlink(resultURL);
                    /* Workaround as their own cdn-API does not return any filenames! */
                    // if (result.contains("imfb.xyz") && fpName != null) {
                    // dl.setFinalFileName(fpName + ".mp4");
                    // }
                    if (specialCrawlerplugin.canHandle(resultURL) && title != null) {
                        dl.setProperty(ImcontentMe.PROPERTY_TITLE, title);
                    }
                    ret.add(dl);
                } catch (final Exception e) {
                    logger.log(e);
                }
            }
        }
        if (ret.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        }
        if (StringUtils.isNotEmpty(title)) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(title).trim());
            fp.addLinks(ret);
        }
        return ret;
    }

    private String getDecryptJS(final Browser br) throws Exception {
        final StringBuilder str = new StringBuilder();
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        // final String scriptURL = "https://static.fbjav.com/wp-content/themes/fbjav/assets/js/custom28919.js";
        // final String scriptURL = "https://fbjav.com/wp-content/themes/fbjav/assets/js/custom.min.js?v11020";
        /* 2023-09-14 */
        final String scriptURL = "/wp-content/themes/fbjav-v3/assets/js/custom.js";
        getPage(br, scriptURL);
        final String func1 = br.getRegex("(function\\s*reverse\\s*\\(\\s*t\\s*\\)\\s*\\{\\s*return[^\\}]+\\s*\\})").getMatch(0);
        if (func1 == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        str.append(func1);
        str.append("\r\n");
        final String func2 = br.getRegex("(function\\s*strtr[^\\{]+\\{.*?)function link_decode").getMatch(0);
        if (func2 == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        str.append(func2);
        str.append("\r\n");
        final String func3 = br.getRegex("(function\\s*link_decode\\([^\\)]+\\)\\s*\\{.*?)\\s*function").getMatch(0);
        if (func3 == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        str.append(func3);
        /* Finally add base64 decode function */
        str.append("\r\n");
        str.append("function atob (f){var g={},b=65,d=0,a,c=0,h,e='',k=String.fromCharCode,l=f.length;for(a='';91>b;)a+=k(b++);a+=a.toLowerCase()+'0123456789+/';for(b=0;64>b;b++)g[a.charAt(b)]=b;for(a=0;a<l;a++)for(b=g[f.charAt(a)],d=(d<<6)+b,c+=6;8<=c;)((h=d>>>(c-=8)&255)||a<l-2)&&(e+=k(h));return e};");
        final String result = str.toString().trim().replace("window.location.host", "\"" + br.getHost() + "\"");
        if (StringUtils.isEmpty(result)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return result;
    }
}