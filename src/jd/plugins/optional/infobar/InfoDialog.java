package jd.plugins.optional.infobar;

import java.awt.Dimension;

import javax.swing.JDialog;

import jd.gui.swing.components.SpeedMeterPanel;
import jd.gui.userio.DummyFrame;
import net.miginfocom.swing.MigLayout;

public class InfoDialog extends JDialog {

    private static InfoDialog INSTANCE = null;

    public static InfoDialog getInstance() {
        if (INSTANCE == null) INSTANCE = new InfoDialog();
        return INSTANCE;
    }

    private static final long serialVersionUID = 4715904261105562064L;

    private InfoDialog() {
        super(DummyFrame.getDialogParent());

        this.setAlwaysOnTop(true);
        this.setModal(false);
        this.setResizable(false);

        initGui();
    }

    private void initGui() {
        this.setLayout(new MigLayout("ins 0, wrap 1"));
        this.add(new SpeedMeterPanel(false, true), "height 30!,width 30:200:300");

        // TODO: Dirty Hack
        this.setSize(new Dimension(300, 100));
    }
}
