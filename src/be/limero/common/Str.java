package be.limero.common;

public class Str extends Bytes {

	public Str(int size) {
		super(size);
	}

	public Str(String s) {
		super(s.length());
		for (int i = 0; i < s.length(); i++)
			write(s.charAt(i));
	}

}
