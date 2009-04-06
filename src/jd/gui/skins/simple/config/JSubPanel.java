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

    private JPanel addSeparator(String title, Icon icon) {
        JLinkButton label = null;
        JPanel ret = new JPanel(new MigLayout("ins 0", "[]10[grow,fill]"));
        try {

            label = new JLinkButton("<html><u><b>" + title + "</b></u></html>", icon, new URL("http://wiki.jdownloader.org/?do=search&id=" + Encoding.urlEncode(title)));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        label.setIconTextGap(8);
        label.setBorder(null);
        ret.add(label, "align left");
        ret.add(new JSeparator());
add(ret);
        return ret;

    }
}
