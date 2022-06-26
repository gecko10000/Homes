package gecko10000.homes;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import redempt.redlib.commandmanager.CommandHook;
import redempt.redlib.commandmanager.CommandParser;
import redempt.redlib.misc.ChatPrompt;
import redempt.redlib.misc.FormatUtils;
import redempt.redlib.sql.SQLHelper;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class CommandHandler {

    private final Homes plugin;

    public CommandHandler(Homes plugin) {
        this.plugin = plugin;
        new CommandParser(plugin.getResource("command.rdcml"))
                .parse().register("homes", this);
    }

    @CommandHook("home")
    public void home(Player player, String name) {
        SQLHelper.Results coords = plugin.sql.queryResults("SELECT world, x, y, z, pitch, yaw FROM homes WHERE uuid=? AND name=?;", player.getUniqueId().toString(), name);
        if (coords.isEmpty()) {
            player.sendMessage(FormatUtils.color("&cThis home does not exist."));
            return;
        }
        String worldUUID = coords.getString(1);
        double x = coords.get(2), y = coords.get(3), z = coords.get(4), pitch = coords.get(5), yaw = coords.get(6);
        World world = Bukkit.getWorld(UUID.fromString(worldUUID));
        if (world == null) {
            player.sendMessage(FormatUtils.color("&cThis world does not exist anymore."));
            return;
        }
        player.teleport(new Location(world, x, y, z, (float) yaw, (float) pitch));
        player.sendMessage(FormatUtils.color("&aTeleported to home " + name + "."));
    }

    public List<String> getHomeNames(Player player) {
        return plugin.sql.queryResultList("SELECT name FROM homes WHERE uuid=?;", player.getUniqueId().toString());
    }

    @CommandHook("homes")
    public void homes(Player player) {
        String homeNames = getHomeNames(player).stream().map(s -> "&a" + s).collect(Collectors.joining("&7, "));
        player.sendMessage(FormatUtils.color(homeNames.equals("") ? "&cYou are homeless." : ("&7Homes: " + homeNames)));
    }

    public int getAllowedHomes(Player player) {
        Set<PermissionAttachmentInfo> permissions = player.getEffectivePermissions();
        int homes = 0;
        for (PermissionAttachmentInfo info : permissions) {
            if (!info.getValue()) continue;
            final String prefix = "homes.amount.";
            if (!info.getPermission().startsWith(prefix)) continue;
            try {
                int parsed = Integer.parseInt(info.getPermission().substring(prefix.length()));
                homes = Math.max(homes, parsed);
            } catch (NumberFormatException e) {}
        }
        return homes;
    }

    @CommandHook("sethome")
    public void sethome(Player player, String name) {
        if (name.equals("all")) {
            player.sendMessage(FormatUtils.color("&cThis name is reserved."));
            return;
        }
        List<String> homes = getHomeNames(player);
        int allowed = getAllowedHomes(player);
        if (!player.isOp() && homes.size() >= allowed) {
            player.sendMessage(FormatUtils.color("&cYou can only set " + allowed + " homes."));
            return;
        }
        Location loc = player.getLocation();
        plugin.sql.execute(homes.contains(name) ? "UPDATE homes SET world=?, x=?, y=?, z=?, pitch=?, yaw=? WHERE uuid=? AND name=?;" : "INSERT INTO homes VALUES (?, ?, ?, ?, ?, ?, ?, ?);",
                loc.getWorld().getUID().toString(), loc.getX(), loc.getY(), loc.getZ(), loc.getPitch(), loc.getYaw(), player.getUniqueId().toString(), name);
        player.sendMessage(FormatUtils.color("&aSet home " + name + " at " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + " in " + loc.getWorld().getName() + "."));
    }

    @CommandHook("delhome")
    public void delhome(Player player, String name) {
        if (name.equals("all")) {
            ChatPrompt.prompt(player, FormatUtils.color("&cType 'yes' to delete all homes, or anything else to cancel."), false, response -> {
                if (response.equals("yes")) {
                    plugin.sql.execute("DELETE FROM homes WHERE uuid=?;", player.getUniqueId());
                    player.sendMessage(FormatUtils.color("&aDeleted all homes."));
                }
            });
            return;
        }
        List<String> homes = getHomeNames(player);
        if (!homes.contains(name)) {
            player.sendMessage(FormatUtils.color("&cThis home does not exist."));
            return;
        }
        plugin.sql.execute("DELETE FROM homes WHERE uuid=? AND name=?;", player.getUniqueId().toString(), name);
        player.sendMessage(FormatUtils.color("&aDeleted home " + name + "."));
    }

}
