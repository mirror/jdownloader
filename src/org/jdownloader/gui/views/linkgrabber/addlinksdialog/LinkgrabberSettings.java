package org.jdownloader.gui.views.linkgrabber.addlinksdialog;

import java.util.ArrayList;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultObjectValue;
import org.jdownloader.settings.annotations.AboutConfig;

public interface LinkgrabberSettings extends ConfigInterface {

    @DefaultObjectValue("[]")
    @AboutConfig
    ArrayList<String> getDownloadDestinationHistory();

    void setDownloadDestinationHistory(ArrayList<String> value);
}
