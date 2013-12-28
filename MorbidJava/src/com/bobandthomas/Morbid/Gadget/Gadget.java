package com.bobandthomas.Morbid.Gadget;

import java.util.ArrayList;

import com.bobandthomas.Morbid.Gadget.Scene.LayerPosition;
import com.bobandthomas.Morbid.graphics.GobList;
import com.bobandthomas.Morbid.graphics.Material;
import com.bobandthomas.Morbid.graphics.SphereGob;
import com.bobandthomas.Morbid.molecule.Atom;
import com.bobandthomas.Morbid.molecule.AtomType;
import com.bobandthomas.Morbid.molecule.Molecule;
import com.bobandthomas.Morbid.molecule.SubstructureRepList;
import com.bobandthomas.Morbid.molecule.SubstructureSet;
import com.bobandthomas.Morbid.utils.*;


/**
 * The Class Gadget.
 * A gadget is a visual representation of a physical or mathematical object. 
 * Gadget transforms some data from the Molecule or the Scene into a GobList.
 * 
 * @author Thomas Kreek 
 */
public abstract class Gadget extends CLoadableItem implements IChangeNotifier {

	/**
	 * The Enum ColorBy, a list of the patterns for coloring, available to gadget subclasses
	 * 
	 * @author Thomas Kreek 
	 * The Enum ColorBy.
	 */
	public enum ColorBy {
		
		TYPE(0, "Atom Type"), 
 CHARGE(1, "Charge"), 
 MONOCHROME(2, "Monochrome"), 
 SUBSTRUCTURE(
				3, "Substructure"), 
 SPATIALDATA(4, "Spatial Data");
		
		int id;
		
		String name;

		/**
		 * Instantiates a new color by.
		 * 
		 * @param i
		 *            the i
		 * @param n
		 *            the n
		 */
		ColorBy(int i, String n) {
			id = i;
			name = n;
		}

		/** The values. */
		private static ColorBy[] values = null;

		/**
		 * From int.
		 * 
		 * @param i
		 *            the i
		 * @return the color by
		 */
		public static ColorBy fromInt(int i) {
			if (ColorBy.values == null) {
				ColorBy.values = ColorBy.values();
			}
			return ColorBy.values[i];
		}
	};

	/**
	 * The Class ColorOption describes the mechanism for determining the current color of 
	 * a gadget component.
	 * This can be by spatial data, or a particular substructure rep list 
	 * 
	 * @author Thomas Kreek The Class ColorOption.
	 */
	public class ColorOption {
		
		/** The color by. */
		ColorBy colorBy;
		
		/** The name. */
		String name;
		
		/** The substructure. */
		SubstructureSet substructure;
		
		/** The reps. */
		SubstructureRepList reps;

		/**
		 * Instantiates a new color option.
		 * 
		 * @param cb
		 *            the cb
		 */
		ColorOption(ColorBy cb) {
			colorBy = cb;
			name = cb.name;
			substructure = null;
		}

		/**
		 * Instantiates a new color option.
		 * 
		 * @param s
		 *            the s
		 */
		ColorOption(SubstructureSet s) {
			colorBy = ColorBy.SUBSTRUCTURE;
			substructure = s;
			name = s.getName();
			reps = s.getDefaultRep();
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return name;
		}
	}

	/** The color option list. */
	ArrayList<ColorOption> colorOptionList = null;

	/**
	 * Gets the color options list for enumeration
	 * 
	 * @param reset
	 *            the reset
	 * @return the color options
	 */
	public ArrayList<ColorOption> getColorOptions(boolean reset) {
		if (colorOptionList != null && !reset)
			return colorOptionList;
		colorOptionList = new ArrayList<ColorOption>();
		for (ColorBy cb : ColorBy.values()) {
			switch (cb) {
			case SUBSTRUCTURE:
				for (SubstructureSet s : molecule.getSubstructures()) {
					ColorOption co = new ColorOption(s);
					colorOptionList.add(co);
				}
				break;
			default:
				colorOptionList.add(new ColorOption(cb));
				break;

			}
		}

		return colorOptionList;
	}

	/**
	 * Sets the current color option.
	 * 
	 * @param index
	 *            the new current color option
	 */
	public void setCurrentColorOption(int index) {
		currentColorOption = colorOptionList.get(index);
		markDirty(new MorbidEvent(this, "currentColorOption"));
	}

	/** The current color option. */
	private ColorOption currentColorOption;
	
	/** The rotate. */
	boolean rotate;
	
	/** The visible. */
	boolean visible;
	
	/** The alpha. */
	double alpha;
	
	/** The transparent. */
	boolean transparent;
	
	/** The m_b first. */
	boolean m_bFirst;
	
	/** The layer. */
	LayerPosition layer;
	
	/** The mat. */
	Material mat; // current material during a working session;

	/** The base color. */
	ColorQuad baseColor;
	
