package jd.gui.skins.jdgui.views;

import javax.swing.Icon;
import javax.swing.JLabel;

import jd.gui.skins.jdgui.interfaces.SwitchPanel;
import jd.gui.skins.jdgui.interfaces.View;
import jd.gui.skins.simple.LogPane;
import jd.utils.JDTheme;

public class TestView extends View {

    public TestView() {
        super();

        LogPane lp;
        SwitchPanel sp;
        this.add(new JLabel("fhjdksjf"));
//        this.setContent(sp = new SwitchPanel() {
//
//            @Override
//            protected void onHide() {
//                // TODO Auto-generated method stub
//
//            }
//
//            @Override
//            protected void onShow() {
//                // TODO Auto-generated method stub
//
//            }
//
//        });
//        sp.add(new JLabel("THIS IS A TESTVIEW"));
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
        // TODO Auto-generated method stub

    }

    @Override
    protected void onShow() {

    }

}