package me.cometkaizo.origins.network;

import com.google.common.collect.ImmutableMap;
import me.cometkaizo.origins.Main;
import me.cometkaizo.origins.util.ClassUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.network.FMLStatusPing;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.artifact.versioning.ArtifactVersion;

import java.util.Map;

import static net.minecraftforge.fml.MavenVersionStringHelper.artifactVersionToString;

public class OriginStatusPing extends FMLStatusPing {
    public static final String ORIGINS_MOD_VERSION_KEY = "originsModVersion";
    private transient String originsVersion = "NONE";

    public OriginStatusPing() {
        ModList.get().forEachModContainer((modId, modContainer) -> {
            if (Main.MOD_ID.equals(modId)) {
                IModInfo modInfo = modContainer.getModInfo();
                ArtifactVersion modVersion = modInfo.getVersion();
                originsVersion = artifactVersionToString(modVersion);
            }
        });
    }

    public OriginStatusPing(String originsVersion, Map<ResourceLocation, Pair<String, Boolean>> deserialized, Map<String,String> modMarkers, int fmlNetVer, boolean truncated) {
        this.originsVersion = originsVersion == null ? "NONE" : originsVersion;
        ClassUtils.setFieldOrThrow("channels", this, ImmutableMap.copyOf(deserialized));
        ClassUtils.setFieldOrThrow("mods", this, modMarkers);
        ClassUtils.setFieldOrThrow("fmlNetworkVer", this, fmlNetVer);
        ClassUtils.setFieldOrThrow("truncated", this, truncated);
    }

    public String getOriginsVersion() {
        return originsVersion;
    }
}
