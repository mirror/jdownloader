package org.jdownloader.iconsetter.gui.icon8;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtSpinner;
import org.appwork.swing.components.ExtTextField;
import org.appwork.uio.UIOManager;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.net.SimpleHTTP;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.dimensor.RememberLastDialogDimension;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.PseudoCombo;
import org.jdownloader.iconsetter.IconResource;
import org.jdownloader.iconsetter.IconSetMaker;
import org.jdownloader.iconsetter.gui.Icon8Resource;
import org.jdownloader.iconsetter.gui.IconSetterConfig;

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGElement;
import com.kitfox.svg.SVGUniverse;
import com.kitfox.svg.animation.AnimationElement;

import jd.nutils.encoding.Base64;
import jd.nutils.encoding.Encoding;

public class Icon8Dialog extends AbstractDialog<Object> {

    private ExtTextField       search;
    private IconResource       res;
    private IconSetMaker       owner;
    private JPanel             color;
    private MigPanel           p;
    private MigPanel           card;
    protected Icon8Table       table;
    private Icon8Resource      selectedIcon;
    private PseudoCombo<Style> style;
    private String             lastSearch;
    private ExtSpinner         alphaSpinner;

    @Override
    protected void setReturnmask(boolean b) {
        selectedIcon = null;
        if (b) {
            try {
                if (table != null) {
                    List<Icon8Resource> sel = table.getModel().getSelectedObjects();
                    if (sel != null && sel.size() > 0) {
                        selectedIcon = sel.get(0);

                        // byte[] png;
                        //
                        // png = selectedIcon.createPNG(32, new EDTHelper<Color>() {
                        //
                        // @Override
                        // public Color edtRun() {
                        // return color.getBackground();
                        // }
                        //
                        // }.getReturnValue());
                        //
                        // if (png != null && png.length > 0) {
                        // filePath.delete();
                        // filePath.getParentFile().mkdirs();
                        // IO.writeToFile(filePath, png);
                        // File info = new File(filePath.getAbsolutePath() + ".icon8");
                        // info.delete();
                        // IO.writeStringToFile(info, selectedIcon.getId());
                        // }
                        final File png = res.getFile(owner.getResoureSet(), "png");
                        final File svg = res.getFile(owner.getResoureSet(), "svg");
                        final File svgInfo = res.getFile(owner.getResoureSet(), "svg.icons8");
                        png.delete();
                        svg.delete();
                        svgInfo.delete();
                        new Thread("Write SVG") {
                            public void run() {
                                try {
                                    File png = res.getFile(owner.getResoureSet(), "png");
                                    File svg = res.getFile(owner.getResoureSet(), "svg");
                                    png.delete();
                                    svg.delete();
                                    byte[] bytes;

                                    bytes = selectedIcon.createSVG(new EDTHelper<Color>() {

                                        @Override
                                        public Color edtRun() {
                                            return color.getBackground();
                                        }

                                    }.getReturnValue());

                                    if (bytes != null && bytes.length > 0) {

                                        svg.getParentFile().mkdirs();
                                        IO.writeToFile(svg, bytes);

                                        IO.writeStringToFile(svgInfo, selectedIcon.getInfoString());
                                    }
                                    new EDTRunner() {

                                        @Override
                                        protected void runInEDT() {
                                            table.invalidate();
                                            table.repaint();
                                        }
                                    };
                                } catch (Throwable e) {
                                    UIOManager.I().showException(e.getMessage(), e);

                                }
                            };
                        }.start();

                    }
                }
            } catch (Throwable e) {
                UIOManager.I().showException(e.getMessage(), e);
                b = false;
            }

        }
        super.setReturnmask(b);

    }

    public Icon8Dialog(IconResource res, IconSetMaker owner) {
        super(0, "Icon 8 Interface", null, null, null);
        this.res = res;
        this.owner = owner;
        setDimensor(new RememberLastDialogDimension("Icon8Dialog"));
    }

    @Override
    protected Object createReturnValue() {
        return null;
    }

