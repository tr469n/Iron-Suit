package dev.ironsuit;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Abilities;

/**
 * Server-side flight for the deployed suit.
 *
 * While the suit is active (helmet + chestplate with a reactor inside) and has energy, the player is allowed to
 * fly exactly like in creative mode: double-tap Space to take off, Space / Shift to climb / descend,
 * sprint (Ctrl) to boost. Flight drains suit energy; when the energy is gone or the suit is retracted,
 * the ability is taken away again (creative / spectator players are never touched).
 */
public final class SuitFlight {
    /** Vanilla creative flying speed is 0.05. */
    private static final float VANILLA_FLY_SPEED = 0.05F;
    private static final float SUIT_FLY_SPEED = 0.09F;

    /** Energy per tick while flying (the energy pool is 10 000, so 1/tick = ~8 minutes of hovering). */
    private static final int COST_FLY = 1;
    private static final int COST_BOOST = 3;

    /** Players to whom *this mod* granted flight (so we never remove flight that came from somewhere else). */
    private static final Set<UUID> GRANTED = new HashSet<>();

    private SuitFlight() {}

    public static void tick(ServerPlayer player) {
        UUID id = player.getUUID();
        boolean usable = SuitManager.isActive(player) && SuitManager.getEnergy(player) > 0;
        Abilities abilities = player.getAbilities();

        if (!usable) {
            revoke(player);
            return;
        }

        if (!abilities.mayfly) {
            abilities.mayfly = true;
            abilities.setFlyingSpeed(SUIT_FLY_SPEED);
            GRANTED.add(id);
            player.onUpdateAbilities();
        }

        if (!GRANTED.contains(id) || !abilities.flying) {
            return;
        }

        // The player is flying right now: pay for it and show the thrusters.
        boolean boosting = player.isSprinting();
        if (!SuitManager.consumeEnergy(player, boosting ? COST_BOOST : COST_FLY)) {
            revoke(player);
            return;
        }
        if (player.level() instanceof ServerLevel level) {
            if (player.tickCount % 2 == 0) {
                level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 0.1, player.getZ(),
                        boosting ? 6 : 2, 0.15, 0.02, 0.15, 0.02);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 0.1, player.getZ(),
                        boosting ? 3 : 1, 0.2, 0.05, 0.2, 0.05);
            }
            if (player.tickCount % 12 == 0) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, boosting ? 0.5F : 0.25F, 1.6F);
            }
        }
    }

    /** Takes the flight ability away if this mod gave it. Safe to call at any time. */
    public static void revoke(ServerPlayer player) {
        if (!GRANTED.remove(player.getUUID())) {
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        Abilities abilities = player.getAbilities();
        abilities.mayfly = false;
        abilities.flying = false;
        abilities.setFlyingSpeed(VANILLA_FLY_SPEED);
        player.onUpdateAbilities();
    }

    /** True while the suit is active and powered - used to cancel fall damage. */
    public static boolean isProtected(ServerPlayer player) {
        return SuitManager.isActive(player) && SuitManager.getEnergy(player) > 0;
    }
}
