package jd.controlling.reconnect.plugins;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.jar.JarFile;

import javax.swing.JComponent;

import jd.controlling.ProgressController;
import jd.controlling.reconnect.ReconnectMethod;
import jd.controlling.reconnect.plugins.upnp.UPNPRouterPlugin;
import jd.nutils.io.JDFileFilter;
import jd.utils.JDUtilities;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;

import com.sun.istack.internal.Nullable;

public class ReconnectPluginController extends ReconnectMethod {
    private static final ReconnectPluginController INSTANCE = new ReconnectPluginController();

    public static ReconnectPluginController getInstance() {
        return ReconnectPluginController.INSTANCE;
    }

    private ArrayList<RouterPlugin> plugins;
    private final Storage           storage;

    private ReconnectPluginController() {
        this.storage = JSonStorage.getStorage("ReconnectPluginController");
        this.scan();

    }

    /**
     * returns the currently active routerplugin. Only one plugin may be active
     * 
     * @return
     */
    public RouterPlugin getActivePlugin() {

        RouterPlugin active = ReconnectPluginController.getInstance().getPluginByID(this.storage.get("ACTIVEPLUGIN", DummyRouterPlugin.getInstance().getID()));
        if (active == null) {
            active = DummyRouterPlugin.getInstance();
        }
        return active;
    }

    /**
     * returns the gui panel. This mopdule uses the new appwork JSonStorage and
     * does not need to use teh old ConfigPanel System.
     * 
     * @return
     */
    public JComponent getGUI() {
        // TODO Auto-generated method stub
        return ReconnectPluginConfigGUI.getInstance();
    }

    @Nullable
    /**
     * Returns the plugin that has the given ID.
     */
    public RouterPlugin getPluginByID(final String activeID) {
        for (final RouterPlugin plg : this.plugins) {
            if (plg.getID().equals(activeID)) { return plg; }
        }
        return null;
    }

    /**
     * Returns all registered Plugins
     * 
     * @return
     */
    public ArrayList<RouterPlugin> getPlugins() {

        return this.plugins;
    }

    @Override
    protected void initConfig() {
        // TODO Auto-generated method stub

    }

    @Override
    protected boolean runCommands(final ProgressController progress) {
        final RouterPlugin active = this.getActivePlugin();
        if (active == DummyRouterPlugin.getInstance()) { return false; }
        return active.doReconnect(progress);
    }

    /**
     * Scans for reconnection plugins
     */
    private void scan() {
        final File[] files = JDUtilities.getResourceFile("reconnect").listFiles(new JDFileFilter(null, ".rec", false));
        this.plugins = new ArrayList<RouterPlugin>();
        this.plugins.add(DummyRouterPlugin.getInstance());
        if (files != null) {
            final File file;
            final String name;
            final String absolutePath;

            final int length = files.length;
            final ArrayList<URL> urls = new ArrayList<URL>();
            for (int i = 0; i < length; i++) {
                try {
                    urls.add(files[i].toURI().toURL());

                    final JarFile jar = new JarFile(files[i]);
                } catch (final IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
            // TODO make sure that we do not load two plugins with the same id

        }
        this.plugins.add(UPNPRouterPlugin.getInstance());
    }

    public void setActivePlugin(final RouterPlugin selectedItem) {
        this.storage.put("ACTIVEPLUGIN", selectedItem.getID());

    }
}
