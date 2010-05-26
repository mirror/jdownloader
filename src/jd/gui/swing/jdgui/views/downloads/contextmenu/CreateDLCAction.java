package jd.gui.swing.jdgui.views.downloads.contextmenu;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;

import jd.gui.swing.GuiRunnable;
import jd.gui.swing.SwingGui;
import jd.gui.swing.components.JDFileChooser;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.nutils.io.JDFileFilter;
import jd.nutils.io.JDIO;
import jd.plugins.DownloadLink;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class CreateDLCAction extends ContextMenuAction {

    private static final long serialVersionUID = 7244681674979415222L;

    private final ArrayList<DownloadLink> links;

    public CreateDLCAction(ArrayList<DownloadLink> links) {
        this.links = links;

        init();
    }

    @Override
    protected String getIcon() {
        return "gui.images.dlc";
    }

    @Override
    protected String getName() {
        return JDL.L("gui.table.contextmenu.dlc", "Create DLC") + " (" + links.size() + ")";
    }

    public void actionPerformed(ActionEvent e) {
        GuiRunnable<File> temp = new GuiRunnable<File>() {
            @Override
            public File runSave() {
                JDFileChooser fc = new JDFileChooser("_LOADSAVEDLC");
                fc.setFileFilter(new JDFileFilter(null, ".dlc", true));
                if (fc.showSaveDialog(SwingGui.getInstance().getMainFrame()) == JDFileChooser.APPROVE_OPTION) return fc.getSelectedFile();
                return null;
            }
        };
        File ret = temp.getReturnValue();
        if (ret == null) return;
        if (JDIO.getFileExtension(ret) == null || !JDIO.getFileExtension(ret).equalsIgnoreCase("dlc")) {
            ret = new File(ret.getAbsolutePath() + ".dlc");
        }
        JDUtilities.getController().saveDLC(ret, links);
    }

}
