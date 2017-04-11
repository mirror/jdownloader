package jd.plugins.decrypter;

import java.io.File;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.ArchiveInfo;
import jd.controlling.linkcrawler.CrawledLink;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.http.requests.GetRequest;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.NZBSAXHandler;

import org.appwork.utils.StringUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "nzbsearch.net" }, urls = { "https?://[\\w\\.]*nzbsearch.net/nzb_get.aspx\\?mid=[1-9A-Za-z]+" }) public class NzbSearchNet extends PluginForDecrypt {

    @Override
    public Boolean siteTesterDisabled() {
        return Boolean.TRUE;
    }

    public NzbSearchNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public CrawledLink convert(DownloadLink link) {
        final CrawledLink ret = super.convert(link);
        ret.setArchiveInfo(archiveInfo);
        return ret;
    }

    private ArchiveInfo archiveInfo = null;

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        URLConnectionAdapter con = null;
        File nzbFile = null;
        try {
            final String nzbID = new Regex(param.getCryptedUrl(), "\\?mid=(.+)$").getMatch(0);
            final Request request = new GetRequest("http://www.nzbsearch.net/nzb_get.aspx\\?mid=" + nzbID);
            request.getHeaders().put("Accept-Encoding", "identity");
            br.setFollowRedirects(true);
            con = br.openRequestConnection(request);
            final String contentType = con.getContentType();
            if (StringUtils.containsIgnoreCase(contentType, "nzb") && con.isOK()) {
                ret.addAll(NZBSAXHandler.parseNZB(con.getInputStream()));
                final String nzbPassword = new Regex(Plugin.getFileNameFromHeader(con), "\\{\\{(.*?)\\}\\}\\.nzb$").getMatch(0);
                if (nzbPassword != null) {
                    if (StringUtils.isNotEmpty(nzbPassword)) {
                        archiveInfo = new ArchiveInfo();
                        archiveInfo.addExtractionPassword(nzbPassword);
                    }
                }
            } else {
                br.followConnection();
            }
        } finally {
            if (con != null) {
                con.disconnect();
            }
            if (nzbFile != null) {
                if (ret.size() == 0) {
                    nzbFile.delete();
                } else {
                    nzbFile.deleteOnExit();
                }
            }
        }
        return ret;
    }
}
