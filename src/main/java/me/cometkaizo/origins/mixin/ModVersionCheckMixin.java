package me.cometkaizo.origins.mixin;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import me.cometkaizo.origins.Main;
import me.cometkaizo.origins.network.OriginStatusPing;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.ServerStatusResponse;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.client.ClientHooks;
import net.minecraftforge.fml.client.ExtendedServerListData;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.fml.network.FMLStatusPing;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.minecraftforge.fml.MavenVersionStringHelper.artifactVersionToString;

public final class ModVersionCheckMixin {

    @Mixin(FMLStatusPing.Serializer.class)
    public static abstract class MixedFMLStatusPingSerializer {
        @Inject(method = "deserialize", at = @At(value = "RETURN", ordinal = 0, shift = At.Shift.BEFORE),
                locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true, remap = false)
        private static void deserializeOriginStatusPing(JsonObject forgeData,
                                                        JsonDeserializationContext ctx,
                                                        CallbackInfoReturnable<FMLStatusPing> info,
                                                        Map<ResourceLocation, Pair<String, Boolean>> channels,
                                                        Map<String, String> mods,
                                                        int remoteFMLVersion,
                                                        boolean truncated) {
            if (JSONUtils.hasField(forgeData, OriginStatusPing.ORIGINS_MOD_VERSION_KEY)) {
                info.setReturnValue(new OriginStatusPing(JSONUtils.getString(forgeData, OriginStatusPing.ORIGINS_MOD_VERSION_KEY), channels, mods, remoteFMLVersion, truncated));
            }
        }

        @Inject(method = "serialize", at = @At("RETURN"), cancellable = true, remap = false)
        private static void serializeOriginStatusPing(FMLStatusPing forgeData,
                                                      JsonSerializationContext ctx,
                                                      CallbackInfoReturnable<JsonObject> info) {
            if (forgeData instanceof OriginStatusPing) {
                OriginStatusPing originData = (OriginStatusPing) forgeData;
                info.getReturnValue().addProperty(OriginStatusPing.ORIGINS_MOD_VERSION_KEY, originData.getOriginsVersion());
                info.setReturnValue(info.getReturnValue());
            }
        }
    }

    @Mixin(ClientHooks.class)
    public static abstract class MixedClientHooks {

        @Inject(method = "processForgeListPingData",
                at = @At(value = "INVOKE",
                        target = "Lnet/minecraftforge/fml/client/ExtendedServerListData;<init>(Ljava/lang/String;ZILjava/lang/String;Z)V",
                        ordinal = 0),
                locals = LocalCapture.CAPTURE_FAILHARD, remap = false, cancellable = true)
        private static void modifyCompatibility(ServerStatusResponse packet,
                                                  ServerData target,
                                                  CallbackInfo info,
                                                  Map<String, String> serverMods,
                                                  Map<ResourceLocation, Pair<String, Boolean>> remoteChannels,
                                                  int fmlVer,
                                                  boolean fmlNetMatches,
                                                  boolean channelsMatch,
                                                  AtomicBoolean result,
                                                  List<String> extraClientMods,
                                                  boolean modsMatch,
                                                  final Map<String, String> extraServerMods,
                                                  String extraReason) {
            if (!(packet.getForgeData() instanceof OriginStatusPing)) return;
            OriginStatusPing originData = (OriginStatusPing) packet.getForgeData();

            boolean isOriginsCompatible = true;
            List<ModInfo> clientMods = ModList.get().getMods();
            ModInfo clientOriginsMod = Main.getOriginsMod(clientMods);

            if (clientOriginsMod == null) {
                extraReason = "origins.multiplayer.origins_mod_not_found";
                isOriginsCompatible = false;
            } else {
                String clientOriginsVersion = artifactVersionToString(clientOriginsMod.getVersion());
                if (!originData.getOriginsVersion().equals(clientOriginsVersion)) {
                    extraReason = "origins.multiplayer.incorrect_origins_mod_version";
                    isOriginsCompatible = false;
                }
            }

            target.forgeData = new ExtendedServerListData("FML",
                    extraServerMods.isEmpty() && fmlNetMatches && channelsMatch && modsMatch && isOriginsCompatible,
                    serverMods.size(), extraReason, packet.getForgeData().isTruncated());
            info.cancel();
        }

    }
}
