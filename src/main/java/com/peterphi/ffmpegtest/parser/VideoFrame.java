package com.peterphi.ffmpegtest.parser;

import org.apache.commons.imaging.common.ImageBuilder;

import java.awt.image.BufferedImage;

/**
 * A very simple uncompressed video frame which stores individual R,G,B channel values
 */
public class VideoFrame
{
	private final int height;
	private final int width;

	private final short[] rgb;


	public VideoFrame(final int height, final int width)
	{
		this.height = height;
		this.width = width;

		this.rgb = new short[height * width * 3];
	}


	public int getHeight()
	{
		return height;
	}


	public int getWidth()
	{
		return width;
	}


	public void getRGB(short[] buffer, final int x, final int y)
	{
		final int offset = getOffset(x, y);

		buffer[0] = rgb[offset];
		buffer[1] = rgb[offset + 1];
		buffer[2] = rgb[offset + 2];
	}


	public void setRGB(int x, int y, final short red, final short green, final short blue)
	{
		final int offset = getOffset(x, y);

		rgb[offset] = red;
		rgb[offset + 1] = green;
		rgb[offset + 2] = blue;
	}


	/**
	 * @param x
	 * 		the x coordinate (height axis)
	 * @param y
	 * 		the y coordinate (width axis)
	 *
	 * @return
	 */
	private final int getOffset(final int x, final int y)
	{
		return ((x * width) + y) * 3;
	}


	public BufferedImage toImage()
	{
		final ImageBuilder builder = new ImageBuilder(width, height, false);

		final short[] rgb = new short[3];
		for (int x = 0; x < height; x++)
		{
			for (int y = 0; y < width; y++)
			{
				// Read the RGB values at x,y into the array rgb
				getRGB(rgb, x, y);


				final int value = ((0xFF << 24) |
				                   ((rgb[0] & 0xFF) << 16) |
				                   ((rgb[1] & 0xFF) << 8) |
				                   ((rgb[2] & 0xFF) << 0));

				builder.setRGB(y, x, value);
			}
		}

		return builder.getBufferedImage();
	}
}
