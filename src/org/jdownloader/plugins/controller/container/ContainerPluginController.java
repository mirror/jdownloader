package org.jdownloader.plugins.controller.container;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jd.plugins.PluginsC;

import org.appwork.utils.Regex;
import org.appwork.utils.logging.Log;
import org.jdownloader.container.AMZ;
import org.jdownloader.container.C;
import org.jdownloader.container.D;
import org.jdownloader.container.MetaLink;
import org.jdownloader.container.R;
import org.jdownloader.container.SFT;

public class ContainerPluginController {

    private static final ContainerPluginController INSTANCE = new ContainerPluginController();

    public static ContainerPluginController getInstance() {
        return ContainerPluginController.INSTANCE;
    }

    private List<PluginsC> list;

    private ContainerPluginController() {
        list = null;
    }

    public void init() {
        List<PluginsC> plugins = new ArrayList<PluginsC>();
        try {
            plugins.add(new AMZ());
            plugins.add(new C());
            plugins.add(new D());
            plugins.add(new MetaLink());
            plugins.add(new R());
            plugins.add(new SFT());
        } catch (final Throwable e) {
            Log.exception(e);
        }
        list = Collections.unmodifiableList(plugins);
    }

    public List<PluginsC> list() {
        lazyInit();
        return list;
    }

    public void setList(List<PluginsC> list) {
        if (list == null) return;
        this.list = list;
    }

    private void lazyInit() {
        if (list != null) return;
        synchronized (this) {
            if (list != null) return;
            init();
        }
    }

    public PluginsC get(String displayName) {
        lazyInit();
        for (PluginsC p : list) {
            if (p.getName().equalsIgnoreCase(displayName)) return p;
        }
        return null;
    }

    public String getContainerExtensions(final String filter) {
        lazyInit();
        StringBuilder sb = new StringBuilder("");
        for (final PluginsC act : list) {
            if (filter != null && !new Regex(act.getName(), filter).matches()) continue;
            String exs[] = new Regex(act.getSupportedLinks().pattern(), "\\.([a-zA-Z0-9]+)").getColumn(0);
            for (String ex : exs) {
                if (sb.length() > 0) sb.append("|");
                sb.append(".").append(ex);
            }
        }
        return sb.toString();
    }
}