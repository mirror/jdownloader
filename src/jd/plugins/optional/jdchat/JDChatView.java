package jd.plugins.optional.jdchat;

import javax.swing.Icon;

import jd.gui.skins.jdgui.interfaces.View;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class JDChatView extends View {

    private static final String JDL_PREFIX = "jd.plugins.optional.jdchat.JDChatView.";

    public JDChatView() {
        super();
    
    }

    @Override
    public Icon getIcon() {
        // TODO Auto-generated method stub
        return JDTheme.II("gui.images.config.tip", 16, 16);
    }

    @Override
    public String getTitle() {
        // TODO Auto-generated method stub
        return JDL.L(JDL_PREFIX + "title", "JD Support Chat");
    }

    @Override
    public String getTooltip() {
        // TODO Auto-generated method stub
        return JDL.L(JDL_PREFIX + "tooltip", "JD Support Chat");
        
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
