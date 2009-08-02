package jd.plugins.optional.jdchat;

import javax.swing.Icon;

import jd.gui.swing.jdgui.views.ClosableView;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class JDChatView extends ClosableView {

    private static final long serialVersionUID = -7876057076125402969L;
    private static final String JDL_PREFIX = "jd.plugins.optional.jdchat.JDChatView.";

    public JDChatView() {
        super();

        init();
    }

    @Override
    public Icon getIcon() {
        return JDTheme.II("gui.images.chat", 16, 16);
    }

    @Override
    public String getTitle() {
        return JDL.L(JDL_PREFIX + "title", "JD Support Chat");
    }

    @Override
    public String getTooltip() {
        return JDL.L(JDL_PREFIX + "tooltip", "JD Support Chat");

    }

    @Override
    protected void onHide() {

    }

    @Override
    protected void onShow() {

    }

}
