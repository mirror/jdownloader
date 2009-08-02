package jd.gui.swing.jdgui.views;

import javax.swing.Icon;

import jd.gui.swing.jdgui.interfaces.View;
import jd.gui.swing.jdgui.views.premiumview.PremiumPanel;
import jd.gui.swing.jdgui.views.toolbar.ViewToolbar;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class PremiumView extends View {

    /**
     * 
     */
    private static final long serialVersionUID = 1576022161763608766L;
    /**
     * DO NOT MOVE THIS CONSTANT. IT's important to have it in this file for the
     * LFE to parse JDL Keys correct
     */
    private static final String IDENT_PREFIX = "jd.gui.swing.jdgui.views.premiumview.";

    public PremiumView() {
        this.setContent(new PremiumPanel());
        ViewToolbar toolbar = new ViewToolbar();

        toolbar.setList(new String[] { "action.premiumview.addacc" });

        this.setToolBar(toolbar);
    }

    @Override
    public Icon getIcon() {
        return JDTheme.II("gui.images.premium", ICON_SIZE, ICON_SIZE);
    }

    @Override
    public String getTitle() {
        return JDL.L(IDENT_PREFIX + "tab.title", "Premiumaccounts");
    }

    @Override
    public String getTooltip() {
        return JDL.L(IDENT_PREFIX + "tab.tooltip", "Here you can setup your Premiumaccounts");
    }

    @Override
    protected void onHide() {
        // TODO Auto-generated method stub

    }

    @Override
    protected void onShow() {
        // TODO Auto-generated method stub

    }

}
