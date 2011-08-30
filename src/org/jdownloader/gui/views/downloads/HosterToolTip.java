package org.jdownloader.gui.views.downloads;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolTip;
import javax.swing.plaf.ComponentUI;

import jd.controlling.packagecontroller.AbstractNode;
import net.miginfocom.swing.MigLayout;

public class HosterToolTip extends JToolTip {
    private AbstractNode obj;
    private JPanel       panel;

    public AbstractNode getObj() {
        return obj;
    }

    public void setObj(AbstractNode obj) {
        this.obj = obj;
    }

    public HosterToolTip() {
        super();

        this.setLayout(new MigLayout("ins 5,wrap 1", "[grow,fill]", "[grow,fill]"));

        panel = new JPanel(new MigLayout("ins 5", "[]", "[]"));
        panel.setBorder(new JTextField().getBorder());
        add(panel);
        setOpaque(false);

    }

    protected void setUI(ComponentUI newUI) {
        // super.setUI(newUI);
    }

    @Override
    public void setTipText(String tipText) {
        // super.setTipText(tipText);
    }

    @Override
    public String getTipText() {
        return null;
    }

    /**
     * Resets the UI property to a value from the current look and feel.
     * 
     * @see JComponent#updateUI
     */
    // public void updateUI() {
    // // setUI((ToolTipUI)UIManager.getUI(this));
    // }

    // @Override
    // protected String getTooltipText(PackageLinkNode value) {
    // sb.delete(0, sb.length());
    // sb.append("<html>");
    //
    // if (value instanceof FilePackage) {
    //
    // sb.append(_GUI._.HosterColumn_getToolTip_packagetitle(((FilePackage)
    // value).getName()));
    //
    // for (String hoster : DownloadLink.getHosterList(((FilePackage)
    // value).getControlledDownloadLinks())) {
    // URL url = Application.getRessourceURL("/jd/img/favicons/" + hoster +
    // ".png");
    // if (url == null) {
    // sb.append(_GUI._.HosterColumn_getToolTip_hoster(hoster));
    // } else {
    // sb.append(_GUI._.HosterColumn_getToolTip_hoster_img(url, hoster));
    // }
    // }
    //
    // } else if (value instanceof DownloadLink) {
    // sb.append(_GUI._.HosterColumn_getToolTip_linktitle(((DownloadLink)
    // value).getName()));
    // URL url = Application.getRessourceURL("/jd/img/favicons/" +
    // ((DownloadLink) value).getHost() + ".png");
    // if (url == null) {
    // sb.append(_GUI._.HosterColumn_getToolTip_hoster(((DownloadLink)
    // value).getHost()));
    // } else {
    // sb.append(_GUI._.HosterColumn_getToolTip_hoster_img(url, ((DownloadLink)
    // value).getHost()));
    // }
    //
    // }
    // sb.append("</html>");
    // return sb.toString();
    // }

}
