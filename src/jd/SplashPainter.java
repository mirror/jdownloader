package jd;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;

import javax.swing.JLabel;

/**
 * <p>
 * Title: SplashPainter
 * </p>
 * <p>
 * Description: Displays Spashimage
 * </p>
 * <p>
 * Copyright: Copyright (c) 2006
 * </p>
 * <p>
 * Company: Nice Dezigns
 * </p>
 *
 * @author Jens Hohl
 * @date 05.08.2006
 * @time 03:36:39
 */
public class SplashPainter
        extends JLabel
{
        /**
     * 
     */
    private static final long serialVersionUID = 1L;
        private Image image;
       
        @Override
        public void paintComponent(Graphics g)
        {
                super.paintComponents(g);
                g.drawImage(image,0,0,this);
        }
       
        
        public void setImage(Image image)
        {
                this.image = image;
                setPreferredSize(new Dimension(image.getWidth(null),image.getHeight(null)));
        }
} 