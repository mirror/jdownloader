//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://wnu.org/licenses/>.


package jd.plugins.webinterface;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
import jd.plugins.PluginOptional;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class JDWebinterface extends PluginOptional  {    
	
	
    static final String PROPERTY_PORT = "PARAM_PORT";

	@Override
    public String getCoder() {
        return "jiaz";
    }

    @Override
    public String getPluginID() {
        return "0.0.0.1";
    }

    @Override
    public String getPluginName() {
        return JDLocale.L("plugins.optional.webinterface.name","WebInterface");
    }

    @Override
    public String getVersion() {
        return "0.0.0.1";
    }

    @Override
    public void enable(boolean enable) throws Exception {
        if(JDUtilities.getJavaVersion()>=1.6){
        if (enable) {
        	JDSimpleWebserver server = new JDSimpleWebserver();
            logger.info("WebInterface ok: java "+JDUtilities.getJavaVersion());
        }
        }        
    }

    @Override
    public String getRequirements() {
        return "JRE 1.6+";
    }

    @Override
    public boolean isExecutable() {
        return false;
    }
    @Override
    public boolean execute() {
        return false;
    }
    
    public JDWebinterface()
    {
    	SubConfiguration subConfig = JDUtilities.getSubConfig("WEBINTERFACE");
    	ConfigEntry cfg;
    	config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, subConfig, PROPERTY_PORT, JDLocale.L("plugins.hoster.rapidshare.com.waitTimeOnBotDetect", "Port"), 1024, 65000));
    	cfg.setStep(1);
    	cfg.setInstantHelp("http://www.google.de");
    	cfg.setDefaultValue(1024);
    }    
}
