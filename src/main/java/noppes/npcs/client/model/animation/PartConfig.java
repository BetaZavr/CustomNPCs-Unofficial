package noppes.npcs.client.model.animation;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagList;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.api.entity.data.IAnimationPart;
import noppes.npcs.constants.EnumParts;
import noppes.npcs.util.ValueUtil;

public class PartConfig implements IAnimationPart {

	// [x, y, z, x1, y1 ] PI <> PI;
	public final float[] rotation = new float[] { 0.0f, 0.0f, 0.0f, 0.0f, 0.0f };
	// [x, y, z] -5 <> 5
	public final float[] offset = new float[] { 0.0f, 0.0f, 0.0f };
	// [x, y, z] 0 <> 5
	public final float[] scale = new float[] { 1.0f, 1.0f, 1.0f };

	public int id = 0;
	public boolean disable = false;
	public boolean show = true;
	public String name = "";
	protected EnumParts type = EnumParts.HEAD;

	public PartConfig() {
		clear();
		setMainName();
	}

	public PartConfig(int id, EnumParts type) {
		this();
		this.id = id;
		this.type = type;
		setMainName();
	}

	private void setMainName() {
		if (id > 7) {
			if (name.isEmpty()) { name = "part_" + type.name().toLowerCase() + "_" + id; }
			return;
		}
		switch (id) {
			case 0: name = "model.head"; break;
			case 1: name = "model.larm"; break;
			case 2: name = "model.rarm"; break;
			case 3: name = "model.body"; break;
			case 4: name = "model.lleg"; break;
			case 5: name = "model.rleg"; break;
			case 6: name = "model.lstack"; break;
			case 7: name = "model.rstack"; break;
		}
	}

	@Override
	public int getType() { return this.type.ordinal(); }
	
	public EnumParts getEnumType() { return this.type; }
	
	@Override
	public void clear() {
		for (int i = 0; i < 5; i++) {
			rotation[i] = 0.0f;
			if (i > 2) { continue; }
			offset[i] = 0.0f;
			scale[i] = 1.0f;
		}
	}

	public PartConfig copy() {
		PartConfig pc = new PartConfig();
		pc.load(save());
		return pc;
	}

	@Override
	public float[] getOffset() {
		return new float[] { offset[0], offset[1], offset[2] };
	}

	@Override
	public float[] getRotation() {
		return new float[] { rotation[0], rotation[1], rotation[2] };
	}
	
	@Override
	public float[] getRotationPart() {
		return new float[] { rotation[3], rotation[4] };
	}

	@Override
	public float[] getScale() {
		return new float[] { scale[0], scale[1], scale[2] };
	}

	@Override
	public boolean isDisable() {
		return this.disable;
	}

	@Override
	public boolean isShow() {
		return this.show;
	}

	public void load(NBTTagCompound compound) {
		int v = compound.getInteger("v");
		float pi = (float) Math.PI;
		for (int i = 0; i < 5; i++) {
			float valueR = compound.getTagList("Rotation", 5).getFloatAt(i);
			float valueO = compound.getTagList("Offset", 5).getFloatAt(i);
			float valueS = compound.getTagList("Scale", 5).getFloatAt(i);
			try {
				float corr = (float) Math.PI / (i == 4 ? 2.0f : 1.0f);
				if (v == 0) { rotation[i] = ValueUtil.correctFloat((2.0f * pi * valueR - pi) / (i == 4 ? 2.0f : 1.0f), -corr, corr); }
				else if (i == 4) { rotation[i] = ValueUtil.correctFloat(valueR, -corr, corr); }
				else { rotation[i] = ValueUtil.wrapRadians(valueR); }
			} catch (Exception e) { LogWriter.error(e); }
			if (i > 2) {
				continue; }
			try {
				if (v == 0) { offset[i] = ValueUtil.correctFloat(valueO * 10.0f - 5.0f, -5.0f, 5.0f); }
				else { offset[i] = ValueUtil.correctFloat(valueO, -5.0f, 5.0f); }
			} catch (Exception e) { LogWriter.error(e); }
			try {
				if (v == 0) { scale[i] = ValueUtil.correctFloat(valueS * 5.0f, 0.0f, 5.0f); }
				else { scale[i] = ValueUtil.correctFloat(valueS, 0.0f, 5.0f); }
			} catch (Exception e) { LogWriter.error(e); }
		}
		id = compound.getInteger("Part");
		if (compound.hasKey("Disabled", 1)) { disable = compound.getBoolean("Disabled"); }
		if (compound.hasKey("Show", 1)) { show = compound.getBoolean("Show"); }
		if (compound.hasKey("Name", 8)) { name = compound.getString("Name"); }
		setMainName();
		if (compound.hasKey("Type", 3)) {
			type = EnumParts.values()[compound.getInteger("Type")];
		}
	}