	/** The plus color. */
	ColorQuad plusColor;
	
	/** The minus color. */
	ColorQuad minusColor;
	
	/** The colors. */
	ColorQuad colors[];

	/** The charge range. */
	MinMax chargeRange;

	/** The base material. */
	Material baseMaterial;
	
	/** The substructure filter list. */
	SubstructureRepList substructureFilterList = null;

	/** The substructure filter. */
	boolean substructureFilter;

	/** The scene. */
	Scene scene = null;
	
	/** The molecule. */
	Molecule molecule = null;

	/**
	 * Gets the gadget type.
	 * 
	 * @return the gadget type
	 */
	public abstract String getGadgetType();

	/**
	 * Gets the layer.
	 * 
	 * @return the layer
	 */
	public LayerPosition getLayer() {
		return layer;
	}

	/**
	 * Scene added callback.  Called when the 
	 * gadget is added to the scene.
	 * 
	 * @param s
	 *            the s
	 */
	public void setScene(Scene s) {
		scene = s;
		molecule = scene.GetMolecule();
		molecule.registerListener(this);
	}

	/**
	 * Scene changed notification.  Called if another gadget
	 * is added to the scene.  This is overridden for gadgets that
	 * depend on the display characteristics of other gadgets
	 * i.e. bonds will hide hydrogens if the atom gadget hides hydrogens
	 * 
	 * @param scene
	 *            the scene
	 */
	public void sceneChanged(Scene scene) {
		// most Gadgets don't care. do nothing
	}

	/**
	 * Checks if is substructure filter (does a substructure list decide visibility).
	 * 
	 * @return true, if is substructure filter
	 */
	public boolean isSubstructureFilter() {
		return substructureFilter;
	}

	/**
	 * Sets the substructure filter.
	 * 
	 * @param substructureFilter
	 *            the new substructure filter
	 */
	public void setSubstructureFilter(boolean substructureFilter) {
		this.substructureFilter = substructureFilter;
		markDirty(new MorbidEvent(this, "substructureFilter"));
	}

	/**
	 * Gets the substructure filter list.
	 * 
	 * @return the substructure filter list
	 */
	public SubstructureRepList getSubstructureFilterList() {
		return substructureFilterList;
	}

	/**
	 * Sets the substructure filter list.
	 * 
	 * @param substructureFilterList
	 *            the new substructure filter list
	 */
	public void setSubstructureFilterList(
			SubstructureRepList substructureFilterList) {
		this.substructureFilterList = substructureFilterList;
		markDirty();
	}

	/**
	 * Checks if is atom visible.
	 * 
	 * @param a
	 *            the a
	 * @return true, if is atom visible
	 */
	public boolean isAtomVisible(Atom a) {
		if (!isSubstructureFilter())
			return true;
		if (substructureFilterList == null)
			return true;
		return substructureFilterList.isVisible(a);

	}

	/**
	 * Sets the layer.
	 * 
	 * @param layer
	 *            the new layer
	 */
	public void setLayer(LayerPosition layer) {
		this.layer = layer;
	}

	/**
	 * Draw  abstract method that produces the representation of the gadget.
	 * 
	 * @param gl
	 *            the goblist to write to
	 */
	abstract void Draw(GobList gl);

	/**
	 * Draw gadget.
	 * Called by the scene - calles overridden method "Draw"
	 * This function ensures the gob list is initialized,
	 * if the gadget is not visible, it makes a small invisible sphere at the origin as a placeholder
	 * for the scene  graph,
	 * if the gadget is visible, calls the "Draw" method.
	 * it then marks the gadget clean (rendered) and the gob list dirty.
	 * 
	 * @param gl
	 *            the list of graphical objects produced from this gadget
	 */
	public void DrawGadget(GobList gl) {
		if (!fixGobList(gl))
			return;
		gl.setGadget(this);
		chargeRange = molecule.Atoms().getChargeRange();
		if (!isVisible()) { // make a small, scratch sphere at the origin to use
							// as replacement for the gadget's drawing.
			SphereGob s = new SphereGob(new Point3D(0, 0, 0), 0.0001);
			s.setLOD(10);
			gl.add(s);
			return;
		} else
			Draw(gl);
		gl.markDirty(new MorbidEvent(this));
		markClean();

	}

	/**
	 * Checks if this gadget is visible.
	 * 
	 * @return true, if is visible
	 */
	public boolean isVisible() {
		return visible;
	}

	/**
	 * Sets the visible.
	 * 
	 * @param show
	 *            the new visible
	 */
	public void setVisible(boolean show) {
		visible = show;
		markDirty(new MorbidEvent(this, "visible"));
	}

	// int GetLayer() { return position; }

