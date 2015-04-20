package org.jdownloader.captcha.v2.solver.browser;

import jd.plugins.Plugin;

import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.solverjob.ResponseList;

public abstract class AbstractBrowserChallenge extends Challenge<String> {

    private Plugin plugin;

    public Plugin getPlugin() {
        return plugin;
    }

    public boolean isSolved() {
        final ResponseList<String> results = getResult();
        return results != null && results.getValue() != null;
    }

    public AbstractBrowserChallenge(String method, Plugin pluginForHost) {

        super(method, null);
        this.plugin = pluginForHost;
    }

    abstract public String getHTML();

    abstract public BrowserViewport getBrowserViewport(BrowserWindow screenResource);

}
