package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(names = { "box.net" }, urls = { "(http://www\\.box\\.net/(shared/static/|rssdownload/).*)|(http://www\\.box\\.net/index\\.php\\?rm=box_download_shared_file\\&file_id=.+?\\&shared_name=\\w+)" }, flags = { 0 }, revision = "$Revision", interfaceVersion = 2)
public class BoxNet extends PluginForHost {
    private static final String TOS_LINK = "https://www.box.net/static/html/terms.html";

    private static final String OUT_OF_BANDWITH_MSG = "out of bandwidth";
    private static final String REDIRECT_DOWNLOAD_LINK = "http://www\\.box\\.net/index\\.php\\?rm=box_download_shared_file\\&file_id=.+?\\&shared_name=\\w+";

    public BoxNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return TOS_LINK;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        logger.finer("starting download: " + link.getDownloadURL());

        // setup referer and cookies for single file downloads
        if (link.getDownloadURL().matches(REDIRECT_DOWNLOAD_LINK)) {
            br.getPage(link.getBrowserUrl());
        }
        dl = br.openDownload(link, link.getDownloadURL(), false, 0);
        if (!dl.getConnection().isContentDisposition()) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        logger.finer("requesting  info for: " + parameter.getDownloadURL());
        
        // setup referer and cookies for single file downloads
        if (parameter.getDownloadURL().matches(REDIRECT_DOWNLOAD_LINK)) {
            br.getPage(parameter.getBrowserUrl());
        }
       
        URLConnectionAdapter urlConnection = br.openGetConnection(parameter.getDownloadURL());
        if (urlConnection.getResponseCode() == 404 || !urlConnection.isOK()) {
            urlConnection.disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        if (!urlConnection.isContentDisposition()) {
            urlConnection.disconnect();

            br.getPage(parameter.getDownloadURL());
            if (br.containsHTML(OUT_OF_BANDWITH_MSG)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);

            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        }

        parameter.setFinalFileName(Plugin.getFileNameFormHeader(urlConnection));
        parameter.setDownloadSize(urlConnection.getLongContentLength());
        urlConnection.disconnect();
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public String getVersion() {
        return "$Revision$";
    }

}
