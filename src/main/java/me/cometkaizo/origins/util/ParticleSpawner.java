package me.cometkaizo.origins.util;

import net.minecraft.entity.Entity;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.particles.IParticleData;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;

import java.util.*;
import java.util.function.Supplier;

public class ParticleSpawner {
    protected static final Random random = new Random();

    private final Set<IParticleData> particleTypes = new HashSet<>(1);
    private ServerWorld world;
    private Vector3d position;
    private Supplier<Vector3d> delta;
    private Supplier<Vector3d> direction;
    private Supplier<Double> speed;
    private Supplier<Integer> count;

    protected static double randomDouble(double origin, double bound) {
        return random.nextDouble() * (bound - origin) + origin;
    }
    protected static int randomInt(int origin, int bound) {
        return origin + random.nextInt(bound - origin);
    }
    protected static double randomGaussian() {
        return random.nextGaussian();
    }

    public ParticleSpawner withParticles(BasicParticleType... particleTypes) {
        this.particleTypes.addAll(Arrays.asList(particleTypes));
        return this;
    }

    public ParticleSpawner with(int count, BasicParticleType... particleTypes) {
        withCount(count);
        return withParticles(particleTypes);
    }

    public ParticleSpawner withCount(int count) {
        this.count = () -> count;
        return this;
    }

    public ParticleSpawner withCount(Supplier<Integer> count) {
        this.count = count;
        return this;
    }

    public ParticleSpawner withRandomCount(int countMin, int countMax) {
        return withCount(() -> randomInt(countMin, countMax));
    }

    public ParticleSpawner withSpeed(double speed) {
        this.speed = () -> speed;
        return this;
    }

    public ParticleSpawner withSpeed(Supplier<Double> speed) {
        this.speed = speed;
        return this;
    }

    public ParticleSpawner withRandomSpeed(double speedMin, double speedMax) {
        return withSpeed(() -> randomDouble(speedMin, speedMax));
    }

    public ParticleSpawner in(ServerWorld world) {
        this.world = world;
        return this;
    }

    public ParticleSpawner at(Vector3d position) {
        this.position = position;
        return this;
    }

    public ParticleSpawner atPos(Entity entity) {
        return at(entity.getPositionVec());
    }

    public ParticleSpawner at(Entity entity) {
        if (entity.world instanceof ServerWorld) in((ServerWorld) entity.world);
        return at(entity.getPositionVec());
    }

    public ParticleSpawner toPos(Vector3d targetPos) {
        Objects.requireNonNull(targetPos, "Target position cannot be null");
        Objects.requireNonNull(position, "Cannot set target position without a starting position");
        this.delta = () -> targetPos.subtract(position);
        return this;
    }

    public ParticleSpawner toPos(double targetX, double targetY, double targetZ) {
        return toPos(new Vector3d(targetX, targetY, targetZ));
    }

    public ParticleSpawner withRange(Vector3d delta) {
        this.delta = () -> delta;
        return this;
    }

    public ParticleSpawner withRange(Supplier<Vector3d> delta) {
        this.delta = delta;
        return this;
    }

    public ParticleSpawner withRange(double deltaX, double deltaY, double deltaZ) {
        return withRange(new Vector3d(deltaX, deltaY, deltaZ));
    }

    public ParticleSpawner withDirection(Vector3d direction) {
        return withDirection(() -> direction);
    }

    public ParticleSpawner withDirection(int x, int y, int z) {
        return withDirection(new Vector3d(x, y, z));
    }

    public ParticleSpawner withDirection(Supplier<Vector3d> direction) {
        this.direction = () -> direction.get().normalize();
        return this;
    }

    public ParticleSpawner withRandomDirection(double xMin, double yMin, double zMin, double xMax, double yMax, double zMax) {
        return withDirection(() -> new Vector3d(randomDouble(xMin, xMax), randomDouble(yMin, yMax), randomDouble(zMin, zMax)));
    }
    public ParticleSpawner withRandomDirection() {
        return withRandomDirection(-1, -1, -1, 1, 1, 1);
    }

