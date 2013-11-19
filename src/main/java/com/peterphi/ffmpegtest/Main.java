package com.peterphi.ffmpegtest;

import com.peterphi.std.system.exec.Exec;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.lang.StringUtils;

import java.awt.image.BufferedImage;
import java.io.IOError;
import java.io.InputStream;

public class Main
{
	public static void main(String[] args) throws Exception
	{
		final InputStream is;

		final String fileName = "test.mp4";

		Exec exec = new Exec("/opt/local/bin/ffmpeg", "-t", "5", "-i", fileName, "-f", "image2pipe", "-vcodec", "ppm", "-");
		exec.setRedirectError(false);

		System.out.println("Spawn: " + StringUtils.join(exec.getProcessBuilder().command(), ' '));

		final Process process = exec.getProcessBuilder().start();

		Thread thread = new Thread(new Runnable()
		{

			@Override
			public void run()
			{
				try
				{
					final InputStream is = process.getErrorStream();

					final byte[] data = new byte[1024];
					while (true)
					{
						final int read = is.read(data);

						if (read == 0)
							Thread.sleep(250);
						else if (read != -1)
						{
							// Suppress ffmpeg output
							//System.err.write(data, 0, read);
						}
						else
							break; // die when we hit EOF
					}
				}
				catch (Exception e)
				{
					throw new IOError(e);
				}
			}
		});
		thread.setDaemon(true);
		thread.start();

		is = process.getInputStream();

		process(is);
	}


	private static void process(final InputStream is) throws Exception
	{
		int frame = 1;
		BufferedImage img;
		BufferedImage last = null;

		System.out.println("frame,score,computeTime");
		while ((img = Imaging.getBufferedImage(is)) != null)
		{
			final long start = System.currentTimeMillis();
			final long score = score(img, last);
			final long duration = System.currentTimeMillis() - start;

			System.out.println(frame + "," + score + "," + duration);

			last = img;
			frame++;
		}

		System.out.println("EOF reached");
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
	private static long score(final BufferedImage a, final BufferedImage b)
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
	private static int score(int a, int b)
	{
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
