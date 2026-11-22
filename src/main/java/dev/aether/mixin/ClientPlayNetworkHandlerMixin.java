package dev.aether.mixin;

import dev.aether.module.impl.PopCounter;
import dev.aether.module.impl.SoundESP;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import dev.aether.module.impl.Velocity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onEntityVelocityUpdate(Lnet/minecraft/network/packet/s2c/play/EntityVelocityUpdateS2CPacket;)V",
            at = @At("HEAD"), cancellable = true)
    private void aether$velocity(EntityVelocityUpdateS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!mc.isOnThread()) return; // let vanilla re-queue onto the main thread first
        Velocity v = Velocity.INSTANCE;
        if (v == null || !v.enabled || mc.player == null || packet.getEntityId() != mc.player.getId()) return;
        ci.cancel();
        Vec3d vel = packet.getVelocity();
        mc.player.setVelocity(vel.x * v.h(), vel.y * v.v(), vel.z * v.h());
    }

    @Inject(method = "onEntityStatus(Lnet/minecraft/network/packet/s2c/play/EntityStatusS2CPacket;)V", at = @At("HEAD"))
    private void aether$pop(EntityStatusS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        PopCounter pc = PopCounter.INSTANCE;
        if (pc == null || !pc.enabled || !mc.isOnThread() || mc.world == null || mc.player == null || packet.getStatus() != 35) return;
        Entity e = packet.getEntity(mc.world);
        if (e instanceof PlayerEntity p) pc.onPop(mc, p.getName().getString(), p == mc.player);
    }

    @Inject(method = "onPlaySound(Lnet/minecraft/network/packet/s2c/play/PlaySoundS2CPacket;)V", at = @At("HEAD"))
    private void aether$sound(PlaySoundS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        SoundESP se = SoundESP.INSTANCE;
        if (se == null || !se.enabled || !mc.isOnThread() || mc.world == null || mc.player == null) return;
        String id = packet.getSound().getKey().map(k -> k.getValue().getPath()).orElse("");
        if (!id.isEmpty()) se.onSound(mc, id, packet.getX(), packet.getY(), packet.getZ());
    }
}
