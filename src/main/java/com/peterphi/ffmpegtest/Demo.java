package com.peterphi.ffmpegtest;

import com.googlecode.javacpp.Loader;
import com.googlecode.javacv.CanvasFrame;
import com.googlecode.javacv.FFmpegFrameGrabber;
import com.googlecode.javacv.FFmpegFrameRecorder;
import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.FrameRecorder;
import com.googlecode.javacv.cpp.opencv_core;
import com.googlecode.javacv.cpp.opencv_imgproc;
import com.googlecode.javacv.cpp.opencv_objdetect;

import java.io.File;
import java.net.URL;

import static com.googlecode.javacv.cpp.opencv_core.CV_AA;
import static com.googlecode.javacv.cpp.opencv_core.CV_FONT_HERSHEY_PLAIN;
import static com.googlecode.javacv.cpp.opencv_core.CvContour;
import static com.googlecode.javacv.cpp.opencv_core.CvMemStorage;
import static com.googlecode.javacv.cpp.opencv_core.CvPoint;
import static com.googlecode.javacv.cpp.opencv_core.CvRect;
import static com.googlecode.javacv.cpp.opencv_core.CvScalar;
import static com.googlecode.javacv.cpp.opencv_core.CvSeq;
import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;
import static com.googlecode.javacv.cpp.opencv_core.IplImage;
import static com.googlecode.javacv.cpp.opencv_core.cvClearMemStorage;
import static com.googlecode.javacv.cpp.opencv_core.cvDrawContours;
import static com.googlecode.javacv.cpp.opencv_core.cvFillConvexPoly;
import static com.googlecode.javacv.cpp.opencv_core.cvGetSeqElem;
import static com.googlecode.javacv.cpp.opencv_core.cvLoad;
import static com.googlecode.javacv.cpp.opencv_core.cvPoint;
import static com.googlecode.javacv.cpp.opencv_core.cvPutText;
import static com.googlecode.javacv.cpp.opencv_core.cvRectangle;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_BGR2GRAY;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_CHAIN_APPROX_SIMPLE;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_POLY_APPROX_DP;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_THRESH_BINARY;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvApproxPoly;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCanny;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvContourPerimeter;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvFindContours;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvThreshold;
import static com.googlecode.javacv.cpp.opencv_objdetect.CV_HAAR_DO_CANNY_PRUNING;
import static com.googlecode.javacv.cpp.opencv_objdetect.CvHaarClassifierCascade;
import static com.googlecode.javacv.cpp.opencv_objdetect.cvHaarDetectObjects;

public class Demo
{

	// Objects allocated with a create*() or clone() factory method are automatically released
	// by the garbage collector, but may still be explicitly released by calling release().
	// You shall NOT call cvReleaseImage(), cvReleaseMemStorage(), etc. on objects allocated this way.
	static CvMemStorage storage = CvMemStorage.create();


