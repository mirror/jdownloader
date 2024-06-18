//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
package jd.plugins.hoster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class PornhitsCom extends KernelVideoSharingComV2 {
    public PornhitsCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    /** Add all KVS hosts to this list that fit the main template without the need of ANY changes to this class. */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "pornhits.com" });
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
        return PornhitsCom.buildAnnotationUrlsDefaultVideosPatternPornhits(getPluginDomains());
    }

    private final String PATERN_EMBED = "https?://[^/]+/embed\\.php\\?id=(\\d+)";

    private static String[] buildAnnotationUrlsDefaultVideosPatternPornhits(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(video/\\d+/[\\w\\-]+/|embed\\.php\\?id=\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    protected String generateContentURL(final String host, final String fuid, final String urlSlug) {
        if (host == null || fuid == null || urlSlug == null) {
            return null;
        }
        return this.getProtocol() + appendWWWIfRequired(host) + "/video/" + fuid + "/" + urlSlug + "/";
    }

    @Override
    public String getFUID(final DownloadLink link) {
        final String fuidFromEmbedURL = new Regex(link.getPluginPatternMatcher(), PATERN_EMBED).getMatch(0);
        if (fuidFromEmbedURL != null) {
            return fuidFromEmbedURL;
        } else {
            return super.getFUID(link);
        }
    }

    @Override
    protected String regexNormalTitleWebsite(final Browser br) {
        String title = br.getRegex("class=\"headline\">\\s*<h1>([^<>\"]+)<").getMatch(0);
        if (title != null) {
            return title;
        } else {
            /* Fallback to upper handling */
            return super.regexNormalTitleWebsite(br);
        }
    }

    @Override
    protected String regexEmbedTitleWebsite(final Browser br) {
        final String title = br.getRegex("vit\\s*:\\s*\"([^\"]+)").getMatch(0);
        if (title != null) {
            return title;
        } else {
            return super.regexEmbedTitleWebsite(br);
        }
    }

    @Override
    protected boolean preferTitleHTML() {
        return true;
    }

    @Override
    protected boolean isEmbedURL(final String url) {
        if (url == null) {
            return false;
        } else if (url.matches(PATERN_EMBED)) {
            return true;
        } else {
            return super.isEmbedURL(url);
        }
    }

    @Override
    protected String getDllink(final DownloadLink link, final Browser br) throws PluginException, IOException {
        String magicString = br.getRegex("invideo_class\\}\\}\\}, '([^\\']+)").getMatch(0);
        if (magicString == null) {
            /* When we're on embed page. */
            magicString = br.getRegex("'([^\\']+)', null\\);\\s*\\} else \\{setTimeout").getMatch(0);
        }
        if (magicString == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String json = decryptMagic(magicString);
        final List<Map<String, Object>> qualities = (List<Map<String, Object>>) restoreFromString(json, TypeRef.OBJECT);
        if (qualities == null || qualities.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (final Map<String, Object> quality : qualities) {
            final String format = quality.get("format").toString();
            final String dllink = this.decryptMagic(quality.get("video_url").toString());
            if (format.equalsIgnoreCase("_hq.mp4")) {
                /* Prefer best */
                return dllink;
            }
        }
        return null;
    }

    /** Magic since april 2023. */
    private String decryptMagic(final String magic) throws PluginException {
        final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(this);
        final ScriptEngine engine = manager.getEngineByName("javascript");
        final StringBuilder sb = new StringBuilder();
        sb.append("function base164decode (e) { var t = 'АВСDЕFGHIJKLМNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.,~',    n = '', r = 0;  /[^АВСЕМA-Za-z0-\\\\.\\\\,\\\\~]/g.exec(e) && this.log('error decoding url'),  e = e.replace(/[^АВСЕМA-Za-z0-9\\\\.\\\\,\\\\~]/g, ''); do {      var i = t.indexOf(e.charAt(r++)),   o = t.indexOf(e.charAt(r++)),   a = t.indexOf(e.charAt(r++)),   s = t.indexOf(e.charAt(r++));   i = i << 2 | o >> 4,    o = (15 & o) << 4 | a >> 2;     var l = (3 & a) << 6 | s;   n += String.fromCharCode(i),    64 != a && (n += String.fromCharCode(o)),   64 != s && (n += String.fromCharCode(l))  } while (r < e.length); return unescape(n)  };");
        sb.append("var res = base164decode('" + magic + "');");
        try {
            engine.eval(sb.toString());
            return engine.get("res").toString();
        } catch (final Exception e) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, null, e);
        }
    }
}