    public void spawn(ParticleSpawner other) {
        Set<IParticleData> particleType = other.particleTypes.isEmpty() ? this.particleTypes : other.particleTypes;
        ServerWorld world = other.world == null ? this.world : other.world;
        Vector3d position = other.position == null ? this.position : other.position;
        Supplier<Vector3d> delta = other.delta == null ? this.delta : other.delta;
        Supplier<Vector3d> direction = other.direction == null ? this.direction : other.direction;
        Supplier<Double> speed = other.speed == null ? this.speed : other.speed;
        Supplier<Integer> count = other.count == null ? this.count : other.count;

        spawn(world, particleType, position, count, delta, direction, speed);
    }

    public void spawn() {
        spawn(world, particleTypes, position,
                count, delta, direction, speed);
    }

    private void spawn(ServerWorld world, Set<IParticleData> particleTypes, Vector3d position, Supplier<Integer> countSup, Supplier<Vector3d> deltaSup, Supplier<Vector3d> directionSup, Supplier<Double> speedSup) {
        for (IParticleData particleType : particleTypes) {
            Supplier<Vector3d> nonNullDeltaSup = deltaSup != null ? deltaSup : () -> Vector3d.ZERO;

            if (directionSup != null) {
                for (int c = 0; c < countSup.get(); c++) {
                    spawnInRangeAndDirection(world, position, nonNullDeltaSup, directionSup, speedSup, particleType);
                }
            } else {
                spawnInRange(world, position, countSup, nonNullDeltaSup, speedSup, particleType);
            }
        }
    }

    private static void spawnInRangeAndDirection(ServerWorld world, Vector3d position, Supplier<Vector3d> deltaSup, Supplier<Vector3d> directionSup, Supplier<Double> speedSup, IParticleData particleType) {
        Vector3d delta = deltaSup.get();
        Vector3d offset = getRandomPosInRange(delta);
        spawnInDirection(world, position.add(offset), directionSup, speedSup, particleType);
    }

    private static Vector3d getRandomPosInRange(Vector3d range) {
        if (range == Vector3d.ZERO) return Vector3d.ZERO;
        double x = randomGaussian() * range.x;
        double y = randomGaussian() * range.y;
        double z = randomGaussian() * range.z;
        return new Vector3d(x, y, z);
    }

    private static void spawnInDirection(ServerWorld world, Vector3d position, Supplier<Vector3d> directionSup, Supplier<Double> speedSup, IParticleData particleType) {
        Vector3d direction = directionSup.get();
        Double speed = speedSup.get();
        world.spawnParticle(
                particleType, position.x, position.y, position.z,
                0, direction.x, direction.y, direction.z, speed);
    }

    private static void spawnInRange(ServerWorld world, Vector3d position, Supplier<Integer> countSup, Supplier<Vector3d> deltaSup, Supplier<Double> speedSup, IParticleData particleType) {
        Integer count = countSup.get();
        Vector3d delta = deltaSup.get();
        Double speed = speedSup.get();
        world.spawnParticle(
                particleType, position.x, position.y, position.z,
                count, delta.x, delta.y, delta.z, speed);
    }

    public void spawnIn(ServerWorld world) {
        spawn(world, particleTypes, position, count, delta, direction, speed);
    }

    public void spawnAt(Vector3d position) {
        spawn(world, particleTypes, position, count, delta, direction, speed);
    }

    public void spawnAtPos(Entity entity) {
        spawn(world, particleTypes, entity.getPositionVec(), count, delta, direction, speed);
    }

    public void spawnAt(ServerWorld world, Entity entity) {
        spawn(world, particleTypes, entity.getPositionVec(), count, delta, direction, speed);
    }

    public void spawnAt(ServerWorld world, Vector3d position) {
        spawn(world, particleTypes, position, count, delta, direction, speed);
    }

    public void spawnWithSpeed(double speed) {
        spawn(world, particleTypes, position, count, delta, direction, () -> speed);
    }

    public void spawn(int count) {
        spawn(world, particleTypes, position, () -> count, delta, direction, speed);
    }

    public void spawn(int count, IParticleData... particleTypes) {
        spawn(world, CollUtils.setOf(particleTypes), position, () -> count, delta, direction, speed);
    }

    public void spawn(IParticleData... particleTypes) {
        spawn(world, CollUtils.setOf(particleTypes), position, count, delta, direction, speed);
    }

    public void spawnWithDelta(Vector3d delta) {
        spawn(world, particleTypes, position, count, () -> delta, direction, speed);
    }
}
