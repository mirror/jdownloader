package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "oe1.orf.at" }, urls = { "https?://oe1\\.orf\\.at/(player|programm)/\\d+/\\d+" })
public class Oe1OrfAt extends PluginForDecrypt {
    public Oe1OrfAt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final String broadCastID = new Regex(parameter.getCryptedUrl(), "/(?:player|programm)/\\d+/(\\d+)").getMatch(0);
        final String broadCastKey = new Regex(parameter.getCryptedUrl(), "/(?:player|programm)/(\\d+)/\\d+").getMatch(0);
        br.getPage("https://audioapi.orf.at/oe1/api/json/current/broadcast/" + broadCastID + "/" + broadCastKey + "?_s=" + System.currentTimeMillis());
        final Map<String, Object> response = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final String broadCastDay = response.get("broadcastDay").toString();
        final String title = (String) response.get("title");
        final List<Map<String, Object>> streams = (List<Map<String, Object>>) response.get("streams");
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title + "_" + broadCastDay);
        int index = 0;
        final String userid = UUID.randomUUID().toString();
        for (Map<String, Object> stream : streams) {
            final String loopStreamId = (String) stream.get("loopStreamId");
            if (loopStreamId == null) {
                continue;
            }
            final DownloadLink link = createDownloadlink("directhttp://http://loopstream01.apa.at/?channel=oe1&shoutcast=0&player=oe1_v1&referer=oe1.orf.at&_=" + System.currentTimeMillis() + "&userid=" + userid + "&id=" + loopStreamId);
            if (streams.size() > 1) {
                link.setFinalFileName(title + "_" + broadCastDay + "_" + (++index) + ".mp3");
            } else {
                link.setFinalFileName(title + "_" + broadCastDay + ".mp3");
            }
            link.setProperty("requestType", "GET");
            link.setAvailable(true);
            link.setLinkID(getHost() + "://" + broadCastID + "/" + broadCastKey + "/" + index);
            ret.add(link);
            fp.add(link);
        }
        return ret;
    }
}
