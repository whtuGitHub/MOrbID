package com.bobandthomas.Morbid.graphics.renderers;

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.media.j3d.AmbientLight;
import javax.media.j3d.Appearance;
import javax.media.j3d.Billboard;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.Font3D;
import javax.media.j3d.FontExtrusion;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.Group;
import javax.media.j3d.J3DGraphics2D;
import javax.media.j3d.LineArray;
import javax.media.j3d.Node;
import javax.media.j3d.PointArray;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Text3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.media.j3d.TriangleArray;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3f;

import com.bobandthomas.Morbid.Gadget.Gadget;
import com.bobandthomas.Morbid.Gadget.Scene.LayerPosition;
import com.bobandthomas.Morbid.graphics.ArrowGob;
import com.bobandthomas.Morbid.graphics.CTM;
import com.bobandthomas.Morbid.graphics.CircleGob;
import com.bobandthomas.Morbid.graphics.CylinderGob;
import com.bobandthomas.Morbid.graphics.Gob;
import com.bobandthomas.Morbid.graphics.GobIndexed;
import com.bobandthomas.Morbid.graphics.GobList;
import com.bobandthomas.Morbid.graphics.GobListSet;
import com.bobandthomas.Morbid.graphics.GobPoly;
import com.bobandthomas.Morbid.graphics.GobType;
import com.bobandthomas.Morbid.graphics.GobVector;
import com.bobandthomas.Morbid.graphics.LabelGob;
import com.bobandthomas.Morbid.graphics.LabeledCircleGob;
import com.bobandthomas.Morbid.graphics.LightSource;
import com.bobandthomas.Morbid.graphics.LightSourceList;
import com.bobandthomas.Morbid.graphics.Material;
import com.bobandthomas.Morbid.graphics.SphereGob;
import com.bobandthomas.Morbid.graphics.StringGob;
import com.bobandthomas.Morbid.graphics.GobPoly.GobPolyType;
import com.bobandthomas.Morbid.graphics.VertexList;
import com.bobandthomas.Morbid.utils.ColorQuad;
import com.bobandthomas.Morbid.utils.Point3D;
import com.bobandthomas.Morbid.utils.Vector3D;
import com.bobandthomas.Morbid.wrapper.Logger;
import com.sun.j3d.utils.behaviors.mouse.MouseRotate;
import com.sun.j3d.utils.behaviors.mouse.MouseTranslate;
import com.sun.j3d.utils.behaviors.mouse.MouseZoom;
import com.sun.j3d.utils.geometry.Cylinder;
import com.sun.j3d.utils.geometry.Primitive;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.geometry.Text2D;
import com.sun.j3d.utils.universe.SimpleUniverse;

/**
 * @author Thomas Kreek
 *
 */
public class RendererJava3D extends Renderer {
	/*
	 *   		rootBranch
	 *      |                          \
	 *      modelTransformGroup       frontTransformGroup
	 *      
	 *      bgMap contains map of the branchGroups by Gadget.
	 *      
	 *      currentBG is the branchGroup we will be writing into  
	 *    
	 */
	private BranchGroup rootBranch;
	private TransformGroup layerTransformGroup; 
	private TransformGroup frontTransformGroup; // front, unrotated and unscaled.
	private TransformGroup modelTransformGroup;  // add model layer primitives to this
	private SimpleUniverse universe; //owned by the port.
	private boolean lightingAdded = false;
	private BranchGroup currentBG = null;
	private Hashtable<Gadget, BranchGroup> bgMap = new Hashtable<Gadget, BranchGroup>();
	private Hashtable<Gadget, GobList> gobListMap = new Hashtable<Gadget, GobList>();
	private Transform3D lookat;
	private BoundingSphere bounds = new BoundingSphere(new Point3d(0.0, 0.0, 0.0),
	        100.0);
	
	private void makeGroupWritable(Group g)
	{
		g.setCapability(BranchGroup.ALLOW_DETACH);
		g.setCapability(Group.ALLOW_CHILDREN_READ);
		g.setCapability(Group.ALLOW_CHILDREN_WRITE);
		g.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		g.setCapability(Node.ALLOW_BOUNDS_READ);
		g.setCapability(Node.ALLOW_BOUNDS_WRITE);		
	}

