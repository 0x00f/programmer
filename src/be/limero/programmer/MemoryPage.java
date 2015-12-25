package be.limero.programmer;

public class MemoryPage {
	int offset;
	int length;

	enum Status {
		NEW, PROGRAMMED
	};

	Status status;
}
