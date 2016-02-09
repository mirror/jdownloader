package org.jdownloader.iconsetter.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;

import javax.imageio.ImageIO;

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGElement;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.SVGUniverse;
import com.kitfox.svg.animation.AnimationElement;

public class Icon8Resource {

    private String id;
    private String name;
    private String platform;

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getSvg() {
        return svg;
    }

    private String url;
    private String svg;

    public Icon8Resource(String id, String name, String platform, String url, String svg) {
        this.id = id;
        this.name = name;
        this.platform = platform;
        this.url = url;
        this.svg = svg;
    }

    public byte[] createPNG(int size, Color color) throws UnsupportedEncodingException, IOException, SVGException {

        SVGUniverse universe = new SVGUniverse();

        URI uri = universe.loadSVG(new ByteArrayInputStream(svg.getBytes("ASCII")), name);

        SVGDiagram diagram = universe.getDiagram(uri);
        SVGElement root = diagram.getRoot();
        // set color
        String hex = "#" + String.format("%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
        root.addAttribute("fill", AnimationElement.AT_CSS, hex);

        diagram.updateTime(0d);
        diagram.setIgnoringClipHeuristic(true);

        BufferedImage bi = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int x = 0;
        int y = 0;

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

        // diagram.render(g);
        g.dispose();
        ByteArrayOutputStream bao;
        ImageIO.write(bi, "png", bao = new ByteArrayOutputStream());
        return bao.toByteArray();
    }
}
