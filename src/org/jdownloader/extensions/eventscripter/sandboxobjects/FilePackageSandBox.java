package org.jdownloader.extensions.eventscripter.sandboxobjects;

import jd.plugins.FilePackage;

import org.appwork.utils.Application;
import org.jdownloader.extensions.eventscripter.ScriptAPI;

@ScriptAPI(description = "The context download list package")
public class FilePackageSandBox {

    private FilePackage filePackage;

    public FilePackageSandBox(FilePackage parentNode) {
        this.filePackage = parentNode;
    }

    public FilePackageSandBox() {

    }

    public boolean isFinished() {
        if (filePackage == null) {
            return false;
        }
        return filePackage.getView().isFinished();
    }

    public String getDownloadFolder() {
        if (filePackage == null) {
            return Application.getResource("").getAbsolutePath();
        }
        return filePackage.getDownloadDirectory();
    }

    public String getName() {
        if (filePackage == null) {
            return "Example FilePackage Name";
        }
        return filePackage.getName();
    }

}
