package jd.gui.skins.simple.startmenu.actions;

import java.awt.event.ActionEvent;
import java.io.File;

import jd.gui.skins.simple.components.JDFileChooser;
import jd.nutils.io.JDFileFilter;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class AddContainerAction extends StartAction {

    private static final long serialVersionUID = 4713690050852393405L;

    public AddContainerAction() {
        super("action.load", "gui.images.load");
    }

    public void actionPerformed(ActionEvent e) {
        JDFileChooser fc = new JDFileChooser("_LOADSAVEDLC");
        fc.setDialogTitle(JDLocale.L("gui.filechooser.loaddlc", "Load DLC file"));
        fc.setFileFilter(new JDFileFilter(null, ".dlc|.rsdf|.ccf|.linkbackup", true));
        if (fc.showOpenDialog(null) == JDFileChooser.APPROVE_OPTION) {
            File ret2 = fc.getSelectedFile();
            if (ret2 != null) {
                JDUtilities.getController().loadContainerFile(ret2);
            }
        }
    }

}
