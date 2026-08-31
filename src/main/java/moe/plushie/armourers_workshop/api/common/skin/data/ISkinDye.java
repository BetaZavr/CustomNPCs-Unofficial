package moe.plushie.armourers_workshop.api.common.skin.data;

import io.netty.buffer.ByteBuf;

@SuppressWarnings("unused")
public interface ISkinDye {

	void addDye(byte[] rgbt);

	void addDye(byte[] rgbt, String name);

	void addDye(int index, byte[] rgbt);

	void addDye(int index, byte[] rgbt, String name);

	byte[] getDyeColour(int index);

	String getDyeName(int index);

	int getNumberOfDyes();

	boolean hasName(int index);

	boolean haveDyeInSlot(int index);

	void readFromBuf(ByteBuf buf);

	void removeDye(int index);

	void writeToBuf(ByteBuf buf);

}
