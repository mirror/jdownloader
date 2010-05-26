package jd.gui.swing.jdgui.views.downloads.contextmenu;

import java.awt.event.ActionEvent;

import jd.controlling.DownloadWatchDog;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.utils.locale.JDL;

public class StopsignAction extends ContextMenuAction {

    private static final long serialVersionUID = 3332656936365114557L;

    private final Object obj;

    public StopsignAction(Object obj) {
        this.obj = obj;

        init();
    }

    @Override
    protected String getIcon() {
        return "gui.images.stopsign";
    }

    @Override
    protected String getName() {
        if (DownloadWatchDog.getInstance().isStopMark(obj)) {
            return JDL.L("gui.table.contextmenu.stopmark.unset", "Unset Stopmark");
        } else {
            return JDL.L("gui.table.contextmenu.stopmark.set", "Set Stopmark");
        }
    }

    public void actionPerformed(ActionEvent e) {
        DownloadWatchDog.getInstance().toggleStopMark(obj);
    }

}
