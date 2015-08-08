package jd.plugins.hoster;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jdownloader.DomainInfo;
import org.jdownloader.images.NewTheme;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision: 31032 $", interfaceVersion = 2, names = { "usenet" }, urls = { "usenet://.+" }, flags = { 0 })
public class UseNet extends PluginForHost {

    public UseNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return null;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        if (parameter.getBooleanProperty("incomplete", Boolean.FALSE)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    private final static AtomicBoolean NZBICON = new AtomicBoolean(false);

    @Override
    public DomainInfo getDomainInfo(DownloadLink link) {
        final DomainInfo ret = super.getDomainInfo(link);
        synchronized (NZBICON) {
            if (NZBICON.compareAndSet(false, true)) {
                ret.setFavIcon(NewTheme.I().getIcon("nzb", -1));
            }
        }
        return ret;
    }

    @Override
    public Boolean siteTesterDisabled() {
        return true;
    }
}
