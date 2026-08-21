package noppes.npcs.controllers.data;

import java.awt.Point;
import java.awt.Polygon;
import java.util.*;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import noppes.npcs.CustomNpcs;
import noppes.npcs.EventHooks;
import noppes.npcs.api.INbt;
import noppes.npcs.api.IPos;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.event.ForgeEvent;
import noppes.npcs.api.handler.data.IAvailability;
import noppes.npcs.api.handler.data.IBorder;
import noppes.npcs.api.util.IRayTraceRotate;
import noppes.npcs.api.util.IRayTraceVec;
import noppes.npcs.api.wrapper.BlockPosWrapper;
import noppes.npcs.api.wrapper.NBTWrapper;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.BorderController;
import noppes.npcs.controllers.PlayerQuestController;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.controllers.ScriptController;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketBorderData;
import noppes.npcs.util.Util;
import noppes.npcs.util.ValueUtil;

import javax.annotation.Nonnull;

public class Zone3D implements IBorder, Predicate<LivingEntity> {

    protected final List<LivingEntity> entitiesWithinRegion = new ArrayList<>();
    protected final List<float[][]> triangleList = new ArrayList<>();
    protected final List<float[]> contourLines = new ArrayList<>();
    protected IPos homePos = BlockPosWrapper.ZERO;
    protected int id = -1;
    protected boolean update;

    public final TreeMap<Integer, Point> points = new TreeMap<>();
    public ResourceLocation dimension = new ResourceLocation("minecraft", "overworld");
    public String name = "Default Region";
    public int[] y = new int[] { 0, 255 };

    public int color;
    public Availability availability;
    public String message; // kick message
    public int questID;
    public boolean questWhenEnter = true;
    public boolean keepOut;
    public boolean showInClient;
    public CompoundTag addData = new CompoundTag();

    public Zone3D() {
        color = (new Random()).nextInt(0xFFFFFF);
        availability = new Availability();
        message = "availability.areaNotAvailable";
        keepOut = false;
        showInClient = false;
        update = true;
    }

    public Zone3D(int id, String dimID, int posX, int posY, int posZ) {
        this();
        this.id = id;
        dimension = new ResourceLocation(dimID);
        y[0] = ValueUtil.correctInt(posY - 1, 0, 255);
        y[1] = ValueUtil.correctInt(posY + 4, 0, 255);
        points.put(0, new Point(posX, posZ - 4));
        points.put(1, new Point(posX + 4, posZ + 2));
        points.put(2, new Point(posX - 4, posZ + 2));
        homePos = getCenter();
        update = true;
    }

    /**
     * Adds a new point to the end
     *
     * @param position - block pos
     */
    public Point addPoint(BlockPos position) { return addPoint(position.getX(), position.getY(), position.getZ()); }

    /**
     * Adds a new point to the end
     *
     * @param x - x pos
     * @param y - y pos
     * @param z - z pos
     */
    @Override
    public Point addPoint(int x, int y, int z) {
        Point point = new Point();
        point.x = x;
        point.y = z;
        return addPoint(point, y);
    }

    @Override
    public Point addPoint(IPos position) { return addPoint(position.getMCBlockPos()); }

    /**
     * Adds a new point to the end
     *
     * @param point - pos
     * @param posY - height
     */
    @Override
    public Point addPoint(Point point, int posY) {
        for (Point p : points.values()) {
            if (p.x == point.x && p.y == point.y) { return null; }
        }
        points.put(points.size(), point);
        posY = ValueUtil.correctInt(posY, 0, 255);
        if (posY < y[0]) { y[0] = posY; }
        else if (posY > y[1]) { y[1] = posY; }
        update = true;
        return point;
    }

    @Override
    public boolean test(LivingEntity entity) {
        if (entity == null || !entity.isAlive()) { return false; }
        return contains(entity.getX(), entity.getY(), entity.getZ(), entity.getBbHeight());
    }

    /**
     * Offsets the position of the zone
     *
     * @param position - block pos
     * @param type = 0 - relative to the zero coordinate; 1 - relative to the center of the described contour; 2 - relative to the center of mass
     */
    public void centerOffsetTo(BlockPos position, boolean type) {
        centerOffsetTo(position.getX(), position.getY(), position.getZ(), type);
    }

    /**
     * Offsets the position of the zone
     *
     * @param posX - x pos
     * @param posY - y pos
     * @param posZ - z pos
     * @param type - 0 - relative to the zero coordinate, 1 - relative to the center of
     *            the described contour;
     */
    @Override
    public void centerOffsetTo(int posX, int posY, int posZ, boolean type) {
        IPos ctr;
        int ry = (y[1] - y[0]) / 2;
        if (type) {
            ctr = getCenter();
            y[0] = posY - ry;
            y[1] = posY + ry;
        } else {
            ctr = Objects.requireNonNull(NpcAPI.Instance()).getIPos(getMinX(), y[0], getMinZ());
            y[0] = posY;
            y[1] = posY + ry * 2;
        }
        for (int key : points.keySet()) {
            Point p = points.get(key);
            points.get(key).move(posX + p.x - (int) ctr.getX(), posZ + p.y - (int) ctr.getZ());
        }
        update = true;
    }

