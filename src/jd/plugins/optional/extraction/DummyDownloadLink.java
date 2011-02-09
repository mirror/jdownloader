package jd.plugins.optional.extraction;

import java.io.File;

import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;

public class DummyDownloadLink extends DownloadLink {
    private static final long serialVersionUID = 4075187183435835432L;

    private File              file;

    public DummyDownloadLink(PluginForHost plugin, String name, String host, String urlDownload, boolean isEnabled) {
        super(plugin, name, host, urlDownload, isEnabled);
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getFileOutput() {
        return file.getAbsolutePath();
    }

    public String getFileOutput0() {
        return file.getAbsolutePath();
    }

    public LinkStatus getLinkStatus() {
        LinkStatus ls = new LinkStatus(this);
        ls.setStatus(LinkStatus.FINISHED);
        return ls;
    }
}