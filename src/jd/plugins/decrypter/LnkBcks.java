//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.SiteType.SiteTemplate;

// DEV NOTES:
// 162.158.0.0/15 cloudflare
// 141.101.64.0 - 141.101.127.255 cloudflare
// 190.93.240/20 cloudflare
// 198.41.128.0/17 cloudflare
// INFORELAY-LAX2-02   (NET-173-205-128-0-1) 173.205.128.0 - 173.205.255.255
// LINKBUCKS LINKBUCKS (NET-173-205-185-80-1) 173.205.185.80 - 173.205.185.95
// www.uid.domain redirects to linkbucks homepage on https.

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class LnkBcks extends antiDDoSForDecrypt {

    /**
     * Returns the annotations names array
     *
     * @return
     */
    public static String[] getAnnotationNames() {
        return new String[] { "jzrputtbut.net", "miniurls.co", "zatnawqy.net", "tnabucks.com", "cash4files.com", "megaline.co", "qqc.co", "theseblogs.com", "theseforums.com", "ultrafiles.net", "urlbeat.net", "whackyvidz.com", "yyv.co", "drstickyfingers.com", "fapoff.com", "freean.us", "galleries.bz", "hornywood.tv", "picbucks.com", "poontown.net", "rqq.co", "youfap.me", "zff.co", "tubeviral.com", "whackyvidz.com", "linkbabes.com", "dyo.gs", "filesonthe.net", "cash4files.com", "seriousdeals.net", "any.gs", "goneviral.com", "ultrafiles.net", "tinylinks.co", "yyv.co", "allanalpass.com", "linkbucks.com" };
    }

    /**
     * returns the annotation pattern array
     *
     * @return
     */
    public static String[] getAnnotationUrls() {
        String[] a = new String[getAnnotationNames().length];
        int i = 0;
        for (final String domain : getAnnotationNames()) {
            a[i] = "http://(?:[a-f0-9]{8}\\." + Pattern.quote(domain) + "/|(?:www\\.)?" + Pattern.quote(domain) + "/(?:[a-f0-9]{8}/url/.+|[0-9a-zA-Z]{5}/url/[a-f0-9]+|(?:link/)?[a-f0-9]{8}|[a-zA-Z0-9]{4,5}))$";
            i++;
        }
        return a;
    }

    public LnkBcks(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    protected boolean useRUA() {
        return true;
    }

    @Override
    protected Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(browserPrepped.containsKey(prepBr) && browserPrepped.get(prepBr) == Boolean.TRUE)) {
            prepBr.getHeaders().put("Accept-Charset", null);
        }
        return super.prepBrowser(prepBr, host);
    }

    private final String surveyLink = "To access the content, you must complete a quick survey\\.";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br = new Browser();
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        // urls containing /link/ are no longer valid, but uid seems to be transferable.
        String parameter = param.toString().replace("/link/", "");
        {
            // these link formats are derived from secondary url, without this information website takes you to some random website.
            String linkInsideLink = new Regex(parameter, "/[a-f0-9]{8}/url/(.+)").getMatch(0);
            if (linkInsideLink != null) {
                if (!StringUtils.startsWithCaseInsensitive(linkInsideLink, "http") && !StringUtils.startsWithCaseInsensitive(linkInsideLink, "ftp")) {
                    // assume they are http
                    linkInsideLink = "http://" + linkInsideLink;
                }
                decryptedLinks.add(createDownloadlink(linkInsideLink));
                return decryptedLinks;
            }
        }

        br.setFollowRedirects(false);
        getPage(parameter);
        long firstGet = System.currentTimeMillis();
        String link = br.getRedirectLocation();
        if (!inValidate(link)) {
            final String puid = getUid(parameter);
            final String cuid = getUid(link);
            // unsupported site but we should grab the link!
            if (StringUtils.equals(puid, cuid)) {
                boolean isMatch = false;
                final String linkhost = Browser.getHost(link);
                for (final String host : getAnnotationNames()) {
                    if (StringUtils.equals(linkhost, host)) {
                        isMatch = true;
                        break;
                    }
                }
                if (!isMatch) {
                    // we don't know about this site, lets post about it!
                    org.jdownloader.statistics.StatsManager.I().track("linkbucks/unidentified_domain/" + Browser.getHost(link));
                }
                getPage(link);
                link = br.getRedirectLocation();
            }
        }
        if ((link != null && link.contains("/notfound/")) || br.containsHTML("(>Link Not Found<|>The link may have been deleted by the owner|" + surveyLink + ")")) {
            decryptedLinks.add(createOfflinelink(parameter, (br.containsHTML(surveyLink) ? "JD NOTE - We don't support surveys" : null), (br.containsHTML(surveyLink) ? "JD NOTE - We don't support surveys" : null)));
            return decryptedLinks;
        }
        if (inValidate(link)) {
            link = br.getRegex(Pattern.compile("<div id=\"lb_header\">.*?/a>.*?<a.*?href=\"(.*?)\".*?class=\"lb", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        }
        if (inValidate(link)) {
            link = br.getRegex(Pattern.compile("AdBriteInit\\(\"(.*?)\"\\)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        }
        if (inValidate(link)) {
            link = br.getRegex(Pattern.compile("Linkbucks\\.TargetUrl = '(.*?)';", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        }
        if (inValidate(link)) {
            link = br.getRegex(Pattern.compile("Lbjs\\.TargetUrl = '(http://[^<>\"]*?)'", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        }
        if (inValidate(link)) {
            link = br.getRegex(Pattern.compile("src=\"http://static\\.linkbucks\\.com/tmpl/mint/img/lb\\.gif\" /></a>.*?<a href=\"(.*?)\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        }
        if (inValidate(link)) {
            link = br.getRegex(Pattern.compile("noresize=\"[0-9+]\" src=\"(http.*?)\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
            if (inValidate(link)) {
                link = br.getRegex(Pattern.compile("\"frame2\" frameborder.*?src=\"(.*?)\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
            }
        }
        if (inValidate(link)) {
            link = br.getRegex(Pattern.compile("id=\"content\" src=\"([^\"]*)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        }
        int count = 0;
        goAgain: for (int i = 0; i <= 1; i++) {
            if (inValidate(link)) {
                // thx FRD, slightly adapted to JD
                // scan for js, they repeat, usually last wins (in browser...)
                String[] jss = br.getRegex("(<script type=\"text/javascript\">[^<]+</script>)").getColumn(0);
                if (jss == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                String js = null;
                for (String j : jss) {
                    // cleanup
                    j = j.replaceAll("[\r\n\\s]+\\/\\/\\s*[^\r\n]+", "");
                    if (new Regex(j, "\\s*var\\s*f\\s*=\\s*window\\['init'\\s*\\+\\s*'Lb'\\s*\\+\\s*'js'\\s*\\+\\s*''\\];[\r\n\\s]+").matches()) {
                        js = j;
                    }
                }
                if (js == null) {
                    // not always an error.. see http://www.linkbucks.com/Cdx4H
                    if (br.getHttpConnection().getContentLength() < 1024) {
                        return decryptedLinks;
                    }
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                String token = new Regex(js, "Token\\s*:\\s*'([a-f0-9]{40})'").getMatch(0);
                if (token == null) {
                    token = new Regex(js, "\\?t=([a-f0-9]{40})").getMatch(0);
                }
                final String authKeyMatchStr = "A(?:'\\s*\\+\\s*')?u(?:'\\s*\\+\\s*')?t(?:'\\s*\\+\\s*')?h(?:'\\s*\\+\\s*')?K(?:'\\s*\\+\\s*')?e(?:'\\s*\\+\\s*')?y";
                final String l1 = new Regex(js, "\\s*params\\['" + authKeyMatchStr + "'\\]\\s*=\\s*(\\d+?);").getMatch(0);
                final String l2 = new Regex(js, "\\s*params\\['" + authKeyMatchStr + "'\\]\\s*=\\s?params\\['" + authKeyMatchStr + "'\\]\\s*\\+\\s*(\\d+?);").getMatch(0);
                if (l1 == null || l2 == null || token == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                {
                    // javascript check!
                    final Browser j = br.cloneBrowser();
                    j.getHeaders().put("Accept", "*/*");
                    j.getHeaders().put("Cache-Control", null);
                    getPage(j, "/scripts/jquery.js?r=" + token);
                }

                // for uid/url/hash links, seem to be the only ones encoded, but for now we will use the JS to determine it as its more
                // likely to be correct longer -- raztoki 20150425
                final String urlEncoded = new Regex(js, "UrlEncoded\\s*:\\s*(true|false|null)").getMatch(0);
                // if (parameter.contains("/url/")) {
                if ("true".equalsIgnoreCase(urlEncoded)) {
                    final String tt = new Regex(js, "TargetUrl:\\s*'([a-f0-9]+)'").getMatch(0);
                    final String x = this.convertFromHex(tt);
                    final String y = this.encode(x);
                    if (y != null) {
                        link = y;
                    }
                } else {
                    final long authKey = Long.parseLong(l1) + Long.parseLong(l2);
                    final Browser br2 = br.cloneBrowser();
                    // swf
                    final String swf = new Regex(js, "SwfUrl\\s*:\\s*('|\")(.*?)\\1").getMatch(1);
                    if (swf != null) {
                        URLConnectionAdapter con = null;
                        try {
                            con = br2.openGetConnection(swf);
                        } catch (final Throwable t) {
                        } finally {
                            try {
                                con.disconnect();
                            } catch (final Throwable e) {
                            }
                        }
                    }
                    final String adurl = new Regex(js, "AdUrl\\s*:\\s*('|\")(.*?)\\1").getMatch(1);
                    if (adurl != null) {
                        final Browser ads = br.cloneBrowser();
                        try {
                            getPage(ads, adurl);
                        } catch (final Throwable t) {
                        }
                    }
                    final long timeLeft = 5033 - (System.currentTimeMillis() - firstGet);
                    if (timeLeft > 0) {
                        sleep(timeLeft, param);
                    }
                    final Browser br3 = br.cloneBrowser();
                    br3.getHeaders().put("Accept", "*/*");
                    br3.getHeaders().put("Cache-Control", null);
                    getPage(br3, "/intermission/loadTargetUrl?t=" + token + "&aK=" + authKey + "&a_b=false");
                    link = PluginJSonUtils.getJsonValue(br3, "Url");
                    if ("awe".equalsIgnoreCase(link)) {
                        // more steps y0!
                        getPage("/awe");
                        firstGet = System.currentTimeMillis();
                        link = null;
                        // prevent inf loop.
                        if (++count > 4) {
                            logger.warning("Decrypter broken for link: " + parameter + " Count == " + count);
                        }
                        continue goAgain;
                    }
                }
                break;
            }
        }
        if (inValidate(link)) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(link));

        return decryptedLinks;
    }

    private String getUid(String link) {
        String uid = new Regex(link, "https?://([a-f0-9]{8})\\.[a-z0-9]+\\.[a-z0-9]+/").getMatch(0);
        if (uid == null) {
            uid = new Regex(link, "(?:www\\.)?[a-z0-9]+\\.[a-z0-9]+/([a-f0-9]{8})/url/.+").getMatch(0);
            if (uid == null) {
                uid = new Regex(link, "(?:www\\.)?[a-z0-9]+\\.[a-z0-9]+/([0-9a-zA-Z]{5})/url/[a-f0-9]+").getMatch(0);
                if (uid == null) {
                    uid = new Regex(link, "(?:www\\.)?[a-z0-9]+\\.[a-z0-9]+/(?:link/)?([a-f0-9]{8})").getMatch(0);
                    if (uid == null) {
                        uid = new Regex(link, "(?:www\\.)?[a-z0-9]+\\.[a-z0-9]+/([a-zA-Z0-9]{4,5})").getMatch(0);
                    }
                }
            }
        }
        return uid;
    }

    @Override
    protected boolean inValidate(final String s) {
        return ("about:blank").equalsIgnoreCase(s) || super.inValidate(s);
    }

    /**
     * translated into Java equivalent from source http://static.linkbucks.com/scripts/intermissionLink.js
     *
     * if (this.UrlEncoded == true) { this.TargetUrl = this.Encode(this.ConvertFromHex(this.TargetUrl)); }
     *
     *
     * @param input
     * @return
     * @author raztoki
     *
     */
    private String encode(final String input) {
        try {
            int[] s = new int[256];
            int j = 0;
            int i, x, y;
            String res = "";
            final String k = "function(str){vars=[],j=0,x,res='',k=arguments.callee.toString().replace(/\\s+/g,\"\");for(vari=0;i<256;i++){s[i]=i;}for(i=0;i<256;i++){j=(j+s[i]+k.charCodeAt(i%k.length))%256;x=s[i];s[i]=s[j];s[j]=x;}i=0;j=0;for(vary=0;y<str.length;y++){i=(i+1)%256;j=(j+s[i])%256;x=s[i];s[i]=s[j];s[j]=x;res+=String.fromCharCode(str.charCodeAt(y)^s[(s[i]+s[j])%256]);}returnres;}";

            for (i = 0; i < 256; i++) {
                s[i] = i;
            }
            for (i = 0; i < 256; i++) {
                j = (j + s[i] + k.codePointAt((i % k.length()))) % 256;
                x = s[i];
                s[i] = s[j];
                s[j] = x;
            }
            i = 0;
            j = 0;
            for (y = 0; y < input.length(); y++) {
                i = (i + 1) % 256;
                j = (j + s[i]) % 256;
                x = s[i];
                s[i] = s[j];
                s[j] = x;
                res += Character.toString((char) (input.codePointAt(y) ^ s[(s[i] + s[j]) % 256]));
            }
            return res;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *
     * @param input
     * @return
     * @author raztoki
     *
     */
    private String convertFromHex(final String input) {
        try {
            String playwiththis = input;
            String result = "";
            while (playwiththis.length() >= 2) {
                final String value = playwiththis.substring(0, 2);
                final int vi = Integer.parseInt(value, 16);
                result += Character.toString((char) vi);
                playwiththis = playwiththis.substring(2, playwiththis.length());
            }
            return result;
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.Linkbucks_Linkbucks;
    }

}