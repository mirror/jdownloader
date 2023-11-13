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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.GenericM3u8;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class ImcontentMe extends PluginForDecrypt {
    public ImcontentMe(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static String decodeData(final String t) {
        final String e = "2";
        final String b64decoded = Encoding.Base64Decode(t);
        final StringBuilder res = new StringBuilder();
        for (int i = 0; i < b64decoded.length(); i++) {
            char c = b64decoded.charAt(i);
            char eChar = e.charAt(i % e.length());
            char xor = (char) (c ^ eChar);
            res.append(xor);
        }
        return res.toString();
    }

    public static void main(String[] args) {
        String t = "testStringHere";
        String result = decodeData(t);
        System.out.println(result);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "player.imcontent.me" });
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
            ret.add("https?://" + buildHostsPatternPart(domains) + "/v/([A-Za-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    public static final String PROPERTY_TITLE = "title";

    /** This plugin processes items returned from other crawler plugin "FbjavNet". */
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final String contentID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        if (contentID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setFollowRedirects(true);
        String referer = null;
        if (param.getDownloadLink() != null) {
            referer = param.getDownloadLink().getReferrerUrl();
        }
        if (StringUtils.isEmpty(referer)) {
            /* Fallback */
            referer = "https://fbjav.com/";
        }
        /* Mandatory else we will run into error "Access Denied". */
        br.getHeaders().put("Referer", referer);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String key = findKey(this, br.getRequest().getHtmlCode());
        if (StringUtils.isEmpty(key)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final UrlQuery query = new UrlQuery();
        query.add("key", Encoding.urlEncode(key));
        query.add("sv", "");
        br.postPage("https://player.imcontent.me/plyr/getvideo", query);
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final String titleSlug = (String) entries.get("jid");
        final String cryptedData = (String) entries.get("data");
        if (StringUtils.isEmpty(cryptedData)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String title;
        final String betterTitle = param.getDownloadLink() != null ? param.getDownloadLink().getStringProperty(PROPERTY_TITLE) : null;
        if (betterTitle != null) {
            title = betterTitle;
        } else {
            title = titleSlug.replace("-", " ").trim();
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String decodedDataJson = decodeData(cryptedData);
        final Map<String, Object> entries2 = restoreFromString(decodedDataJson, TypeRef.MAP);
        final String hlsMaster = entries2.get("url").toString();
        final DownloadLink video = this.createDownloadlink(hlsMaster);
        video.setProperty(GenericM3u8.PRESET_NAME_PROPERTY, title);
        ret.add(video);
        /* 2023-11-11: This is not a subtitle but a list of thumbnails / preview-seek-frames. */
        // final String subtitleURL = entries2.get("vtt").toString();
        // final DownloadLink subtitle = this.createDownloadlink(DirectHTTP.createURLForThisPlugin(subtitleURL));
        // subtitle.setFinalFileName(title + ".vtt");
        // subtitle.setAvailable(true);
        // ret.add(subtitle);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        fp.addLinks(ret);
        return ret;
    }

    public static final String findKey(final Plugin plg, final String src) {
        /* Old function used in hoster-plugins "StreamonTo" and "UpvideoTo" until including revision 44721. */
        String[][] hunters = new Regex(src, "<script[^>]*>\\s*(var _[^<]*?\\})eval(\\(function\\(h,u,n,t,e,r\\).*?)</script>").getMatches();
        int counter = 0;
        for (final String[] hunter : hunters) {
            final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(plg);
            final ScriptEngine engine = manager.getEngineByName("javascript");
            final StringBuilder sb = new StringBuilder();
            /* First function always has the same functionality but is always named differently */
            sb.append(hunter[0].trim());
            /* 2nd function always calls the same function but with different parameters. */
            sb.append("var res = " + hunter[1].trim() + ";");
            String result = null;
            try {
                engine.eval(sb.toString());
                result = engine.get("res").toString();
                // System.out.println(counter + ":\r\n" + result);
                final String keyReversed = new Regex(result, "code = .([A-Za-z0-9]{4,})").getMatch(0);
                if (keyReversed != null) {
                    return new StringBuilder(keyReversed).reverse().toString();
                }
                // final String relevantPart1 = new Regex(result,
                // "\\);[\r\n\\s]*(var.*?)\\s*(?:window\\.videoConfig|function)").getMatch(0);
                // if (relevantPart1 != null) {
                // final StringBuilder sb2 = new StringBuilder();
                // sb2.append(relevantPart1);
                // sb2.append("function atob (f){var
                // g={},b=65,d=0,a,c=0,h,e='',k=String.fromCharCode,l=f.length;for(a='';91>b;)a+=k(b++);a+=a.toLowerCase()+'0123456789+/';for(b=0;64>b;b++)g[a.charAt(b)]=b;for(a=0;a<l;a++)for(b=g[f.charAt(a)],d=(d<<6)+b,c+=6;8<=c;)((h=d>>>(c-=8)&255)||a<l-2)&&(e+=k(h));return
                // e};");
                // sb2.append("var baabaffcac = efdcdbbfbefd.replace(\"RWFiZGVkYmJlZg\", \"\");var bbafcafccebb= atob(baabaffcac);var towait
                // = 5;var fabefacdffbd = \"#badaebaccbacff\";var res =
                // cccebacdeccc.replace(\"YWJmNDQ0YWIyOWFiYTdlNzE2ZjgwMzdlMjIyZGEwOGM\", \"\");var res2 =
                // res.replace(\"NTljOTkzNTcxMTdjZDc1YzZiMTlhYzFjODY0NjBhZGE=\", \"\");var decode = atob(res2);");
                // engine.eval(sb2.toString());
                // dllink = engine.get("decode").toString();
                // if (!StringUtils.isEmpty(dllink)) {
                // plg.getLogger().info("js success");
                // break;
                // } else {
                // plg.getLogger().warning("Hunter failed -> Hunter won?");
                // }
                // }
            } catch (final Exception e) {
                plg.getLogger().log(e);
            }
            counter += 1;
        }
        return null;
    }
}
