package com.bobandthomas.Morbid.UI;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;

public abstract class ControlPanel extends ControlPanelBase  {

	/**
	 * 
	 */
	private static final long serialVersionUID = -430660293743502924L;
	JToggleButton banner;

	public ControlPanel(String name) {
		super(name);
		setMaximumSize(new Dimension(200, 500));
		setMinimumSize(new Dimension(200, 40));
		banner = new JToggleButton(name);
		banner.setMaximumSize(new Dimension(200,10));
		banner.setMinimumSize(new Dimension(200,10));
		banner.addActionListener(this);
		add(banner, BorderLayout.NORTH);
		
		child = new JPanel(new GridLayout(0,1,0,0));
		child.setVisible(banner.isSelected());
		add(child, BorderLayout.CENTER);
		createLabel(name);
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == banner)
		{
			child.setVisible(banner.isSelected());
			return;
		}
		super.actionPerformed(e);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		super.stateChanged(e);

	}


}
