package noppes.npcs.roles;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.constants.JobType;
import noppes.npcs.api.entity.data.role.IJobPuppet;
import noppes.npcs.constants.EnumAnimationType;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.util.ValueUtil;

import javax.annotation.Nonnull;

public class JobPuppet extends JobInterface implements IJobPuppet {

    protected final @Nonnull EntityNPCInterface npc;

    public PartConfig head = new PartConfig();
    public PartConfig larm = new PartConfig();
    public PartConfig rarm = new PartConfig();
    public PartConfig body = new PartConfig();
    public PartConfig lleg = new PartConfig();
    public PartConfig rleg = new PartConfig();
    public PartConfig head2 = new PartConfig();
    public PartConfig larm2 = new PartConfig();
    public PartConfig rarm2 = new PartConfig();
    public PartConfig body2 = new PartConfig();
    public PartConfig lleg2 = new PartConfig();
    public PartConfig rleg2 = new PartConfig();
    public boolean whileStanding = true;
    public boolean whileAttacking = false;
    public boolean whileMoving = false;
    public boolean animate = false;
    public int animationSpeed = 4;
    private int prevTicks = 0;
    private int startTick = 0;
    private float val = 0.0f;
    private float valNext = 0.0f;

    public JobPuppet(@Nonnull EntityNPCInterface npcIn) {
        super(npcIn);
        npc = npcIn;
        type = JobType.PUPPET;
    }

    @Override
    public IJobPuppet.IJobPuppetPart getPart(int part) {
        switch (part) {
            case 0: return head;
            case 1: return larm;
            case 2: return rarm;
            case 3: return body;
            case 4: return lleg;
            case 5: return rleg;
            case 6: return head2;
            case 7: return larm2;
            case 8: return rarm2;
            case 9: return body2;
            case 10: return lleg2;
            case 11: return rleg2;
            default: throw new CustomNPCsException("Unknown part " + part);
        }
    }

    @Override
    public NBTTagCompound save(NBTTagCompound compound) {
        compound.setTag("PuppetHead", head.save());
        compound.setTag("PuppetLArm", larm.save());
        compound.setTag("PuppetRArm", rarm.save());
        compound.setTag("PuppetBody", body.save());
        compound.setTag("PuppetLLeg", lleg.save());
        compound.setTag("PuppetRLeg", rleg.save());
        compound.setTag("PuppetHead2", head2.save());
        compound.setTag("PuppetLArm2", larm2.save());
        compound.setTag("PuppetRArm2", rarm2.save());
        compound.setTag("PuppetBody2", body2.save());
        compound.setTag("PuppetLLeg2", lleg2.save());
        compound.setTag("PuppetRLeg2", rleg2.save());
        compound.setBoolean("PuppetStanding", whileStanding);
        compound.setBoolean("PuppetAttacking", whileAttacking);
        compound.setBoolean("PuppetMoving", whileMoving);
        compound.setBoolean("PuppetAnimate", animate);
        compound.setInteger("PuppetAnimationSpeed", animationSpeed);
        return compound;
    }

    @Override
    public void load(NBTTagCompound compound) {
        head.load(compound.getCompoundTag("PuppetHead"));
        larm.load(compound.getCompoundTag("PuppetLArm"));
        rarm.load(compound.getCompoundTag("PuppetRArm"));
        body.load(compound.getCompoundTag("PuppetBody"));
        lleg.load(compound.getCompoundTag("PuppetLLeg"));
        rleg.load(compound.getCompoundTag("PuppetRLeg"));
        head2.load(compound.getCompoundTag("PuppetHead2"));
        larm2.load(compound.getCompoundTag("PuppetLArm2"));
        rarm2.load(compound.getCompoundTag("PuppetRArm2"));
        body2.load(compound.getCompoundTag("PuppetBody2"));
        lleg2.load(compound.getCompoundTag("PuppetLLeg2"));
        rleg2.load(compound.getCompoundTag("PuppetRLeg2"));
        whileStanding = compound.getBoolean("PuppetStanding");
        whileAttacking = compound.getBoolean("PuppetAttacking");
        whileMoving = compound.getBoolean("PuppetMoving");
        setIsAnimated(compound.getBoolean("PuppetAnimate"));
        setAnimationSpeed(compound.getInteger("PuppetAnimationSpeed"));
    }

