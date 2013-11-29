package com.bobandthomas.Morbid.molecule.data;

import javax.swing.JPanel;

import com.bobandthomas.Morbid.molecule.Atom;
import com.bobandthomas.Morbid.molecule.Molecule;
import com.bobandthomas.Morbid.molecule.data.control.SpatialDataControl;
import com.bobandthomas.Morbid.utils.Point3D;

public class SpatialDataCharge extends SpatialData {

	static class SpatialDataType
	{
		static public  SpatialData create(Molecule m)
		{
			return new SpatialDataCharge(m);
		}
	}
	public SpatialDataCharge(Molecule m)
	{
		super(m);
		setName("Charge");
	}
	@Override
	public double Calculate(Point3D pos) {
		double value = 0.0;
		for ( Atom a: molecule.Atoms())
		{

			double distanceSqr = pos.distanceSquared(a.pos);
			value += a.getCharge() /(0.1 + distanceSqr);
				
				
		}
		return value;	
	}
	@Override
	public SpatialDataControl getControlPanel(JPanel parentPanel) {
		return new SpatialDataControl(this, getName(), parentPanel);
	}

}
