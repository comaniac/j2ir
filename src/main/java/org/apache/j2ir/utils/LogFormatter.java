package org.apache.j2ir.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter {
	private static final DateFormat df = new SimpleDateFormat(
			"dd/MM/yy HH:mm:ss.SSS");

	public String format(LogRecord record) {
		StringBuilder builder = new StringBuilder(1024);
		builder.append(df.format(new Date(record.getMillis()))).append(" ");
		builder.append("[").append(record.getLevel()).append("]");
		String className = record.getSourceClassName();
		if (className.lastIndexOf(".") != -1)
			className = className.substring(className.lastIndexOf(".") + 1);
		builder.append("[").append(className).append(".");
		builder.append(record.getSourceMethodName()).append("] ");
		builder.append(formatMessage(record));
		builder.append("\n");

		return builder.toString();
	}
}