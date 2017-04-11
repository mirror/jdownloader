//jDownloader - Downloadmanager
//Copyright (C) 2016  JD-Team support@jdownloader.org
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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.http.Browser;
import jd.http.Request;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.storage.JSonStorage;
import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.envjs.EnvJSBrowser;
import org.jdownloader.scripting.envjs.EnvJSBrowser.DebugLevel;
import org.jdownloader.scripting.envjs.PermissionFilter;
import org.jdownloader.scripting.envjs.XHRResponse;

/**
 * @author raztoki
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "footfetishvideos.net" }, urls = { "https?://(?:www\\.)?footfetishvideos\\.net/(?:file/[a-z0-9]{14}|[^/\\s]+-siterip/?)" }) 
public class FtFthVidNt extends PluginForDecrypt {

    @SuppressWarnings("deprecation")
    public FtFthVidNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    // uid is needed, the rest isn't
    private final String            type_captcha   = "https?://(?:www\\.)?footfetishvideos\\.net/file/[a-z0-9]{14}";
    private final String            type_siterip   = "https?://(?:www\\.)?footfetishvideos\\.net/[^/\\s]+-siterip/?";
    private CryptedLink             param          = null;
    private String                  parameter      = null;
    private ArrayList<DownloadLink> decryptedLinks = null;
    private FilePackage             fp             = null;

    @SuppressWarnings("deprecation")
    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        decryptedLinks = new ArrayList<DownloadLink>();
        br = new Browser();
        this.param = param;

        // no https, redirects to camelhost.net
        parameter = param.toString().replaceFirst("^https://", "http://");

        br.setFollowRedirects(true);
        br.getPage(parameter);

        // error handling here
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }

        // ok two page types
        if (parameter.matches(type_captcha)) {
            // so we have a backup
            brOut = br.toString();
            dothis();
            processCaptchaType();
        }
        if (parameter.matches(type_siterip)) {
            processSiterip();
        }

        if (fp != null) {
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /**
     * these are direct links to click.tf.
     **/
    private void processSiterip() {
        String fpName = br.getRegex("<h1 class=\"entry-title[^>]*>(.*? SITERIP)</h1>").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("<title>(.*? SITERIP)").getMatch(0);
        }
        if (fpName != null) {
            fp = FilePackage.getInstance();
            fp.setName(fpName);
        }
        final String[] links = br.getRegex(type_captcha).getColumn(-1);
        if (links != null) {
            for (final String link : links) {
                decryptedLinks.add(createDownloadlink(link));
            }
        }
    }

    /**
     * these are indirect links protected by javascript
     **/
    private void processCaptchaType() {
        // ok we have javascript stuff to run.
        final String url = br.getRegex("<iframe[^>]* src=(\"|')(.*?)\\1").getMatch(1);
        if (StringUtils.isNotEmpty(url)) {
            decryptedLinks.add(createDownloadlink(url));
        }
    }

    public boolean hasCaptcha(final CryptedLink link, final jd.plugins.Account acc) {
        // whilst there is captcha its done in another plugin!
        return false;
    }

    private String brOut = null;

    private void dothis() {
        // envjs
        synchronized (DownloadWatchDog.getInstance()) {
            mewEnvJs();
            envJs.setPermissionFilter(getPermissionFilter());
            envJs.setUserAgent(br.getHeaders().get("User-Agent"));
            envJs.setDebugLevel(DebugLevel.INFO);
            envJs.getPage(br.getURL());
            br.getRequest().setHtmlCode(envJs.getDocument());
        }
    }

    private EnvJSBrowser envJs = null;

    private void mewEnvJs() {
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
                if (!request.getUrl().contains("footfetishvideos.net/")) {
                    return null;
                }
                try {
                    if (url.matches(".+/jquery-\\d+\\.\\d+\\.\\d+\\.min\\.js")) {
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