package jd.plugins.decrypter;

import java.io.File;
import java.util.ArrayList;

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.ArchiveInfo;
import jd.controlling.linkcrawler.CrawledLink;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.NZBSAXHandler;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "nzbking.com" }, urls = { "https?://[\\w\\.]*nzbking.com/details(:|%3a)[0-9a-zA-Z]+" })
public class NzbKingCom extends PluginForDecrypt {

    public NzbKingCom(PluginWrapper wrapper) {
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
            br.setLoadLimit(Integer.MAX_VALUE);
            br.setFollowRedirects(true);
            br.getPage(param.getCryptedUrl());
            final Form form = br.getFormbyAction("/nzb/");
            final Request request = br.createFormRequest(form);
            request.getHeaders().put("Accept-Encoding", "identity");
            con = br.openRequestConnection(request);
            if (con.isOK()) {
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
