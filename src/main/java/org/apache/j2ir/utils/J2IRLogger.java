package org.apache.j2ir.utils;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class J2IRLogger {
	public final Logger logger;

	public J2IRLogger(String levelStr) {
		try {
			final Level level = Level.parse(levelStr);
			logger = Logger.getLogger("J2IR");

			for (Handler h : logger.getHandlers())
				logger.removeHandler(h);

			logger.setLevel(level);
			logger.setUseParentHandlers(false);
			LogFormatter formatter = new LogFormatter();
			ConsoleHandler handler = new ConsoleHandler();
			handler.setLevel(level);
			handler.setFormatter(formatter);
			logger.addHandler(handler);

		} catch (final Exception e) {
			throw new RuntimeException("Exception " + e + " in logging setup");
		}
	}

	public J2IRLogger() {
		this("FINE");
	}
}