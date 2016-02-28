package jd.gui.swing.dialog;

import java.awt.Window;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

import org.appwork.swing.MigPanel;
import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.dimensor.RememberLastDialogDimension;
import org.appwork.utils.swing.dialog.locator.CenterOfScreenDialogLocator;

public class CreditsDialog extends AbstractDialog<Object> {

    private Window owner;

    public CreditsDialog(Window parent) {
        super(UIOManager.BUTTONS_HIDE_CANCEL | UIOManager.BUTTONS_HIDE_OK | Dialog.STYLE_HIDE_ICON, "Credits", null, null, null);
        this.owner = parent;
        setLocator(new CenterOfScreenDialogLocator());
        setDimensor(new RememberLastDialogDimension("CreditsDialog"));
    }

    @Override
    protected Object createReturnValue() {
        return null;
    }

    @Override
    public Window getOwner() {
        if (owner != null) {
            return owner;
        }
        return super.getOwner();
    }

    @Override
    protected int getPreferredWidth() {
        return 650;
    }

    @Override
    protected int getPreferredHeight() {
        return 450;
    }

    @Override
    public JComponent layoutDialogContent() {
        MigPanel p = new MigPanel("ins 2", "[grow,fill]", "[grow,fill]");
        CreditsTable table = new CreditsTable();
        p.add(new JScrollPane(table));
        return p;
    }

}
