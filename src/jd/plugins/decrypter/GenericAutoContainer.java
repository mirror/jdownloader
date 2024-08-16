package jd.plugins.decrypter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin.FEATURE;
import org.jdownloader.plugins.controller.crawler.LazyCrawlerPlugin;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.LinkCrawlerConfig;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "genericautocontainer" }, urls = { "https?://[\\w\\.:\\-@]*/.*\\.(dlc|ccf|rsdf|nzb|sfdl)$" })
public class GenericAutoContainer extends PluginForDecrypt {
    @Override
    public Boolean siteTesterDisabled() {
        return Boolean.TRUE;
    }

    public GenericAutoContainer(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.GENERIC };
    }

    @Override
    public boolean canHandle(String data) {
        return super.canHandle(data) && JsonConfig.create(LinkCrawlerConfig.class).isAutoImportContainer();
    }

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
        final String type = new Regex(url, this.getSupportedLinks()).getMatch(0);
        final URLConnectionAdapter con = br.openGetConnection(url);
        File containerTemp = null;
        try {
            if (con.isOK()) {
                boolean seemsValidContainer = StringUtils.containsIgnoreCase(con.getContentType(), type);
                seemsValidContainer = seemsValidContainer | (con.isContentDisposition() && StringUtils.containsIgnoreCase(Plugin.getFileNameFromConnection(con), type)) || (con.getContentLength() > 100 && (con.getContentType() == null || !StringUtils.containsIgnoreCase(con.getContentType(), "text")));
                if (seemsValidContainer) {
                    containerTemp = org.appwork.utils.Application.getResource("tmp/autocontainer/" + System.nanoTime() + "." + type);
                    br.downloadConnection(containerTemp, con);
                    if (containerTemp.exists() && containerTemp.length() > 100) {
                        ret.addAll(loadContainerFile(containerTemp));
                    }
                }
            }
        } catch (final IOException e) {
            logger.log(e);
            if (ret.size() == 0) {
                ret.add(createDownloadlink(url));
            }
        } finally {
            if (containerTemp != null && containerTemp.exists()) {
                containerTemp.delete();
            }
            con.disconnect();
        }
        if (containerTemp != null && ret.size() == 0) {
            ret.add(createDownloadlink(url));
        }
        return ret;
    }
}
