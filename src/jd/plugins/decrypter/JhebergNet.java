//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.components.UserAgents;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "jheberg.net" }, urls = { "https?://(?:www\\.|download\\.)?jheberg\\.net/(captcha/|download/|mirrors/|go/|redirect/)?[A-Z0-9a-z\\.\\-_]+" })
public class JhebergNet extends antiDDoSForDecrypt {
    public JhebergNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* 2017-01-28: They block IPs when there are too many requests in a short time. */
    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    private String agent = null;

    private Browser prepBrowser(Browser prepBr) {
        prepBr.setFollowRedirects(true);
        if (agent == null) {
            agent = UserAgents.stringUserAgent();
        }
        prepBr.getHeaders().put("User-Agent", agent);
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        prepBr.setCookie("https://jheberg.net/", "npqf_unique_user", "1");
        return prepBr;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        prepBrowser(br);
        if (StringUtils.contains(parameter, "/redirect/")) {
            br.setFollowRedirects(false);
            getPage(parameter);
            final String finallink = br.getRedirectLocation();
            if (finallink != null) {
                final DownloadLink dl = createDownloadlink(finallink.replace("\\", ""));
                ret.add(dl);
            }
            return ret;
        } else {
            final String linkID = new Regex(parameter, ".+/([A-Z0-9a-z\\.\\-_]+)").getMatch(0);
            getPage("https://download.jheberg.net/go/" + linkID);
            final String fileInfos[] = br.getRegex("<h4>\\s*(.*?)\\s*\\(\\s*<strong>\\s*([0-9\\.]+\\s*[^<]+)\\s*</").getRow(0);
            if (br.getHttpConnection().getResponseCode() == 404 || fileInfos == null || fileInfos.length == 0) {
                return ret;
            }
            getPage("https://api.jheberg.net/file/" + linkID);
            final Map<String, Object> map = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            final String id = (String) map.get("id");
            final Number size = JavaScriptEngineFactory.toLong(map.get("size"), -1);
            final FilePackage fp;
            if (id != null) {
                fp = FilePackage.getInstance();
                fp.setName(id);
            } else {
                fp = null;
            }
            final List<Map<String, Object>> links = (List<Map<String, Object>>) map.get("links");
            for (final Map<String, Object> link : links) {
                if (isAbort()) {
                    break;
                }
                final String status = (String) link.get("status");
                final Number hosterID = JavaScriptEngineFactory.toLong(link.get("hosterId"), -1);
                if (StringUtils.equalsIgnoreCase("SUCCESS", status) && hosterID != null && hosterID.longValue() != -1) {
                    final Browser br2 = br.cloneBrowser();
                    br2.setFollowRedirects(false);
                    try {
                        sleep(1000, param);
                    } catch (InterruptedException e) {
                        if (isAbort()) {
                            return ret;
                        } else {
                            throw e;
                        }
                    }
                    getPage(br2, "https://download.jheberg.net/redirect/" + linkID + "-" + hosterID);
                    final String finalLink = br2.getRedirectLocation();
                    if (finalLink != null) {
                        final DownloadLink dl = createDownloadlink(finalLink.replace("\\", ""));
                        if (!StringUtils.isEmpty(fileInfos[0])) {
                            dl.setName(fileInfos[0]);
                        } else if (id != null) {
                            dl.setName(id);
                        }
                        if (size != null && size.longValue() != -1) {
                            dl.setDownloadSize(size.longValue());
                        } else if (!StringUtils.isEmpty(fileInfos[1])) {
                            dl.setDownloadSize(SizeFormatter.getSize(fileInfos[1]));
                        }
                        if (fp != null) {
                            fp.add(dl);
                        }
                        distribute(dl);
                        ret.add(dl);
                    }
                }
            }
        }
        return ret;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }
}