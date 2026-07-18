package noppes.npcs.controllers;

import java.io.DataInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import noppes.npcs.CustomNpcs;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketSyncRemove;
import noppes.npcs.packets.client.PacketSyncUpdate;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.api.constants.AnimationKind;
import noppes.npcs.api.handler.IAnimationHandler;
import noppes.npcs.client.model.animation.AnimationConfig;
import noppes.npcs.client.model.animation.EmotionConfig;
import noppes.npcs.util.Util;

public class AnimationController implements IAnimationHandler {

	protected final TreeMap<Integer, AnimationConfig> animations = new TreeMap<>();
	protected final TreeMap<Integer, EmotionConfig> emotions = new TreeMap<>();
	protected static AnimationController instance;

	protected int baseMaxAnimID = 0;

	public static AnimationController getInstance() {
		if (instance == null) { instance = new AnimationController(); }
		return instance;
	}

	@Override
	public AnimationConfig createNewAnim() {
		AnimationConfig ac = new AnimationConfig();
		ac.id = getUnusedAnimId();
		animations.put(ac.id, ac);
		return ac;
	}

	@Override
	public EmotionConfig createNewEmtn() {
		EmotionConfig ec = new EmotionConfig();
		ec.id = getUnusedEmtnId();
		emotions.put(ec.id, ec);
		return ec;
	}

	@Override
	public AnimationConfig getAnimation(int animationId) {
		if (animations.containsKey(animationId)) { return animations.get(animationId); }
		return null;
	}

	@Override
	public AnimationConfig getAnimation(String animationName) {
		for (AnimationConfig ac : animations.values()) {
			if (ac.getName().equalsIgnoreCase(animationName)) { return ac; }
		}
		return null;
	}

	@Override
	public EmotionConfig getEmotion(int emotionId) {
		if (emotions.containsKey(emotionId)) { return emotions.get(emotionId); }
		return null;
	}

	@Override
	public EmotionConfig getEmotion(String emotionName) {
		for (EmotionConfig ec : emotions.values()) {
			if (ec.getName().equalsIgnoreCase(emotionName)) { return ec; }
		}
		return null;
	}

	@Override
	public AnimationConfig[] getAnimations() { return animations.values().toArray(new AnimationConfig[0]); }

	public List<AnimationConfig> getAnimations(List<Integer> ids) {
		List<AnimationConfig> list = new ArrayList<>();
		if (ids == null || ids.isEmpty()) { return list; }
		for (AnimationConfig ac : animations.values()) {
			for (int id : ids) {
				if (ac.getId() == id) {
					list.add(ac);
					break;
				}
			}
		}
		return list;
	}

	public int getUnusedEmtnId() {
		int id = 0;
		while (emotions.containsKey(id)) { id++; }
		return id;
	}

	public int getUnusedAnimId() {
		int id = Math.max(baseMaxAnimID, 0);
		while (animations.containsKey(id)) { id++; }
		return id;
	}

	public AnimationConfig loadAnimation(NBTTagCompound nbtAnimation) {
		if (nbtAnimation == null || !nbtAnimation.hasKey("ID", 3) || nbtAnimation.getInteger("ID") < 0) { return null; }
		AnimationConfig ac;
		if (animations.containsKey(nbtAnimation.getInteger("ID"))) {
			ac = animations.get(nbtAnimation.getInteger("ID"));
			ac.load(nbtAnimation);
			return ac;
		}
		ac = new AnimationConfig();
		ac.load(nbtAnimation);
		animations.put(ac.id, ac);
		return animations.get(nbtAnimation.getInteger("ID"));
	}

	public EmotionConfig loadEmotion(NBTTagCompound nbtEmotion) {
		if (nbtEmotion == null || !nbtEmotion.hasKey("ID", 3) || nbtEmotion.getInteger("ID") < 0) { return null; }
		if (emotions.containsKey(nbtEmotion.getInteger("ID"))) {
			emotions.get(nbtEmotion.getInteger("ID")).read(nbtEmotion);
			return emotions.get(nbtEmotion.getInteger("ID"));
		}
		EmotionConfig ec = new EmotionConfig();
		ec.read(nbtEmotion);
		emotions.put(nbtEmotion.getInteger("ID"), ec);
		return emotions.get(nbtEmotion.getInteger("ID"));
	}


