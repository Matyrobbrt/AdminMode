package com.matyrobbrt.adminmode;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Manager extends SavedData {

    private final Map<UUID, Data> data = new HashMap<>();

    @Override
    public CompoundTag save(CompoundTag tag) {
        this.data.forEach((id, data) -> {
            final var nbt = new CompoundTag();
            nbt.putBoolean("adminMode", data.isAdminMode);
            if (data.adminInv != null) {
                nbt.put("adminInv", data.adminInv);
            }
            if (data.survivalInv != null) {
                nbt.put("survivalInv", data.survivalInv);
            }
            tag.put(id.toString(), nbt);
        });
        return tag;
    }

    public Data get(UUID player) {
        return data.computeIfAbsent(player, u -> new Data());
    }

    public static Manager load(CompoundTag tag) {
        final var manager = new Manager();
        tag.getAllKeys()
            .forEach(id -> {
                final var dataTag = tag.getCompound(id);
                final var data = new Data();
                data.isAdminMode = dataTag.getBoolean("adminMode");
                if (dataTag.contains("adminInv"))
                    data.adminInv = dataTag.getList("adminInv", 10);
                if (dataTag.contains("survivalInv"))
                    data.survivalInv = dataTag.getList("survivalInv", 10);
                manager.data.put(UUID.fromString(id), data);
            });
        return manager;
    }

    public static Manager get(MinecraftServer server) {
        return Objects.requireNonNull(server.getLevel(Level.OVERWORLD)).getDataStorage().computeIfAbsent(Manager::load, Manager::new, AdminMode.MOD_ID);
    }

    public static class Data {
        public boolean isAdminMode;
        public ListTag survivalInv;
        public ListTag adminInv;
    }
}
