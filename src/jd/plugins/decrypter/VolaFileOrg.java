package jd.plugins.decrypter;

import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.websocket.WebSocketClient;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.net.websocket.ReadWebSocketFrame;
import org.appwork.utils.net.websocket.WebSocketFrameHeader;

@DecrypterPlugin(revision = "$Revision: 36721 $", interfaceVersion = 2, names = { "volafile.org" }, urls = { "https?://(?:www\\.)?volafile\\.(?:org|io)/r/[A-Za-z0-9\\-_]+" })
public class VolaFileOrg extends PluginForDecrypt {
    public VolaFileOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getPage(parameter.getCryptedUrl().replace(".io/", ".org/"));
        br.followRedirect();
        final String checksum = br.getRegex("\"checksum2\"\\s*:\\s*\"(.*?)\"").getMatch(0);
        final String room = new Regex(parameter.getCryptedUrl(), "/r/([A-Za-z0-9\\-_]+)").getMatch(0);
        final WebSocketClient wsc = new WebSocketClient(br, new URL("https://volafile.org/api/?EIO=3&transport=websocket"));
        try {
            wsc.connect();
            wsc.writeFrame(wsc.buildUTF8TextFrame("4[-1,[[0,[\"subscribe\",{\"room\":\"" + room + "\",\"checksum\":\"" + checksum + "\",\"checksum2\":\"" + checksum + "\",\"nick\":\"Alke\"}]],0]]"));// subscribe
            wsc.readNextFrame();// sid
            wsc.readNextFrame();// session
            final ReadWebSocketFrame frame = wsc.readNextFrame();// subscription
            if (WebSocketFrameHeader.OP_CODE.UTF8_TEXT.equals(frame.getOpCode()) && frame.isFin()) {
                String string = new String(frame.getPayload(), "UTF-8");
                string = string.replaceFirst("^\\d+", "");
                final List<Object> list = JSonStorage.restoreFromString(string, TypeRef.LIST);
                final List<Object> files = (List<Object>) ((Map<String, Object>) ((List<Object>) ((List<Object>) ((List<Object>) list.get(4)).get(0)).get(1)).get(1)).get("files");
                for (final Object file : files) {
                    final List<Object> fileInfo = (List<Object>) file;
                    final String fileName = String.valueOf(fileInfo.get(1));
                    final Number fileSize = ((Number) fileInfo.get(3));
                    final DownloadLink downloadLink = createDownloadlink("https://volafile.org/download/" + fileInfo.get(0) + "/" + URLEncoder.encode(fileName, "UTF-8"));
                    downloadLink.setFinalFileName(fileName);
                    downloadLink.setVerifiedFileSize(fileSize.longValue());
                    downloadLink.setAvailable(true);
                    ret.add(downloadLink);
                }
            } else {
                logger.severe("Unsupported:" + frame);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } finally {
            wsc.close();
        }
        return ret;
    }
}