    @Override
    public void centerOffsetTo(IPos position, boolean type) {
        centerOffsetTo(position.getMCBlockPos(), type);
    }

    /**
     * Offsets the position of the zone
     *
     * @param point - pos
     * @param type = 0 - relative to the zero coordinate; 1 - relative to the center of the described contour; 2 - relative to the center of mass
     */
    @Override
    public void centerOffsetTo(Point point, boolean type) {
        centerOffsetTo(point.x, (y[0] + y[1]) / 2, point.y, type);
    }

    @Override
    public void clear() {
        points.clear();
        availability.clear();
        entitiesWithinRegion.clear();
        y[0] = 255;
        y[1] = 0;
        message = "availability.areaNotAvailable";
        keepOut = false;
        showInClient = false;
        update = true;
    }

    public boolean contains(Entity entity) {
        return entity.level().dimension().location().equals(dimension) && contains(entity.getX(), entity.getY(), entity.getZ(), entity.getBbHeight());
    }

    @Override
    public boolean contains(IEntity<?> entity) {
        return entity.getWorld().getMCLevel().dimension().location().equals(dimension) && contains(entity.getX(), entity.getY(), entity.getZ(), entity.getHeight());
    }

    @Override
    public boolean contains(double posX, double posY, double posZ, double height) {
        if (posY + height < y[0] || posY - height > y[1]) { return false; }
        int dx = (int) (posX * 10.0d);
        int dz = (int) (posZ * 10.0d);
        Polygon poly = new Polygon();
        boolean isIn = false;
        for (Point p : points.values()) {
            int px = 5 + (p.x * 10);
            int py = 5 + (p.y * 10);
            poly.addPoint(px, py);
            isIn = (px == dx && py == dz);
            if (isIn) { break; }
        }
        if (isIn) { return true; }
        isIn = poly.contains(dx, dz);
        return isIn;
    }

    @Override
    public boolean contains(int posX, int posZ) {
        for (Point p : points.values()) {
            if (p.x == posX && p.y == posZ) { return true; }
        }
        return false;
    }

    @Override
    public double distanceTo(double posX, double posZ) {
        IPos pos = getCenter();
        return Util.instance.distanceTo(pos.getX() + 0.5d, 0.0d, pos.getZ() + 0.5d, posX, 0.0d, posZ);
    }

    public double distanceTo(Entity entity) {
        if (entity == null) {
            return -1;
        }
        IPos c = getCenter();
        return Util.instance.distanceTo(entity.getX(), entity.getY(), entity.getZ(), c.getX() + 0.5d, c.getY() + 0.5d, c.getZ() + 0.5d);
    }

    @Override
    public double distanceTo(IEntity<?> entity) {
        return distanceTo(entity.getMCEntity());
    }

    public boolean equals(Zone3D zone) {
        if (zone == null) {
            return false;
        }
        if (zone.y[0] != y[0] || zone.y[1] != y[1]) {
            return false;
        }
        if (zone.points.size() != points.size()) {
            return false;
        }
        for (int key : zone.points.keySet()) {
            if (!points.containsKey(key)) {
                return false;
            }
            Point p0 = zone.points.get(key);
            Point p1 = points.get(key);
            if (p0.x != p1.x || p0.y != p1.y) {
                return false;
            }
        }
        return true;
    }

    /**
     * orders positions
     */
    public void fix() {
        TreeMap<Integer, Point> newPoints = new TreeMap<>();
        int i = 0;
        boolean needChange = false;
        for (int pos : points.keySet()) {
            newPoints.put(i, points.get(pos));
            if (i != pos) {
                needChange = true;
            }
            i++;
        }
        if (needChange) {
            points.clear();
            points.putAll(newPoints);
        }
        getHomePos();
        triangleList.clear();
        List<Point> allPoints = new ArrayList<>(points.values());
        boolean forward;
        Point p0, p1;
        while (allPoints.size() > 2) {
            if (allPoints.size() == 3) {
                triangleList.add(getTriangles(allPoints.get(0), allPoints.get(1), allPoints.get(2)));
                break;
            }
            p0 = allPoints.get(0);
            p1 = allPoints.get(2);
            double x = (double) (p0.x + p1.x) / 2.0d;
            double z = (double) (p0.y + p1.y) / 2.0d;
            forward = !contains(x, getMinY(), z, 0);
            if (forward) {
                p0 = allPoints.get(0);
                triangleList.add(getTriangles(allPoints.get(1), p0, allPoints.get(allPoints.size() - 1)));
                Collections.swap(allPoints, 0, allPoints.size() - 1);
                allPoints.remove(p0);
            } else {
                p0 = allPoints.get(1);
                triangleList.add(getTriangles(allPoints.get(0), p0, allPoints.get(2)));
                allPoints.remove(p0);
            }
        }
        contourLines.clear();
        for (float[][] tri : triangleList) {
            for (i = 0; i < 3; i++) {
                float f0 = tri[i][0];
                float f1 = tri[i][1];
                float f2 = tri[i == 2 ? 0 : i + 1][0];
                float f3 = tri[i == 2 ? 0 : i + 1][1];
                boolean has = false;
                for (float[] cl : contourLines) {
                    if ((cl[0] == f0 && cl[1] == f1 && cl[2] == f2 && cl[3] == f3) ||
                            (cl[0] == f2 && cl[1] == f3 && cl[2] == f0 && cl[3] == f1)) {
                        has = true;
                        break;
                    }
                }
                if (!has) { contourLines.add(new float[] { f0, f1, f2, f3 }); }
            }
        }
    }

