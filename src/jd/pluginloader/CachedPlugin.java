package jd.pluginloader;

import org.appwork.storage.Storable;

/**
 * Dataclass! This class is serialized to a json object to cache class
 * informations Do not implement any logic here.
 * 
 * @author thomas
 * 
 */
public class CachedPlugin implements Storable {
    private int[]    flags;
    private String[] names;
    private String[] patterns;
    private String   revision;
    private int      interfaceVersion;

    public int[] getFlags() {
        return flags;
    }

    public void setFlags(int[] flags) {
        this.flags = flags;
    }

    public String[] getNames() {
        return names;
    }

    public void setNames(String[] names) {
        this.names = names;
    }

    public String[] getPatterns() {
        return patterns;
    }

    public void setPatterns(String[] patterns) {
        this.patterns = patterns;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public CachedPlugin(int[] flags, String[] names, String[] patterns, String revision, int interfaceVersion) {
        this.flags = flags;
        this.names = names;
        this.patterns = patterns;
        this.interfaceVersion = interfaceVersion;
        this.revision = revision;
    }

    public int getInterfaceVersion() {
        return interfaceVersion;
    }

    public void setInterfaceVersion(int interfaceVersion) {
        this.interfaceVersion = interfaceVersion;
    }

    protected CachedPlugin() {
    }
}
