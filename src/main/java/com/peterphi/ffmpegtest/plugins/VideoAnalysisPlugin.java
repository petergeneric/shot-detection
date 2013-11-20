package com.peterphi.ffmpegtest.plugins;

import java.awt.image.BufferedImage;

public interface VideoAnalysisPlugin
{
	public void start();

	/**
	 * Designed to handle a video frame in some fashion
	 *
	 * @param frame
	 * 		the frame count (starting at 0)
	 * @param image
	 * 		the frame image
	 */
	public void frame(int frame, BufferedImage image);

	public void end();
}
