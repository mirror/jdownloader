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
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.TableModelEvent;

import jd.gui.skins.simple.SimpleGUI;

import jd.utils.JDLocale;

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
        private float duration = 1000.0f; 

        private long startTime = 0;

        private boolean isBlendIn = true, isBlendOut = false;
        private JWindow window;

        private final Timer timer;
        private JProgressBar progressBar;
        public void setText(final String text)
        {
            
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    progressBar.setString(text);}
            });
          
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
        public void setValue(final int perc)
        {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    progressBar.setValue(Math.min(100,perc));
                }
            });
          
          
//            if(perc>70)
//               
//            if(perc>99)
//                
            
        }
        public void finish()
        {
           // blendOut();
            timer.stop();
            window.dispose();
        }
        public SplashScreen(String path) throws IOException, AWTException
        {
                //final URL url = this.getClass().getClassLoader().getResource(path);
                SimpleGUI.setUIManager();
                image = ImageIO.read(new File(path));

                currentImage = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(image.getWidth(null), image.getHeight(null));
                final Dimension screenDimension = Toolkit.getDefaultToolkit().getScreenSize();
                progressBar=new JProgressBar();
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

                window = new JWindow();
                window.setAlwaysOnTop(true);
                window.setSize(300, 200);
                Container content = window.getContentPane();
                content.add(BorderLayout.NORTH,label);
               // content.add(label);
               content.add(BorderLayout.SOUTH,progressBar);
               progressBar.setStringPainted(true);
                
               window.pack();
               //indow.setLocationRelativeTo(null);
               window.setLocation(x, y);

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
                if(isBlendOut&&percent<=0.0f){
                    window.dispose();
                }
                
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