	/**
	 * Instantiates a new gadget.
	 */
	Gadget() {
		setName(getGadgetType());
		// markDirty();
		visible = true;
		rotate = true;
		layer = LayerPosition.LayerModel;
		SetColor(StaticColorQuad.LiteGray.cq(), StaticColorQuad.Red.cq(),
				StaticColorQuad.Blue.cq());
		baseMaterial = new Material(baseColor);
		alpha = 0.5;
		transparent = false;
		this.currentColorOption = new ColorOption(ColorBy.TYPE);
	}

	/**
	 * Gets the scene.
	 * 
	 * @return the scene
	 */
	Scene GetScene() {
		return ((GadgetList) parentSet).GetScene();
	}

	/**
	 * Gets the molecule.
	 * 
	 * @return the molecule
	 */
	public Molecule GetMolecule() {
		return GetScene().GetMolecule();
	}
	
	/**
	 * clears gobList and sets name. returns true if draw should continue.
	 * 
	 * @param gobList
	 *            the gob list
	 * @return true, if successful
	 */
	boolean fixGobList(GobList gobList)

	{
		if (isDirty()) {
			gobList.clear();
			gobList.setName(getGadgetType());
		} else
			return false;

		gobList.markDirty();

		if (!isVisible()) {
			markClean();
			return false;
		}
		return true;
	}

	/**
	 * Sets the color.
	 * 
	 * @param base
	 *            the base
	 */
	void SetColor(ColorQuad base) {
		baseColor = base;
	}

	/**
	 * Sets the color.
	 * 
	 * @param base
	 *            the base color of the gadget
	 * @param plus
	 *            the plus color that represents positive values
	 * @param minus
	 *            the minus color that represents negative values
	 */
	void SetColor(ColorQuad base, ColorQuad plus, ColorQuad minus) {
		this.baseColor = base;
		this.plusColor = plus;
		this.minusColor = minus;
		colors = new ColorQuad[3];
		colors[0] = minusColor;
		colors[1] = baseColor;
		colors[2] = plusColor;
	}

	/**
	 * Gets the transparency.
	 * 
	 * @return the transparency
	 */
	public double getTransparency() {
		return alpha;
	}

	/**
	 * Sets the transparency.
	 * 
	 * @param transparency
	 *            the new transparency
	 */
	public void setTransparency(double transparency) {
		this.alpha = transparency;
		baseMaterial.setAlpha(alpha);
		markDirty(new MorbidEvent(this, "transparency"));
	}

	/**
	 * Checks if is transparent.
	 * 
	 * @return true, if is transparent
	 */
	public boolean isTransparent() {
		return transparent;
	}

	/**
	 * Sets the transparent.
	 * 
	 * @param useTransparency
	 *            the new transparent
	 */
	public void setTransparent(boolean useTransparency) {
		this.transparent = useTransparency;
		baseMaterial.setUseFilter(useTransparency);
		markDirty();
	}

	/**
	 * Gets the fraction color.
	 * 
	 * @param range
	 *            the range
	 * @param value
	 *            the value
	 * @return the fraction color
	 */
	ColorQuad getFractionColor(MinMax range, double value) {
		return ColorQuad.multiBlend(colors, value);
	}

	/**
	 * Gets the atom color. handles coloring by the base class color options.
	 * 
	 * @param a
	 *            the atom
	 * @return the atom color
	 */
	ColorQuad getAtomColor(Atom a) {
		ColorQuad theColor;
		switch (currentColorOption.colorBy) {
		case CHARGE:
			double fraction = chargeRange.getFraction(a.getCharge());
			theColor = ColorQuad.multiBlend(colors, fraction);
			mat = new Material(baseMaterial);
			mat.setColor(theColor);
			break;
		case TYPE: {
			AtomType at = a.getAtomType();
			if (at != null) {
				theColor = at.color;
				mat = new Material(at.mat);
			} else {
				theColor = baseColor;
				mat = baseMaterial;
			}
		}
			break;
		case SUBSTRUCTURE: {
			if (currentColorOption.reps == null) {
				mat = baseMaterial;
				theColor = StaticColorQuad.LiteGray.cq();
			} else {
				mat = new Material(baseMaterial);
				theColor = currentColorOption.reps.getColor(a);
				mat.setColor(theColor);
			}
		}
			break;
		default:
			theColor = baseColor;
			mat = baseMaterial;

		}
		if (theColor == null) {
			theColor = StaticColorQuad.LiteGray.cq();
			mat = new Material(baseMaterial);
		}
		if (transparent)
			mat.setAlpha(alpha);
		return theColor;
	}

	/**
	 * Gets the current color option.
	 * 
	 * @return the current color option
	 */
	public ColorOption getCurrentColorOption() {
		return currentColorOption;
	}

	/**
	 * Sets the current color option.
	 * 
	 * @param currentColorOption
	 *            the new current color option
	 */
	public void setCurrentColorOption(ColorOption currentColorOption) {
		this.currentColorOption = currentColorOption;
		markDirty(new MorbidEvent(this, "currentColorOption"));
	}
	

};