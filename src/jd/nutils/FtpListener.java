package jd.nutils;

import java.util.EventListener;

public interface FtpListener extends EventListener {

    void onDownloadProgress(FtpEvent event);

}
