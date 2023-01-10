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
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.plugins.components.config.HitomiLaConfig;
import org.jdownloader.plugins.components.config.HitomiLaConfig.PreferredImageFormat;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.FunctionObject;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.hoster.DirectHTTP;

/**
 *
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hitomi.la" }, urls = { "https?://(?:www\\.)?hitomi\\.la/(?:galleries/\\d+\\.html|reader/\\d+\\.html|[^/]+/.*?-\\d+\\.html)" })
public class HitomiLa extends antiDDoSForDecrypt {
    public HitomiLa(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY, LazyPlugin.FEATURE.XXX };
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            engine = null;
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        String gallery_id = new Regex(parameter, "/(?:galleries|reader)/(\\d+)").getMatch(0);
        if (gallery_id == null) {
            gallery_id = new Regex(parameter, "/[^/]+/.*?-(\\d+)\\.html").getMatch(0);
        }
        final String url_name = new Regex(parameter, "https?://(?:www\\.)?hitomi\\.la/(?:(?:galleries|reader)/)?(.*?)(?:-\\d+)?\\.html$").getMatch(0);
        if (gallery_id == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setFollowRedirects(true);
        String fpName = null;
        int i = 0;
        String imghost = getImageHost(gallery_id) + "a";
        int numberOfPages;
        final boolean use_Thumbnails = false;
        if (use_Thumbnails) {
            this.getPage(parameter);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String[] thumbnails = br.getRegex("<img src=\"//tn\\.hitomi\\.la/bigtn/(./\\d+/[a-f0-9]+)\\.jpg\">").getColumn(0);
            numberOfPages = thumbnails.length;
            final DecimalFormat df = numberOfPages > 999 ? new DecimalFormat("0000") : numberOfPages > 99 ? new DecimalFormat("000") : new DecimalFormat("00");
            for (final String thumbnail : thumbnails) {
                final DownloadLink dl = createDownloadlink("directhttp://https://" + imghost + ".hitomi.la/webp/" + thumbnail + ".webp");
                dl.setReferrerUrl(br.getURL());
                dl.setProperty("requestType", "GET");
                dl.setAvailable(true);
                dl.setFinalFileName(df.format(i) + ".webp");
                dl.setProperty(DirectHTTP.PROPERTY_RATE_LIMIT, 500);
                ret.add(dl);
            }
        } else {
            getPage("https://hitomi.la/galleries/" + gallery_id + ".html");
            // this.getPage(parameter);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String extra_redirect = br.getRegex("<meta http-equiv=\"refresh\" content=\"\\d+;url=(http[^\"]+)\">").getMatch(0);
            if (extra_redirect != null) {
                this.getPage(Encoding.htmlOnlyDecode(extra_redirect));
            }
            // get the image host.
            // retval = subdomain_from_galleryid(g) + retval;
            String js = br.getRegex("src\\s*=\\s*\"([^\"]+" + gallery_id + "\\.js)\"").getMatch(0);
            if (js == null) {
                js = br.getURL("//ltn.hitomi.la/galleries/" + gallery_id + ".js").toExternalForm();
            }
            if (js == null) {
                logger.info("Seems like this is no downloadable/supported content");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Browser brc = br.cloneBrowser();
            getPage(brc, js);
            Map<String, Object> entries = null;
            final Object map = JavaScriptEngineFactory.jsonToJavaObject(brc.toString().replaceFirst("(var\\s*galleryinfo\\s*=\\s*)", ""));
            fpName = (String) JavaScriptEngineFactory.walkJson(map, "title");
            if (fpName == null) {
                /* fallback from URL */
                fpName = URLEncode.decodeURIComponent(url_name);
                fpName = fpName != null ? fpName.replaceFirst("^([^/]+/)", "") : null;
            }
            final List<Object> ressourcelist;
            if (map instanceof List) {
                ressourcelist = (List<Object>) map;
            } else {
                entries = (Map<String, Object>) map;
                ressourcelist = (List<Object>) entries.get("files");
            }
            numberOfPages = ressourcelist.size();
            final DecimalFormat df = numberOfPages > 999 ? new DecimalFormat("0000") : numberOfPages > 99 ? new DecimalFormat("000") : new DecimalFormat("00");
            // boolean checked = false;
            final Map<String, Integer> dupCheck = new HashMap<String, Integer>();
            for (final Object picO : ressourcelist) {
                ++i;
                final Map<String, String> picInfo = (Map<String, String>) picO;
                final DownloadLink dl = getImage(df, dupCheck, gallery_id, picInfo, i);
                ret.add(dl);
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(ret);
        }
        return ret;
    }

    protected DownloadLink getImage(DecimalFormat df, Map<String, Integer> dupCheck, final String gallery_id, Map<String, String> picInfo, int i) throws Exception {
        final String ext;
        final String url;
        final long haswebp = JavaScriptEngineFactory.toLong(picInfo.get("haswebp"), 0);
        final long hasavif = JavaScriptEngineFactory.toLong(picInfo.get("hasavif"), 0);
        final PreferredImageFormat preferredFormat = PluginJsonConfig.get(HitomiLaConfig.class).getPreferredImageFormat();
        if (haswebp == 1 && (preferredFormat == PreferredImageFormat.WEBP || hasavif == 0)) {
            url = url_from_url_from_hash(gallery_id, picInfo, "webp", null, "a");
        } else if (hasavif == 1) {
            url = url_from_url_from_hash(gallery_id, picInfo, "avif", null, "a");
        } else {
            url = url_from_url_from_hash(gallery_id, picInfo, null, null, null);
        }
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE && i == 1) {
            final Browser br2 = br.cloneBrowser();
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openHeadConnection(url);
                if (!looksLikeDownloadableContent(con)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    con.disconnect();
                }
            } catch (final IOException e) {
                logger.log(e);
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        ext = Plugin.getFileNameExtensionFromURL(url);
        final Integer existing = dupCheck.put(url, i);
        if (existing != null) {
            logger.info("Dupe URL:" + url + "|" + existing + "," + i);
        }
        final DownloadLink dl = createDownloadlink("directhttp://" + url);
        dl.setLinkID("hitomi.la://" + gallery_id + "/" + i);
        dl.setReferrerUrl(br.getURL());
        dl.setProperty("requestType", "GET");
        dl.setProperty(DirectHTTP.PROPERTY_RATE_LIMIT, 500);
        dl.setAvailable(true);
        dl.setFinalFileName(df.format(i) + ext);
        return dl;
    }

    /**
     * they do some javascript trickery (check reader.js,common.js). rewritten in java.
     *
     * @param guid
     * @return
     * @throws DecrypterException
     */
    private String getImageHost(final String guid) throws DecrypterException {
        // number of subdmains, var number_of_frontends = 3;
        final int i = 3;
        // guid is always present, so not sure why they have failover. That said you don't need subdomain either base domain works also!
        String g = new Regex(guid, "^\\d*(\\d)$").getMatch(0);
        if (false && "1".equals(g)) {
            g = "0";
        }
        final String subdomain = Character.toString((char) (97 + (Integer.parseInt(g) % i)));
        return subdomain;
    }

    /* 2020-02-10: Thx to forum user "damo" - See also: https://board.jdownloader.org/showpost.php?p=457258&postcount=16 */
    /* ####################################################################################################################### */
    public static final Pattern SUBDOMAIN_FROM_URL_PATTERN  = Pattern.compile("/[0-9a-f]/([0-9a-f]{2})/");
    public static final Pattern URL_FROM_URL_PATTERN        = Pattern.compile("//..?\\.hitomi\\.la/");
    public static final Pattern FULL_PATH_FROM_HASH_PATTERN = Pattern.compile("^.*(..)(.)$");

    private String subdomain_from_galleryid(int g, int number_of_frontends) {
        int o = g % number_of_frontends;
        return String.valueOf((char) (97 + o));
    }

    private ScriptEngine engine = null;

    public static String jsStringPad(String string) {
        final String ret = StringUtils.fillPre(string, "0", 3);
        return ret;
    }

    private String subdomain_from_url(String url, String base) throws Exception {
        initEngine();
        try {
            if (base != null) {
                engine.eval("var result=subdomain_from_url('" + url + "','" + base + "');");
            } else {
                engine.eval("var result=subdomain_from_url('" + url + "');");
            }
            final String result = engine.get("result").toString();
            return result;
        } catch (final Exception e) {
            if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                e.printStackTrace();
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, null, e);
        }
    }

    private String url_from_url(String url, String base) throws Exception {
        return URL_FROM_URL_PATTERN.matcher(url).replaceAll("//" + subdomain_from_url(url, base) + ".hitomi.la/");
    }

    protected String fetchGG(Browser br) throws IOException {
        final Browser brc = br.cloneBrowser();
        brc.setFollowRedirects(true);
        String gg = brc.getPage("https://ltn.hitomi.la/gg.js");
        gg = gg.replaceFirst("('use strict';\\s*)", "var ");
        return gg;
    }

    private void initEngine() throws Exception {
        if (engine == null) {
            Browser brc = br.cloneBrowser();
            brc.setFollowRedirects(true);
            brc.getPage("https://ltn.hitomi.la/common.js");
            String js = brc.getRegex("(var gg.*?)function\\s*show_loading").getMatch(0);
            if (js == null) {
                js = brc.getRegex("(function subdom.*?)function\\s*show_loading").getMatch(0);
            }
            // String.padStart not available in old Rhino version
            // js = js.replace("g.toString(16).padStart(3, '0')", "jsStringPad(g.toString(16))");
            if (StringUtils.isEmpty(js)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(this);
            engine = manager.getEngineByName("javascript");
            final Context jsContext = Context.enter();
            try {
                // required to avoid max 64k heap exception
                jsContext.setOptimizationLevel(-1);
                final Method jsStringPad = HitomiLa.class.getMethod("jsStringPad", new Class[] { String.class });
                engine.put("jsStringPad", new FunctionObject("jsStringPad", jsStringPad, jsContext.initStandardObjects()));
                engine.eval("var navigator={};");
                engine.eval("navigator[\"userAgent\"] = \"test\";");
                engine.eval(js);
                final String gg = fetchGG(br);
                engine.eval(gg);
            } catch (final Exception e) {
                if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                    engine = null;
                    logger.log(e);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, null, e);
            } finally {
                Context.exit();
            }
        }
    }

    private String full_path_from_hash(String hash) throws Exception {
        initEngine();
        try {
            engine.eval("var result=full_path_from_hash('" + hash + "');");
            final String result = engine.get("result").toString();
            return result;
        } catch (final Exception e) {
            if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                e.printStackTrace();
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, null, e);
        }
    }

    private String last(String[] array) {
        return array[array.length - 1];
    }

    private String url_from_hash(String galleryid, Map<String, String> image, String dir, String ext) throws Exception {
        ext = isNotBlank(ext) ? ext : (isNotBlank(dir) ? dir : last(image.get("name").split("\\.")));
        dir = isNotBlank(dir) ? dir : "images";
        return "https://a.hitomi.la/" + dir + '/' + full_path_from_hash(image.get("hash")) + '.' + ext;
    }

    private String url_from_url_from_hash(String galleryid, Map<String, String> image, String dir, String ext, String base) throws Exception {
        if ("tn".equals(base)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return url_from_url(url_from_hash(galleryid, image, dir, ext), base);
    }

    boolean isNotBlank(String str) {
        return str != null && !str.isEmpty();
    }

    @Override
    public Class<? extends HitomiLaConfig> getConfigInterface() {
        return HitomiLaConfig.class;
    }
}
