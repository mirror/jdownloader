package jd.gui.swing.jdgui.views.settings.panels.packagizer;

import java.awt.event.ActionEvent;

import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.packagizer.PackagizerRule;
import org.jdownloader.translate._JDT;

public class DuplicateAction extends AppAction {

    /**
	 * 
	 */
    private static final long     serialVersionUID = 3259061642936241235L;
    private PackagizerRule        contextObject;
    private PackagizerFilterTable filterTable;

    public DuplicateAction(PackagizerRule contextObject, PackagizerFilterTable filterTable) {
        setName(_JDT._.DuplicateAction_DuplicateAction_());
        setIconKey("copy");
        this.contextObject = contextObject;
        this.filterTable = filterTable;
    }

    public boolean isEnabled() {
        return contextObject != null;
    }

    public void actionPerformed(ActionEvent e) {
        PackagizerRule newRule = contextObject.duplicate();
        NewAction.add(newRule);
    }

}
