package jd.controlling.reconnect;

import org.appwork.utils.event.SimpleEvent;

public class ReconnecterEvent extends SimpleEvent<Reconnecter, Object, jd.controlling.reconnect.ReconnecterEvent.Type> {

    private final RouterPlugin plugin;

    public RouterPlugin getPlugin() {
        return plugin;
    }

    public Reconnecter.ReconnectResult getResult() {
        return result;
    }

    private final Reconnecter.ReconnectResult result;

    public ReconnecterEvent(Type type, RouterPlugin plugin) {
        this(type, plugin, null);
    }

    public ReconnecterEvent(Type type, RouterPlugin plugin, Reconnecter.ReconnectResult result) {
        super(Reconnecter.getInstance(), type);
        this.plugin = plugin;
        this.result = result;
    }

    public static enum Type {
        BEFORE,
        /**
         * PARAMETER[0]: Boolean | Success
         */
        AFTER
    }

}
