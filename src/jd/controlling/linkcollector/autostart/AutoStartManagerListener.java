package jd.controlling.linkcollector.autostart;

import java.util.EventListener;

public interface AutoStartManagerListener extends EventListener {

    void onAutoStartManagerDone();

    void onAutoStartManagerReset();

    void onAutoStartManagerRunning();

}