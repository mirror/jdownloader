package jd.gui.swing.jdgui.views;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JMenuBar;

import jd.gui.swing.jdgui.borders.JDBorderFactory;
import jd.gui.swing.jdgui.settings.ConfigPanel;
import jd.utils.locale.JDL;

public class ConfigPanelView extends ClosableView {

    private ConfigPanel panel;
    private Icon icon;
    private String title;

    public ConfigPanelView(ConfigPanel premium, String title, Icon icon) {
        super();
        panel = premium;
        this.title = title;
        this.icon = icon;
        this.setContent(panel);
        this.init();
    }
    protected void initMenu(JMenuBar menubar) {
        JButton top;
        menubar.add(top = new JButton(icon));
        top.setBorderPainted(false);
        top.setContentAreaFilled(false);
        menubar.add(top= new JButton(title));
        top.setBorderPainted(false);
        top.setContentAreaFilled(false);
        
        menubar.setBorder(JDBorderFactory.createInsideShadowBorder(0, 0, 1, 0));
    }
    @Override
    public Icon getIcon() {

        return icon;
    }

    @Override
    public String getTitle() {

        return title;
    }

    @Override
    public String getTooltip() {

        return null;
    }

    @Override
    protected void onHide() {

    }

    @Override
    protected void onShow() {

    }

}
