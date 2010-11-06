package jd.plugins.optional.jdfeedme.posts;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import jd.nutils.io.JDIO;
import jd.utils.JDUtilities;

public class JDFeedMePost implements Serializable, Comparable<JDFeedMePost>
{
	private static final long serialVersionUID = 4495435642660256840L;
	
	public static final String ADDED_NO = "no";
    public static final String ADDED_YES = "yes";
    public static final String ADDED_YES_NO_FILES = "yes-no-files";
    public static final String ADDED_YES_OTHER_FEED = "yes-other-feed";
	
	private String title;
	private String link;
	private String timestamp;
	private String files;
	private String added;
	private boolean newpost;
	private String description;
	
	public JDFeedMePost()
	{
		this.title = "";
		this.link = "";
		this.timestamp = "";
		this.files = "";
		this.added = ADDED_NO;
		this.newpost = false;
		this.description = "";
	}
	
	public void setTimestamp(String timestamp)
    {
        this.timestamp = timestamp;
    }
	
	public String getTimestamp()
    {
        return timestamp;
    }
	
	public void setTitle(String title)
    {
        this.title = title;
    }
	
	public String getTitle()
    {
        return title;
    }
	
	public void setLink(String link)
    {
        this.link = link;
    }
	
	public String getLink()
    {
        return link;
    }
	
	public void setFiles(String files)
    {
        this.files = files;
    }
	
	public void addFile(String file)
	{
		if (!hasValidFiles()) this.files = file;
		else this.files += "\n" + file;
	}
	
	public String getFiles()
    {
        return files;
    }
	
	public void setAdded(String added)
    {
        this.added = added;
    }
	
	public String getAdded()
    {
        return added;
    }
	
	public void setNewpost(boolean newpost)
    {
        this.newpost = newpost;
    }
	
	public boolean getNewpost()
    {
        return newpost;
    }
	
	public void setDescription(String description)
    {
        this.description = description;
    }
	
	public String getDescription()
    {
        return description;
    }
	
	public boolean isValid()
    {
        // make sure we have a publication timestamp
        if (!hasValidTimestamp()) return false;
        
        // make sure we either have files or a link
        if (hasValidFiles()) return true;
        if (hasValidLink()) return true;
        return false;
    }
    
    public boolean hasValidLink()
    {
        if (this.link == null) return false;
        if (this.link.trim().length() == 0) return false;
        return true;
    }
    
    public boolean hasValidFiles()
    {
        if (this.files == null) return false;
        if (this.files.trim().length() == 0) return false;
        return true;
    }
    
    public boolean hasValidTimestamp()
    {
    	if (this.timestamp == null) return false;
        if (this.timestamp.trim().length() == 0) return false;
        DateFormat formatter = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z");
        try
        {
            formatter.parse(this.timestamp);
        } 
        catch (Exception e)
        {
            return false;
        }
        return true;
    }
    
    // Sat, 09 Oct 2010 13:02:19 +0000
    public boolean isTimestampNewer(String other)
    {
        if (other == null) return true;
        DateFormat formatter = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z");
        
        // parse my date
        Date my_date;
        try
        {
            my_date = (Date)formatter.parse(this.timestamp);
        } 
        catch (Exception e)
        {
            return false;
        }
        
        // parse other date
        Date other_date;
        try
        {
            other_date = (Date)formatter.parse(other);
        } 
        catch (Exception e)
        {
            return true;
        }
        
        // compare
        if (my_date.after(other_date)) return true;
        else return false;
    }
    
    public int compareTo(JDFeedMePost other) 
    {
    	String other_timestamp = other.getTimestamp();
    	if (timestamp.equals(other_timestamp)) return 0;
    	if (isTimestampNewer(other_timestamp)) return -1;
    	else return 1;
    }
	
	// location is something like "cfg/jdfeedme/posts.xml"
    public synchronized static void saveXML(HashMap<String, ArrayList<JDFeedMePost>> posts, String location)
    {
    	Thread.currentThread().setContextClassLoader(JDUtilities.getJDClassLoader());
    	
    	/* CODE_FOR_INTERFACE_5_START
    	JDIO.saveObject(null, posts, JDUtilities.getResourceFile(location), null, null, true);
    	CODE_FOR_INTERFACE_5_END */
        /* CODE_FOR_INTERFACE_7_START */
    	JDIO.saveObject(posts, JDUtilities.getResourceFile(location), true);
        /* CODE_FOR_INTERFACE_7_END */
    	
    }
    
    // location is something like "cfg/jdfeedme/posts.xml"
    @SuppressWarnings("unchecked")
	public synchronized static HashMap<String, ArrayList<JDFeedMePost>> loadXML(String location)
    {
    	Thread.currentThread().setContextClassLoader(JDUtilities.getJDClassLoader());
    	
    	/* CODE_FOR_INTERFACE_5_START
    	Object loaded = JDIO.loadObject(null, JDUtilities.getResourceFile(location), true);
    	CODE_FOR_INTERFACE_5_END */
        /* CODE_FOR_INTERFACE_7_START */
    	Object loaded = JDIO.loadObject(JDUtilities.getResourceFile(location), true);
        /* CODE_FOR_INTERFACE_7_END */
    	
    	if (loaded != null) return (HashMap<String, ArrayList<JDFeedMePost>>)loaded;
    	else return new HashMap<String, ArrayList<JDFeedMePost>>();
    }
}