	public RendererJava3D() {
		rootBranch = new BranchGroup();
		rootBranch.setName("rootBranch");
		makeGroupWritable(rootBranch);

		modelTransformGroup = new TransformGroup();
		layerTransformGroup = modelTransformGroup;
		modelTransformGroup.setName("objTrans");
		makeGroupWritable(modelTransformGroup);

		Transform3D lookat = new Transform3D();
		lookat.set(0.15);
		modelTransformGroup.setTransform(lookat);
		
		setupMouse(modelTransformGroup);
		rootBranch.addChild(modelTransformGroup);
		
		frontTransformGroup = new TransformGroup();
		frontTransformGroup.setName("frontTransformGroup");
		makeGroupWritable(frontTransformGroup);
		rootBranch.addChild(frontTransformGroup);
	}
	
	public void postRender(J3DGraphics2D j2D) {
		Rectangle rect = j2D.getDeviceConfiguration().getBounds();
		for (Gadget gadget : gobListMap.keySet()) {
			if (gadget.getLayer() != LayerPosition.LayerFront)
				continue;

			for (Gob g : gobListMap.get(gadget)) {
				int x = 0;
				int y = 0;
				Point3D position = g.center();
				if (position.x < 0)
					x = 10;
				else if (position.x > 0)
					x = rect.width - 100;

				if (position.y < 0)
					y = 10;
				else if (position.y > 0)
					y = rect.height - 10;

				if (g.Type() == GobType.Label) {
					Color c = g.Color.getJColor();
					j2D.setColor(c);
					String s = ((LabelGob) g).getName();
					if (s != null && s.length() > 0) j2D.drawString(s, x, y);
				}
				j2D.flush(false);
			}
		}
	}
	
	public void preRender()
	{
		
	}


	public BranchGroup getBranchGroup(Gadget gadget) {
		BranchGroup branchGroup;
		branchGroup = bgMap.get(gadget);
		if (branchGroup == null) {
			branchGroup = new BranchGroup();
			branchGroup.setName(gadget.getName());
			makeGroupWritable(branchGroup);

			layerTransformGroup.addChild(branchGroup);
			bgMap.put(gadget, branchGroup);
		}
		return branchGroup;
	}
	public void setCurrentBranchGroup(GobList gl) {
		currentBG = getBranchGroup(gl.getGadget());
	}
	private void printNodeLabel(String name, int depth)
	{
		String label="";
		for (int i=0; i< depth; i++)
			label += "      ";
		label +="|_______" + name;
		Logger.addMessage(this, label);
	}
	@SuppressWarnings({ "unused"})
	private void printGraphTree(Group n, int depth)
	{
		if (n == null) return;
		printNodeLabel(n.getClass().getName() + "-" + n.getName(), depth);
		Enumeration<?> e = n.getAllChildren();
		if (e== null) return;
		while(e.hasMoreElements())
		{
			Object next = e.nextElement();
			if 
			(Group.class.isAssignableFrom(next.getClass()))
			{
				printGraphTree((Group)next, depth+1);
			}
			else
			{
				printNodeLabel(next.getClass().toString(), depth);
			}
		}
	}

	@Override
	public void SetPort(Port p) {
		super.SetPort(p);
		if (p == null)
		{
			return;
		}

		PortJava3D j3p = (PortJava3D) p;
		j3p.setRenderer(this);
		universe = j3p.universe;
		
		lookat = new Transform3D();
		Rescale();
		
		if (Scale.x >0.0000000001)
			lookat.set(Scale.x/400);
		else
			lookat.set(0.15*zoom);
		layerTransformGroup.setTransform(lookat);
		
		universe.getViewingPlatform().setNominalViewingTransform();
	//	universe.addBranchGraph(rootBranch);
	}
	
	private void setupLights(LightSourceList LSList)
	{
		if (lightingAdded) return;
		BranchGroup lights;
			//BranchGroup lights = getBranchGroup("Lights Branch");
			lights = rootBranch;
			lights.detach();
			BoundingSphere bounds = new BoundingSphere(new Point3d(0.0, 0.0,
					0.0), 100.0);
			for (LightSource ls : LSList) {
				DirectionalLight light = new DirectionalLight(ls.color.Cf(),
						ls.getNormal().getVec3f());

				light.setInfluencingBounds(bounds);

				lights.addChild(light);
				ls.setRenderedLight(light);
			}
			AmbientLight ambient = new AmbientLight(new Color3f(1.0f, 1.0f,
					1.0f));
			ambient.setInfluencingBounds(bounds);
			lights.addChild(ambient);
			lightingAdded = true;
			universe.addBranchGraph(rootBranch);

	}

