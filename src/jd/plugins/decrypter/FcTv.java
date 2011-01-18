package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.html.XPath;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.Regex;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "freecaster.tv" }, urls = { "http://[\\w\\.]*?freecaster\\.tv/[a-zA-Z]+/[0-9]+/[a-zA-Z0-9-]+" }, flags = { 0 })
public class FcTv extends PluginForDecrypt {

    private static String host = "gateway.freecaster.tv";

    public FcTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String param = parameter.toString();

        this.setBrowserExclusive();
        br.getPage(param);
        String[] url_elements = param.split("/");
        String video_name = url_elements[url_elements.length - 1];

        String swf_url = br.getRegex("<link rel=\"video_src\" type=\"\" href=\"(.*?)\"").getMatch(0);
        Regex id_matcher = new Regex(swf_url, "id=(.*?)&");
        String video_id = id_matcher.getMatch(0);
        String base64video_link = Encoding.Base64Encode(swf_url);

        String xml_url = "http://" + host + "/VP/" + video_id + "/" + base64video_link;
        String xml = br.getPage(xml_url);

        String query = "/request/streams/stream";
        XPath xpath = new XPath(xml, query, false);
        NodeList streams = xpath.getMatchesAsNodeList();
        FilePackage fp = FilePackage.getInstance();
        fp.setName(video_name);
        for (int i = 0; i < streams.getLength(); i++) {
            Node node = streams.item(i);
            String quality = node.getParentNode().getAttributes().getNamedItem("quality").getTextContent();
            String link = node.getTextContent();
            String[] names = link.split("\\.");
            String filename = video_name + "(" + quality + ")" + "." + names[names.length - 1];
            DownloadLink thislink = createDownloadlink(link);
            thislink.setBrowserUrl(link);
            thislink.setFinalFileName(filename);
            thislink.setProperty("filename", filename);
            decryptedLinks.add(thislink);
        }
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

}
