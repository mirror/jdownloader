package jd;

import java.awt.AWTException;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import jd.gui.skins.simple.SimpleGUI;

/**
 * <p>
 * Title: SplashScreen
 * </p>
 * <p>
 * Description: Animated Splashscreen
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
 * @time 01:14:25
 */
public class SplashScreen implements ActionListener
{
        // desktop
        private final BufferedImage background;

        // Your logo
        private final BufferedImage image;

        // actual image
        private BufferedImage currentImage;

        private SplashPainter label;

        private final int speed = 1000 / 20; 

        /**
        * Duration in Time Mills
        */
        private float duration = 1500.0f; 

        private long startTime = 0;

        private boolean isBlendIn = true, isBlendOut = false;
        private JWindow window;

        private final Timer timer;
        private JProgressBar progressBar;
        public void setText(String text)
        {
            progressBar.setString(text);
        }
        public void setVisible(boolean b)
        {
            window.setVisible(b);
        }
        public void increase()
        {
            increase(1);
        }
        public void increase(int i)
        {
            setValue(getValue()+i);
        }
        public int getValue()
        {
            return progressBar.getValue();
        }
        public void setValue(int perc)
        {
            progressBar.setValue(perc);
            if(perc>70)
               blendOut();
            if(perc>99)
                window.dispose();
            
        }
        public void finish()
        {
            setValue(100);
        }
        public SplashScreen(String path) throws IOException, AWTException
        {
                //final URL url = this.getClass().getClassLoader().getResource(path);

                image = ImageIO.read(new File(path));

                currentImage = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(image.getWidth(null), image.getHeight(null));
                final Dimension screenDimension = Toolkit.getDefaultToolkit().getScreenSize();
                final int x = (int) (screenDimension.getWidth() / 2 - image.getWidth(null) / 2);
                final int y = (int) (screenDimension.getHeight() / 2 - image.getHeight(null) / 2);
                final int w = image.getWidth(null);
                final int h = image.getHeight(null);

                final Robot robot = new Robot();
                final Rectangle rectangle = new Rectangle(x, y, w, h);
                background = robot.createScreenCapture(rectangle);
                drawImage(0f);

                label = new SplashPainter();
                label.setImage(background);
                SimpleGUI.setUIManager();
                window = new JWindow();
                window.setSize(300, 200);
                Container content = window.getContentPane();
                content.add(BorderLayout.NORTH,label);
               // content.add(label);
               content.add(BorderLayout.SOUTH,progressBar=new JProgressBar());
               progressBar.setStringPainted(true);
                
               window.pack();
               window.setLocationRelativeTo(null);

                timer = new Timer(speed, this);
                timer.setCoalesce(true);
                timer.start();
                startTime = System.currentTimeMillis();
        }

        public void blendOut()
        {
            if(isBlendOut) return;
                isBlendOut = true;
                isBlendIn = false;
                startTime = System.currentTimeMillis();
                timer.start();
        }

        public void actionPerformed(ActionEvent e)
        {
                float percent;

                if (isBlendIn)
                {
                        percent = (System.currentTimeMillis() - startTime) / duration;
                        percent = Math.min(1.0f, percent);
                }
                else
                {
                        percent = (System.currentTimeMillis() - startTime) / duration;
                        percent = Math.min(1.0f, percent);
                        percent = 1.0f - percent;
                }

                float alphaValue = percent;

                if (percent >= 1.0)
                {
                        timer.stop();
                        // blendOut(); // Einkommentieren damit die animation sofort wieder
                        // ausgeblendet wird
                }
                else if (alphaValue <= 0.0f)
                {
                        timer.stop();
                        SwingUtilities.getWindowAncestor(label).dispose();
                }

                drawImage(alphaValue);
                label.setImage(currentImage);
                label.repaint();
                
        }

        /**
        * Draws Background, then draws image over it
        *
        * @param alphaValue
        */
        private void drawImage(float alphaValue)
        {
                final Graphics2D g2d = (Graphics2D) currentImage.getGraphics();
                g2d.drawImage(background, 0, 0, null);
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaValue));
                g2d.drawImage(image, 0, 0, null);
                g2d.dispose();
        }
} 