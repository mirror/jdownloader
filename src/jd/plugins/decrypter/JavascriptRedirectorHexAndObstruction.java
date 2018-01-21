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

import org.jdownloader.scripting.envjs.EnvJSBrowser;
import org.jdownloader.scripting.envjs.EnvJSBrowser.DebugLevel;
import org.jdownloader.scripting.envjs.PermissionFilter;
import org.jdownloader.scripting.envjs.XHRResponse;

import org.appwork.utils.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Request;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

/**
 * @keywords hex, 'var link', 'var _0xdc0b'
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "jerkplanet.org", "superload.me" }, urls = { "https?://(www\\.)?jerkplanet\\.org/file/[0-9a-zA-Z]+", "(?:http://(www\\.)?superload\\.me|https://(?:www\\.)?superload\\.me(?:443)?)/file/[0-9a-zA-Z]+" })
public class JavascriptRedirectorHexAndObstruction extends PluginForDecrypt {

    public JavascriptRedirectorHexAndObstruction(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final String parameter = param.toString();
        final EnvJSBrowser envJs = new EnvJSBrowser(br) {

            @Override
            public String onBeforeSourceCompiling(String source, net.sourceforge.htmlunit.corejs.javascript.Evaluator compiler, net.sourceforge.htmlunit.corejs.javascript.ErrorReporter compilationErrorReporter, String sourceName, int lineno, Object securityDomain) {
                if (this.isInitDone() && StringUtils.isNotEmpty(source)) {
                    // System.out.println("Execute: \r\n" + source);
                }
                return super.onBeforeSourceCompiling(source, compiler, compilationErrorReporter, sourceName, lineno, securityDomain);
            };
        };
        envJs.setDebugLevel(DebugLevel.INFO);

        envJs.setPermissionFilter(new PermissionFilter() {

            @Override
            public String onBeforeExecutingInlineJavaScript(String type, String js) {
                if (js.contains("liveinternet")) {
                    // ads ... do not evaluate
                    return "console.log('Blocked js')";
                }
                // I use it to filter ads or other not required stuff
                return js;
            }

            @Override
            public Request onBeforeXHRRequest(Request request) {
                try {
                    // only load websites with the same domain.
                    if (StringUtils.equalsIgnoreCase(new URL(request.getUrl()).getHost(), JavascriptRedirectorHexAndObstruction.this.getHost())) {
                        return request;
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }

                // do not load the request
                return null;
            }

            @Override
            public Request onBeforeLoadingExternalJavaScript(String type, String src, Request request) {
                // do not load external js
                return null;
            }

            @Override
            public void onAfterXHRRequest(Request request, XHRResponse ret) {
                JavascriptRedirectorHexAndObstruction.this.getLogger().info(request + "");
            }

            @Override
            public String onAfterLoadingExternalJavaScript(String type, String src, String sourceCode, Request request) {

                return sourceCode;
            }
        });

        envJs.getPage(parameter);
        final String decoded = envJs.getDocument();
        final String link = new Regex(decoded, "<iframe [^>]*src=('|\")(.*?)\\1").getMatch(1);
        if (link != null) {
            decryptedLinks.add(createDownloadlink(link));
        }

        return decryptedLinks;
    }

    // @Override
    // public SiteTemplate siteTemplateType() {
    // return SiteTemplate.Unknown_JavascriptRedirectorHexAndObstruction;
    // }

}
