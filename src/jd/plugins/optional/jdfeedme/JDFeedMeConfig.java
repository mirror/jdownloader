package jd.plugins.optional.jdfeedme;

import java.io.File;
import java.io.Serializable;
import jd.nutils.io.JDIO;
import jd.utils.JDUtilities;

public class JDFeedMeConfig implements Serializable
{
	private static final long serialVersionUID = 1295475612060256840L;
	
	private boolean startdownloads;
	private int syncintervalhours;
	
	public JDFeedMeConfig()
	{
		startdownloads = true;
		syncintervalhours = 2;
	}
	
	public boolean getStartdownloads()
	{
		return this.startdownloads;
	}
	
	public void setStartdownloads(boolean startdownloads)
	{
		this.startdownloads = startdownloads;
	}
	
	public int getSyncintervalhours()
	{
		return this.syncintervalhours;
	}
	
	public void setSyncintervalhours(int syncintervalhours)
	{
		this.syncintervalhours = syncintervalhours;
	}
	
    // location is something like "cfg/jdfeedme/config.xml"
    public synchronized static void saveXML(JDFeedMeConfig config, String location)
    {
    	Thread.currentThread().setContextClassLoader(JDUtilities.getJDClassLoader());
    	
    	/* CODE_FOR_INTERFACE_5_START
    	JDIO.saveObject(null, config, JDUtilities.getResourceFile(location), null, null, true);
    	CODE_FOR_INTERFACE_5_END */
    	/* CODE_FOR_INTERFACE_7_START */
    	JDIO.saveObject(config, JDUtilities.getResourceFile(location), true);
    	/* CODE_FOR_INTERFACE_7_END */
    	
    }
    
    // location is something like "cfg/jdfeedme/config.xml"
    public synchronized static JDFeedMeConfig loadXML(String location)
    {
    	Thread.currentThread().setContextClassLoader(JDUtilities.getJDClassLoader());
    	
    	File xmlFile = JDUtilities.getResourceFile(location);
        if (xmlFile.exists()) 
        {
        	/* CODE_FOR_INTERFACE_5_START
        	Object loaded = JDIO.loadObject(null, xmlFile, true);
        	CODE_FOR_INTERFACE_5_END */
        	/* CODE_FOR_INTERFACE_7_START */
        	Object loaded = JDIO.loadObject(xmlFile, true);
        	/* CODE_FOR_INTERFACE_7_END */
    	
        	if (loaded != null) return (JDFeedMeConfig)loaded;
        }
    	return new JDFeedMeConfig();
    }
}