	@Override
	public void DoRender(GobListSet goblists, LightSourceList lsList,
			CTM totalCTM) {
		setupLights(lsList);
		if (goblists.get(0).getLayer() == LayerPosition.LayerModel)
			layerTransformGroup = modelTransformGroup;
		else
			layerTransformGroup = frontTransformGroup;
		
		for (GobList gl : goblists) {
			if (!gl.isDirty())
				continue;
			Gadget g = gl.getGadget();
			if (g == null) break;
			gobListMap.put(gl.getGadget(), gl);
			setCurrentBranchGroup(gl); //always returns the parent.
			// detach brach group from the objTrans
			currentBG.detach();
		    currentBG.removeAllChildren();
			Dispatch(gl);
			gl.markClean();
			layerTransformGroup.addChild(currentBG);
			universe.getViewingPlatform().setNominalViewingTransform();
			
//			printGraphTree(rootBranch, 1); //debugging
			
			
		}

	}

	private void setupMouse(TransformGroup tGroup) {
		MouseRotate behavior = new MouseRotate();
		behavior.setTransformGroup(tGroup);
		tGroup.addChild(behavior);
		behavior.setSchedulingBounds(bounds);
		
        MouseZoom behavior2 = new MouseZoom();
        behavior2.setTransformGroup(tGroup);
        tGroup.addChild(behavior2);
		behavior2.setSchedulingBounds(bounds);
		
		MouseTranslate behavior3 = new MouseTranslate();
		behavior3.setTransformGroup(tGroup);
		tGroup.addChild(behavior3);
        behavior3.setSchedulingBounds(bounds);
		
        tGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
	}

	private Appearance getAppearance(Material m, ColorQuad cq) {
		ColorQuad color = cq == null ? m.getColor(): cq;
		// ColoringAttributes ca = new ColoringAttributes(m.diffuse.Cf(),
		// ColoringAttributes.NICEST);
		Appearance app = new Appearance();
		if (m==null) return app;
		javax.media.j3d.Material J3DMaterial = new javax.media.j3d.Material();
		J3DMaterial.setDiffuseColor(color.Cf());
		J3DMaterial.setAmbientColor((color.multiply(m.getkAmbient())).Cf());
		J3DMaterial.setShininess(m.getSpecularity());
		if (m.isUseFilter()) {
			TransparencyAttributes ta = new TransparencyAttributes();
			ta.setTransparencyMode(TransparencyAttributes.BLENDED);
			ta.setTransparency((float) m.getAlpha());
			app.setTransparencyAttributes(ta);
		}

		app.setMaterial(J3DMaterial);

		return app;
	}

	private Transform3D getRotationTransform(GobVector g) {
		// creates the transform matrix to rotate the object to vector in g

		Vector3f unitV = g.getUnitVector().getVec3f();
		Vector3f unitY = new Vector3f(0.0f, 1.0f, 0.0f);

		Vector3f cross = new Vector3f();
		cross.cross(unitV, unitY);
		float dot = unitV.dot(unitY);

		Quat4d q = new Quat4d(cross.x, cross.y, cross.z, -(1 + dot));
		q.normalize();
		Transform3D t = new Transform3D();
		t.set(q);
		return t;
	}

	@Override
	void Arrow(ArrowGob g) {
		// TODO complete rednering Arrows
		CylinderGob cyl = new CylinderGob(g.center(), g.EndPoint, 0.01f);
		cyl.setMaterial(g.getMaterial());
		Cylinder(cyl);
		
	}

	@Override
	void Circle(CircleGob g) {
		// TODO eliminate 2d circles

	}

	@Override
	void Cylinder(CylinderGob g) {

		float length = g.getLength();

		Cylinder cyl = new Cylinder(g.getRadius(), length);

		cyl.setAppearance(getAppearance(currentMaterial, g.Color));

		TransformGroup tg = new TransformGroup();
		tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

		Transform3D transform = new Transform3D();
		transform.set(getRotationTransform(g));

		transform.setTranslation(g.getCenter().getVector().getVec3f());

		tg.setTransform(transform);
		tg.addChild(cyl);
		currentBG.addChild(tg);
	}

	@Override
	void Indexed(GobIndexed g) {
		// TODO eliminate IndexedGobs

	}


	@Override
	void LabeledCircle(LabeledCircleGob g) {
		// TODO render or eliminate labeled circle

	}

