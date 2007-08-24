package jd.gui;

import java.util.Vector;
import java.util.logging.Logger;

import jd.Configuration;
import jd.event.ControlEvent;
import jd.event.UIEvent;
import jd.event.UIListener;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.event.PluginEvent;

public interface UIInterface {

    public Vector<DownloadLink> getDownloadLinks();
    public Configuration        getConfiguration();
    public String               getCaptchaCodeFromUser(Plugin plugin, String captchaAddress);
    
    public void setLogger       (Logger logger);
    public void setConfiguration(Configuration configuration);
    public void setDownloadLinks(Vector<DownloadLink> downloadLinks);
    public void setPluginActive(Plugin plugin, boolean isActive);
    
    public void controlEvent(ControlEvent event);
    public void pluginEvent(PluginEvent event);

    public void addUIListener(UIListener listener);
    public void removeUIListener(UIListener listener);
    public void fireUIEvent(UIEvent pluginEvent);

}
