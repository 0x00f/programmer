package be.limero.programmer;

import akka.actor.UntypedActor;
import be.limero.util.Cbor;

public class WriteMemory extends UntypedActor {

	@Override
	public void onReceive(Object object) throws Exception {
		if ( object instanceof Cbor ) {
			
		}

	}
}