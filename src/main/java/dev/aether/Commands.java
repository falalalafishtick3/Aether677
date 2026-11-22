package dev.aether;

import dev.aether.module.ClientModule;
import dev.aether.module.ModuleManager;
import dev.aether.module.impl.Panic;
import dev.aether.util.Friends;
import dev.aether.util.WaypointStore;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.stream.Collectors;

/** Chat commands: type .help in chat. Messages starting with "." + a letter never reach the server. */
public final class Commands {
    public static void init() {
        ClientSendMessageEvents.ALLOW_CHAT.register(msg -> {
            if (msg.length() < 2 || msg.charAt(0) != '.' || !Character.isLetter(msg.charAt(1))) return true;
            try { run(MinecraftClient.getInstance(), msg.substring(1).trim()); }
            catch (Exception e) { say(MinecraftClient.getInstance(), "Error: " + e.getMessage()); }
            return false;
        });
    }

    private static void say(MinecraftClient mc, String s) {
        if (mc.player != null) mc.player.sendMessage(Text.literal("[Aether] " + s), false);
    }

    private static ClientModule find(String name) {
        for (ClientModule m : ModuleManager.MODULES) if (m.name.equalsIgnoreCase(name)) return m;
        return null;
    }

    private static int keyOf(String name) {
        try { return GLFW.class.getField("GLFW_KEY_" + name.toUpperCase().replace(' ', '_')).getInt(null); }
        catch (Exception e) { return -2; }
    }

    private static void run(MinecraftClient mc, String line) {
        String[] a = line.split("\\s+");
        String c = a[0].toLowerCase();
        switch (c) {
            case "help" -> {
                say(mc, "Commands (prefix .):");
                say(mc, ".toggle <module> (.t)   .bind <module> <KEY>   .unbind <module>   .modules");
                say(mc, ".friend add|remove|list [name]   .wp add [name] | list | del <name>   .coords   .panic");
            }
            case "toggle", "t" -> {
                ClientModule m = a.length > 1 ? find(a[1]) : null;
                if (m == null) { say(mc, "Unknown module. Try .modules"); return; }
                m.toggle();
                say(mc, m.name + (m.enabled ? " enabled" : " disabled"));
            }
            case "bind" -> {
                ClientModule m = a.length > 2 ? find(a[1]) : null;
                if (m == null) { say(mc, "Usage: .bind <module> <KEY>  e.g. .bind Freecam F4"); return; }
                int k = keyOf(a[2]);
                if (k == -2) { say(mc, "Unknown key '" + a[2] + "' (use GLFW names like F4, X, RIGHT_SHIFT, LEFT_ALT)"); return; }
                m.key = k;
                say(mc, m.name + " bound to " + a[2].toUpperCase());
            }
            case "unbind" -> {
                ClientModule m = a.length > 1 ? find(a[1]) : null;
                if (m == null) { say(mc, "Unknown module"); return; }
                m.key = -1; say(mc, m.name + " unbound");
            }
            case "modules" -> say(mc, ModuleManager.MODULES.stream().map(m -> (m.enabled ? "+" : "") + m.name).collect(Collectors.joining(", ")));
            case "friend" -> {
                String sub = a.length > 1 ? a[1].toLowerCase() : "";
                if (sub.equals("list")) { say(mc, "Use FriendToggle to manage; names are stored in config/aether/friends.json"); return; }
                if (a.length < 3) { say(mc, "Usage: .friend add|remove <name>"); return; }
                boolean isF = Friends.is(a[2]);
                if (sub.equals("add") && !isF) { Friends.toggle(a[2]); say(mc, "Added " + a[2]); }
                else if (sub.equals("remove") && isF) { Friends.toggle(a[2]); say(mc, "Removed " + a[2]); }
                else say(mc, sub.equals("add") ? a[2] + " is already a friend" : a[2] + " is not a friend");
            }
            case "wp" -> {
                String sub = a.length > 1 ? a[1].toLowerCase() : "list";
                String dim = mc.world.getRegistryKey().getValue().getPath();
                switch (sub) {
                    case "add" -> {
                        WaypointStore.Entry e = new WaypointStore.Entry();
                        e.name = a.length > 2 ? a[2] : "WP " + (WaypointStore.all().size() + 1);
                        e.dim = dim; e.x = (int) Math.floor(mc.player.getX()); e.y = (int) Math.floor(mc.player.getY()); e.z = (int) Math.floor(mc.player.getZ());
                        e.hue = (float) Math.random();
                        WaypointStore.add(e);
                        say(mc, "Saved " + e.name + " at " + e.x + ", " + e.y + ", " + e.z);
                    }
                    case "del" -> {
                        if (a.length < 3) { say(mc, "Usage: .wp del <name>"); return; }
                        for (WaypointStore.Entry e : WaypointStore.all()) if (e.name.equalsIgnoreCase(a[2])) { WaypointStore.remove(e); say(mc, "Deleted " + e.name); return; }
                        say(mc, "No waypoint named " + a[2]);
                    }
                    default -> {
                        var l = WaypointStore.all();
                        if (l.isEmpty()) say(mc, "No waypoints");
                        for (WaypointStore.Entry e : l) say(mc, e.name + ": " + e.x + ", " + e.y + ", " + e.z + " (" + e.dim + ")");
                    }
                }
            }
            case "coords" -> {
                String s = (int) mc.player.getX() + " " + (int) mc.player.getY() + " " + (int) mc.player.getZ();
                mc.keyboard.setClipboard(s);
                say(mc, "Copied " + s);
            }
            case "panic" -> { for (ClientModule m : ModuleManager.MODULES) if (m instanceof Panic) m.onEnable(); }
            default -> say(mc, "Unknown command. Type .help");
        }
    }
}
