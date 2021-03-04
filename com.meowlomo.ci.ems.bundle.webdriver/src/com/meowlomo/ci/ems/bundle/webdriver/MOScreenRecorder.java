package com.meowlomo.ci.ems.bundle.webdriver;

import static org.monte.media.FormatKeys.EncodingKey;
import static org.monte.media.FormatKeys.FrameRateKey;
import static org.monte.media.FormatKeys.KeyFrameIntervalKey;
import static org.monte.media.FormatKeys.MIME_QUICKTIME;
import static org.monte.media.FormatKeys.MediaTypeKey;
import static org.monte.media.FormatKeys.MimeTypeKey;
import static org.monte.media.VideoFormatKeys.CompressorNameKey;
import static org.monte.media.VideoFormatKeys.DepthKey;
import static org.monte.media.VideoFormatKeys.ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE;
import static org.monte.media.VideoFormatKeys.QualityKey;

import java.awt.AWTException;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.monte.media.Format;
import org.monte.media.FormatKeys.MediaType;
import org.monte.media.Registry;
import org.monte.media.math.Rational;
import org.monte.screenrecorder.ScreenRecorder;

import com.meowlomo.ci.ems.bundle.utils.SGLogger;

import it.sauronsoftware.jave.AudioAttributes;
import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.EncodingAttributes;
import it.sauronsoftware.jave.VideoAttributes;

public class MOScreenRecorder extends ScreenRecorder {
	// for video
	private static GraphicsConfiguration gc;

	public MOScreenRecorder(GraphicsConfiguration cfg, Format fileFormat, Format screenFormat, Format mouseFormat,
			Format audioFormat) throws IOException, AWTException {
		super(cfg, fileFormat, screenFormat, mouseFormat, audioFormat);
	}

	public static MOScreenRecorder screenRecorderFormat(String videoDir) throws IOException, AWTException {
		// Create a instance of GraphicsConfiguration to get the Graphics
		// configuration of the Screen. This is needed for ScreenRecorder class.

//		String awt = System.getProperty("java.awt.graphicsenv");
//		System.out.println("java.awt.graphicsenv in OSGi:" + awt);
		System.setProperty("java.awt.headless", "false");

		gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
		// Create a instance of ScreenRecorder with the required configurations
		MOScreenRecorder sru = new MOScreenRecorder(gc,
				new Format(MediaTypeKey, MediaType.FILE, MimeTypeKey, MIME_QUICKTIME),
				new Format(MediaTypeKey, MediaType.VIDEO, EncodingKey, ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE,
						CompressorNameKey, ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE, DepthKey, 24, FrameRateKey,
						Rational.valueOf(15), QualityKey, 1.0f, KeyFrameIntervalKey, 15 * 60),
				new Format(MediaTypeKey, MediaType.VIDEO, EncodingKey, "black", FrameRateKey, Rational.valueOf(30)),
				null);

		return sru;
	}

	@Override
	public File createMovieFile(Format fileFormat) throws IOException {
		if (!movieFolder.exists()) {
			movieFolder.mkdirs();
		} else if (!movieFolder.isDirectory()) {
			throw new IOException("\"" + movieFolder + "\" is not a directory.");
		}
		File f = new File(movieFolder, "ScreenRecorderTemp." + Registry.getInstance().getExtension(fileFormat));
		return f;
	}

	public String getScreenRecorderPath() {
		String screenRecorderPath = "";
		List<File> createdMovieFiles = getCreatedMovieFiles();
		for (File movie : createdMovieFiles) {
			screenRecorderPath = movie.getAbsolutePath();
		}
		return screenRecorderPath;
	}

	public File formatToMP4(String sourcePath, String logPath) {
		try {
			File source = new File(sourcePath);
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd 'at' HH.mm.ss");
			String movieName = logPath + "video/" + dateFormat.format(new Date()) + ".mp4";
			File target = new File(movieName);
			AudioAttributes audio = new AudioAttributes();
			audio.setCodec("libvorbis");
			VideoAttributes video = new VideoAttributes();
			video.setCodec("mpeg4");
			video.setTag("DIVX");
			video.setBitRate(new Integer(160000));
			video.setFrameRate(new Integer(30));
			EncodingAttributes attrs = new EncodingAttributes();
			attrs.setFormat("mpegvideo");
			attrs.setAudioAttributes(audio);
			attrs.setVideoAttributes(video);
			Encoder encoder = new Encoder();
			encoder.encode(source, target, attrs);
			return target;

		} catch (Exception e) {
			SGLogger.errorTitle(" [      错误      ] ", "视频格式化成mp4发生异常." + e.getMessage());
		}
		return null;
	}

}
