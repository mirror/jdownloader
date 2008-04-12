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
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.


package jd.update;



import java.io.File;
import java.io.Serializable;
import java.util.HashMap;





public class SubConfiguration extends Property implements Serializable {



    /**
	 * 
	 */
    private static HashMap<String,SubConfiguration> subConfigs=new HashMap<String,SubConfiguration>();
	private static final long serialVersionUID = 7803718581558607222L;
	//private transient Logger        logger;
    private String name;

    /**
     * 
     */
    @SuppressWarnings("unchecked")
	public SubConfiguration(String name) {
       // logger = JDUtilities.getLogger();
        this.name=name;
        File file;
        Object props = utils.loadObject(file=new File("config/"+name+".cfg"));
       System.out.println("Config file: "+file.getAbsolutePath());
       file.getParentFile().mkdirs();
        if(props!=null){
            this.setProperties((HashMap<String, Object>)props);
        }
    }
    public void save(){
        utils.saveObject(this.getProperties(),  new File("config/"+name+".cfg"));
    }
    public static SubConfiguration getSubConfig(String name){
        if(subConfigs.containsKey(name))return subConfigs.get(name);
        
        SubConfiguration cfg = new SubConfiguration(name);
        subConfigs.put(name, cfg);
        return cfg;
        
    
    }

}