	public void loadAnimations() {
		CustomNpcs.debugData.start(null);
		LogWriter.info("Start load animations");
		boolean needSave = false;
		File animDir;
		animations.clear();
		loadDefaultAnimations();
		File emtnDir;
		emotions.clear();
		// check old data
		if (CustomNpcs.Dir != null) {
			File oldFile = new File(CustomNpcs.Dir, "animations.dat");
			if (oldFile.exists()) {
				try { loadOldAnimations(CompressedStreamTools.readCompressed(new DataInputStream(Files.newInputStream(oldFile.toPath())))); } catch (Exception e) { LogWriter.error(e); }
				Util.instance.removeFile(oldFile);
			}
			animDir = new File (CustomNpcs.Dir,  "animations");
			if (animDir.exists()) {
				try {
					loadAnimations(animDir);
					save();
				}
				catch (Exception e) { LogWriter.error(e); }
			}
			emtnDir = new File (CustomNpcs.Dir,  "emotions");
			if (emtnDir.exists()) {
				try {
					loadEmotions(emtnDir);
					save();
				}
				catch (Exception e) { LogWriter.error(e); }
			}
		}
		// normal load
		animDir = new File (CustomNpcs.getWorldSaveDirectory(),  "animations");
		if (animDir.exists()) {
			try { loadAnimations(animDir); } catch (Exception e) { LogWriter.error(e); }
		}
		else { needSave = true; }
		emtnDir = new File (CustomNpcs.getWorldSaveDirectory(),  "emotions");
		if (emtnDir.exists()) { try { loadEmotions(emtnDir); } catch (Exception e) { LogWriter.error(e); } }
		else { needSave = true; }
		if (needSave) { save(); }
		LogWriter.info("End load animations");
		CustomNpcs.debugData.end(null);
	}

	private void loadOldAnimations(NBTTagCompound compound) {
		NBTTagList listA = compound.getTagList("Animations", 10);
		if (compound.hasKey("Animations", 9)) { listA = compound.getTagList("Animations", 10); }
		else if (compound.hasKey("Data", 9)) { listA = compound.getTagList("Data", 10); }
		for (int i = 0; i < listA.tagCount(); ++i) {
			AnimationConfig anim = loadAnimation(listA.getCompoundTagAt(i));
			if (anim.id < 43) { anim.immutable = true; }
		}
		emotions.clear();
		NBTTagList listE = compound.getTagList("Emotions", 10);
		for (int i = 0; i < listE.tagCount(); ++i) {
			EmotionConfig emtn = loadEmotion(listE.getCompoundTagAt(i));
			emtn.immutable = true;
		}
	}

	private void loadAnimations(File file) {
		List<NBTTagCompound> afterAnimations = new ArrayList<>();
		for (File f : Objects.requireNonNull(file.listFiles())) {
			try {
				NBTTagCompound nbt = CompressedStreamTools.readCompressed(new DataInputStream(Files.newInputStream(f.toPath())));
				int id = -1;
				try { id = Integer.parseInt(f.getName().toLowerCase().replace(".dat", "")); } catch (Exception e) { LogWriter.error(e); }
				if (id == -1 || animations.containsKey(id) || id < baseMaxAnimID) { afterAnimations.add(nbt); }
				else {
					nbt.setInteger("ID", id);
					loadAnimation(nbt);
				}
			} catch (Exception e) { LogWriter.error(e); }
		}
		for (NBTTagCompound nbt : afterAnimations) {
			int id = nbt.getInteger("ID");
			if (id == -1 || animations.containsKey(id) || id < baseMaxAnimID) { nbt.setInteger("ID", getUnusedAnimId()); }
			loadAnimation(nbt);
		}
	}

	private void loadEmotions(File file) {
		for (File f : Objects.requireNonNull(file.listFiles())) {
			try {
				try {
					NBTTagCompound nbt = CompressedStreamTools.readCompressed(new DataInputStream(Files.newInputStream(f.toPath())));
					int id = -1;
					try { id = Integer.parseInt(f.getName().toLowerCase().replace(".dat", "")); } catch (Exception e) { LogWriter.error(e); }
					if (id != -1 && animations.containsKey(id)) { nbt.setInteger("ID", getUnusedAnimId()); }
					else { nbt.setInteger("ID", id); }
					loadEmotion(nbt);
				} catch (Exception e) { LogWriter.error(e); }
			} catch (Exception e) { LogWriter.error(e); }
		}
	}

	private void loadDefaultAnimations() {
		baseMaxAnimID = -1;
		InputStream inputStream = Util.instance.getModInputStream("a_def.dat");
		if (inputStream == null) { return; }
		NBTTagCompound compound = new NBTTagCompound();
		try { compound = CompressedStreamTools.readCompressed(inputStream); } catch (Exception e) { LogWriter.error(e); }
		NBTTagList listA = compound.getTagList("Animations", 10);
		if (!listA.hasNoTags()) {
			for (int i = 0; i < listA.tagCount(); ++i) {
				NBTTagCompound nbt = listA.getCompoundTagAt(i);
				int id = nbt.getInteger("ID");
				if (animations.containsKey(id)) {
					if (!animations.get(id).immutable || !animations.get(id).name.equals(nbt.getString("Name"))) {
						boolean found = false;
						for (AnimationConfig anim : animations.values()) {
							if (anim.name.equals(nbt.getString("Name"))) {
								id = anim.id;
								found = true;
								break;
							}
						}
						if (!found) { id = getUnusedAnimId(); }
						nbt.setInteger("ID", id);
					}
				}
				AnimationConfig iAnim = loadAnimation(nbt);
				if (baseMaxAnimID < id) { baseMaxAnimID = id + 1; }
				if (iAnim != null) { iAnim.immutable = true; }
			}
		}
		NBTTagList listE = compound.getTagList("Emotions", 10);
		if (!listE.hasNoTags()) {
			for (int i = 0; i < listE.tagCount(); ++i) {
				NBTTagCompound nbt = listE.getCompoundTagAt(i);
				int id = nbt.getInteger("ID");
				if (emotions.containsKey(id)) {
					if (!emotions.get(id).immutable || !emotions.get(id).name.equals(nbt.getString("Name"))) {
						boolean found = false;
						for (EmotionConfig emtn : emotions.values()) {
							if (emtn.name.equals(nbt.getString("Name"))) {
								id = emtn.id;
								found = true;
								break;
							}
						}
						if (!found) { id = getUnusedEmtnId(); }
						nbt.setInteger("ID", id);
					}
				}
				loadEmotion(nbt);
			}
		}
	}

