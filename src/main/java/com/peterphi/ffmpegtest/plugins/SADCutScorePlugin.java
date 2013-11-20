package com.peterphi.ffmpegtest.plugins;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Simple plugin that produces a cut score of each frame compared to the past using the naive Sum of Absolute Differences
 * approach and writes the data to a CSV file
 */
public class SADCutScorePlugin implements VideoAnalysisPlugin
{
	private BufferedImage last;
	private PrintWriter csvFile;


	public SADCutScorePlugin(File csvFile) throws IOException
	{
		this(new PrintWriter(new FileWriter(csvFile)));
	}


	public SADCutScorePlugin(final PrintWriter writer)
	{
		this.csvFile = writer;
	}


	@Override
	public void start()
	{
		csvFile.println("frame,score,processingTime");
	}


	@Override
	public void frame(final int frame, final BufferedImage image)
	{
		final long start = System.currentTimeMillis();
		long score = score(last, image);
		final long timeTaken = System.currentTimeMillis() - start;

		csvFile.println(frame + "," + score + "," + timeTaken);

		// Now store this as the last frame
		last = image;
	}


	@Override
	public void end()
	{
		csvFile.close();
	}


	/**
	 * Score the differences between 2 frames
	 *
	 * @param a
	 * 		a frame
	 * @param b
	 * 		another frame (with the same dimensions as the original)
	 *
	 * @return
	 */
	private final long score(final BufferedImage a, final BufferedImage b)
	{
		if (a == null || b == null)
			return Long.MAX_VALUE;

		long score = 0;

		for (int x = 0; x < a.getWidth(); x++)
		{
			for (int y = 0; y < a.getHeight(); y++)
			{
				final int aPixel = a.getRGB(x, y);
				final int bPixel = b.getRGB(x, y);

				final int delta = score(aPixel, bPixel);

				score += delta;
			}
		}
		return score;
	}


	/**
	 * Score the difference between two pixels
	 *
	 * @param a
	 * 		a pixel (encoded as 0xAARRGGBB)
	 * @param b
	 * 		a pixel (encoded as 0xAARRGGBB)
	 *
	 * @return A score for that pixel computed as the sum of the absolute deltas between each individual channel across the 2
	 * pixels (i.e. <code>abs(a.red-b.red) + abs(a.green+b.green) + abs(a.blue+b.blue)</code>)
	 */
	private final int score(final int a, final int b)
	{
		// Fast-path for identical pixels
		if (a == b)
			return 0;

		// Extract individual channels from A
		final int aRed = (a >> 16) & 0xFF;
		final int aGreen = (a >> 8) & 0xFF;
		final int aBlue = (a) & 0xFF;

		// Extract individual channels from B
		final int bRed = (b >> 16) & 0xFF;
		final int bGreen = (b >> 8) & 0xFF;
		final int bBlue = (b) & 0xFF;

		// Produce absolute delta values
		final int dRed = Math.abs(aRed - bRed);
		final int dGreen = Math.abs(aGreen - bGreen);
		final int dBlue = Math.abs(aBlue - bBlue);

		return dRed + dGreen + dBlue;
	}
}
