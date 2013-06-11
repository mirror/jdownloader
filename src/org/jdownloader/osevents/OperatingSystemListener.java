package org.jdownloader.osevents;

import java.util.EventListener;

public interface OperatingSystemListener extends EventListener {

    void onOperatingSystemSessionEnd();

    void onOperatingSystemShutdownVeto(ShutdownOperatingSystemVetoEvent event);

    void onOperatingSystemSignal(String name, int number);

}