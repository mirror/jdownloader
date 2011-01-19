package jd.pluginloader;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.appwork.storage.JSonStorage;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;

/**
 * reads and writes a JSON file which caches all plugininfo
 * 
 * @author thomas
 * 
 * @param <T>
 */
public abstract class PluginCache<T> {
    private HashMap<String, T> data;
    private File               file;
    private boolean            changed = false;

    public PluginCache(String tmpPlugincacheJson) {
        try {
            file = Application.getRessource(tmpPlugincacheJson);
            String cachedString = null;
            if (file.exists()) cachedString = IO.readFileToString(file);
            data = restore(cachedString);

        } catch (Exception e) {
            data = new HashMap<String, T>();
            e.printStackTrace();
        }
    }

    protected abstract HashMap<String, T> restore(String cachedString);

    /**
     * Creates a unique id for c. It would be better to use the classfile hash
     * here. Since lastModified is much faster, we use this
     * 
     * @TODO: Requires Testing to see if lastMod is ok
     * @param c
     * @return
     */
    private String getID(VirtualClass c) {
        return c.getSimpleName() + "(" + c.getFile().lastModified() + ")";
    }

    public T getEntry(VirtualClass c) {
        return data.get(getID(c));
    }

    /**
     * Updates the Cache and sets the changed flag. Call {@link #save()} to
     * write to harddisk
     * 
     * @param c
     * @param cache
     */
    public void update(VirtualClass c, T cache) {
        data.put(getID(c), cache);
        changed = true;
    }

    /**
     * Saves the cache to Harddisk and removes it from RAM. Only writes to HD if
     * there were changes
     * 
     * @throws Exception
     */
    public void save() throws Exception {
        if (data == null) throw new IllegalStateException("Do not call This method twice");
        if (changed) {
            try {
                String txt = JSonStorage.serializeToJson(data);
                file.delete();
                IO.writeStringToFile(file, txt);
            } catch (JsonGenerationException e) {
                e.printStackTrace();
            } catch (JsonMappingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        data = null;
    }

}
