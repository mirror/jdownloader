package jd.plugins;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.TYPE)
public @interface HostPlugin {
    /**
     * Current Interface Version id for the hostplugin interface. Increase this
     * id if you changed the host plugin's interface. This will mark plugins
     * that do not fit as outdated.
     */
    int INTERFACE_VERSION = 1;

    /**
     * A list of pluginnames. A plugin may be used under various names.
     * 
     * Take care that names, urls and flags ALWAYS have to have the same length.
     * 
     * @return
     */
    String[] names();

    /**
     * A list of url patterns. a plugin may fit to various urls/domains.
     * 
     * Take care that names, urls and flags ALWAYS have to have the same length.
     * 
     * @return
     */
    String[] urls();

    /**
     * A list of Pluginflags. For most plugins the flag 0 is ok. Use
     * PluginWrapper.*** FLAGS for special cases
     * 
     * Take care that names, urls and flags ALWAYS have to have the same length.
     * 
     * @return
     */

    int[] flags();

    /**
     * A Plugins interface always has to have the same value as
     * HostPlugin.INTERFACE_VERSION . This value is used to filter out outdated
     * plugins if the plugin interface has changed.
     * 
     * @return
     */
    int interfaceVersion();

    /**
     * The Revision is autoset by SVN. always is revision="$Revision$" and to
     * not forget to ste the svn keyword svn:keywords to Revision
     * 
     * @return
     */
    String revision();
}