	@Override
	void Poly(GobPoly g) {
		
		GeometryArray array;
		int size = g.size();
		int flags = GeometryArray.COORDINATES;
		if (g.isHasColors())
			flags |= GeometryArray.COLOR_3;
		if (g.isHasNormals())
			flags |= GeometryArray.NORMALS;
		if (g.GetPolyType() == GobPolyType.Points)
			array = new PointArray(size, flags);
		if (g.GetPolyType() == GobPolyType.Segments)
			array = new LineArray(size, flags);
		else
			array = new TriangleArray(size, flags);
		Point3d points[] = new Point3d[size];
		VertexList vertices = g.getVertices();
		
		for (int i = 0; i< size; i++)
		{
			points[i]=vertices.get(i);
		}
		array.setCoordinates(0,points);
		if (g.isHasColors())
		{
			Color3f colors[] = new Color3f[size];
			for (int i = 0; i< size; i++)
			{
				colors[i]=vertices.get(i).getColor().Cf();
			}

			array.setColors(0,colors);
		}
		if (g.isHasNormals())
		{
			Vector3f normals[] = new Vector3f[size];
			for (int i = 0; i< size; i++)
			{
				Vector3D p = vertices.get(i).getNormal();
				normals[i]=p.getVec3f();
			}

			array.setNormals(0,normals);
				
		}
		Appearance ap = getAppearance(currentMaterial, g.Color);
		if (g.GetPolyType() == GobPolyType.Lines)
			ap.setPolygonAttributes(new PolygonAttributes(PolygonAttributes.POLYGON_LINE,PolygonAttributes.CULL_NONE, 0f));
		Shape3D shape = new Shape3D(array, ap);
		
		currentBG.addChild(shape);

	}

	@Override
	void Sphere(SphereGob g) {
		
		int tessalations;
		if (g.getLOD() < 0.01) tessalations = 200;
		else tessalations = (int) (200/g.getLOD());
		Sphere sphere = new Sphere((float) g.getRadius(), Primitive.GENERATE_NORMALS, tessalations);
		sphere.setName(g.getName());

		sphere.setAppearance(getAppearance(currentMaterial,g.Color));

		TransformGroup tg = new TransformGroup();
		Transform3D transform = new Transform3D();
		Vector3f vector = g.center().getVector().getVec3f();
		transform.setTranslation(vector);
		tg.setTransform(transform);
		tg.addChild(sphere);
		currentBG.addChild(tg);
	}

	//@Override
	void String(StringGob g) {
		Font3D font = new Font3D(new java.awt.Font("Garamond", Font.PLAIN, 11), new FontExtrusion());
		Text3D text = new Text3D(font , g.getName(), new Point3f(0f,0f,0f));
		
		Shape3D shape = new Shape3D();
		shape.setGeometry(text);
		shape.setAppearance(getAppearance(g.getMaterial(), g.Color));

		Vector3f vector = g.center().getVector().getVec3f();

		TransformGroup billboardGroup = new TransformGroup();
		billboardGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		Billboard board = new Billboard(billboardGroup, Billboard.ROTATE_ABOUT_POINT, new Vector3f(0,0,0));
		board.setSchedulingBounds(bounds);
		billboardGroup.addChild(board);
		billboardGroup.addChild(shape);
				
		
		TransformGroup tg = new TransformGroup();
		Transform3D transform = new Transform3D();
		transform.setTranslation(vector);
		transform.setScale(0.05*g.getScale());
		tg.setTransform(transform);

		tg.addChild(billboardGroup);
		
		
		currentBG.addChild(tg);

	}

	//@Override
	void String2(StringGob g) {
		if(g.getName() == null || g.getName().length() <1 )
			return;
		Text2D text = new Text2D(g.getName(),g.Color.Cf(),
			        "Serif", 70, Font.ITALIC);	
		text.setRectangleScaleFactor((float) g.getScale()/256);

		TransformGroup tg = new TransformGroup();
		Transform3D transform = new Transform3D();
		Vector3f vector = g.center().getVector().getVec3f();
		transform.setTranslation(vector);
		tg.setTransform(transform);
		tg.addChild(text);
		
		currentBG.addChild(tg);
	}
	@Override
	void Vector(GobVector g) {
		// TODO render GobVector 

	}

	@Override
	void Label(LabelGob g) {
		//TODO render or eliminate Labels
	}

}
