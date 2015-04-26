package org.jdownloader.captcha.v2.solver.browser;

import java.io.IOException;

import jd.plugins.Plugin;

import org.appwork.utils.net.httpserver.requests.GetRequest;
import org.appwork.utils.net.httpserver.requests.PostRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;
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

    public boolean onGetRequest(BrowserReference browserReference, GetRequest request, HttpResponse response) throws IOException {
        return false;
    }

    public boolean onPostRequest(BrowserReference browserReference, PostRequest request, HttpResponse response) throws IOException {

        return false;

    }

}