	public static void main(String[] args) throws Exception
	{
		String classifierName = null;
		if (args.length > 0)
		{
			classifierName = args[0];
		}
		else
		{
			URL url = new URL("https://raw.github.com/Itseez/opencv/master/data/haarcascades/haarcascade_frontalface_alt.xml");

			File file = Loader.extractResource(url, null, "classifier", ".xml");
			file.deleteOnExit();
			classifierName = file.getAbsolutePath();
		}

		// Preload the opencv_objdetect module to work around a known bug.
		Loader.load(opencv_objdetect.class);

		// We can "cast" Pointer objects by instantiating a new object of the desired class.
		CvHaarClassifierCascade classifier = new CvHaarClassifierCascade(cvLoad(classifierName));
		if (classifier.isNull())
		{
			System.err.println("Error loading classifier file \"" + classifierName + "\".");
			System.exit(1);
		}

		// The available FrameGrabber classes include OpenCVFrameGrabber (opencv_highgui),
		// DC1394FrameGrabber, FlyCaptureFrameGrabber, OpenKinectFrameGrabber,
		// PS3EyeFrameGrabber, VideoInputFrameGrabber, and FFmpegFrameGrabber.
		FrameGrabber grabber;
		{
			grabber = new FFmpegFrameGrabber("test.mp4");
			grabber.start();
		}

		// FAQ about IplImage:
		// - For custom raw processing of data, getByteBuffer() returns an NIO direct
		//   buffer wrapped around the memory pointed by imageData, and under Android we can
		//   also use that Buffer with Bitmap.copyPixelsFromBuffer() and copyPixelsToBuffer().
		// - To get a BufferedImage from an IplImage, we may call getBufferedImage().
		// - The createFrom() factory method can construct an IplImage from a BufferedImage.
		// - There are also a few copy*() methods for BufferedImage<->IplImage data transfers.
		IplImage grabbedImage = grabber.grab();
		int width = grabbedImage.width();
		int height = grabbedImage.height();
		IplImage grayImage = IplImage.create(width, height, IPL_DEPTH_8U, 1);


		// The OpenCVFrameRecorder class simply uses the CvVideoWriter of opencv_highgui,
		// but FFmpegFrameRecorder also exists as a more versatile alternative.
		final FrameRecorder recorder;

		if (true)
		{
			recorder = new FFmpegFrameRecorder("output.mp4", width, height, 0);
			recorder.start();
		}

		UnbufferedCanvasFrame frame = new UnbufferedCanvasFrame("Some Title", CanvasFrame.getDefaultGamma() / grabber.getGamma());

		// We can allocate native arrays using constructors taking an integer as argument.
		CvPoint hatPoints = new CvPoint(3);

		// Font for frame counter
		opencv_core.CvFont font = new opencv_core.CvFont(CV_FONT_HERSHEY_PLAIN, 1, 1);


		int framecount = 0;
		while (frame.isVisible() && (grabbedImage = grabber.grab()) != null)
		{
			framecount++;
			cvClearMemStorage(storage);

			// Create a grayscale version of the image
			cvCvtColor(grabbedImage, grayImage, CV_BGR2GRAY);

			if (false)
				drawBoxesOnFaces(classifier, grabbedImage, grayImage, hatPoints);

			// Run edge detection
			grabbedImage = edges(grayImage, grayImage);

			// Log the frame
			cvPutText(grabbedImage, "Frame " + framecount, new CvPoint(0,50), font, CvScalar.WHITE);

			frame.showImage(grabbedImage);

			if (recorder != null)
				recorder.record(grabbedImage);
		}
		frame.dispose();
		if (recorder != null)
			recorder.stop();
		grabber.stop();
	}


	private static void drawBoxesOnFaces(final CvHaarClassifierCascade classifier,
	                                     final IplImage grabbedImage,
	                                     final IplImage grayImage,
	                                     final CvPoint hatPoints)
	{
		CvSeq faces = cvHaarDetectObjects(grayImage, classifier, storage, 1.1, 10, CV_HAAR_DO_CANNY_PRUNING);
		int total = faces.total();
		for (int i = 0; i < total; i++)
		{
			CvRect r = new CvRect(cvGetSeqElem(faces, i));

			int x = r.x(), y = r.y(), w = r.width(), h = r.height();
			cvRectangle(grabbedImage, cvPoint(x, y), cvPoint(x + w, y + h), CvScalar.RED, 1, CV_AA, 0);

			if (true)
			{
				// To access or pass as argument the elements of a native array, call position() before.
				hatPoints.position(0).x(x - w / 10).y(y - h / 10);
				hatPoints.position(1).x(x + w * 11 / 10).y(y - h / 10);
				hatPoints.position(2).x(x + w / 2).y(y - h / 2);
				cvFillConvexPoly(grabbedImage, hatPoints.position(0), 3, CvScalar.GREEN, CV_AA, 0);
			}
		}
	}


	public static IplImage edges(IplImage gray, IplImage src)
	{
		int N = 7;
		int aperature_size = N;
		double lowThresh = 10; // 10:130 and 20:130 quite good for mostly black outline
		double highThresh = 130;
		cvCanny(gray, gray, lowThresh * N * N, highThresh * N * N, aperature_size);

		return gray;
	}


	public static IplImage detectObjects(IplImage img, final IplImage src)
	{
		CvSeq contours = new CvSeq();
		CvSeq ptr = new CvSeq();

		cvThreshold(img, img, 100, 200, CV_THRESH_BINARY);
		cvFindContours(img,
		               storage,
		               contours,
		               Loader.sizeof(CvContour.class),
		               opencv_imgproc.CV_RETR_LIST,
		               CV_CHAIN_APPROX_SIMPLE,
		               cvPoint(0, 0));

		CvRect boundbox;

		for (ptr = contours; ptr != null; ptr = ptr.h_next())
		{
			CvSeq points = cvApproxPoly(ptr,
			                            Loader.sizeof(CvContour.class),
			                            storage,
			                            CV_POLY_APPROX_DP,
			                            cvContourPerimeter(ptr) * 0.02,
			                            0);


			cvDrawContours(src, points, CvScalar.WHITE, CvScalar.WHITE, -1, 1, opencv_core.CV_AA);
		}

		return src;
	}
}
