package jd.gui.skins.jdgui.views;

import javax.swing.Icon;
import javax.swing.JLabel;

import jd.gui.skins.jdgui.interfaces.View;
import jd.utils.JDTheme;

public class TestView extends View {

    private static final long serialVersionUID = 8698758386841005256L;

    public TestView() {
        super();

        this.add(new JLabel("fhjdksjf"));
    }

    @Override
    public Icon getIcon() {
        return JDTheme.II("gui.images.taskpanes.log", ICON_SIZE, ICON_SIZE);
    }

    @Override
    public String getTitle() {
        return "test";
    }

    @Override
    public String getTooltip() {
        return "Tooltip";
    }

    @Override
    protected void onHide() {
    }

    @Override
    protected void onShow() {
    }

}