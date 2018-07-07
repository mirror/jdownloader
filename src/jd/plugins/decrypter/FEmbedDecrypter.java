package jd.plugins.decrypter;

import java.util.ArrayList;

import org.appwork.utils.Regex;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.requests.PostRequest;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision: 34675 $", interfaceVersion = 2, names = { "fembed.com" }, urls = { "https?://(www\\.)?fembed.com/(f|v)/([a-zA-Z0-9_-]+)" })
public class FEmbedDecrypter extends PluginForDecrypt {
    public FEmbedDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String _filename;

    public void setFilename(String filename) {
        this._filename = filename;
    }

    public String getFilename() {
        return this._filename;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String filename = getFilename();
        String file_id = new Regex(parameter.getCryptedUrl(), "/(?:f|v)/([a-zA-Z0-9_-]+)$").getMatch(0);
        final PostRequest postRequest = new PostRequest("https://www.fembed.com/api/source/" + file_id);
        String data = br.getPage(postRequest);
        String success = PluginJSonUtils.getJson(PluginJSonUtils.unescape(data), "success");
        if (success.equals("false")) {
            DownloadLink link = createDownloadlink(parameter.getCryptedUrl().replaceAll("https?://", "decryptedforFEmbedHosterPlugin://"));
            link.setForcedFileName(filename + ".mp4");
            link.setAvailable(false);
            ret.add(link);
            return ret;
        }
        String json_data = PluginJSonUtils.getJson(PluginJSonUtils.unescape(data), "data");
        String[] json_data_ar = PluginJSonUtils.getJsonResultsFromArray(json_data);
        for (String data_ar : json_data_ar) {
            String label = PluginJSonUtils.getJson(data_ar, "label");
            String type = PluginJSonUtils.getJson(data_ar, "type");
            DownloadLink link = createDownloadlink(parameter.getCryptedUrl().replaceAll("https?://", "decryptedforFEmbedHosterPlugin://"));
            link.setProperty("label", label);
            link.setLinkID("fembed" + "." + file_id + "." + label);
            if (!filename.isEmpty()) {
                link.setFinalFileName(filename + "-" + label + "." + type);
            } else {
                link.setForcedFileName(file_id + "-" + label + "." + type);
            }
            ret.add(link);
        }
        if (json_data_ar.length > 1) {
            final FilePackage filePackage = FilePackage.getInstance();
            String title;
            if (filename != null && !filename.isEmpty()) {
                title = filename;
            } else {
                title = file_id;
            }
            filePackage.setName(title);
            filePackage.addLinks(ret);
        }
        return ret;
    }
}