	@Override
	public void setDisable(boolean bo) {
		this.disable = bo;
	}

	@Override
	public void setOffset(float x, float y, float z) {
		x %= 5.0f;
		y %= 5.0f;
		z %= 5.0f;
		offset[0] = ValueUtil.correctFloat(x / 5.0f, 0.0f, 1.0f);
		offset[1] = ValueUtil.correctFloat(y / 5.0f, 0.0f, 1.0f);
		offset[2] = ValueUtil.correctFloat(z / 5.0f, 0.0f, 1.0f);
	}

	@Override
	public void setRotation(float x, float y, float z) {
		float pi = (float) Math.PI;
		rotation[0] = ValueUtil.wrapRadians(x / 180.0f * pi);
		rotation[1] = ValueUtil.wrapRadians(y / 180.0f * pi);
		rotation[2] = ValueUtil.wrapRadians(z / 180.0f * pi);
	}

	@Override
	public void setRotation(float x1, float y1) {
		float pi = (float) Math.PI;
		rotation[3] = ValueUtil.wrapRadians(x1 / 180.0f * pi);
		rotation[4] = ValueUtil.correctFloat(ValueUtil.wrapRadians(y1 / 180.0f * pi), -pi / 2.0f, pi / 2.0f);
	}

	@Override
	public void setScale(float x, float y, float z) {
		x %= 5.0f;
		y %= 5.0f;
		z %= 5.0f;
		scale[0] = ValueUtil.correctFloat(x / 5.0f, 0.0f, 1.0f);
		scale[1] = ValueUtil.correctFloat(y / 5.0f, 0.0f, 1.0f);
		scale[2] = ValueUtil.correctFloat(z / 5.0f, 0.0f, 1.0f);
	}

	@Override
	public void setShow(boolean bo) {
		this.show = bo;
	}

	public NBTTagCompound save() {
		NBTTagCompound compound = new NBTTagCompound();
		NBTTagList listRot = new NBTTagList();
		NBTTagList listOff = new NBTTagList();
		NBTTagList listSc = new NBTTagList();
		for (int i = 0; i < 3; i++) {
			listRot.appendTag(new NBTTagFloat(this.rotation[i]));
			listOff.appendTag(new NBTTagFloat(this.offset[i]));
			listSc.appendTag(new NBTTagFloat(this.scale[i]));
		}
		listRot.appendTag(new NBTTagFloat(this.rotation[3]));
		listRot.appendTag(new NBTTagFloat(this.rotation[4]));
		compound.setTag("Rotation", listRot);
		compound.setTag("Offset", listOff);
		compound.setTag("Scale", listSc);
		
		compound.setInteger("Part", this.id);
		compound.setBoolean("Disabled", this.disable);
		compound.setBoolean("Show", this.show);
		compound.setString("Name", this.name);
		compound.setInteger("Type", this.type.ordinal());

		compound.setInteger("v", 1);

		return compound;
	}

	/**
	 * @param inBase [ 0:rotX, 1:rotY, 2:rotZ, 3:ofsX, 4:ofsY, 5:ofsZ, 6:scX, 7:scY, 8:scZ, 9:rotX1, 10:rotY1 ]
	 */
    public void set(Float[] inBase) {
		if (inBase == null) {
			clear();
			return;
		}
		for (int i = 0; i < 11; i++) {
			if (inBase.length > i) {
				if (i < 3) { rotation[i] = ValueUtil.wrapRadians(inBase[i]); }
				else if (i < 6) { offset[i - 3] = inBase[i]; }
				else if (i < 9){ scale[i - 6] = inBase[i]; }
				else { rotation[i - 6] = ValueUtil.wrapRadians(inBase[i]); }
			}
		}
    }

}
