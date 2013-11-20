package com.peterphi.ffmpegtest.ffmpeg;

import com.peterphi.ffmpegtest.plugins.VideoAnalysisPlugin;
import com.peterphi.std.util.HexHelper;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.common.bytesource.ByteSourceInputStream;
import org.apache.commons.imaging.formats.pnm.PnmImageParser;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FFmpegEngine
{
	private final File ffmpeg;
	private int limitSeconds = 0;


	public FFmpegEngine(final File ffmpeg)
	{
		this.ffmpeg = ffmpeg;
	}


	/**
	 * Set the number of seconds to limit video processing to; 0 for no limit
	 *
	 * @param limit
	 */
	public void setLimitSeconds(int limit)
	{
		this.limitSeconds = limit;
	}


	public void analyse(final File file, final VideoAnalysisPlugin plugin)
	{
		DigestInputStream dis = null;
		try
		{
			final Process process = spawn(file);

			eatStream(process.getErrorStream());

			final InputStream is = process.getInputStream();
			analyse(is, plugin);
		}
		catch (ImageReadException e)
		{
			throw new IOError(e);
		}
		catch (IOException e)
		{
			throw new IOError(e);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		finally
		{
			if (dis != null)
			{
				System.out.println("SHA1 Digest of read data: " + HexHelper.toHex(dis.getMessageDigest().digest()));
			}
		}
	}


	private static final PnmImageParser PPM_PARSER = new PnmImageParser();


	private BufferedImage parse(InputStream is) throws IOException, ImageReadException
	{
		// peek to see if we're at EOF
		// Otherwise we'll get an exception from getBufferedImage
		is.mark(1);

		final int read = is.read();
		if (read == -1)
		{
			return null; // at EOF
		}
		else
		{
			is.reset();

			return PPM_PARSER.getBufferedImage(new ByteSourceInputStream(is, null), new HashMap<String, Object>());
		}
	}


	protected void analyse(final InputStream is, final VideoAnalysisPlugin plugin) throws IOException, ImageReadException
	{
		if (!is.markSupported())
			throw new IllegalArgumentException("PPM Input Stream MUST support mark!");

		plugin.start();

		for (int frame = 0; ; frame++)
		{
			final BufferedImage img = parse(is);

			if (img != null)
				plugin.frame(frame, img);
			else
				break;
		}

		plugin.end();
	}


	/**
	 * Spawn a thread to consume the contents of a stream and discard them
	 *
	 * @param is
	 */
	private void eatStream(final InputStream is)
	{
		Thread thread = new Thread(new Runnable()
		{

			@Override
			public void run()
			{
				try
				{
					final byte[] data = new byte[1024];
					while (true)
					{
						final int read = is.read(data);

						// Die on EOF
						if (read == -1)
							break;
					}
				}
				catch (IOException e)
				{
					throw new IOError(e);
				}
			}
		});
		thread.setDaemon(true);
		thread.start();
	}


	private Process spawn(final File file) throws IOException
	{
		final ProcessBuilder pb = new ProcessBuilder(getFfmpegCommand(file));

		return pb.start();
	}


	private List<String> getFfmpegCommand(final File file)
	{
		List<String> cmd = new ArrayList<>();

		cmd.add(ffmpeg.getAbsolutePath());
		if (limitSeconds > 0)
		{
			cmd.add("-t");
			cmd.add(Integer.toString(limitSeconds));
		}
		cmd.add("-i");
		cmd.add(file.getAbsolutePath());
		cmd.add("-f");
		cmd.add("image2pipe");
		cmd.add("-vcodec");
		cmd.add("ppm");
		cmd.add("-");

		return cmd;
	}
}