    private float calcRotation(float r, float r2, float partialTicks) {
        if (!animate) {
            return r;
        }
        if (prevTicks != npc.ticksExisted) {
            float speed;
            switch (animationSpeed) {
                case 0: speed = 40.0F; break;
                case 1: speed = 24.0F; break;
                case 2: speed = 13.0F; break;
                case 3: speed = 10.0F; break;
                case 4: speed = 7.0F; break;
                case 5: speed = 4.0F; break;
                case 6: speed = 3.0F; break;
                case 7: speed = 2.0F; break;
                default: speed = 0.0F; break;
            }
            float ticks = (float) npc.ticksExisted - (float) startTick;
            val = 1.0f - (MathHelper.cos(ticks / speed * (float) Math.PI / 2.0f) + 1.0f) / 2.0f;
            valNext = 1.0f - (MathHelper.cos((ticks + 1) / speed * (float) Math.PI / 2.0f) + 1.0f) / 2.0f;
            prevTicks = npc.ticksExisted;
        }
        float result = r - r2;
        if (Math.abs(result) > 1.0f) {
            r2 = 2.0f * (result < 0.0f ? -1.0f : 1.0f) + r2;
        }
        return r + (r2 - r) * (val + (valNext - val) * partialTicks);
    }

    public float getRotationX(PartConfig part1, PartConfig part2, float partialTicks) {
        return calcRotation(part1.rotationX, part2.rotationX, partialTicks);
    }

    public float getRotationY(PartConfig part1, PartConfig part2, float partialTicks) {
        return calcRotation(part1.rotationY, part2.rotationY, partialTicks);
    }

    public float getRotationZ(PartConfig part1, PartConfig part2, float partialTicks) {
        return calcRotation(part1.rotationZ, part2.rotationZ, partialTicks);
    }

    @Override
    public void reset() {
        val = 0.0f;
        valNext = 0.0f;
        prevTicks = 0;
        startTick = npc.ticksExisted;
    }

    public boolean isActive() {
        return npc.isEntityAlive() && ((whileAttacking && npc.isAttacking()) || (whileMoving && npc.isWalking()) || (whileStanding && !npc.isWalking()));
    }

    @Override
    public boolean getIsAnimated() { return animate; }

    @Override
    public int getAnimationSpeed() { return animationSpeed; }

    @Override
    public void setAnimationSpeed(int speed) {
        animationSpeed = ValueUtil.correctInt(speed, 0, 7);
        npc.updateClient = true;
    }

    @Override
    public void setIsAnimated(boolean bo) {
        if (npc.advanced.animationType == EnumAnimationType.PUPPET) {
            if (!(animate = bo)) {
                val = 0.0f;
                valNext = 0.0f;
                prevTicks = 0;
            }
            else { startTick = npc.ticksExisted; }
            npc.updateClient = true;
        }
    }

    public class PartConfig implements IJobPuppet.IJobPuppetPart {
        public float rotationX = 0.0f;
        public float rotationY = 0.0f;
        public float rotationZ = 0.0f;
        public boolean disabled = false;

        public NBTTagCompound save() {
            NBTTagCompound compound = new NBTTagCompound();
            compound.setFloat("RotationX", rotationX);
            compound.setFloat("RotationY", rotationY);
            compound.setFloat("RotationZ", rotationZ);
            compound.setBoolean("Disabled", disabled);
            return compound;
        }

        public void load(NBTTagCompound compound) {
            rotationX = ValueUtil.correctFloat(compound.getFloat("RotationX"), -1.0f, 1.0f);
            rotationY = ValueUtil.correctFloat(compound.getFloat("RotationY"), -1.0f, 1.0f);
            rotationZ = ValueUtil.correctFloat(compound.getFloat("RotationZ"), -1.0f, 1.0f);
            disabled = compound.getBoolean("Disabled");
        }

        @Override
        public int getRotationX() { return (int) ((rotationX + 1.0f) * 180.0f); }

        @Override
        public int getRotationY() { return (int) ((rotationY + 1.0f) * 180.0f); }

        @Override
        public int getRotationZ() { return (int) ((rotationZ + 1.0f) * 180.0f); }

        @Override
        public void setRotation(int x, int y, int z) {
            disabled = false;
            rotationX = ValueUtil.correctFloat(x / 180.0f - 1.0f, -1.0f, 1.0f);
            rotationY = ValueUtil.correctFloat(y / 180.0f - 1.0f, -1.0f, 1.0f);
            rotationZ = ValueUtil.correctFloat(z / 180.0f - 1.0f, -1.0f, 1.0f);
            npc.updateClient = true;
        }

    }

}
