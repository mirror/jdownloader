package jd.pluginloader;

import java.io.File;

public class VirtualClass {

    private Class<?> clazz;
    private String   packageName;

    public String getPackageName() {
        return packageName;
    }

    public File getFile() {
        return file;
    }

    private String      simpleName;
    private ClassLoader classLoader;
    private File        file;

    /**
     * Creates a new Virtualclass based on a class which has already been loaded
     * 
     * @param c
     */
    public VirtualClass(Class<?> c) {
        clazz = c;

        this.simpleName = c.getSimpleName();
        packageName = c.getPackage().getName();
    }

    /**
     * Creates a Virtual class based on the classloader, and the classfile
     * 
     * @param classLoader
     * @param file
     * @param packageName
     * @param simpleName
     */
    public VirtualClass(ClassLoader classLoader, File file, String packageName, String simpleName) {
        this.classLoader = classLoader;
        this.packageName = packageName;
        this.simpleName = simpleName;
        this.file = file;
    }

    /**
     * Load this class if it has not been loaded yet
     * 
     * @return
     * @throws ClassNotFoundException
     */
    public Class<?> loadClass() throws ClassNotFoundException {
        synchronized (this) {
            if (clazz != null) return clazz;
            clazz = classLoader.loadClass(packageName + '.' + simpleName);

        }
        return clazz;
    }

    /**
     * 
     * @return Class.getSimpleName
     */
    public String getSimpleName() {
        return simpleName;
    }

    /**
     * 
     * @return if class actually has been loaded so far
     */

    public boolean isLoaded() {
        return clazz != null;
    }

    public Class<?> getLoadedClass() {
        return clazz;
    }

}
