//    jDownloader - Downloadmanager
//    Copyright (C) 2015  JD-Team support@jdownloader.org
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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.http.Browser;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storable;
import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.envjs.EnvJSBrowser;
import org.jdownloader.scripting.envjs.EnvJSBrowser.DebugLevel;
import org.jdownloader.scripting.envjs.PermissionFilter;
import org.jdownloader.scripting.envjs.XHRResponse;

/**
 * HAHAHAHA
 *
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "downl.ink" }, urls = { "https?://(?:www\\.)?downl\\.ink/(?-i)[a-f0-9]{6}" }) 
@SuppressWarnings("deprecation")
public class DownlInk extends antiDDoSForDecrypt {

    public DownlInk(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String parameter = null;

    @Override
    protected Browser prepBrowser(Browser prepBr, String host) {
        super.prepBrowser(prepBr, host);
        prepBr.addAllowedResponseCodes(401);
        return prepBr;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        parameter = param.toString();
        br = new Browser();
        // http can redirect to https
        br.setFollowRedirects(true);
        getPage(parameter);
        if ("Page not found".equalsIgnoreCase(brOut)) {
            return decryptedLinks;
        }
        // recaptchav2 can be here, they monitor based ip ? or maybe cloudflare cookie
        final Form captcha = br.getForm(0);
        if (captcha != null && captcha.containsHTML("class=(\"|')g-recaptcha\\1")) {
            final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
            captcha.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            // no need for runPostRequestTask. usually cloudflare event is on FIRST request, so lets bypass.
            br.submitForm(captcha);
            // they will respond with 401 here which can throw exception without response code adding.

            // then another get here, here comes the JS we need
            getPage(br.getURL());
        }
        // the single download link, returned in iframe
        final String[] iframes = envJs.getRegex("<\\s*iframe\\s+[^>]+>").getColumn(-1);
        if (iframes != null && iframes.length > 0) {
            for (final String iframe : iframes) {
                String link = new Regex(iframe, "src\\s*=\\s*(\"|')(.*?)\\1").getMatch(1);
                if (inValidate(link)) {
                    link = new Regex(iframe, "src\\s*=\\s*([^\\s]+)").getMatch(0);
                }
                if (!inValidate(link)) {
                    decryptedLinks.add(createDownloadlink(link));
                }
            }
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    public static class enjStorable implements Storable {
        public enjStorable(/* storable */) {

        }

        private ArrayList<Integer> usedScripts;
        private long               disabledUntil = 0;

        public long getDisabledUntil() {
            return disabledUntil;
        }

        public void setDisabledUntil(long disabledUntil) {
            this.disabledUntil = disabledUntil;
        }

        public ArrayList<Integer> getUsedScripts() {
            return usedScripts;
        }

        public void setUsedScripts(ArrayList<Integer> usedScripts) {
            this.usedScripts = usedScripts;
        }

    }

    @Override
    protected void runPostRequestTask(final Browser ibr) {
        brOut = br.toString();
        final Form captcha = ibr.getForm(0);
        if (captcha == null || !captcha.containsHTML("class=(\"|')g-recaptcha\\1")) {
            // recaptcha will fail here
            dothis(ibr);
        }
    }

    private LinkedHashSet<String> dupe  = new LinkedHashSet<String>();
    private EnvJSBrowser          envJs = null;
    private String                brOut = null;

    private void dothis(final Browser br) {
        // envjs
        synchronized (DownloadWatchDog.getInstance()) {
            envJs = new EnvJSBrowser(br) {
                @Override
                public String loadExternalScript(String type, String src, String url, Object window) {

                    return super.loadExternalScript(type, src, url, window);
                }

                public String xhrRequest(String url, String method, String data, String requestHeaders) throws java.io.IOException {
                    // http://code.jquery.com/jquery-1.4.4.js
                    if (br.getURL().equals(url)) {

                        XHRResponse ret = new XHRResponse();
                        for (Entry<String, List<String>> s : br.getRequest().getResponseHeaders().entrySet()) {
                            ret.getResponseHeader().put(s.getKey(), s.getValue().get(0));
                        }
                        ret.setEncoding(br.getRequest().getHttpConnection().getHeaderField("Content-Encoding"));
                        ret.setReponseMessage(br.getRequest().getHttpConnection().getResponseMessage());
                        ret.setResponseCode(br.getRequest().getHttpConnection().getResponseCode());
                        ret.setResponseText(br.getRequest().getHtmlCode());
                        return JSonStorage.serializeToJson(ret);
                    } else {
                        return "";
                    }

                };
            };
            envJs.setPermissionFilter(getPermissionFilter());
            envJs.setUserAgent(br.getHeaders().get("User-Agent"));
            envJs.setDebugLevel(DebugLevel.NONE);
            envJs.getPage(br.getURL());
            br.getRequest().setHtmlCode(envJs.getDocument());
        }
    }

    private PermissionFilter getPermissionFilter() {

        return new PermissionFilter() {

            @Override
            public Request onBeforeXHRRequest(Request request) {
                // only load websites with the same domain.
                try {
                    if (StringUtils.equalsIgnoreCase(new URL(request.getUrl()).getHost(), new URL(parameter).getHost())) {
                        return request;
                    }
                } catch (MalformedURLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public Request onBeforeLoadingExternalJavaScript(String type, String url, Request request) {
                if (!request.getUrl().contains(getHost() + "/")) {
                    return null;
                }
                try {
                    if (url.matches(".+/jquery-\\d+\\.\\d+\\.\\d+\\.min\\.js")) {
                        dupe.add(url);
                        return request;
                    }
                    return null;
                } catch (final Throwable e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public String onBeforeExecutingInlineJavaScript(String type, String js) {
                String trimmed = js.trim();
                // filter javascript.
                if (js.contains("liveinternet")) {
                    // ads ... do not evaluate
                    return "console.log('Blocked js')";
                }
                if (trimmed.startsWith("(function(i,s,o,g,r,a,m)")) {
                    return "console.log('Do not Excute');";
                }
                if (trimmed.startsWith("(function(s,o,l,v,e,d)")) {
                    return "console.log('Do not Excute');";
                }
                return trimmed;
            }

            @Override
            public void onAfterXHRRequest(Request request, XHRResponse ret) {
            }

            @Override
            public String onAfterLoadingExternalJavaScript(String type, String src, String sourceCode, Request request) {
                return sourceCode;
            }
        };
    }

}