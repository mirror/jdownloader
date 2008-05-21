package jd.plugins.optional;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;


import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.config.MenuItem;
import jd.config.SubConfiguration;
import jd.controlling.JDController;
import jd.event.ControlEvent;
import jd.plugins.DownloadLink;
import jd.plugins.PluginOptional;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class JDLowSpeed extends PluginOptional {
    private String version = "0.1";
    private static final String PROPERTY_ENABLED = "PROPERTY_ENABLED";
    private static final String PROPERTY_RAPIDSHAREONLY = "PROPERTY_RAPIDSHAREONLY";
    private static final String PROPERTY_MAXSPEED = "PROPERTY_MAXSPEED";
    private static final String PROPERTY_MINSPEED = "PROPERTY_MINSPEED";
    private SubConfiguration subConfig = JDUtilities.getSubConfig("ADDONS_JDLOWSPEED");
    private Thread pluginThread = null;
    private boolean isRunning = false;
    
    public JDLowSpeed() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PROPERTY_RAPIDSHAREONLY, JDLocale.L("plugins.optional.jdlowspeed.rsonly", "Nur Raidshare überwachen")));
        cfg.setDefaultValue(true);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, subConfig, PROPERTY_MAXSPEED, JDLocale.L("plugins.optional.jdlowspeed.maxspeed", "Maximale Internetgeschwindigkeit in kb/s (DSL 3000≈350kb/s)"), 1, 1000000));
        cfg.setDefaultValue("350");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, subConfig, PROPERTY_MINSPEED, JDLocale.L("plugins.optional.jdlowspeed.minspeed", "Mindestgeschwindigkeit für einen Download in kb/s"), 1, 100000));
        cfg.setDefaultValue("40");
    }

    @Override
    public String getRequirements() {
        return "JRE 1.5+";
    }

    @Override
    public boolean initAddon() {
        JDUtilities.getController().addControlListener(this);
        logger.info("JDLowSpeed detection ok");
        return true;
    }

    @Override
    public void onExit() {
        // TODO Auto-generated method stub

    }

    @Override
    public ArrayList<MenuItem> createMenuitems() {
        ArrayList<MenuItem> menu = new ArrayList<MenuItem>();
        MenuItem m;

        if (!subConfig.getBooleanProperty(PROPERTY_ENABLED, false)) {

            menu.add(m = new MenuItem(MenuItem.TOGGLE, JDLocale.L("addons.jdlowspeed.statusmessage.enabled", "Low Speederkennung einschalten"), 0).setActionListener(this));
            m.setSelected(false);
        } else {
            menu.add(m = new MenuItem(MenuItem.TOGGLE, JDLocale.L("addons.jdlowspeed.statusmessage.disabled", "Low Speederkennung ausschalten"), 1).setActionListener(this));
            m.setSelected(true);
        }
        return menu;
    }
    private void controllDownload(final DownloadLink downloadLink,final int minspeed)
    {
        if(downloadLink.getSpeedMeter().getSpeed()<minspeed)
        {
            new Thread(new Runnable(){

                public void run() {
                    for (int i = 0; i < 20; i++) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        if(downloadLink.getSpeedMeter().getSpeed()>minspeed)
                        {
                            return;
                        }
                    }
                    logger.info("reset download: "+downloadLink.getName());
                    downloadLink.setStatus(DownloadLink.STATUS_TODO);
                    downloadLink.setStatusText("");
                    downloadLink.reset();

                }
                
            }).start();
        }
    }
    private void initPlugin() {
        if (!isRunning) {
            logger.info("start");
            isRunning=true;
            pluginThread = new Thread(new Runnable(){

                public void run() {
                    while (isRunning) {
                        int maxspeed = JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0);
                        if(maxspeed==0)
                            maxspeed = subConfig.getIntegerProperty(PROPERTY_MAXSPEED, 350);
                        maxspeed=maxspeed*4/5;
                        maxspeed*=1024;
                        Boolean rsOnly = subConfig.getBooleanProperty(PROPERTY_RAPIDSHAREONLY, true);
                        if(JDUtilities.getController().getSpeedMeter()<maxspeed)
                        {
                        int minspeed = subConfig.getIntegerProperty(PROPERTY_MINSPEED, 350)*1024;

                        Iterator<DownloadLink> ff = JDUtilities.getController().getDownloadLinks().iterator();
                        while (ff.hasNext()) {
                            DownloadLink dl = ff.next();

                            if (dl.getStatus() == DownloadLink.STATUS_DOWNLOAD_IN_PROGRESS && (!rsOnly || dl.getPlugin().getHost().equals("rapidshare.com"))) {
                                controllDownload(dl, minspeed);
                            }
                        }
                        }
                        try {
                            Thread.sleep(20000);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    
                }});
            pluginThread.start();
        }
    }

    private void stopPlugin() {
        logger.info("stop");
        isRunning=false;

    }

    public void controlEvent(ControlEvent event) {
        super.controlEvent(event);
        if (subConfig.getBooleanProperty(PROPERTY_ENABLED, false)) {
            if (event.getID() == ControlEvent.CONTROL_DOWNLOAD_START) {
                initPlugin();
            } else if (event.getID() == ControlEvent.CONTROL_DOWNLOAD_STOP) {
                stopPlugin();
            } else if (event.getID() == ControlEvent.CONTROL_ALL_DOWNLOADS_FINISHED) {
                stopPlugin();
            }
        }

    }

    public void actionPerformed(ActionEvent e) {
        MenuItem mi = (MenuItem) e.getSource();
        if (mi.getActionID() == 0) {
            subConfig.setProperty(PROPERTY_ENABLED, true);
            if(JDUtilities.getController().getDownloadStatus()==JDController.DOWNLOAD_RUNNING)
            {
                initPlugin();
            }
            subConfig.save();
        } else {
            subConfig.setProperty(PROPERTY_ENABLED, false);
            stopPlugin();
            subConfig.save();

        }
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getPluginID() {
        // TODO Auto-generated method stub
        return getPluginName() + " " + version;
    }

    @Override
    public String getPluginName() {
        return JDLocale.L("plugins.optional.jdlowspeed.name", "LowSpeed Detection");
    }

    @Override
    public String getVersion() {
        // TODO Auto-generated method stub
        return version;
    }

}
