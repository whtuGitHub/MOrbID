package com.bobandthomas.Morbid.Gadget;

import com.bobandthomas.Morbid.graphics.CylinderGob;
import com.bobandthomas.Morbid.graphics.GobList;
import com.bobandthomas.Morbid.graphics.GobVector;
import com.bobandthomas.Morbid.graphics.StringGob;
import com.bobandthomas.Morbid.molecule.Atom;
import com.bobandthomas.Morbid.molecule.AtomType;
import com.bobandthomas.Morbid.molecule.Bond;
import com.bobandthomas.Morbid.molecule.Molecule;
import com.bobandthomas.Morbid.utils.Point3D;

public class BondGadget extends Gadget {

	@Override
	public String getGadgetType() {
		return "Bond Gadget";
	}

	public enum BondColor {
		MonoChrome, HalfColor, ColorBondOrder, ChargeGradient;
	    private static BondColor[] values = null;
	    public static BondColor fromInt(int i) {
	        if(BondColor.values == null) {
	        	BondColor.values = BondColor.values();
	        }
	        return BondColor.values[i];
	    }
};

	public enum BondRep {
		Lines, Cylinder, Frames;
	    private static BondRep[] values = null;
	    public static BondRep fromInt(int i) {
	        if(BondRep.values == null) {
	        	BondRep.values = BondRep.values();
	        }
	        return BondRep.values[i];
	    }
};

	double bondScale;
	BondColor colorBy;
	BondRep rep;
	boolean labelBO;
	boolean labelDistance;

	int iBondScale;
	
	public double getBondScale() {
		return bondScale;
	}

	public void setBondScale(double d) {
		this.bondScale = d;
		markDirty();
	}

	public BondColor getColorBy() {
		return colorBy;
	}

	public void setColorBy(BondColor colorBy) {
		this.colorBy = colorBy;
		markDirty();
	}

	public BondRep getRep() {
		return rep;
	}

	public void setRep(BondRep rep) {
		this.rep = rep;
		markDirty();
	}

	public boolean isLabelBO() {
		return labelBO;
	}

	public void setLabelBO(boolean labelBO) {
		this.labelBO = labelBO;
		markDirty();
	}

	public boolean isLabelDistance() {
		return labelDistance;
	}

	public void setLabelDistance(boolean labelDistance) {
		this.labelDistance = labelDistance;
	}



	public int getIBondScale() {
		return (int) bondScale * 100;
	}

	public void setIBondScale(int v) {
		bondScale = (v / 100.0f);
	}

	public BondGadget() {
		super();
		bondScale = 0.05f;
		rep = BondRep.Cylinder;
		colorBy = BondColor.HalfColor;
		labelBO = false;
		labelDistance = false;
	}

	@Override
	void Draw(GobList gobList) {
		int i;

		Molecule mol = GetMolecule();
		if (mol == null)
			return;
		if (!isDirty()) return;
		gobList.clear();

			double atomScale = 0.0f;
			AtomGadget pGadget;
			GadgetList gadgetlist = (GadgetList) parentSet;
			boolean showHydrogens = true;
			boolean showLonePairs = true;

			for (Gadget g : gadgetlist) {
				if (g.getGadgetType().equals("Atom") && g.isVisible()) {
					pGadget = (AtomGadget) g;
					if (pGadget.AtomScale > atomScale)
						atomScale = pGadget.AtomScale;
					if (pGadget.ShowHydrogens == false)
						showHydrogens = false;
					if (pGadget.ShowLonePairs == false)
						showLonePairs = false;
				}
			}

			atomScale *= 0.5;

			for (i = 0; i < mol.NumBonds(); i++) {
				double ri, rj, length;
				int bondOrder;
				Bond bond = mol.GetBond(i);
				Atom a1 = bond.atom(0);
				Atom a2 = bond.atom(1);

				if (a1.getAtomicNumber() > 254 || a2.getAtomicNumber() > 254)
					continue;
				if (!showHydrogens
						&& (a1.getAtomicNumber() == 1 || a2.getAtomicNumber() == 1))
					continue;
				if (!showLonePairs
						&& (a1.getAtomicNumber() == 0 || a2.getAtomicNumber() == 0))
					continue;
				bondOrder = bond.getNominalBondOrder();
				Point3D ai = new Point3D(a1.Position());
				Point3D aj = new Point3D(a2.Position());
				Point3D bondVector = ai.Sub(aj);
				length = (float) bondVector.Length();
				ri = a1.Radius();
				rj = a2.Radius();
				if ((length - ((ri + rj) * atomScale)) > 0) {
					ai = ai.Sub(bondVector.Scale(ri * atomScale));
					aj = aj.Add(bondVector.Scale(rj * atomScale));
				} else
					continue;
				Point3D midPoint = (ai.Add(aj)).Scale(0.5);
				if (colorBy == BondColor.HalfColor) {
					AtomType ati = a1.getAtomType();
					AtomType atj = a2.getAtomType();
					if (rep == BondRep.Cylinder) {
						CylinderGob cg = new CylinderGob(ai, midPoint,
								(float) bondScale);
						cg.Color = ati.color;
						cg.setMaterial(ati.mat);
						if (bondOrder == 2)
							cg.SetRadius(cg.getRadius() * 2);
						gobList.add(cg);

						cg = new CylinderGob(aj, midPoint, (float)bondScale);
						cg.Color = atj.color;
						cg.setMaterial(atj.mat);
						if (bondOrder == 2)
							cg.SetRadius(cg.getRadius() * 2);
						gobList.add(cg);

					} else {
						GobVector vg = new GobVector(ai, midPoint);
						vg.Color = ati.color;
						gobList.add(vg);

						vg = new GobVector(aj, midPoint);
						vg.Color = atj.color;
						gobList.add(vg);
					}
				} else {
					if (rep == BondRep.Cylinder) {
						CylinderGob cg = new CylinderGob(ai, aj, (float) bondScale);
						if (bondOrder == 2)
							cg.SetRadius(cg.getRadius() * 2);
						cg.Color = baseColor;
						gobList.add(cg);
					} else {
						GobVector vg = new GobVector(ai, aj);
						vg.Color = baseColor;
						gobList.add(vg);
					}
				}
				if (labelBO || labelDistance) {
					String str = "";
					String s = "";
					if (labelBO) {

						s = String.valueOf(bond.getBondOrder());
						str += s;
					}
					if (labelDistance) {
						s = String.valueOf(bondVector.Length());
						if (str.length() > 0)
							str += ", ";
						str += s;
					}
					StringGob sg = new StringGob(str, midPoint);
					sg.Color = baseColor;
					gobList.add(sg);
				}
			}
		markClean();
	}

}