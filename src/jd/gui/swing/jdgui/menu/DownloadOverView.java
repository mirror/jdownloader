package jd.gui.swing.jdgui.menu;

import javax.swing.Box;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.appwork.swing.components.ExtCheckBox;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class DownloadOverView extends MenuEditor {

    /**
	 * 
	 */
    private static final long serialVersionUID = 5406904697287119514L;
    private JLabel            lbl;

    public DownloadOverView() {
        super();
        setLayout(new MigLayout("ins 2", "6[grow,fill][][]", "[grow,fill]"));

        setOpaque(false);

        lbl = getLbl(_GUI._.DownloadOverView_DownloadOverView_(), NewTheme.I().getIcon("bottombar", 18));

        add(lbl);
        add(new ExtCheckBox(CFG_GUI.DOWNLOAD_PANEL_OVERVIEW_VISIBLE, lbl), "width 20!");
        add(Box.createHorizontalGlue(), "height 22!,width 90!");

    }
}
