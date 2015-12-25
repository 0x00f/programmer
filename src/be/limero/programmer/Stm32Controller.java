package be.limero.programmer;

import be.limero.programmer.commands.Stm32Go;
import be.limero.programmer.ui.Stm32Programmer;

public class Stm32Controller {
	// start Mqtt exchange thread
	Stm32Model stm32;
	Stm32Programmer ui;

	public Stm32Controller(Stm32Programmer ui) {
		stm32 = new Stm32Model();
		ui.updateView();
	}
	
	public void loadFile() {
		
	}
	
	public void program() {
		
	}
	
	public void reset(){
		
	}
	
	public void verify(){
		
	}
	
	public void go(){
		new Stm32Go(0x4000000);
	}
}
