package jd.gui.swing.jdgui.views.downloads.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.gui.UserIO;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.DownloadLink;
import jd.utils.locale.JDL;

public class SetPasswordAction extends ContextMenuAction {

    private static final long serialVersionUID = -6673856992749946616L;

    private final ArrayList<DownloadLink> links;

    public SetPasswordAction(ArrayList<DownloadLink> links) {
        this.links = links;

        init();
    }

    @Override
    protected String getIcon() {
        return "gui.images.password";
    }

    @Override
    protected String getName() {
        return JDL.L("gui.table.contextmenu.setdlpw", "Set download password") + " (" + links.size() + ")";
    }

    public void actionPerformed(ActionEvent e) {
        String pw = UserIO.getInstance().requestInputDialog(0, JDL.L("gui.linklist.setpw.message", "Set download password"), null);
        for (DownloadLink link : links) {
            link.setProperty("pass", pw);
        }
    }

}