	@Override
	public boolean removeAnimation(int animationId) {
		if (animations.containsKey(animationId)) {
			animations.remove(animationId);
			save();
			return true;
		}
		return false;
	}

	@Override
	public boolean removeAnimation(String animationName) {
		for (int id : animations.keySet()) {
			if (animations.get(id).getName().equalsIgnoreCase(animationName)) {
				animations.remove(id);
				save();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean removeEmotion(int emotionId) {
		if (emotions.containsKey(emotionId)) {
			emotions.remove(emotionId);
			save();
			return true;
		}
		return false;
	}

	@Override
	public boolean removeEmotion(String emotionName) {
		for (int id : emotions.keySet()) {
			if (emotions.get(id).getName().equalsIgnoreCase(emotionName)) {
				emotions.remove(id);
				save();
				return true;
			}
		}
		return false;
	}


	public void save() {
		CustomNpcs.debugData.start(null);
		File animDir = CustomNpcs.getWorldSaveDirectory("animations");
		if (animDir != null) {
			if (animDir.exists() || animDir.mkdirs()) {
				for (int id : animations.keySet()) {
					if (animations.get(id).immutable) { continue; }
					try { CompressedStreamTools.writeCompressed(animations.get(id).save(), Files.newOutputStream(new File(animDir, id + ".dat").toPath())); }
					catch (Exception e) { LogWriter.error(e); }
				}
			}
			File emtnDir = CustomNpcs.getWorldSaveDirectory("emotions");
			if (emtnDir == null) {
				CustomNpcs.debugData.end(null);
				return;
			}
			if (emtnDir.exists() || emtnDir.mkdirs()) {
				for (int id : emotions.keySet()) {
					try { CompressedStreamTools.writeCompressed(emotions.get(id).save(), Files.newOutputStream(new File(emtnDir, id + ".dat").toPath())); }
					catch (Exception e) { LogWriter.error(e); }
				}
			}
		}
		CustomNpcs.debugData.end(null);
	}

	public void sendTo(EntityPlayerMP player) {
		if (CustomNpcs.Server != null && CustomNpcs.Server.isSinglePlayer() && player.getName().equals(CustomNpcs.Server.getServerOwner())) { return; }
		Packets.send(player, new PacketSyncRemove(-1, 9));
		Packets.send(player, new PacketSyncRemove(-1, 10));
		for (AnimationConfig ac : animations.values()) { Packets.send(player, new PacketSyncUpdate(0, 9, ac.save())); }
		for (EmotionConfig ec : emotions.values()) { Packets.send(player, new PacketSyncUpdate(0, 10, ec.save())); }
	}

	@Override
	public EmotionConfig[] getEmotions() { return emotions.values().toArray(new EmotionConfig[0]); }

	public void clearAnimations() { animations.clear(); }

	public void clearEmotions() { emotions.clear(); }

	public AnimationConfig copy(int id, AnimationKind type) {
		if (!animations.containsKey(id)) { return null; }
		AnimationConfig ac = animations.get(id).copy();
		ac.id = getUnusedAnimId();
		ac.immutable = false;
		if (type != null) { ac.type = type; }
		animations.put(ac.id, ac);
		return ac;
	}

	public boolean hasAnimation(int id) { return animations.containsKey(id); }

	public boolean hasEmotion(int id) { return emotions.containsKey(id); }

	public void sendAnimationToAll(int id) {
		if (CustomNpcs.Server != null) {
			NBTTagCompound data = animations.containsKey(id) ? animations.get(id).save() : null;
			for (EntityPlayerMP player : CustomNpcs.Server. getPlayerList().getPlayers()) {
				if (id < 0) { sendTo(player); }
				else if (data == null) { Packets.send(player, new PacketSyncRemove(id, 9)); }
				else { Packets.send(player, new PacketSyncUpdate(0, 9, data)); }
			}
		}
	}

	public void sendEmotionToAll(int id) {
		if (CustomNpcs.Server != null) {
			NBTTagCompound data = emotions.containsKey(id) ? emotions.get(id).save() : null;
			for (EntityPlayerMP player : CustomNpcs.Server. getPlayerList().getPlayers()) {
				if (id < 0) { sendTo(player); }
				else if (data == null) { Packets.send(player, new PacketSyncRemove(id, 10)); }
				else { Packets.send(player, new PacketSyncUpdate(0, 10, data)); }
			}
		}
	}

}
