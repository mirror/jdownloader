package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
import org.jdownloader.container.NZB;
import org.jdownloader.plugins.controller.LazyPlugin.FEATURE;
import org.jdownloader.plugins.controller.crawler.LazyCrawlerPlugin;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.ArchiveInfo;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawlerConfig;
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
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "nzb" }, urls = { "https?://.+/.*\\.nzb($|(\\?|&)[^\\s<>\"']*)" })
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
        if (archiveInfo != null) {
            ret.setArchiveInfo(archiveInfo);
        }
        return ret;
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.GENERIC };
    }

    private ArchiveInfo archiveInfo = null;

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String url = param.getCryptedUrl();
        final List<LazyCrawlerPlugin> nextLazyCrawlerPlugins = findNextLazyCrawlerPlugins(url);
        if (nextLazyCrawlerPlugins.size() > 0) {
            // forward to next supporting plugin
            ret.add(createDownloadlink(url));
            return ret;
        }
        final List<LazyHostPlugin> nextLazyHostPlugins = findNextLazyHostPlugins(url);
        if (nextLazyHostPlugins.size() > 0) {
            // forward to next supporting plugin
            ret.add(createDownloadlink(url));
            return ret;
        }
        if (!JsonConfig.create(LinkCrawlerConfig.class).isAutoImportContainer()) {
            // instead of parsing, please download the nzb file
            ret.add(createDownloadlink(DirectHTTP.createURLForThisPlugin(url)));
            return ret;
        }
        URLConnectionAdapter con = null;
        try {
            final Request request = new GetRequest(url);
            request.getHeaders().put(HTTPConstants.HEADER_REQUEST_ACCEPT_ENCODING, "identity");
            br.setFollowRedirects(true);
            br.setLoadLimit(br.getLoadLimit() * 4);
            con = br.openRequestConnection(request);
            if (StringUtils.containsIgnoreCase(con.getContentType(), "nzb") && con.isOK()) {
                ret.addAll(NZBSAXHandler.parseNZB(con.getInputStream()));
            } else {
                final String response = br.followConnection();
                if (response.startsWith("<?xml")) {
                    ret.addAll(NZBSAXHandler.parseNZB(response));
                }
            }
            if (ret.size() > 0) {
                final String nzbPassword = new Regex(Plugin.getFileNameFromConnection(con), NZB.PATTERN_COMMON_FILENAME_SCHEME).getMatch(1);
                if (nzbPassword != null) {
                    if (StringUtils.isNotEmpty(nzbPassword)) {
                        archiveInfo = new ArchiveInfo();
                        archiveInfo.addExtractionPassword(nzbPassword);
                    }
                }
            }
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
        return ret;
    }
}