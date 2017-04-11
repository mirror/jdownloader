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
import org.jdownloader.plugins.controller.crawler.LazyCrawlerPlugin.FEATURE;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "nzb" }, urls = { "https?://.+/.*\\.nzb($|\\?[^\\s<>\"']*)" })
public class GenericNZBDecrypter extends PluginForDecrypt {

    @Override
    public Boolean siteTesterDisabled() {
        return Boolean.TRUE;
    }

    public GenericNZBDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public CrawledLink convert(DownloadLink link) {
        final CrawledLink ret = super.convert(link);
        ret.setArchiveInfo(archiveInfo);
        return ret;
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.GENERIC };
    }

    private ArchiveInfo archiveInfo = null;

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        URLConnectionAdapter con = null;
        File nzbFile = null;
        try {
            final Request request = new GetRequest(param.getCryptedUrl());
            request.getHeaders().put("Accept-Encoding", "identity");
            br.setFollowRedirects(true);
            br.setLoadLimit(br.getLoadLimit() * 4);
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
                final String response = br.followConnection();
                if (response.startsWith("<?xml")) {
                    ret.addAll(NZBSAXHandler.parseNZB(response));
                    final String nzbPassword = new Regex(Plugin.getFileNameFromHeader(con), "\\{\\{(.*?)\\}\\}\\.nzb$").getMatch(0);
                    if (nzbPassword != null) {
                        if (StringUtils.isNotEmpty(nzbPassword)) {
                            archiveInfo = new ArchiveInfo();
                            archiveInfo.addExtractionPassword(nzbPassword);
                        }
                    }
                }
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