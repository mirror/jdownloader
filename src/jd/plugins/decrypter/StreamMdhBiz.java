package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

import org.appwork.utils.Files;
import org.appwork.utils.Regex;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ExtensionsFilterInterface;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "stream-mydirtyhobby.biz" }, urls = { "https?://(stream-mydirtyhobby.biz|(?:old\\.|www\\.)?stream-mdh.se)(/view/.*?/.+|/video/.*?/.+)" })
public class StreamMdhBiz extends antiDDoSForDecrypt {
    public StreamMdhBiz(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String streamID = new Regex(parameter.getCryptedUrl(), "/view/.*?/(.+)").getMatch(0);
        if (streamID != null) {
            if (parameter.getCryptedUrl().contains("old.stream")) {
                getPage("https://old.stream-mdh.se/embed/" + streamID);
            } else {
                getPage("https://stream-mdh.se/embed/" + streamID);
            }
        } else {
            getPage(parameter.getCryptedUrl());
        }
        final String iFrameSrc = br.getRegex("(?i)iframe src=\"(https?://.+?)\"").getMatch(0);
        if (iFrameSrc != null) {
            final String title = br.getRegex("<title>\\s*(.*?)\\s*</title>").getMatch(0);
            final DownloadLink link = createDownloadlink(iFrameSrc);
            if (title != null) {
                final ExtensionsFilterInterface extension = CompiledFiletypeFilter.getExtensionsFilterInterface(Files.getExtension(link.getName()));
                if (CompiledFiletypeFilter.DocumentExtensions.HTML.equals(extension) || extension == null || CompiledFiletypeFilter.VideoExtensions.MP4.isSameExtensionGroup(extension)) {
                    link.setName(title);
                }
            }
            ret.add(link);
        }
        return ret;
    }
}
