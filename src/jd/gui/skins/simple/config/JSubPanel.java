package jd.gui.skins.simple.config;

import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import jd.config.ConfigGroup;
import jd.gui.skins.simple.components.JLinkButton;
import jd.http.Encoding;
import net.miginfocom.swing.MigLayout;

public class JSubPanel extends JPanel {

    private static final long serialVersionUID = 1823383684914263748L;
    private ConfigGroup group;

    public JSubPanel(ConfigGroup group) {
        super(new MigLayout("ins 5,wrap 1", "[fill,grow]", "[fill,grow]"));
        this.group = group;
        this.setName(group.getName());
        addSeparator(group.getName(), group.getIcon());

        // if (subPanelName.startsWith("hide-")) {
        // this.setBorder(BorderFactory.createEtchedBorder());
        // } else {
        // this.setBorder(BorderFactory.createTitledBorder(group.getName()));
        // }
    }

    public ConfigGroup getGroup() {
        // TODO Auto-generated method stub
        return group;
    }


}
