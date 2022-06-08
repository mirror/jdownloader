package jd.plugins.decrypter;

import java.util.ArrayList;

import org.appwork.utils.Files;
import org.appwork.utils.Regex;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ExtensionsFilterInterface;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "stream-mdh.co" }, urls = { "https?://(?:www\\.)?stream-mdh.co/(view|video)/.*?/.+" })
public class StreamMdh extends antiDDoSForDecrypt {
    public StreamMdh(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String streamID = new Regex(param.getCryptedUrl(), "/view/.*?/(.+)").getMatch(0);
        br.setFollowRedirects(true);
        if (streamID != null) {
            getPage("https://stream-mdh.co/embed/" + streamID);
        } else {
            getPage(param.getCryptedUrl());
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!this.canHandle(br.getURL())) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String iFrameSrc = br.getRegex("(?i)iframe src\\s*=\\s*\"(https?://.+?)\"").getMatch(0);
        if (iFrameSrc == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String title = br.getRegex("<title>\\s*(.*?)\\s*</title>").getMatch(0);
        final DownloadLink link = createDownloadlink(iFrameSrc);
        if (title != null) {
            final ExtensionsFilterInterface extension = CompiledFiletypeFilter.getExtensionsFilterInterface(Files.getExtension(link.getName()));
            if (CompiledFiletypeFilter.DocumentExtensions.HTML.equals(extension) || extension == null || CompiledFiletypeFilter.VideoExtensions.MP4.isSameExtensionGroup(extension)) {
                link.setName(title);
            }
        }
        ret.add(link);
        return ret;
    }
}
