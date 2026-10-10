package dev.ironsuit;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side weapons of the deployed suit.
 *
 * <ul>
 *   <li><b>Repulsors</b> (hold the key): a fast beam from the palms every few ticks, small energy cost.</li>
 *   <li><b>Reactor beam</b> (hold to charge, release to fire): a heavy beam from the chest reactor.
 *       The longer it is charged, the more damage, range and explosion it has; charging drains energy.</li>
 * </ul>
 * Both are instant ray casts (no projectile entities), so nothing extra has to be registered.
 */
public final class SuitCombat {
    public static final int WEAPON_REPULSOR = 0;
    public static final int WEAPON_BEAM = 1;

    // ---- Repulsors
    private static final int REPULSOR_COST = 40;
    private static final int REPULSOR_COOLDOWN = 5;
    private static final float REPULSOR_DAMAGE = 6.0F;
    private static final double REPULSOR_RANGE = 48.0;

    // ---- Reactor beam
    private static final int BEAM_MIN_CHARGE = 15;
    private static final int BEAM_MAX_CHARGE = 60;
    private static final int BEAM_COST_PER_TICK = 25;
    private static final float BEAM_DAMAGE_MIN = 12.0F;
    private static final float BEAM_DAMAGE_MAX = 40.0F;
    private static final double BEAM_RANGE = 96.0;
    private static final float BEAM_EXPLOSION_MIN = 1.5F;
    private static final float BEAM_EXPLOSION_MAX = 3.5F;
    /** Set to {@link Level.ExplosionInteraction#TNT} if the beam should also break blocks. */
    private static final Level.ExplosionInteraction BEAM_BLOCK_MODE = Level.ExplosionInteraction.NONE;

    private static final double STEP = 0.25;

    private static final class State {
        boolean repulsor;
        boolean beamHeld;
        int cooldown;
        int charge;
        boolean leftHand;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    private SuitCombat() {}

    // ------------------------------------------------------------------ input

    public static void setInput(ServerPlayer player, int weapon, boolean down) {
        if (down && !SuitManager.isActive(player)) {
            return;
        }
        State state = STATES.computeIfAbsent(player.getUUID(), id -> new State());
        if (weapon == WEAPON_REPULSOR) {
            state.repulsor = down;
        } else if (weapon == WEAPON_BEAM) {
            state.beamHeld = down;
        }
    }

    public static void cancel(UUID id) {
        STATES.remove(id);
    }

    // ------------------------------------------------------------------ tick

    public static void tick(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        if (state == null) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level) || !SuitManager.isActive(player)) {
            STATES.remove(player.getUUID());
            return;
        }

        if (state.cooldown > 0) {
            state.cooldown--;
        }

        // ---- repulsors
        if (state.repulsor && state.cooldown == 0) {
            if (SuitManager.consumeEnergy(player, REPULSOR_COST)) {
                fireRepulsor(level, player, state.leftHand);
                state.leftHand = !state.leftHand;
                state.cooldown = REPULSOR_COOLDOWN;
            } else if (player.tickCount % 20 == 0) {
                player.sendSystemMessage(Component.translatable("message.ironsuit.low_energy"), true);
            }
        }

        // ---- reactor beam
        if (state.beamHeld) {
            if (state.charge < BEAM_MAX_CHARGE && SuitManager.consumeEnergy(player, BEAM_COST_PER_TICK)) {
                state.charge++;
                chargeEffect(level, player, state.charge);
            } else if (state.charge < BEAM_MAX_CHARGE && player.tickCount % 20 == 0) {
                player.sendSystemMessage(Component.translatable("message.ironsuit.low_energy"), true);
            }
            if (state.charge > 0 && player.tickCount % 3 == 0) {
                int percent = state.charge * 100 / BEAM_MAX_CHARGE;
                player.sendSystemMessage(Component.translatable("message.ironsuit.charging", percent), true);
            }
        } else if (state.charge > 0) {
            if (state.charge >= BEAM_MIN_CHARGE) {
                fireBeam(level, player, state.charge / (float) BEAM_MAX_CHARGE);
            }
            state.charge = 0;
        }

