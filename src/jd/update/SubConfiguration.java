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