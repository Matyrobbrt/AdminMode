package com.matyrobbrt.adminmode;

import static net.minecraft.commands.Commands.LEVEL_GAMEMASTERS;
import static net.minecraft.commands.Commands.literal;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.network.NetworkConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

@Mod(AdminMode.MOD_ID)
public class AdminMode {
    public static final String MOD_ID = "adminmode";
    public static final Logger LOGGER = LoggerFactory.getLogger("AdminMode");

    public AdminMode() {
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (ver, remote) -> true));

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC, MOD_ID + "-server.toml");

        MinecraftForge.EVENT_BUS.addListener(AdminMode::gameModeChange);
        MinecraftForge.EVENT_BUS.addListener(AdminMode::registerCommands);
    }

    static void registerCommands(final RegisterCommandsEvent event) {
        event.getDispatcher().register(literal(MOD_ID)
                .requires(p -> p.hasPermission(LEVEL_GAMEMASTERS))
                .then(literal("enter")
                        .executes(ctx -> doAdmin(ctx, true)))
                .then(literal("exit")
                        .executes(ctx -> doAdmin(ctx, false)))
        );
    }

    static int doAdmin(CommandContext<CommandSourceStack> ctx, boolean enter) throws CommandSyntaxException {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            return Command.SINGLE_SUCCESS;
        }

        final var data = Manager.get(player.server).get(player.getUUID());
        final var old = player.gameMode.getGameModeForPlayer();

        if (enter && data.isAdminMode) {
            ctx.getSource().sendSuccess(new TextComponent("You already are in admin mode!"), true);
            return Command.SINGLE_SUCCESS;
        }
        if (!enter && !data.isAdminMode) {
            ctx.getSource().sendSuccess(new TextComponent("You are not in admin mode!"), true);
            return Command.SINGLE_SUCCESS;
        }

        player.setGameMode(enter ? GameType.CREATIVE : GameType.SURVIVAL);
        if (!ServerConfig.ENTER_ON_GAMEMODE_CHANGE.get()) {
            doOnPlayer(player, old, player.gameMode.getGameModeForPlayer());
        }
        return Command.SINGLE_SUCCESS;
    }

    static void gameModeChange(PlayerEvent.PlayerChangeGameModeEvent event) {
        if (ServerConfig.ENTER_ON_GAMEMODE_CHANGE.get()) {
            doOnPlayer(event.getPlayer(), event.getNewGameMode(), event.getCurrentGameMode());
        }
    }
    
    static void doOnPlayer(Player player, GameType newGameMode, GameType oldGameMode) {
        if (player.level.isClientSide())
            return;

        final var manager = Manager.get(Objects.requireNonNull(player.getServer()));
        final var data = manager.get(player.getUUID());
        if (newGameMode == GameType.CREATIVE && oldGameMode == GameType.SURVIVAL) {
            data.isAdminMode = true;
            data.survivalInv = player.getInventory().save(new ListTag());
            player.getInventory().clearContent();
            if (data.adminInv != null) {
                player.getInventory().load(data.adminInv);
                data.adminInv = null;
            }
            player.displayClientMessage(new TextComponent("Entered Admin Mode"), true);
            manager.setDirty();
            LOGGER.debug("{} entered admin mode!", player.getName().getContents());
        } else if (newGameMode == GameType.SURVIVAL && oldGameMode == GameType.CREATIVE) {
            data.isAdminMode = false;
            data.adminInv = player.getInventory().save(new ListTag());
            player.getInventory().clearContent();
            if (data.survivalInv != null) {
                player.getInventory().load(data.survivalInv);
                data.survivalInv = null;
            }
            player.displayClientMessage(new TextComponent("Exited Admin Mode"), true);
            manager.setDirty();
            LOGGER.debug("{} exited admin mode!", player.getName().getContents());
        }
    }
}
