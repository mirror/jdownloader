import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;

import javax.swing.JComponent;
public class ImageComponent extends JComponent
{
private Image image;
public ImageComponent (Image image)
{
this.image = image;
setPreferredSize (new Dimension (image.getWidth(null),
image.getHeight(null)));
}
public int getImageHeight(){
	return image.getHeight(this);
	
}
public int getImageWidth(){
	return image.getWidth(this);
	
}
public void paintComponent (Graphics g)
{
g.drawImage(image,0,0,null);
}
}