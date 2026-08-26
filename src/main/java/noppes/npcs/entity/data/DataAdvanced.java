package noppes.npcs.entity.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import noppes.npcs.NBTTags;
import noppes.npcs.api.constants.JobType;
import noppes.npcs.api.constants.RoleType;
import noppes.npcs.api.entity.data.INPCAdvanced;
import noppes.npcs.client.controllers.MusicController;
import noppes.npcs.constants.EnumSeeTarget;
import noppes.npcs.controllers.data.*;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketPlaySound;
import noppes.npcs.shared.client.gui.util.NoppesStringUtils;
import noppes.npcs.util.ValueUtil;

import java.util.HashSet;

public class DataAdvanced implements INPCAdvanced {

   private final EntityNPCInterface npc;
   public Lines interactLines = new Lines();
   public Lines worldLines = new Lines();
   public Lines attackLines = new Lines();
   public Lines killedLines = new Lines();
   public Lines killLines = new Lines();
   public Lines npcInteractLines = new Lines();
   private String idleSound = "";
   private String angrySound = "";
   private String hurtSound = "minecraft:entity.player.hurt";
   private String deathSound = "minecraft:entity.player.hurt";
   private String stepSound = "";
   public FactionOptions factions = new FactionOptions();
   public boolean attackOtherFactions = false;
   public boolean defendFaction = false;
   public boolean disablePitch = false;
   public boolean orderedLines = false;
   public boolean throughWalls = true;
   public DataScenes scenes;

   // New from Unofficial (BetaZavr)
   /*
    * 0 - none
    * 1 - puppet
    * 2 - custom anims
    * 3 - geckolib
    */
   public int animationType = 0;
   public HashSet<Integer> attackFactions = new HashSet<>();
   public HashSet<Integer> friendFactions = new HashSet<>();
   public EntityNPCInterface spawner;

   public DataAdvanced(EntityNPCInterface npcIn) {
      npc = npcIn;
      scenes = new DataScenes(npcIn);
   }

   public CompoundTag save(CompoundTag compound) {
      compound.put("NpcLines", worldLines.save());
      compound.put("NpcKilledLines", killedLines.save());
      compound.put("NpcInteractLines", interactLines.save());
      compound.put("NpcAttackLines", attackLines.save());
      compound.put("NpcKillLines", killLines.save());
      compound.put("NpcInteractNPCLines", npcInteractLines.save());
      compound.putBoolean("OrderedLines", orderedLines);
      compound.putString("NpcIdleSound", idleSound);
      compound.putString("NpcAngrySound", angrySound);
      compound.putString("NpcHurtSound", hurtSound);
      compound.putString("NpcDeathSound", deathSound);
      compound.putString("NpcStepSound", stepSound);
      compound.putInt("FactionID", npc.getFaction().id);
      compound.putBoolean("AttackOtherFactions", attackOtherFactions);
      compound.putBoolean("DefendFaction", defendFaction);
      compound.putBoolean("DisablePitch", disablePitch);
      compound.putInt("Role", npc.role.getType());
      compound.putInt("NpcJob", npc.job.getType());
      compound.put("FactionPoints", factions.save(new CompoundTag()));
      compound.put("NpcScenes", scenes.save(new CompoundTag()));
      compound.putIntArray("NPCDialogOptions", npc.dialogs);
      // New from Unofficial (BetaZavr)
      compound.putInt("AnimationType", animationType);
      compound.putBoolean("ThroughWalls", throughWalls);
      compound.put("AttackFactions", NBTTags.nbtIntegerCollection(attackFactions));
      compound.put("FrendFactions", NBTTags.nbtIntegerCollection(friendFactions));
      return compound;
   }

   public void load(CompoundTag compound) {
      interactLines.load(compound.getCompound("NpcInteractLines"));
      worldLines.load(compound.getCompound("NpcLines"));
      attackLines.load(compound.getCompound("NpcAttackLines"));
      killedLines.load(compound.getCompound("NpcKilledLines"));
      killLines.load(compound.getCompound("NpcKillLines"));
      npcInteractLines.load(compound.getCompound("NpcInteractNPCLines"));
      orderedLines = compound.getBoolean("OrderedLines");
      idleSound = compound.getString("NpcIdleSound");
      angrySound = compound.getString("NpcAngrySound");
      hurtSound = compound.getString("NpcHurtSound");
      deathSound = compound.getString("NpcDeathSound");
      stepSound = compound.getString("NpcStepSound");
      npc.setFaction(compound.getInt("FactionID"));
      npc.faction = npc.getFaction();
      attackOtherFactions = compound.getBoolean("AttackOtherFactions");
      defendFaction = compound.getBoolean("DefendFaction");
      disablePitch = compound.getBoolean("DisablePitch");
      setRole(compound.getInt("Role"));
      setJob(compound.getInt("NpcJob"));
      factions.load(compound.getCompound("FactionPoints"));
      scenes.load(compound.getCompound("NpcScenes"));
      if (compound.contains("NPCDialogOptions", 11)) {
         npc.dialogs = compound.getIntArray("NPCDialogOptions");
      }
      else if (compound.contains("NPCDialogOptions", 9)) {
         ListTag list = compound.getList("NPCDialogOptions", 10);
         npc.dialogs = new int[list.size()];
         for (int i = 0; i < list.size(); ++i) {
            npc.dialogs[i] = list.getCompound(i).getCompound("NPCDialog").getInt("Dialog");
         }
      }
      // New from Unofficial (BetaZavr)
      if (compound.contains("AnimationType", 3)) { setAnimationType(compound.getInt("AnimationType")); }
      if (!compound.contains("ThroughWalls", 1)) { throughWalls = true; }
      else { throughWalls = compound.getBoolean("ThroughWalls"); }
      attackFactions = NBTTags.getIntegerSet(compound.getList("AttackFactions", 10));
      friendFactions = NBTTags.getIntegerSet(compound.getList("FrendFactions", 10));
   }

