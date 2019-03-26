package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jd.controlling.ProgressController;
import jd.http.requests.PostRequest;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.net.HTTPHeader;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision: 40170 $", interfaceVersion = 3, names = { "gofile.io" }, urls = { "https?://(?:www\\.)?gofile\\.io/\\?c=[A-Za-z0-9]+$" })
public class GoFileIo extends PluginForDecrypt {
    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String id = new Regex(parameter.getCryptedUrl(), "c=([A-Za-z0-9]+)").getMatch(0);
        br.getPage(parameter.getCryptedUrl());
        final PostRequest post = br.createPostRequest("https://api.gofile.io/getUpload.php?c=" + id, "");
        post.getHeaders().put(new HTTPHeader(HTTPConstants.HEADER_REQUEST_ORIGIN, "https://gofile.io"));
        br.getPage(post);
        final Map<String, Object> response = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        if ("ok".equals(response.get("status"))) {
            int index = 0;
            final List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
            for (Map<String, Object> entry : data) {
                final int fileIndex = index + 1;
                index = fileIndex;
                final Number size = JavaScriptEngineFactory.toLong(entry.get("size"), -1);
                final Number fileID = JavaScriptEngineFactory.toLong(entry.get("id"), -1);
                final DownloadLink link = createDownloadlink("https://gofile.io/?c=" + id + "#index=" + fileIndex + "&id=" + fileID);
                link.setContentUrl(parameter.getCryptedUrl());
                if (size.longValue() >= 0) {
                    // not verified!
                    link.setDownloadSize(size.longValue());
                }
                final String name = (String) entry.get("name");
                if (name != null) {
                    link.setFinalFileName(name);
                }
                ret.add(link);
            }
        }
        return ret;
    }
}
