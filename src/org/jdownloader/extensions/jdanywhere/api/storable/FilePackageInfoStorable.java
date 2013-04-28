package org.jdownloader.extensions.jdanywhere.api.storable;

import java.util.ArrayList;
import java.util.List;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.storage.Storable;

public class FilePackageInfoStorable implements Storable {
    public String getName() {
        return pkg.getName();
    }

    public String getComment() {
        String comment = pkg.getComment();
        if (comment == null || comment.length() == 0) return "";
        return comment;
    }

    public String getDirectory() {
        return pkg.getDownloadDirectory();
    }

    public String getPassword() {
        String password = "---";
        for (DownloadLink link : pkg.getChildren()) {
            if (password == null || password.length() == 0 || password.equals("---"))
                password = link.getDownloadPassword();
            else if (!link.getDownloadPassword().equals(password)) { return ""; }
        }
        if (password == null || password.length() == 0 || password.equals("---")) return "";
        return password;
    }

    public List<String> getHoster() {
        List<String> links = new ArrayList<String>(pkg.size());
        for (DownloadLink link : pkg.getChildren()) {
            if (!links.contains(link.getHost())) links.add(link.getHost());
        }
        return links;
    }

    // private List<DownloadLinkAPIStorable> links;
    private FilePackage pkg;

    @SuppressWarnings("unused")
    private FilePackageInfoStorable() {
    }

    public FilePackageInfoStorable(FilePackage pkg) {
        this.pkg = pkg;
    }
}