        if (!state.repulsor && !state.beamHeld && state.charge == 0 && state.cooldown == 0) {
            STATES.remove(player.getUUID());
        }
    }

    // ------------------------------------------------------------------ repulsor

    private static void fireRepulsor(ServerLevel level, ServerPlayer player, boolean leftHand) {
        Vec3 look = player.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 from = player.getEyePosition()
                .add(look.scale(0.6))
                .add(right.scale(leftHand ? -0.35 : 0.35))
                .add(0, -0.35, 0);

        Hit hit = trace(level, player, from, look, REPULSOR_RANGE);
        drawBeam(level, from, hit.end, 1.0, false);

        level.sendParticles(ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFF),
            from.x, from.y, from.z, 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, hit.end.x, hit.end.y, hit.end.z, 8, 0.15, 0.15, 0.15, 0.1);
        play(level, player, SoundEvents.FIREWORK_ROCKET_BLAST, 0.8F, 1.7F);

        if (hit.target != null) {
            hit.target.hurtServer(level, level.damageSources().playerAttack(player), REPULSOR_DAMAGE);
            knockback(hit.target, look, 0.4);
        }
    }

    // ------------------------------------------------------------------ chest reactor beam

    private static void chargeEffect(ServerLevel level, ServerPlayer player, int charge) {
        Vec3 chest = chestPos(player);
        if (player.tickCount % 2 == 0) {
            level.sendParticles(ParticleTypes.END_ROD, chest.x, chest.y, chest.z, 3, 0.25, 0.25, 0.25, 0.02);
        }
        if (player.tickCount % 4 == 0) {
            play(level, player, SoundEvents.AMETHYST_BLOCK_CHIME, 0.6F, 0.5F + 1.5F * charge / BEAM_MAX_CHARGE);
        }
    }

    private static void fireBeam(ServerLevel level, ServerPlayer player, float power) {
        Vec3 look = player.getLookAngle();
        Vec3 from = chestPos(player).add(look.scale(0.4));
        double range = BEAM_RANGE * (0.5 + 0.5 * power);

        Hit hit = trace(level, player, from, look, range);
        drawBeam(level, from, hit.end, 0.5, true);

        level.sendParticles(ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFF),
            from.x, from.y, from.z, 1, 0.0, 0.0, 0.0, 0.0);
        play(level, player, SoundEvents.WARDEN_SONIC_BOOM, 1.2F, 1.4F);

        float damage = BEAM_DAMAGE_MIN + (BEAM_DAMAGE_MAX - BEAM_DAMAGE_MIN) * power;
        if (hit.target != null) {
            hit.target.hurtServer(level, level.damageSources().playerAttack(player), damage);
            hit.target.igniteForSeconds(4.0F);
            knockback(hit.target, look, 1.2 * power + 0.5);
        }

        float radius = BEAM_EXPLOSION_MIN + (BEAM_EXPLOSION_MAX - BEAM_EXPLOSION_MIN) * power;
        level.explode(player, hit.end.x, hit.end.y, hit.end.z, radius, BEAM_BLOCK_MODE);

        // Recoil: the blast pushes the shooter back a little.
        player.push(-look.x * 0.4 * power, 0.05, -look.z * 0.4 * power);
        player.hurtMarked = true;
    }

    // ------------------------------------------------------------------ ray casting

    private record Hit(Vec3 end, LivingEntity target) {}

    /**
     * Marches along the ray: stops at the first solid block, and finds the nearest living entity before that.
     * Uses only block collision shapes and entity bounding boxes, so no vanilla ray-cast classes are needed.
     */
    private static Hit trace(ServerLevel level, ServerPlayer shooter, Vec3 from, Vec3 dir, double range) {
        double blockDistance = range;
        for (double d = STEP; d <= range; d += STEP) {
            Vec3 point = from.add(dir.scale(d));
            BlockPos pos = BlockPos.containing(point);
            BlockState state = level.getBlockState(pos);
            if (!state.getCollisionShape(level, pos).isEmpty()) {
                blockDistance = Math.max(0.0, d - STEP);
                break;
            }
        }
        Vec3 end = from.add(dir.scale(blockDistance));

        AABB area = new AABB(from, end).inflate(1.0);
        LivingEntity best = null;
        double bestDistance = blockDistance;
        for (Entity entity : level.getEntities(shooter, area,
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator())) {
            Optional<Vec3> point = entity.getBoundingBox().inflate(0.3).clip(from, end);
            if (point.isPresent()) {
                double distance = from.distanceTo(point.get());
                if (distance <= bestDistance) {
                    bestDistance = distance;
                    best = (LivingEntity) entity;
                }
            }
        }
        if (best != null) {
            end = from.add(dir.scale(bestDistance));
        }
        return new Hit(end, best);
    }

    private static void drawBeam(ServerLevel level, Vec3 from, Vec3 to, double spacing, boolean heavy) {
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length < 1.0E-3) {
            return;
        }
        Vec3 step = delta.scale(spacing / length);
        Vec3 point = from;
        for (double d = 0.0; d < length; d += spacing) {
            level.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, heavy ? 2 : 1, 0.02, 0.02, 0.02, 0.0);
            if (heavy) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, 1, 0.12, 0.12, 0.12, 0.05);
            }
            point = point.add(step);
        }
    }

    // ------------------------------------------------------------------ helpers

    private static Vec3 chestPos(ServerPlayer player) {
        return new Vec3(player.getX(), player.getY() + 1.15, player.getZ());
    }

    private static void knockback(LivingEntity target, Vec3 dir, double strength) {
        target.push(dir.x * strength, 0.15 + dir.y * strength * 0.5, dir.z * strength);
        target.hurtMarked = true;
    }

    private static void play(ServerLevel level, ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }
}
