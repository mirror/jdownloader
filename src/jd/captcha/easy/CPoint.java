package jd.captcha.easy;

import java.awt.Point;
import java.io.Serializable;

import jd.captcha.pixelgrid.Captcha;

public class CPoint extends Point implements Serializable {
	
	/**
	 * 
	 */
	public final static short LAB_DISTANCE = 1;
	public final static short RGB_DISTANCE1 = 2;
	public final static short RGB_DISTANCE2 = 3;
	public final static short HUE_DISTANCE = 4;
	public final static short SATURATION_DISTANCE = 5;
	public final static short BRIGHTNESS_DISTANCE = 6;
	public final static short RED_DISTANCE = 7;
	public final static short GREEN_DISTANCE = 8;
	public final static short BLUE_DISTANCE = 9;

	private static final long serialVersionUID = 333616481245029882L;
	private int color, distance;
	private boolean foreground = true;
	private short colorDistanceMode = LAB_DISTANCE;
	public short getColorDistanceMode() {
		return colorDistanceMode;
	}

	public void setColorDistanceMode(short colorDistanceMode) {
		this.colorDistanceMode = colorDistanceMode;
	}

	public boolean isForeground() {
		return foreground;
	}

	public void setForeground(boolean foreground) {
		this.foreground = foreground;
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
	}

	public int getDistance() {
		return distance;
	}
	
	public void setDistance(int distance) {
		this.distance = distance;
	}
	public CPoint() {
	}
	public CPoint(int x, int y, int distance, Captcha captcha) {
		this(x, y, distance, captcha.getPixelValue(x, y));
	}

	public CPoint(int x, int y, int distance, int color) {
		super(x, y);
		this.color = color;
		this.distance = distance;
	}
	@Override
	public Object clone() {
		return new CPoint(x,y,distance,color);
	}
	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		return super.equals(obj) || ((CPoint) obj).color == color;
	}
}
