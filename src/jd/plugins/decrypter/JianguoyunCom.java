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
import java.util.Random;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;
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
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "jianguoyun.com" }, urls = { "https?://(?:www\\.)?jianguoyun\\.com/p/[A-Za-z0-9\\-_]+(?:#dir=[^<>\"/:]+)?" })
public class JianguoyunCom extends antiDDoSForDecrypt {
    public JianguoyunCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String parameter = param.getCryptedUrl().replaceFirst("(?i)http://", "https://");
        final String fid = new Regex(parameter, "/p/([A-Za-z0-9\\-_]+)").getMatch(0);
        String subfolder = new Regex(parameter, "#dir=(.+)").getMatch(0);
        if (subfolder == null) {
            subfolder = "/";
        } else {
            subfolder = Encoding.htmlDecode(subfolder);
        }
        subfolder = Encoding.urlEncode(subfolder);
        if (fid == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.setFollowRedirects(true);
        br.getPage(parameter);
        final String pageJson = jd.plugins.hoster.JianguoyunCom.getWebsiteJson(br);
        DownloadLink dl = null;
        final String isdir = new Regex(pageJson, "isdir\\s*:\\s*(true|false)").getMatch(0);
        if ("false".equals(isdir)) {
            /* Single file */
            dl = createDownloadlink("http://jianguoyundecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
            dl.setProperty("singlefile", true);
            dl.setProperty("folderid", fid);
            dl.setProperty("relPath", subfolder);
            dl.setProperty("mainlink", parameter);
            jd.plugins.hoster.JianguoyunCom.scanFileinfoFromWebsite(br, dl);
            ret.add(dl);
            return ret;
        }
        br.getPage("https://www.jianguoyun.com/d/ajax/dirops/pubDIRBrowse?hash=" + fid + "&relPath=" + subfolder + "&_=" + System.currentTimeMillis());
        Map<String, Object> entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        final List<Object> ressourcelist = (List) entries.get("objects");
        FilePackage fp = null;
        for (final Object foldero : ressourcelist) {
            entries = (Map<String, Object>) foldero;
            final String type = (String) entries.get("type");
            final String relPath = (String) entries.get("relPath");
            final long filesize = JavaScriptEngineFactory.toLong(entries.get("size"), 0);
            if (type == null || relPath == null) {
                continue;
            }
            if (type.equals("directory")) {
                dl = createDownloadlink("https://www.jianguoyun.com/p/" + fid + "#dir=" + Encoding.urlEncode(relPath));
            } else {
                dl = createDownloadlink("http://jianguoyundecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
                final Regex pathregex = new Regex(relPath, "^(/.*?)([^/]+)$");
                final String contenturl = "https://www.jianguoyun.com/p/" + fid + "#dir=" + Encoding.urlEncode(pathregex.getMatch(0));
                final String filename = pathregex.getMatch(1);
                if (filesize > 0) {
                    dl.setDownloadSize(filesize);
                    dl.setAvailable(true);
                }
                if (filename != null) {
                    dl.setName(filename);
                }
                dl.setProperty("folderid", fid);
                dl.setProperty("relPath", relPath);
                dl.setProperty("mainlink", parameter);
                dl.setContentUrl(contenturl);
                dl.setRelativeDownloadFolderPath(relPath);
                if (fp == null) {
                    fp = FilePackage.getInstance();
                    fp.setName(relPath);
                }
                dl._setFilePackage(fp);
            }
            ret.add(dl);
        }
        return ret;
    }
}
