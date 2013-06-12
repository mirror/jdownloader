package org.jdownloader.osevents;

import java.util.EventListener;

public interface OperatingSystemListener extends EventListener {

    void onOperatingSystemTerm();

}