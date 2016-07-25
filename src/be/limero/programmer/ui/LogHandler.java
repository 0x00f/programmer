package be.limero.programmer.ui;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import org.fusesource.mqtt.client.Callback;

public class LogHandler extends java.util.logging.Handler {

	Callback<String> cb;
	DateFormat dateFormat;

	void addHandler() {
		LogManager.getLogManager().getLogger("").addHandler(this);

	}

	public LogHandler() {
		cb = null;
		dateFormat = new SimpleDateFormat("HH:mm:ss");

	}

	public void register(Callback<String> cb) {
		this.cb = cb;
		addHandler();
	}

	@Override
	public void publish(final LogRecord record) {
		StringWriter text = new StringWriter();
		PrintWriter out = new PrintWriter(text);
		Date currentDate = new Date(record.getMillis());
		out.printf("%8s %4.4s %20.20s.%10.10s | %s", dateFormat.format(currentDate), record.getLevel(),
				record.getSourceClassName(), record.getSourceMethodName(), record.getMessage());
		if (cb != null)
			cb.onSuccess(text.toString());
	}

	@Override
	public void flush() {

	}

	@Override
	public void close() throws SecurityException {

	}

	// ...
}