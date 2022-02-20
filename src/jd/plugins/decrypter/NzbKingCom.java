package jd.plugins.decrypter;

import java.io.File;
import java.util.ArrayList;

import org.jdownloader.container.NZB;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.ArchiveInfo;
import jd.controlling.linkcrawler.CrawledLink;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
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
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setLoadLimit(Integer.MAX_VALUE);
        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        File nzbFile = null;
        try {
            br.getPage(param.getCryptedUrl());
            final String title = br.getRegex("<div class='post-detail-subject'[^>]*>([^<]+)</div>").getMatch(0);
            final Form form = br.getFormbyAction("/nzb/");
            final Request request = br.createFormRequest(form);
            request.getHeaders().put("Accept-Encoding", "identity");
            con = br.openRequestConnection(request);
            if (con.isOK()) {
                ret.addAll(NZBSAXHandler.parseNZB(con.getInputStream()));
                final String nzbFilename = Plugin.getFileNameFromHeader(con);
                final Regex nzbCommonFilenameScheme = new Regex(nzbFilename, NZB.PATTERN_COMMON_FILENAME_SCHEME);
                if (nzbFilename != null && nzbCommonFilenameScheme.matches()) {
                    if (nzbCommonFilenameScheme.matches()) {
                        /* Set extract-password and package name by information grabbed inside .nzb filename. */
                        archiveInfo = new ArchiveInfo();
                        archiveInfo.addExtractionPassword(nzbCommonFilenameScheme.getMatch(1));
                        final FilePackage fp = FilePackage.getInstance();
                        fp.setName(nzbCommonFilenameScheme.getMatch(0));
                        fp.addLinks(ret);
                    } else {
                        final FilePackage fp = FilePackage.getInstance();
                        fp.setName(nzbFilename);
                        fp.addLinks(ret);
                    }
                } else if (title != null) {
                    final FilePackage fp = FilePackage.getInstance();
                    fp.setName(Encoding.htmlDecode(title).trim());
                    fp.addLinks(ret);
                } else if (nzbFilename != null) {
                    /* Last chance: Use filename as packagename. */
                    final FilePackage fp = FilePackage.getInstance();
                    fp.setName(nzbFilename);
                    fp.addLinks(ret);
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

    public boolean hasCaptcha(final CryptedLink link, final jd.plugins.Account acc) {
        return false;
    }
}
