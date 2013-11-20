package com.peterphi.ffmpegtest;

import com.peterphi.ffmpegtest.ffmpeg.FFmpegEngine;
import com.peterphi.ffmpegtest.plugins.SADCutScorePlugin;

import java.io.File;

public class Main
{
	public static void main(String[] args) throws Exception
	{
		final FFmpegEngine ffmpeg = new FFmpegEngine(new File("/opt/local/bin/ffmpeg"));

		// Only process 1 second
		ffmpeg.setLimitSeconds(5);

		// Write the analysis to sad.csv
		SADCutScorePlugin plugin = new SADCutScorePlugin(new File("sad.csv"));

		final long start = System.currentTimeMillis();
		ffmpeg.analyse(new File("test.mp4"), plugin);
		final long timeTaken = System.currentTimeMillis() - start;

		System.out.println("Processing took: " + timeTaken + " ms");
	}
}
