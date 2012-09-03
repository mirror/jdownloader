package org.jdownloader.extensions.streaming;

import jd.Launcher;
import jd.nutils.Executer;
import jd.plugins.DownloadLink;

import org.appwork.utils.Application;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.extensions.streaming.upnp.PlayToDevice;
import org.jdownloader.logging.LogController;

public abstract class PlayToLocalBinary implements PlayToDevice {

    private StreamingExtension extension;
    private LogSource          logger;

    public PlayToLocalBinary(StreamingExtension streamingExtension) {
        this.extension = streamingExtension;
        logger = LogController.getInstance().getLogger(StreamingExtension.class.getName());
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public void play(final DownloadLink link, final String id, final String subpath) {

        new Thread() {
            public void run() {
                try {

                    final String url = extension.createStreamUrl(id, getUniqueDeviceID(), null, subpath);

                    Executer exec = new Executer(getBinaryPath());
                    exec.setLogger(LogController.CL());
                    exec.addParameters(getParams(url));
                    exec.setRunin(Application.getRoot(Launcher.class));
                    exec.setWaitTimeout(0);
                    exec.start();

                } catch (final Throwable e) {
                    logger.log(e);
                }
            };
        }.start();

    }

    protected String[] getParams(String url) {
        return new String[] { url };
    }

    protected abstract String getBinaryPath();
}