    @Override
    public JComponent layoutDialogContent() {
        okButton.setEnabled(false);
        p = new MigPanel("ins 5,wrap 2", "[][grow,fill]", "[24!][24!][24!][grow,fill]");
        search = new ExtTextField();
        search.setText(res.getTags(owner.getResoureSet()));
        search.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (color != null) {
                    updateSearch();
                }
            }
        });
        style = new PseudoCombo<Style>(Style.values()) {
            @Override
            protected String getLabel(Style v, boolean closed) {
                return v.getLabel();
            }

            @Override
            public void onChanged(Style newValue) {
                super.onChanged(newValue);
                JsonConfig.create(IconSetterConfig.class).setLastUsedStyle(style.getSelectedItem());
                if (color != null) {
                    updateSearch();
                }
            }
        };

        p.add(new JLabel("Search:"));
        p.add(search, "spanx");
        p.add(new JLabel("Style:"));
        p.add(style, "spanx");
        style.setSelectedItem(JsonConfig.create(IconSetterConfig.class).getLastUsedStyle());
        p.add(new JLabel("Color:"));
        color = new JPanel();
        color.setOpaque(true);
        color.setBackground(new Color(JsonConfig.create(IconSetterConfig.class).getColor(), true));

        color.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Color newColor = JColorChooser.showDialog(color, _GUI.T.AdvancedValueColumn_onSingleClick_colorchooser_title_(), color.getBackground());
                if (newColor != null && !newColor.equals(color.getBackground())) {
                    color.setBackground(newColor);
                    JsonConfig.create(IconSetterConfig.class).setColor(newColor.getRGB());
                    card.repaint();
                }
            }
        });

        p.add(color);
        // p.add(new JLabel("Alpha:"));
        // alphaSpinner= new ExtSpinner(new SpinnerNumberModel(255,1, 255, 1));
        // alphaSpinner.setValue(value);
        card = new MigPanel("ins 0", "[grow,fill]", "[grow,fill]");
        p.add(card, "spanx,pushx,growx");

        new Thread("Icons8Scanner") {
            public void run() {
                query(new EDTHelper<String>() {

                    @Override
                    public String edtRun() {
                        return search.getText();
                    }
                }.getReturnValue(), new EDTHelper<Style>() {

                    @Override
                    public Style edtRun() {
                        return style.getSelectedItem();
                    }
                }.getReturnValue());

            };
        }.start();
        return p;
    }

    protected synchronized void query(String searchTags, Style style) {
        lastSearch = searchTags;
        try {
            new EDTRunner() {

                @Override
                protected void runInEDT() {
                    table = null;
                    okButton.setEnabled(false);
                    card.removeAll();
                    card.add(new JLabel("...Please wait..."));
                    card.revalidate();

                }
            }.waitForEDT();
            SimpleHTTP br = new SimpleHTTP();

            String xml = null;

            if (style == null || style == Style.ALL) {
                xml = br.getPage(new URL("https://api.icons8.com/api/iconsets/search?term=" + Encoding.urlEncode(searchTags) + "&amount=100"));
            } else {
                xml = br.getPage(new URL("https://api.icons8.com/api/iconsets/search?term=" + Encoding.urlEncode(searchTags) + "&amount=100" + "&platform=" + Encoding.urlEncode(style.getKey())));
            }
            String[] icons = new Regex(xml, "(<icon .*?</icon>)").getColumn(0);
            final ArrayList<Icon8Resource> iconsList = new ArrayList<Icon8Resource>();
            for (String icon : icons) {
                String id = new Regex(icon, "id=\"(\\d+)").getMatch(0);
                String name = new Regex(icon, "name=\"([^\"]+)").getMatch(0);
                String platform = new Regex(icon, "platform=\"([^\"]+)").getMatch(0);
                String url = new Regex(icon, "url=\"([^\"]+)").getMatch(0);
                String svg = new Regex(icon, "<svg>([^<]+)").getMatch(0);
                String svgXML = new String(Base64.decode(svg), "ASCII");
                iconsList.add(new Icon8Resource(id, name, platform, url, svgXML));

            }
            new EDTRunner() {

                @Override
                protected void runInEDT() {
                    card.removeAll();
                    table = new Icon8Table(owner, Icon8Dialog.this, new Icon8TableModel(Icon8Dialog.this, iconsList)) {
                        protected boolean onDoubleClick(MouseEvent e, Icon8Resource obj) {
                            setReturnmask(true);
                            dispose();
                            return true;

                        };
                    };
                    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                    table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

                        @Override
                        public void valueChanged(ListSelectionEvent e) {
                            new EDTRunner() {

                                @Override
                                protected void runInEDT() {
                                    okButton.setEnabled(table.getModel().getSelectedObjects().size() > 0);
                                }
                            };
                        }
                    });
                    card.add(new JScrollPane(table));
                    card.revalidate();
                }
            }.waitForEDT();
        } catch (Throwable e) {
            new EDTRunner() {

                @Override
                protected void runInEDT() {
                    card.removeAll();
                    card.add(new JLabel("...Error..."));
                    card.revalidate();

                }

            }.waitForEDT();
            UIOManager.I().showException(e.getMessage(), e);

        }
    }

    public Icon getIcon(final Icon8Resource value, final int size) {

        return new Icon() {

            @Override
            public void paintIcon(Component c, Graphics gg, int x, int y) {
                try {
                    SVGUniverse universe = new SVGUniverse();

                    URI uri;

                    uri = universe.loadSVG(new ByteArrayInputStream(value.getSvg().getBytes("ASCII")), value.getName());

                    SVGDiagram diagram = universe.getDiagram(uri);
                    SVGElement root = diagram.getRoot();
                    // set color
                    float alpha = color.getBackground().getAlpha() / 255f;

                    String hex = "#" + String.format("%02x%02x%02x", color.getBackground().getRed(), color.getBackground().getGreen(), color.getBackground().getBlue());
                    root.addAttribute("fill", AnimationElement.AT_CSS, hex);
                    root.addAttribute("fill-opacity", AnimationElement.AT_CSS, alpha + "");

                    // Node svg = doc.getElementsByTagName("svg").item(0);
                    // NamedNodeMap attributes = svg.getAttributes();
                    // Node style = attributes.getNamedItem("style");
                    // if (style != null) {
                    // String css = style.getNodeValue();
                    // if (StringUtils.isNotEmpty(css) && !css.trim().endsWith(";")) {
                    // css = css.trim() + ";";
                    // }
                    // style.setNodeValue(css + "fill:" + hex + ";fill-opacity:" + alpha);
                    //
                    // } else {
                    // ((Element) svg).setAttribute("style", "fill:" + hex + ";fill-opacity:" + alpha);
                    // }

                    diagram.updateTime(0d);
                    diagram.setIgnoringClipHeuristic(true);

                    // BufferedImage bi = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = (Graphics2D) gg;
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    // int x = 0;
                    // int y = 0;
                    // g.setColor(Color.RED);
                    // g.drawRect(1, 1, size, size);
                    int width = size;
                    int height = size;
                    g.translate(x, y);
                    final Rectangle2D.Double rect = new Rectangle2D.Double();
                    diagram.getViewRect(rect);
                    AffineTransform scaleXform = new AffineTransform();
                    scaleXform.setToScale(width / rect.width, height / rect.height);

                    AffineTransform oldXform = g.getTransform();
                    g.transform(scaleXform);

                    diagram.render(g);

                    g.setTransform(oldXform);

                    g.translate(-x, -y);
                    g.setColor(Color.RED);
                    g.drawRect(1, 1, size, size);
                    // diagram.render(g);
                    // g.dispose();

                } catch (Throwable e) {
                    e.printStackTrace();
                }

            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }

        };

    }

    private void updateSearch() {
        new Thread("Icons8Scanner") {
            public void run() {
                query(new EDTHelper<String>() {

                    @Override
                    public String edtRun() {
                        return search.getText();
                    }
                }.getReturnValue(), new EDTHelper<Style>() {

                    @Override
                    public Style edtRun() {
                        return style.getSelectedItem();
                    }
                }.getReturnValue());

            };
        }.start();
    }

    public String getLastSearchString() {
        return lastSearch;
    }
}
