package be.limero.programmer.ui;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;


public class LogHandler extends java.util.logging.Handler {
	
	public interface LogLine {
		void log(String line);
	};
	LogLine _logLine=null;

	DateFormat dateFormat;

	void addHandler() {
		LogManager.getLogManager().getLogger("").addHandler(this);

	}

	public LogHandler() {
		dateFormat = new SimpleDateFormat("HH:mm:ss");
	}

	public void register(LogLine logLine) {
		_logLine = logLine;
		addHandler();
	}

	@Override
	public void publish(final LogRecord record) {
		StringWriter text = new StringWriter();
		PrintWriter out = new PrintWriter(text);
		Date currentDate = new Date(record.getMillis());
		out.printf("%8s %4.4s %20.20s.%10.10s | %s", dateFormat.format(currentDate), record.getLevel(),
				record.getSourceClassName(), record.getSourceMethodName(), record.getMessage());
		if (_logLine != null)
			_logLine.log(text.toString());
	}

	@Override
	public void flush() {

	}

	@Override
	public void close() throws SecurityException {

	}

}