    private float[][] getTriangles(Point p0, Point p1, Point p2) {
        float[][] fls = new float[3][2];
        fls[0][0] = p0.x;
        fls[0][1] = p0.y;
        fls[1][0] = p1.x;
        fls[1][1] = p1.y;
        fls[2][0] = p2.x;
        fls[2][1] = p2.y;
        return fls;
    }

    public List<float[][]> getTriangleList() { return new ArrayList<>(triangleList); }

    public List<float[]> getContourLines() { return new ArrayList<>(contourLines); }

    @Override
    public IAvailability getAvailability() { return availability; }

    /**
     * @return center of mass of the zone
     */
    @Override
    public IPos getCenter() {
        double x = 0.0d, z = 0.0d;
        for (Point v : points.values()) {
            x += v.x;
            z += v.y;
        }
        if (!points.isEmpty()) {
            x /= points.size();
            z /= points.size();
        }
        Level level = null;
        if (CustomNpcs.Server != null) { level = CustomNpcs.Server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension)); }
        return new BlockPosWrapper(level, x, (double) y[0] + ((double) y[1] - (double) y[0]) / 2.0d, z);
    }

    @Override
    public int getClosestPoint(Point point, IPos pos) { return getClosestPoint(point, pos.getX(), pos.getZ()); }

    public int getClosestPoint(Point point, double x, double z) {
        if (points.isEmpty()) { return -1; }
        if (points.size() == 1) { return 0; }
        int n = 0;
        double dm0 = points.get(0).distance(point);
        double dm1 = points.get(1).distance(point);
        double dm2 = points.get(0).distance(x, z);
        double dm3 = points.get(1).distance(x, z);
        for (int p = 0; (p + 1) < points.size(); p++) {
            double d0 = points.get(p).distance(point);
            double d1 = points.get(p + 1).distance(point);
            double d2 = points.get(p).distance(x, z);
            double d3 = points.get(p + 1).distance(x, z);
            if (dm0 + dm1 + dm2 + dm3 > d0 + d1 + d2 + d3) {
                dm0 = d0;
                dm1 = d1;
                dm2 = d2;
                dm3 = d3;
                n = p;
            }
        }
        double d0 = points.get(0).distance(point);
        double d1 = points.get(points.size() - 1).distance(point);
        double d2 = points.get(0).distance(x, z);
        double d3 = points.get(points.size() - 1).distance(x, z);
        if (dm0 + dm1 + dm2 + dm3 > d0 + d1 + d2 + d3) { n = points.size() - 1; }
        return n;
    }

    @Override
    public Point[] getClosestPoints(Point point, IPos pos) { return getClosestPoints(point, pos.getX(), pos.getZ()); }

    public Point[] getClosestPoints(Point point, double x, double z) {
        Point[] ps = new Point[2];
        ps[0] = null;
        ps[1] = null;
        int n = getClosestPoint(point, x, z);
        if (points.containsKey(n)) { ps[0] = points.get(n); }
        if (points.containsKey(n + 1)) { ps[1] = points.get(n + 1); }
        else if (n == points.size() - 1) { ps[1] = points.get(0); }
        return ps;
    }

    private @Nonnull Point[] getClosestWall(double x, double z) {
        double minDistance = Double.MAX_VALUE;
        Point[] closestEdge = new Point[2];
        closestEdge[0] = points.containsKey(0) ? points.get(0) : new Point(0, 0);
        closestEdge[1] = points.containsKey(1) ? points.get(1) : closestEdge[0];
        for (int i = 0; i < points.size(); i++) {
            Point a = points.get(i);
            Point b = points.get(i < points.size() - 1 ? i + 1 : 0);
            double distance = calculateDistanceToSegment(x, z, a, b);
            if (distance < minDistance) {
                minDistance = distance;
                closestEdge[0] = a;
                closestEdge[1] = b;
            }
        }
        return closestEdge;
    }

    private double calculateDistanceToSegment(double x, double z, Point A, Point B) {
        x *= 10.0d;
        z *= 10.0d;
        double x1 = A.getX() * 10 + 5;
        double y1 = A.getY() * 10 + 5;
        double x2 = B.getX() * 10 + 5;
        double y2 = B.getY() * 10 + 5;
        double numerator = Math.abs((y2 - y1) * x - (x2 - x1) * z + x2 * y1 - y2 * x1);
        double denominator = Math.sqrt((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1));
        return numerator / denominator;
    }

    @Override
    public int getColor() {
        return color;
    }

    @Override
    public String getDimension() {
        return dimension.toString();
    }

    public int getHeight() {
        return y[1] - y[0];
    }

    @Override
    public IPos getHomePos() {
        if (homePos == null ||
                homePos.getMCBlockPos().equals(BlockPosWrapper.ZERO.getMCBlockPos()) ||
                keepOut == contains(homePos.getX() + 0.5d, homePos.getY() + 0.5d, homePos.getZ() + 0.5d, 0.0d)) {
            homePos = getCenter();
            if (keepOut && !points.isEmpty()) {
                for (int i = 0; i < 4; i++) {
                    int x = points.get(0).x, z = points.get(0).y;
                    switch (i) {
                        case 1: {
                            x--;
                            break;
                        }
                        case 2: {
                            z++;
                            break;
                        }
                        case 3: {
                            z--;
                            break;
                        }
                        default: {
                            x++;
                        }
                    }
                    if (!contains(x, z)) {
                        homePos = Objects.requireNonNull(NpcAPI.Instance()).getIPos(x, y[0] + (double) (y[1] - y[0]) / 2, z);
                    }
                }
            }
        }
        return homePos;
    }

    @Override
    public int getId() {
        return id;
    }

    public int getIdNearestPoint(BlockPos pos) {
        if (points.isEmpty() || pos == null) { return -1; }
        double min = Double.MAX_VALUE;
        int id = -1;
        for (int i : points.keySet()) {
            double dist = Util.instance.distanceTo(points.get(i).x, 0, points.get(i).y, pos.getX(), 0, pos.getZ());
            if (dist <= min) {
                min = dist;
                id = i;
            }
        }
        return id;
    }

    @Override
    public int getMaxX() {
        if (points.isEmpty()) {
            return 0;
        }
        int value = points.get(0).x;
        for (Point v : points.values()) {
            if (value < v.x) {
                value = v.x;
            }
        }
        return value;
    }

    @Override
    public int getMaxY() {
        return Math.max(y[0], y[1]);
    }

    @Override
    public int getMaxZ() {
        if (points.isEmpty()) {
            return 0;
        }
        int value = points.get(0).y;
        for (Point v : points.values()) {
            if (value < v.y) {
                value = v.y;
            }
        }
        return value;
    }

    @Override
    public String getMessage() { return message; }

    @Override
    public int getQuestID() { return questID; }

    @Override
    public void setQuestID(int id) { questID = ValueUtil.correctInt(id, 0, Integer.MAX_VALUE); }

    @Override
    public boolean isQuestWhenEnter() { return questWhenEnter; }

    @Override
    public void setIsQuestWhenEnter(boolean bo) { questWhenEnter = bo; }

    @Override
    public int getMinX() {
        if (points.isEmpty()) {
            return 0;
        }
        int value = points.get(0).x;
        for (Point v : points.values()) {
            if (value > v.x) {
                value = v.x;
            }
        }
        return value;
    }

    @Override
    public int getMinY() {
        return Math.min(y[0], y[1]);
    }

    @Override
    public int getMinZ() {
        if (points.isEmpty()) {
            return 0;
        }
        int value = points.get(0).y;
        for (Point v : points.values()) {
            if (value > v.y) {
                value = v.y;
            }
        }
        return value;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public INbt getNbt() {
        CompoundTag nbtRegion = new CompoundTag();
        load(nbtRegion);
        return new NBTWrapper(nbtRegion);
    }

    @Override
    public Point[] getPoints() {
        return points.values().toArray(new Point[0]);
    }

    /**
     * @return size X x Y x Z
     */
    public String getSize() {
        return (getMaxX() - getMinX()) + "x" + getHeight() + "x" + (getMaxZ() - getMinZ());
    }
    /**
     * Adds a new point between two existing ones (through the smallest lengths)
     *
     * @param posX - x pos
     * @param posY - y pos
     * @param posZ - z pos
     */
    @Override
    public boolean insertPoint(int posX, int posY, int posZ, IPos pos) {
        Point p = new Point();
        p.x = posX;
        p.y = posZ;
        return insertPoint(p, posY, pos);
    }

    /**
     * Adds a new point between two existing ones (through the smallest lengths)
     *
     * @param pos0 - start block pos
     * @param pos1 - end block pos
     */
    @Override
    public boolean insertPoint(IPos pos0, IPos pos1) {
        return insertPoint((int) pos0.getX(), (int) pos0.getY(), (int) pos0.getZ(), pos1);
    }

    @Override
    public boolean insertPoint(Point point, int posY, IPos pos) {
        posY = ValueUtil.correctInt(posY, 0, 255);
        if (posY < y[0]) { y[0] = posY; }
        if (posY > y[1]) { y[1] = posY; }
        if (contains(point.x, point.y)) { return false; }
        if (points.size() < 2) { return addPoint(point, posY) != null; }
        int n = getClosestPoint(point, pos);
        TreeMap<Integer, Point> newPoints = new TreeMap<>();
        for (int i = 0, j = 0; i < points.size(); i++) {
            newPoints.put(i + j, points.get(i));
            if (i == n) {
                j = 1;
                newPoints.put(i + j, point);
            }
        }
        if (newPoints.size() != points.size()) {
            points.clear();
            points.putAll(newPoints);
            update = true;
        }
        return update;
    }

    @Override
    public boolean isShowToPlayers() {
        return showInClient;
    }

    private ServerPlayer convertToPlayer(Entity entity) {
        if (entity instanceof ServerPlayer) { return (ServerPlayer) entity; }
        else if (entity instanceof ThrownEnderpearl pearl && pearl.getOwner() instanceof ServerPlayer player) { return player; }
        return null;
    }

    /**
     * Offsets the entire zone by the specified value
     *
     * @param position - block pos
     */
    public void offset(BlockPos position) {
        offset(position.getX(), position.getY(), position.getZ());
    }

    /**
     * (y[1]+y[0])/2 Offsets the entire zone by the specified value
     *
     * @param posX - x pos
     * @param posY - y pos
     * @param posZ - z pos
     */
    @Override
    public void offset(int posX, int posY, int posZ) {
        y[0] = ValueUtil.correctInt(y[0] + posY, 0, 255);
        y[1] = ValueUtil.correctInt(y[1] + posY, 0, 255);
        for (int key : points.keySet()) {
            Point p = points.get(key);
            points.get(key).move(p.x + posX, p.y + posZ);
        }
        update = true;
    }

    @Override
    public void offset(IPos pos) {
        offset(pos.getMCBlockPos());
    }

    /**
     * Offsets the entire zone by the specified value
     *
     * @param point - pos
     */
    @Override
    public void offset(Point point) {
        offset(point.x, 0, point.y);
    }

    public void load(CompoundTag nbtRegion) {
        id = nbtRegion.getInt("ID");
        name = nbtRegion.getString("Name");
        dimension = new ResourceLocation(nbtRegion.getString("DimensionID"));
        color = nbtRegion.getInt("Color");

        int[] sy = nbtRegion.getIntArray("AxisY");
        if (sy.length > 0) { y[0] = ValueUtil.correctInt(sy[0], 0, 255); }
        if (sy.length > 1) { y[1] = ValueUtil.correctInt(sy[1], 0, 255); }

        points.clear();
        for (int i = 0; i < nbtRegion.getList("Points", 11).size(); i++) {
            int[] p = nbtRegion.getList("Points", 11).getIntArray(i);
            points.put(i, new Point(p[0], p[1]));
        }
        availability.load(nbtRegion.getCompound("Availability"));
        message = nbtRegion.getString("Message");

        questID = nbtRegion.getInt("QuestID");
        questWhenEnter = nbtRegion.getBoolean("QuestWhenEnter");

        if (nbtRegion.contains("HomePos", 4)) {
            BlockPos pos = BlockPos.of(nbtRegion.getLong("HomePos"));
            setHomePos(pos.getX(), pos.getY(), pos.getZ());
        } else if (nbtRegion.contains("HomePos", 11)) { // old
            int[] pos = nbtRegion.getIntArray("HomePos");
            setHomePos(pos[0], pos[1], pos[2]);
        }
        keepOut = nbtRegion.getBoolean("IsKeepOut");
        showInClient = nbtRegion.getBoolean("ShowInClient");

        addData = nbtRegion.getCompound("AddData");
        fix();
        update = false;
        entitiesWithinRegion.clear();
    }

    /**
     * Remove point from polygon
     *
     * @param x - x pos
     * @param z - z pos
     */
    @Override
    public boolean removePoint(int x, int z) {
        if (points.size() <= 1) { return false; }
        for (int key : points.keySet()) {
            if (points.get(key).x == x && points.get(key).y == z) {
                points.remove(key);
                fix();
                update = true;
                return true;
            }
        }
        return false;
    }

    /**
     * Remove point from polygon
     *
     * @param point - pos
     */
    @Override
    public boolean removePoint(Point point) {
        if (point == null || points.size() <= 1) { return false; }
        return removePoint(point.x, point.y);
    }

    /**
     * Expand or Shrink a zone outline by a specific value
     *
     * @param radius - distance
     * @param type - type
     */
    @Override
    public void scaling(double radius, boolean type) {
        if (points.isEmpty()) { return; }
        y[0] = ValueUtil.correctInt(y[0] - (int) radius, 0, 255);
        y[1] = ValueUtil.correctInt(y[0] + (int) radius, 0, 255);
        IPos pos;
        if (type) { pos = getCenter(); }
        else { pos = Objects.requireNonNull(NpcAPI.Instance()).getIPos(getMinX(), y[0], getMinZ()); }
        for (int id : points.keySet()) {
            Point v = points.get(id);
            IRayTraceRotate d = Util.instance.getAngles3D(pos.getX(), 0, pos.getZ(), v.x, 0, v.y);
            IRayTraceVec p = Util.instance.getPosition(pos.getX(), 0, pos.getZ(), d.getYaw(), d.getPitch(), radius + d.getRadiusXZ());
            points.put(id, new Point((int) p.getX(), (int) p.getZ()));
        }
        update = true;
    }

    /**
     * Scale zone outline
     *
     * @param scale - percentage where 100% = 1.0f
     * @param type - type
     */
    @Override
    public void scaling(float scale, boolean type) {
        if (points.isEmpty()) { return; }
        IPos pos;
        if (type) { pos = getCenter(); }
        else { pos = Objects.requireNonNull(NpcAPI.Instance()).getIPos(getMinX(), y[0], getMinZ()); }
        for (int key : points.keySet()) {
            Point v = points.get(key);
            IRayTraceRotate d = Util.instance.getAngles3D(pos.getX(), pos.getY(), pos.getZ(), v.x, pos.getY(), v.y);
            IRayTraceVec p = Util.instance.getPosition(pos.getX(), pos.getY(), pos.getZ(), d.getYaw(), d.getPitch(), (double) scale * d.getRadiusXZ());
            points.put(key, new Point((int) p.getX(), (int) p.getZ()));
            if (y[0] > (int) p.getY()) { y[0] = ValueUtil.correctInt((int) p.getY(), 0, 255); }
            if (y[1] < (int) p.getY()) { y[1] = ValueUtil.correctInt((int) p.getY(), 0, 255); }
        }
        update = true;
    }

    @Override
    public void setColor(int color) {
        this.color = color;
        update = true;
    }

    @Override
    public void setDimensionId(String dimensionId) {
        dimension = new ResourceLocation(dimensionId);
        update = true;
    }

    @Override
    public void setHomePos(int x, int y, int z) {
        if (homePos == null || keepOut != contains(x + 0.5d, y, z + 0.5d, 0.0d)) {
            return;
        }
        homePos = Objects.requireNonNull(NpcAPI.Instance()).getIPos(x, y, z);
        update = true;
    }

    @Override
    public void setMessage(String message) {
        this.message = message == null ? "" : message;
        update = true;
    }

    @Override
    public void setName(String name) {
        if (name == null || name.isEmpty()) {
            name = "Default Region";
        }
        this.name = name;
        update = true;
    }

    @Override
    public void setNbt(INbt nbt) {
        save(nbt.getMCNBT());
        update = true;
    }

    /**
     * Sets a point instead of an existing one
     *
     * @param index - index
     * @param position - new block pos
     */
    public Point setPoint(int index, BlockPos position) {
        return setPoint(index, position.getX(), position.getY(), position.getZ());
    }

    /**
     * Sets a point instead of an existing one
     *
     * @param index - index
     * @param x - new x pos
     * @param y - new y pos
     * @param z - new z pos
     */
    @Override
    public Point setPoint(int index, int x, int y, int z) {
        Point point = new Point();
        point.x = x;
        point.y = z;
        update = true;
        return setPoint(index, point, y);
    }

    @Override
    public Point setPoint(int index, IPos position) {
        return setPoint(index, position.getMCBlockPos());
    }

    /**
     * Sets a point instead of an existing one
     *
     * @param index - index
     * @param point - new pos
     */
    @Override
    public Point setPoint(int index, Point point) {
        if (!points.containsKey(index) || index > points.size()) { return null; }
        points.put(index, point);
        update = true;
        return point;
    }

    /**
     * Sets a point instead of an existing one
     *
     * @param index - index
     * @param posY - new height
     * @param point - new pos
     */
    @Override
    public Point setPoint(int index, Point point, int posY) {
        if (!points.containsKey(index) || index > points.size()) { return null; }
        points.put(index, point);
        posY = ValueUtil.correctInt(posY, 0, 255);
        if (posY < y[0]) { y[0] = posY; }
        if (posY > y[1]) { y[1] = posY; }
        update = true;
        return point;
    }

    @Override
    public void setShowToPlayers(boolean show) {
        showInClient = show;
        update = true;
    }

    /**
     * @return number of vertices of a zone
     */
    @Override
    public int size() {
        return points.size();
    }

    @Override
    public String toString() {
        return "ID:" + id + "; name: \"" + name + "\"";
    }

    @Override
    public void update() { update = true; }

    public void update(ServerLevel level) {
        if (update) {
            BorderController.getInstance().update(id);
            Packets.sendAll(new PacketBorderData(save(new CompoundTag())));
            update = false;
        }
        if (!points.isEmpty() && dimension.equals(level.dimension().location())) {
            List<LivingEntity> entitiesInside = level.getEntitiesOfClass(LivingEntity.class, getAxisAlignedBB().inflate(1.0d), this);
            for (LivingEntity entity : entitiesInside) {
                if (!entitiesWithinRegion.contains(entity) && canEntityEnter(entity)) {
                    entitiesWithinRegion.add(entity);
                    if (questID > 0 && questWhenEnter && entity instanceof Player player) {
                        Quest quest = QuestController.instance.get(questID);
                        if (quest != null) { PlayerQuestController.addActiveQuest(quest, player, false); }
                    }
                }
            }
            for (LivingEntity entity : new ArrayList<>(entitiesWithinRegion)) {
                if (!entitiesInside.contains(entity) && canEntityLeave(entity)) {
                    entitiesWithinRegion.remove(entity);
                    if (questID > 0 && !questWhenEnter && entity instanceof Player player) {
                        Quest quest = QuestController.instance.get(questID);
                        if (quest != null) { PlayerQuestController.addActiveQuest(quest, player, false); }
                    }
                }
            }
        }
    }

    private boolean canEntityEnter(Entity entity) {
        ForgeEvent.EnterToRegion event = new ForgeEvent.EnterToRegion(entity, this);
        EventHooks.onEvent(ScriptController.Instance.playerScripts, EnumScriptType.REGION_ENTER, event);
        if (entity.isRemoved()) { return false; }
        if (!MinecraftForge.EVENT_BUS.post(event) && !event.isCanceled()) {
            ServerPlayer player = convertToPlayer(entity);
            if (player == null || player.isCreative() || availability.isAvailable(player) || !keepOut) { return true; }
            motionPlayer(player);
            if (entity instanceof ThrownEnderpearl) { entity.remove(Entity.RemovalReason.DISCARDED); }
        }
        return false;
    }

    private boolean canEntityLeave(Entity entity) {
        ForgeEvent.LeaveRegion event = new ForgeEvent.LeaveRegion(entity, this);
        EventHooks.onEvent(ScriptController.Instance.playerScripts, EnumScriptType.REGION_LEAVE, event);
        if (entity.isRemoved()) { return true; }
        if (!MinecraftForge.EVENT_BUS.post(event) && !event.isCanceled()) {
            ServerPlayer player = convertToPlayer(entity);
            if (player == null || availability.isAvailable(player) || player.isCreative() || keepOut) { return true; }
            motionPlayer(player);
            if (entity instanceof ThrownEnderpearl) { entity.remove(Entity.RemovalReason.DISCARDED); }
        }
        return false;
    }

    private void motionPlayer(ServerPlayer player) {
        double impulse = player.getDeltaMovement().length();
        Vec3 vec = null;
        boolean isDown = player.getY() < getMinY();
        if (isDown) {
            if (keepOut) { vec = new Vec3(0.0d, -2.0d * impulse, 0.0d); }
        }
        else if (player.getY() > getMaxY()) {
            if (!keepOut) { vec = new Vec3(0.0d, -2.0d * impulse, 0.0d); }
        }
        else {
            Point[] pts = getClosestWall(player.getX(), player.getZ());
            Vec3 wallVectorXZ = new Vec3(pts[1].getX(), 0, pts[1].getY()).subtract(new Vec3(pts[0].getX(), 0, pts[0].getY()));
            vec = new Vec3(wallVectorXZ.z, 0, -wallVectorXZ.x);
            boolean bo = contains(vec.x, getMinY(), vec.z, 0.0d);
            if (bo == keepOut) { vec = vec.multiply(-1.0d, 0.0, -1.0d); }
            vec = vec.normalize().multiply(-impulse, 0.0, -impulse);
        }
        if (vec != null) {
            player.setDeltaMovement(vec);
            player.hurtMarked = true;
        }
        else { player.teleportTo(getHomePos().getX() + 0.5d, getHomePos().getY(), getHomePos().getZ() + 0.5d); }
        if (!message.isEmpty()) { player.sendSystemMessage(Component.translatable(message), true); }
    }

    public CompoundTag save(CompoundTag nbtRegion) {
        nbtRegion.putInt("ID", id);
        nbtRegion.putString("Name", name);
        nbtRegion.putString("DimensionID", dimension.toString());
        nbtRegion.putInt("Color", color);
        ListTag ps = new ListTag();
        for (int pos : points.keySet()) {
            ps.add(new IntArrayTag(new int[] { points.get(pos).x, points.get(pos).y }));
        }
        nbtRegion.put("Points", ps);
        nbtRegion.putIntArray("AxisY", y);
        nbtRegion.put("Availability", availability.save(new CompoundTag()));
        nbtRegion.putString("Message", message);

        nbtRegion.putInt("QuestID", questID);
        nbtRegion.putBoolean("QuestWhenEnter", questWhenEnter);

        if (homePos == null) { homePos = getCenter(); }
        nbtRegion.putLong("HomePos", homePos.getMCBlockPos().asLong());
        nbtRegion.putBoolean("IsKeepOut", keepOut);
        nbtRegion.putBoolean("ShowInClient", showInClient);
        nbtRegion.put("AddData", addData);

        fix();
        return nbtRegion;
    }

    public AABB getAxisAlignedBB() {
        return new AABB(
                (5.0d + getMinX() * 10.0d) / 10.0d,
                (5.0d + getMinY() * 10.0d) / 10.0d,
                (5.0d + getMinZ() * 10.0d) / 10.0d,
                (5.0d + getMaxX() * 10.0d) / 10.0d,
                (5.0d + getMaxY() * 10.0d) / 10.0d,
                (5.0d + getMaxZ() * 10.0d) / 10.0d);
    }

    @Override
    public Vec3 intersectsWithLine(Vec3 startPos, Vec3 endPos) {
        // create vertices
        double baseY = getMinY();
        double height = getMaxY() - baseY;
        List<Vec3> vertices = new ArrayList<>();
        for (Point point : points.values()) { vertices.add(new Vec3(point.x, baseY, point.y)); }

        // Create the top and bottom base of the prism
        List<Vec3> topVertices = createOffsetVertices(vertices, height);

        // We check the intersection of the ray with the upper and lower faces
        Vec3 topResult = checkIntersection(startPos, endPos, topVertices, baseY);
        Vec3 bottomResult = checkIntersection(startPos, endPos, vertices, baseY);

        // Check intersection with vertical sides
        Vec3 wallResult = null;
        for (int i = 0; i < vertices.size(); i++) {
            Vec3 v1 = vertices.get(i);
            Vec3 v2 = vertices.get((i + 1) % vertices.size());
            Vec3 topV1 = new Vec3(v1.x, v1.y + height, v1.z);
            Vec3 topV2 = new Vec3(v2.x, v2.y + height, v2.z);
            wallResult = checkSegmentIntersection(startPos, endPos, v1, v2);
            if (wallResult != null) { break; }
            wallResult = checkSegmentIntersection(startPos, endPos, topV1, topV2);
            if (wallResult != null) { break; }
        }
        if (wallResult != null) {
            if (topResult == null && bottomResult == null) { return wallResult; }
            double topDist = Double.MAX_VALUE;
            double bottomDist = Double.MAX_VALUE;
            double wallDist = startPos.distanceTo(wallResult);
            if (topResult != null) { topDist = startPos.distanceTo(topResult); }
            if (bottomResult != null) { bottomDist = startPos.distanceTo(bottomResult); }
            if (topDist < wallDist) {
                return topDist < bottomDist ? topResult : bottomResult;
            }
            return bottomDist < wallDist ? bottomResult : wallResult;
        }
        if (topResult == null) { return bottomResult; }
        return topResult;
    }

    private List<Vec3> createOffsetVertices(List<Vec3> originalVertices, double offset) {
        List<Vec3> offsetVertices = new ArrayList<>();
        for (Vec3 vertex : originalVertices) { offsetVertices.add(new Vec3(vertex.x, vertex.y + offset, vertex.z)); }
        return offsetVertices;
    }

    private static Vec3 checkIntersection(Vec3 start, Vec3 end, List<Vec3> polygon, double baseY) {
        // Ray-Polygon Intersection Check Algorithm
        // Use Mo's algorithm to determine ray-polygon intersection
        int crossings = 0;
        for (int i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
            Vec3 edgeStart = polygon.get(i);
            Vec3 edgeEnd = polygon.get(j);
            if ((edgeStart.y > start.y) != (edgeEnd.y > start.y) && start.x < (edgeEnd.x - edgeStart.x) * (start.y - edgeStart.y) / (edgeEnd.y - edgeStart.y) + edgeStart.x) {
                crossings++;
            }
        }
        return (crossings % 2 == 1) ? new Vec3((end.x - start.x) / (end.y - start.y) * (baseY - start.y) + start.x, baseY, (end.z - start.z) / (end.y - start.y) * (baseY - start.y) + start.z) : null;
    }

    private Vec3 checkSegmentIntersection(Vec3 startPos, Vec3 endPos, Vec3 segmentStart, Vec3 segmentEnd) {
        //Algorithm for checking the intersection of two segments
        double denominator = (segmentEnd.z - segmentStart.z) * (endPos.x - startPos.x) - (segmentEnd.x - segmentStart.x) * (endPos.z - startPos.z);
        if (denominator == 0) { return null; } // Parallel lines
        double ua = ((segmentEnd.x - segmentStart.x) * (startPos.z - segmentStart.z) - (segmentEnd.z - segmentStart.z) * (startPos.x - segmentStart.x)) / denominator;
        double ub = ((endPos.x - startPos.x) * (startPos.z - segmentStart.z) - (endPos.z - startPos.z) * (startPos.x - segmentStart.x)) / denominator;
        if (ua >= 0 && ua <= 1 && ub >= 0 && ub <= 1) { // Calculate the intersection point
            double x = startPos.x + ua * (endPos.x - startPos.x);
            double y = startPos.y + ua * (endPos.y - startPos.y);
            double z = startPos.z + ua * (endPos.z - startPos.z);
            return new Vec3(x, y, z);
        }
        return null;
    }

}
