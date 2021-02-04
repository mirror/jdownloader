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
import java.util.Locale;

import javax.script.ScriptEngine;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "asbook.info" }, urls = { "https?://(www\\.)?(asbook\\.info|asbookonline\\.com)/[^/]+/?" })
public class AsBook extends antiDDoSForDecrypt {
    public AsBook(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String fpName = null;
        String embedURL = br.getRegex("<script[^>]+defer\\s+src=\\s*'\\s*([^']+)\\s*'[^>]+>\\s*(?:</\\s*script\\s*>)?\\s*<table[^>]+class\\s*=\\s*'[^']*xframe-meta[^']*'").getMatch(0);
        fpName = br.getRegex("<title>\\s*([^<]+)\\s+слушать бесплатно онлайн").getMatch(0);
        if (StringUtils.isNotEmpty(embedURL)) {
            final Browser br2 = br.cloneBrowser();
            embedURL = Encoding.htmlDecode(embedURL);
            getPage(br2, embedURL);
            String iframeURL = br2.getRegex("<iframe[^>]+id\\s*=\\s*\\\\\"[^\"]*xframe[^\"]*\"[^>]+src\\s*=\\s*\\\\\"([^\"]*)\"[^>]*>").getMatch(0);
            if (StringUtils.isNotEmpty(iframeURL)) {
                iframeURL = Encoding.htmlOnlyDecode(iframeURL.replace("\\", ""));
                getPage(br2, iframeURL);
                String[][] trackDetails = br2.getRegex("<a[^>]+href\\s*=\\s*\"([^\"]+)\"[^>]+data-code\\s*=\\s*\"([^\"]+)\"[^>]*>").getMatches();
                final String decryptJS = getDecryptJS(br2);
                final ScriptEngine engine = JavaScriptEngineFactory.getScriptEngineManager(null).getEngineByName("js");
                engine.eval("var res = \"\";");
                engine.eval(decryptJS);
                if (trackDetails != null && trackDetails.length > 0) {
                    int trackcount = trackDetails.length;
                    int trackNumber = 1;
                    String trackNumber_suffix = null;
                    String ext = null;
                    int padlength = getPadLength(trackDetails.length);
                    for (String[] trackDetail : trackDetails) {
                        String track = trackDetail[0];
                        String datacode = trackDetail[1];
                        String decodedLink = "";
                        engine.eval("res = d(f(d(i(\"" + datacode + "\"), p())), c());");
                        engine.eval("res = n(res);"); // Throws an "org.mozilla.javascript.ConsString cannot be cast to java.lang.String"
                        // error in the Rhino engine, but works fine in Nashorn, so we're using the latter
                        // above even though it's deprecated as of JDK 11.
                        decodedLink = (String) engine.get("res");
                        decodedLink = decodedLink.replaceAll("^//", "https://");
                        DownloadLink dl = createDownloadlink(decodedLink);
                        if (StringUtils.isNotEmpty(fpName)) {
                            String trackNumber_formatted = String.format(Locale.US, "%0" + padlength + "d", trackNumber);
                            trackNumber_suffix = trackcount > 1 ? (" - " + trackNumber_formatted) : "";
                            if (ext == null) {
                                /* No general extension given? Get it from inside the URL. */
                                ext = getFileNameExtensionFromURL(decodedLink, ".mp3");
                            }
                            final String album_name = Encoding.htmlDecode(fpName.trim());
                            dl.setFinalFileName(album_name + trackNumber_suffix + ext);
                        }
                        decryptedLinks.add(dl);
                        trackNumber++;
                    }
                }
            }
        }
        if (StringUtils.isNotEmpty(fpName)) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private final int getPadLength(final int size) {
        return String.valueOf(size).length();
    }

    private String getDecryptJS(Browser br) throws Exception {
        /*
         * See ticket #87471 for more context. NOTE: A castrated version of
         * https://cdn-x4.xframeonline.com/audiobook/js/app.js?id=706bcafc9823cf61f4d8 was minified using https://jscompress.com/ and then
         * escaped using https://www.freeformatter.com/java-dotnet-escape.html
         */
        /*
         * Value on 2019-10-05:
         * "function n(n){for(var r,t,e,o,u,i,c=m()+\"=\",f=0,a=\"\";r=(i=c.indexOf(n.charAt(f++))<<18|c.indexOf(n.charAt(f++))<<12|(o=c.indexOf(n.charAt(f++)))<<6|(u=c.indexOf(n.charAt(f++))))>>16&255,t=i>>8&255,e=255&i,a+=64==o?String.fromCharCode(r):64==u?String.fromCharCode(r,t):String.fromCharCode(r,t,e),f<n.length;);return a}function t(){return i(\"jihgfedcbaZYXWVUTSRQPONMLKJIHGFEDCBA\")}function m(){return t()+i(a().join(\"\")+\"zyxwvutsrqponmlk\")+i(\"/+\")}function o(){return\"edoc\"}function i(n){return u(r(n).reverse())}function a(){for(var n=[],r=0;r<10;r++)n.push(r);return r=\"0\".charCodeAt(0),n.reverse().map(function(n){return String.fromCharCode(n+r)})}function r(n){return n.split(\"\")}function s(){return\"atad\"}function l(n){return n.replace(/[\\-\\[\\]\\/\\{\\}\\(\\)\\*\\+\\?\\.\\\\\\^\\$\\|]/g,\"\\\\$&\")}function u(n){return n.join(\"\")}function d(n,r){var t=Object.keys(r).map(l);return n.split(RegExp(\"(\"+t.join(\"|\")+\")\")).map(function(n){return r[n]||n}).join(\"\")}function c(){return{\";\":\"===\",\",\":\"==\",\".\":\"=\",\"\u0429\":\"z\",\"\u0426\":\"x\",\"{\":\"ja\",\"}\":\"4L\"}}function f(n){return decodeURIComponent(n.replace(/\\+/g,\" \"))}function p(){return{\"?\":\"%\",\"#\":\"%2\",\"[\":\"%A\",\"]\":\"%D\",\"@\":\"0\"}}"
         */
        final Browser brc = br.cloneBrowser();
        getPage(brc, "https://cdn-x4.xframeonline.com/audiobook/js/app.js?id=706bcafc9823cf61f4d8");
        String result = brc.getRegex("function\\s*\\(\\s*e\\s*\\)\\{\\s*\"use strict\"\\s*\\;\\s*(function\\s*t\\s*\\(.*)\\s*var\\s*y\\s*,\\s*g\\s*,\\s*_\\s*,\\s*v\\s*=").getMatch(0);
        return result;
    }
}