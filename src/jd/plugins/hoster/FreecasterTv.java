package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision: 9647 $", interfaceVersion = 2, names = { "freecaster.tv" }, urls = { "http://videos.freecaster.com/.++" }, flags = { 2 })
public class FreecasterTv extends PluginForHost {


	public FreecasterTv(PluginWrapper wrapper) {
        super(wrapper);
    }

	@Override
	public String getAGBLink() {
		return "http://freecaster.tv/info/general-terms-of-service";
	}

	@Override
	public void handleFree(DownloadLink link) throws Exception {
		dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getDownloadURL(), true, 0);
        dl.startDownload();

	}

	@Override
	public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
		this.setBrowserExclusive();
        
        if (br.openGetConnection(downloadLink.getDownloadURL()).getResponseCode() == 200) {
            long size = br.getHttpConnection().getLongContentLength();
            downloadLink.setDownloadSize(Long.valueOf(size));
            return AvailableStatus.TRUE;
        }

        return AvailableStatus.FALSE;
    }

	@Override
	public void reset() {
	}

	@Override
	public void resetDownloadlink(DownloadLink link) {
	}


    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }
}
