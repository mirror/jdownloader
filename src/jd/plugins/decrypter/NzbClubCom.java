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
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.NZBSAXHandler;

import org.appwork.utils.StringUtils;
import org.appwork.utils.net.URLHelper;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "nzbclub.com" }, urls = { "https?://[\\w\\.]*nzbclub.com/nzb_view/\\d+" }) public class NzbClubCom extends PluginForDecrypt {

    @Override
    public Boolean siteTesterDisabled() {
        return Boolean.TRUE;
    }

    public NzbClubCom(PluginWrapper wrapper) {
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
            final String nzbID = new Regex(param.getCryptedUrl(), "/(\\d+)$").getMatch(0);
            br.setFollowRedirects(true);
            br.getPage(param.getCryptedUrl());
            final String pkgName = br.getRegex("ContentPlaceHolder1_ui_titlesmall\">(.*?)<").getMatch(0);
            final Request request = new GetRequest(URLHelper.parseLocation(br.getRequest().getURL(), "/nzb_get/" + nzbID));
            request.getHeaders().put("Accept-Encoding", "identity");
            con = br.openRequestConnection(request);
            final String contentType = con.getContentType();
            if (StringUtils.containsIgnoreCase(contentType, "nzb") && con.isOK()) {
                ret.addAll(NZBSAXHandler.parseNZB(con.getInputStream()));
                if (pkgName != null) {
                    final FilePackage fp = FilePackage.getInstance();
                    fp.setName(pkgName);
                    fp.addLinks(ret);
                }
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