   private Lines getLines(int type) {
      return switch (type) {
         case 0 -> interactLines;
         case 1 -> attackLines;
         case 2 -> worldLines;
         case 3 -> killedLines;
         case 4 -> killLines;
         case 5 -> npcInteractLines;
         default -> null;
      };
   }

   @Override
   public void setLine(int type, int slot, String text, String sound) {
      slot = ValueUtil.correctInt(slot, 0, 7);
      Lines lines = getLines(type);
      if (lines == null) { return; }
      if (text != null && !text.isEmpty()) {
         Line line = lines.lines.computeIfAbsent(slot, k -> new Line());
         line.setText(text);
         line.setSound(sound);
      }
      else { lines.lines.remove(slot); }
   }

   @Override
   public String getLine(int type, int slot) {
      Lines lines = getLines(type);
      if (lines == null) { return ""; }
      Line line = lines.lines.get(slot);
      return line == null ? "" : line.getText();
   }

   @Override
   public int getLineCount(int type) {
      Lines lines = getLines(type);
      if (lines == null) { return 0; }
      return lines.lines.size();
   }

   @Override
   public String getSound(int type) {
      String sound = switch (type) {
          case 0 -> idleSound;
          case 1 -> angrySound;
          case 2 -> hurtSound;
          case 3 -> deathSound;
          default -> stepSound;
      };
      return sound == null || sound.isEmpty() ? null : NoppesStringUtils.cleanResource(sound);
   }

   public void playSound(int type, float volume, float pitch) {
      String sound = getSound(type);
      if (sound != null) {
         if (!npc.level().isClientSide) {
            Packets.sendNearby(npc.level(), npc.blockPosition(), 16,
                 new PacketPlaySound(sound, SoundSource.NEUTRAL, npc.getX(), npc.getY(), npc.getZ(), volume, pitch));
         }
         else { MusicController.Instance.playSound(SoundSource.VOICE, sound, npc.getX(), npc.getY(), npc.getZ(), volume, pitch); }
      }
   }

   @Override
   public void setSound(int type, String sound) {
      if (sound == null) { sound = ""; }
      sound = NoppesStringUtils.cleanResource(sound);
      switch (type) {
         case 0: idleSound = sound; break;
         case 1: angrySound = sound; break;
         case 2: hurtSound = sound; break;
         case 3: deathSound = sound; break;
         default: stepSound = sound; break;
      }
   }

   public Line getInteractLine() { return interactLines.getLine(!orderedLines); }

   public Line getAttackLine() { return attackLines.getLine(!orderedLines); }

   public Line getKilledLine() { return killedLines.getLine(!orderedLines); }

   public Line getKillLine() { return killLines.getLine(!orderedLines); }

   public Line getLevelLine() { return worldLines.getLine(!orderedLines); }

   public Line getNPCInteractLine() { return npcInteractLines.getLine(!orderedLines); }

   public void setRole(int id) { RoleType.get(id).setToNpc(npc); }

   public void setJob(int id) {
      JobType.get(id).setToNpc(npc);
      if (!npc.level().isClientSide()) { npc.job.reset(); }
   }

   public boolean hasLevelLines() { return !worldLines.isEmpty(); }

   // New from Unofficial (BetaZavr)
   public void tryDefendFaction(int id, LivingEntity possibleFriend, LivingEntity attacked) {
      if (npc.isKilled() || !defendFaction || possibleFriend.equals(attacked)) { return; }
      boolean canSee = npc.canSee(possibleFriend) || (npc.ais.directLOS != EnumSeeTarget.NONE && npc.ais.directLOS != EnumSeeTarget.BLIND) || npc.canSee(attacked);
      if (!canSee && throughWalls) {
         canSee = npc.distanceTo(possibleFriend) <= npc.stats.aggroRange;
      }
      if (!(npc.faction.id == id || npc.faction.frendFactions.contains(id) || friendFactions.contains(id)) || !canSee) { return; }
      npc.onAttack(attacked);
   }

   @Override
   public int getAnimationType() { return animationType; }

   @Override
   public void setAnimationType(int type) {
      animationType = ValueUtil.onlyPositiveInt(type, 100);
      npc.updateClient = true;
   }

}
