package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.requests.GetRequest;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.net.HTTPHeader;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "gofile.io" }, urls = { "https?://(?:www\\.)?gofile\\.io/(#download#|\\?c=)[A-Za-z0-9]+$" })
public class GoFileIo extends PluginForDecrypt {
    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String c = new Regex(parameter.getCryptedUrl(), "(#download#|\\?c=)([A-Za-z0-9]+)").getMatch(1);
        br.getPage(parameter.getCryptedUrl());
        final GetRequest server = br.createGetRequest("https://apiv2.gofile.io/getServer?c=" + c);
        server.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_ORIGIN, "https://gofile.io"));
        Browser brc = br.cloneBrowser();
        brc.getPage(server);
        Map<String, Object> response = JSonStorage.restoreFromString(brc.toString(), TypeRef.HASHMAP);
        String serverHost = null;
        if ("ok".equals(response.get("status"))) {
            final Map<String, Object> data = (Map<String, Object>) response.get("data");
            serverHost = (String) data.get("server");
        } else if ("error".equals(response.get("status"))) {
            return ret;
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (serverHost == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final GetRequest post = br.createGetRequest("https://" + serverHost + ".gofile.io/getUpload?c=" + c);
        post.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_ORIGIN, "https://gofile.io"));
        brc = br.cloneBrowser();
        brc.getPage(post);
        response = JSonStorage.restoreFromString(brc.toString(), TypeRef.HASHMAP);
        if ("ok".equals(response.get("status"))) {
            final Map<String, Object> data = (Map<String, Object>) response.get("data");
            final Map<String, Map<String, Object>> files = (Map<String, Map<String, Object>>) data.get("files");
            for (final Entry<String, Map<String, Object>> file : files.entrySet()) {
                final String fileID = file.getKey();
                final DownloadLink link = createDownloadlink("https://gofile.io/?c=" + c + "#file=" + fileID);
                final Map<String, Object> entry = file.getValue();
                final Number size = JavaScriptEngineFactory.toLong(entry.get("size"), -1);
                if (size.longValue() >= 0) {
                    link.setDownloadSize(size.longValue());
                }
                final String name = (String) entry.get("name");
                if (name != null) {
                    link.setFinalFileName(name);
                }
                link.setAvailable(true);
                ret.add(link);
            }
        }
        return ret;
    }
}
