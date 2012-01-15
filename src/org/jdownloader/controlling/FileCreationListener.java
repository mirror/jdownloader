package org.jdownloader.controlling;

import java.io.File;
import java.util.EventListener;

public interface FileCreationListener extends EventListener {

    void onNewFile(Object caller, File[] fileList);

}