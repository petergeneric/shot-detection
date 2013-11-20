package com.peterphi.ffmpegtest.plugins;

import com.peterphi.ffmpegtest.parser.VideoFrame;

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
	private VideoFrame last;
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
	public void frame(final int frame, final VideoFrame image)
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
	private final long score(final VideoFrame a, final VideoFrame b)
	{
		if (a == null || b == null)
			return Long.MAX_VALUE;

		long score = 0;

		final short[] aPixel = new short[3];
		final short[] bPixel = new short[3];

		for (int x = 0; x < a.getHeight(); x++)
		{
			for (int y = 0; y < a.getWidth(); y++)
			{
				a.getRGB(aPixel, x, y);
				b.getRGB(bPixel, x, y);

				final int delta = score(aPixel, bPixel);

				score += delta;
			}
		}
		return score;
	}


	private final int score(final short[] a, final short[] b)
	{
		return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]) + Math.abs(a[2] - b[2]);
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
