package jd.captcha.easy;

import java.awt.Point;

import jd.captcha.pixelgrid.Captcha;

public class CPoint extends Point {
	private static final long serialVersionUID = 1L;
	int color;
	int distance;

	public CPoint(int x, int y, int distance, Captcha captcha) {
		this(x, y, distance, captcha.getPixelValue(x, y));
	}

	public CPoint(int x, int y, int distance, int color) {
		super(x, y);
		this.color = color;
		this.distance = distance;
	}

	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		return super.equals(obj) || ((CPoint) obj).color == color;
	}
}
