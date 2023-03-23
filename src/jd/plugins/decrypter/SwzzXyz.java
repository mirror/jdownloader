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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class SwzzXyz extends MightyScriptAdLinkFly {
    public SwzzXyz(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "swzz.xyz" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:link/)?[A-Za-z0-9]+/?");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String url = param.getCryptedUrl();
        if (url.matches("https?://[^/]+/[A-Za-z0-9]+/?$")) {
            return super.decryptIt(param, progress);
        } else {
            br.setFollowRedirects(true);
            getPage(url);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML("(?i)<em>\\s*Questo Link Non è ancora attivo\\.\\.\\.riprova tra qualche istante!<em>")) {
                throw new DecrypterRetryException(RetryReason.HOST_RATE_LIMIT);
            }
            // within packed
            String finallink = br.getRegex("<a\\s*href\\s*=\\s*\"(https?[^\"]+)\"\\s*class\\s*=\\s*\"btn\\-wrapper link\"").getMatch(0);
            if (StringUtils.isEmpty(finallink)) {
                finallink = br.getRegex("var\\s*link\\s*=\\s*(\"|')([^'\"]*)").getMatch(1);
            }
            if (StringUtils.isEmpty(finallink)) {
                final String js = br.getRegex("eval\\((function\\(p,a,c,k,e,d\\)[^\r\n]+\\))\\)").getMatch(0);
                final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(null);
                final ScriptEngine engine = manager.getEngineByName("javascript");
                try {
                    engine.eval("var res = " + js + ";");
                    final String result = (String) engine.get("res");
                    finallink = new Regex(result, "var link\\s*=\\s*(\"|')(.*?)\\1").getMatch(1);
                } catch (final Exception e) {
                    logger.log(e);
                }
            }
            if (StringUtils.isEmpty(finallink)) {
                finallink = br.getRegex("href\\s*=\\s*\"(https?[^\"]+)\"\\s*role=\\s*\"button\"").getMatch(0);
            }
            if (StringUtils.isEmpty(finallink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                ret.add(createDownloadlink(finallink));
                return ret;
            }
        }
    }
}
