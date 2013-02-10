package org.jdownloader.extensions.jdanywhere.api.content;

import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.jdownloader.api.content.ContentAPIImpl;
import org.jdownloader.extensions.jdanywhere.CheckUser;

public class ContentMobileAPIImpl implements ContentMobileAPI {

    ContentAPIImpl    ctAPI = new ContentAPIImpl();
    private CheckUser checkUser;

    public ContentMobileAPIImpl(String user, String pass) {
        super();
        this.user = user;
        this.pass = pass;
        checkUser = new CheckUser(user, pass);
    }

    private String user;
    private String pass;

    @Override
    public void favicon(RemoteAPIRequest request, RemoteAPIResponse response, String hostername, final String username, final String password) {
        if (!checkUser.check(username, password)) return;
        ctAPI.favicon(request, response, hostername);
    }

    public void fileIcon(RemoteAPIRequest request, RemoteAPIResponse response, String extension, final String username, final String password) {
        if (!checkUser.check(username, password)) return;
        ctAPI.fileIcon(request, response, extension);
    }

    public String getUsername() {
        return user;
    }

    public String getPassword() {
        return pass;
    }
}
