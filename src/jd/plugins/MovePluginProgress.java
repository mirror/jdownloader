package jd.plugins;

import java.io.File;

import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.PluginTaskID;
import org.jdownloader.translate._JDT;

public class MovePluginProgress extends PluginProgress {

    private File   file;
    private String msg;

    public MovePluginProgress() {
        super(-1, 100, null);
        setIcon(new AbstractIcon(IconKey.ICON_SAVETO, 18));
    }

    @Override
    public String getMessage(Object requestor) {
        return msg;
    }

    @Override
    public long getCurrent() {
        // double perc = getPercent();
        return super.getCurrent();
    }

    @Override
    public double getPercent() {
        return super.getPercent();
    }

    @Override
    public PluginTaskID getID() {
        return PluginTaskID.MOVE_FILE;
    }

    public void setFile(File newFile) {
        this.file = newFile;
        msg = _JDT._.MovePluginProgress(file.getAbsolutePath());
    }

}
