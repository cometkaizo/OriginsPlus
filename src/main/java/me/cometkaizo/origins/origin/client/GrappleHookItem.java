package me.cometkaizo.origins.origin.client;

/*@Mod.EventBusSubscriber(modid = Main.MOD_ID)*/
public class GrappleHookItem /*extends Item implements KeySensitiveItem, InternalCooldownItem*/ {/*

    private static final double PARTICLE_PERIOD = 1.5;
    *//**
     * The cooldown to start grappling at
     *//*
    public static final int grappleStart = 5;
    public static final int maxCooldown = 15;
    public static final float grappleReach = 50.0F;
    public static final double distanceToStop = 4.5D;
    protected int cooldown = 0;
    protected BlockRayTraceResult rayTraceResult = null;
    protected Vector3d speedBasedVector;
    protected Timer timer = new Timer();
    protected PlayerEntity player = null;
    protected Random random = new Random();
    // -1 means not swinging
    protected double previousRadius = -1;



    @Override
    public void onKeyDown(PlayerEntity player, KeyMapping key, boolean isMainHand) {
        if (cooldown > 0) {
            Main.LOGGER.info(cooldown);
            return;
        }
        this.player = player;

        cooldown = maxCooldown;
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (cooldown <= 0) {
                    stop();
                    return;
                }

                Vector3d playerPosition = player.getPositionVec();

                if (cooldown == maxCooldown) {
                    rayTraceResult = rayCastFromEntity(player.world, player, grappleReach);
                    if (rayTraceResult == null) {
                        Main.LOGGER.info("Cancelled because BlockRayTraceResult was found to be null during ray-casting");
                        stop();
                        return;
                    }
                    new ItemStack(GrappleHookItem.this).hurtAndBreak(1, player, p -> {
                        p.broadcastBreakEvent(p.getUsedItemHand());
                    });
                    BlockPos hitBlock = rayTraceResult.getPos();
                    Main.LOGGER.info("X: " +
                            hitBlock.getX() + ", Y: " + hitBlock.getY() + ", Z: " + hitBlock.getZ()
                    );
                }
                if (cooldown > grappleStart) {
                    // slow motion effect because it helps you aim and also is cool
                    player.setMotion(player.getMotion().scale(0.60));
                }

                if (rayTraceResult == null) {
                    Main.LOGGER.info("Cancelled because BlockRayTraceResult was null");
                    stop();
                    return;
                }

                if (cooldown < grappleStart && !(playerPosition.distanceTo(rayTraceResult.getHitVec()) < distanceToStop && !Minecraft.getInstance().options.keyShift.isDown())) {
                    Main.LOGGER.info("Distance to block: " + playerPosition.distanceTo(rayTraceResult.getHitVec()));
                    cooldown = grappleStart;
                }

                if (cooldown <= grappleStart) {

                    player.world.addParticle(ParticleTypes.FLAME,
                            rayTraceResult.getHitVec().x + random.nextDouble(1),
                            rayTraceResult.getHitVec().y + random.nextDouble(1),
                            rayTraceResult.getHitVec().z + random.nextDouble(1),
                            random.nextDouble(-0.03D, 0.03D), random.nextDouble(-0.03D, 0.03D), random.nextDouble(-0.03D, 0.03D));

                    displayParticles(player.getEyePosition(0), rayTraceResult.getHitVec());

                    Vector3d initialVec = player.getMotion();

                    if (!Minecraft.getInstance().gameSettings.keyBindSneak.isKeyDown()) {
                        previousRadius = -1;
                        player.setNoGravity(true);
                        if (cooldown == grappleStart) {
                            speedBasedVector = PhysicsUtils.getVelocityTowards(playerPosition, rayTraceResult.getHitVec(), 0.3);
                        }
                        Vector3d movingVec = initialVec
                                .mul(0.75, 0.75, 0.75)
                                .add(speedBasedVector
                                        .mul(1 + ((maxCooldown - cooldown) / (double) maxCooldown),
                                                1 + ((maxCooldown - cooldown) / (double) maxCooldown),
                                                1 + ((maxCooldown - cooldown) / (double) maxCooldown)
                                        )
                                );
                        player.setMotion(movingVec);
                    } else {
                        player.setNoGravity(false);

                        if (cooldown == grappleStart) {
                            Vector3d oldDeltaMovement = player.getMotion();

                            double distance = playerPosition.distanceTo(rayTraceResult.getHitVec());

                            double correction = previousRadius == -1 ? 0 : distance - previousRadius;
                            Main.LOGGER.info(
                                    "JC distance to center: {}, Previous distance to center: {}, Difference: {}",
                                    distance,
                                    previousRadius,
                                    correction
                            );

                            //if (previousRadius == -1) {
                                previousRadius = distance;
                            //}

                            Vector3d nextVectorFromCenter = playerPosition
                                    .add(player.getMotion().scale(1.08))
                                    .subtract(rayTraceResult.getHitVec());
                            Vector3d nextSwingPosition = rayTraceResult.getHitVec().add(nextVectorFromCenter.normalize().scale(distance - correction));
                            player.setMotion(nextSwingPosition.subtract(playerPosition));


                            Main.LOGGER.info("Distance from center: " + distance +
                                    ", Delta movement (this tick): " + oldDeltaMovement +
                                    ", Next vector from center: " + nextVectorFromCenter +
                                    ", Next vector from center normalized: " + nextVectorFromCenter.normalize() +
                                    ", Next swing position: " + nextSwingPosition +
                                    ", Delta movement: " + player.getMotion());
                            cooldown = grappleStart;
                        }

                    }

                }
                cooldown --;
                Main.LOGGER.info("Cooldown: " + cooldown);

            }
        }, 1000/20, 1000/20);

    }

    private void displayParticles(Vector3d start, Vector3d end) {
        Vector3d speedBasedVector = PhysicsUtils.getVelocityTowards(start, end, PARTICLE_PERIOD);
        double distance = start.distanceTo(end);
        Vector3d travelled = new Vector3d(start.x, start.y, start.z);

        for (; distance > PARTICLE_PERIOD; ) {

            player.world.addParticle(ParticleTypes.CLOUD, travelled.x, travelled.y, travelled.z,
                    0, 0, 0);

            travelled = travelled.add(speedBasedVector);
            distance = travelled.distanceTo(end);
        }
    }

    @Override
    public void onKeyUp(PlayerEntity player, KeyMapping key, boolean isMainHand) {
        stop();
    }

    private void stop() {
        Main.LOGGER.info("Cooldown when cancelled: " + cooldown);
        timer.cancel();
        player.setNoGravity(false);
        cooldown = 0;
        rayTraceResult = null;
        previousRadius = -1;
        speedBasedVector = null;
    }

    public static BlockRayTraceResult rayCastFromEntity(World world, Entity entity, float maxDistance) {
        float xRot = entity.rotationYaw;
        float yRot = entity.rotationPitch;
        Vector3d eyePosition = entity.getEyePosition(0);

        float f2 = MathHelper.cos(-yRot * ((float)Math.PI / 180F) - (float)Math.PI);
        float f3 = MathHelper.sin(-yRot * ((float)Math.PI / 180F) - (float)Math.PI);
        float f4 = -MathHelper.cos(-xRot * ((float)Math.PI / 180F));
        float f5 = MathHelper.sin(-xRot * ((float)Math.PI / 180F));
        float f6 = f3 * f4;
        float f7 = f2 * f4;

        Vector3d Vector3d1 = eyePosition.add((double)f6 * maxDistance, (double)f5 * maxDistance, (double)f7 * maxDistance);
        BlockRayTraceResult result = world.rayTraceBlocks(new RayTraceContext(eyePosition, Vector3d1, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, entity));
        return world.getBlockState(result.getPos()).getMaterial().isSolid() ? result : null;
    }

    @Override
    public int getInternalCooldown() {
        return cooldown;
    }

    @Override
    public int getMaxCooldown() {
        return maxCooldown;
    }*/
}
