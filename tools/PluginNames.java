import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import jd.DecryptPluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.nutils.ClassFinder;
import jd.plugins.DecrypterPlugin;
import jd.utils.JDUtilities;

public class PluginNames {
    private static ClassLoader CL;

    /**
     * Returns a classloader to load plugins (class files); Depending on runtype
     * (dev or local jared) a different classoader is used to load plugins
     * either from installdirectory or from rundirectory
     * 
     * @return
     */
    private static ClassLoader getPluginClassLoader() {
        if (CL == null) {
            try {
                if (JDUtilities.getRunType() == JDUtilities.RUNTYPE_LOCAL_JARED) {
                    CL = new URLClassLoader(new URL[] { JDUtilities.getJDHomeDirectoryFromEnvironment().toURI().toURL() }, Thread.currentThread().getContextClassLoader());
                } else {
                    CL = Thread.currentThread().getContextClassLoader();
                }
            } catch (MalformedURLException e) {
                JDLogger.exception(e);
            }
        }
        return CL;
    }

    public static void main(String[] args) {
        try {
            for (Class<?> c : ClassFinder.getClasses("jd.plugins.decrypter", getPluginClassLoader())) {
                try {
                    if (c != null && c.getAnnotations().length > 0) {
                        DecrypterPlugin help = (DecrypterPlugin) c.getAnnotations()[0];
                        if (help.toString().contains("8729")) System.out.println(help.toString() + "" + help.revision());
                        if (help.interfaceVersion() != DecrypterPlugin.INTERFACE_VERSION) {
                            continue;
                        }
                        String[] names = help.names();

                        String[] patterns = help.urls();
                        int[] flags = help.flags();

                        // Needed for changing the pattern from
                        // UCMS/Wordpress/Redirecter/...
                        String dump = "";
                        // See if there are cached annotations
                        if (names.length == 0) {
                            SubConfiguration cfg = SubConfiguration.getConfig("jd.JDInit.loadPluginForDecrypt");
                            names = cfg.getGenericProperty(c.getName() + "_names_" + dump + help.revision(), names);
                            patterns = cfg.getGenericProperty(c.getName() + "_pattern_" + dump + help.revision(), patterns);
                            flags = cfg.getGenericProperty(c.getName() + "_flags_" + dump + help.revision(), flags);
                        }
                        // if not, try to load them from static functions
                        if (names.length == 0) {
                            names = (String[]) c.getMethod("getAnnotationNames", new Class[] {}).invoke(null, new Object[] {});
                            patterns = (String[]) c.getMethod("getAnnotationUrls", new Class[] {}).invoke(null, new Object[] {});
                            flags = (int[]) c.getMethod("getAnnotationFlags", new Class[] {}).invoke(null, new Object[] {});
                            SubConfiguration cfg = SubConfiguration.getConfig("jd.JDInit.loadPluginForDecrypt");
                            cfg.setProperty(c.getName() + "_names_" + help.revision(), names);
                            cfg.setProperty(c.getName() + "_pattern_" + help.revision(), patterns);
                            cfg.setProperty(c.getName() + "_flags_" + help.revision(), flags);
                            cfg.save();

                        }
                        for (int i = 0; i < names.length; i++) {
                            try {
                                new DecryptPluginWrapper(names[i], c.getSimpleName(), patterns[i], flags[i], help.revision());
                                // System.out.println("Add decrypter for " +
                                // names[i]);
                            } catch (Throwable e) {
                                JDLogger.exception(e);
                            }
                        }
                    }
                } catch (Throwable e) {

                    JDLogger.exception(e);
                }
            }
        } catch (Throwable e) {
            JDLogger.exception(e);
        }
    }
}
