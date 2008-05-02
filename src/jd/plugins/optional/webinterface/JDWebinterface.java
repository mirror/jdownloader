//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
// TODO:    -Restart Button (Wartebalken noch!!)
//          -Add Link Button (ne einfache variante ist schon drin) (inklusive Template)
//          -stable template system
//          -anderes layout


package jd.plugins.optional.webinterface;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.MenuItem;
import jd.config.SubConfiguration;
import jd.plugins.PluginOptional;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class JDWebinterface extends PluginOptional  {    
	
	
    static final String PROPERTY_PORT = "PARAM_PORT";
    static final String PROPERTY_USER = "PARAM_USER";
    static final String PROPERTY_PASS = "PARAM_PASS";
    static final String PROPERTY_HTTPS = "PARAM_HTTPS";
    static final String PROPERTY_LOGIN = "PARAM_LOGIN";
    static final String PROPERTY_CONNECTIONS = "PARAM_CONNECTIONS";
    public static JDWebinterface instance;
    
	@Override
    public String getCoder() {
        return "jiaz";
    }

    @Override
    public String getPluginID() {
        return "0.0.0.2";
    }

    @Override
    public String getPluginName() {
        return JDLocale.L("plugins.optional.webinterface.name","WebInterface");
    }

    @Override
    public String getVersion() {
        return "0.0.0.2";
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
    

    
    public JDWebinterface()
    {
        instance= this;
    	SubConfiguration subConfig = JDUtilities.getSubConfig("WEBINTERFACE");
    	ConfigEntry cfg;
    	config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, subConfig, PROPERTY_PORT, JDLocale.L("plugins.optional.webinterface.port", "Port"), 1024, 65000));
    	cfg.setStep(1);    	
    	cfg.setDefaultValue(8765);
    	config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, subConfig, PROPERTY_CONNECTIONS, JDLocale.L("plugins.optional.webinterface.connections", "Max. Connections"), 1, 20));
    	cfg.setStep(1);    	
    	cfg.setDefaultValue(10);    
    	config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PROPERTY_LOGIN, JDLocale.L("plugins.optional.webinterface.needlogin", "Need User Authentication")));
        cfg.setDefaultValue(true);
//        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PROPERTY_HTTPS, JDLocale.L("plugins.optional.webinterface.https", "Use HTTPS")));
//        cfg.setDefaultValue(false); TODO
    	config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, subConfig, PROPERTY_USER, JDLocale.L("plugins.optional.webinterface.loginname", "Login Name")));
        cfg.setDefaultValue("JD");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, subConfig, PROPERTY_PASS, JDLocale.L("plugins.optional.webinterface.loginpass", "Login Pass")));
        cfg.setDefaultValue("JD");
    }

    @Override
    public String getRequirements() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ArrayList<MenuItem> createMenuitems() {
        // TODO Auto-generated method stub
        return null;
    }

    public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
        
    }    
}
