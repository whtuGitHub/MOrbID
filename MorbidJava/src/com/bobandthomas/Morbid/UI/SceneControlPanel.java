package com.bobandthomas.Morbid.UI;

import com.bobandthomas.Morbid.Gadget.GadgetFactoryManager;
import com.bobandthomas.Morbid.Gadget.Scene;

public class SceneControlPanel extends ControlPanel {

	Scene scene;
	ControlPanelSideBar bar;
	public SceneControlPanel(Scene sc, String name, ControlPanelSideBar bar) {
		super(name);
		scene = sc;
		this.bar = bar;
		createButton("Add Gadget");
		createLabel("________________");
		banner.setSelected(true);
		child.setVisible(banner.isSelected());

	}

	@Override
	public void changeValue(String label, Integer value) {
		// TODO Auto-generated method stub
		
		if (label.equals("Zoom"))
			scene.setZoom(value/100.0);
		if (label.equals("Add Gadget"))
			GadgetFactoryManager.getOne().selectNewGadget(scene, bar);
			

	}

}
