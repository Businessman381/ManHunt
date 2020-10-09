package businessman381.manhunt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class ManHunt extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
  private HashMap<UUID, Integer> hunters;
  
  public void onEnable() {
    getServer().getPluginManager().registerEvents(this, (Plugin)this);
    getServer().getPluginCommand("hunter").setExecutor(this);
    getServer().getPluginCommand("hunter").setTabCompleter(this);
    this.hunters = new HashMap<>();
  }
  
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (command.getName().equalsIgnoreCase("hunter") && 
      sender instanceof Player) {
      Player player = (Player)sender;
      if (args.length == 2) {
        if (args[0].equalsIgnoreCase("join")) {
          if (isInt(args[1])) {
            int hunterNumber = Integer.parseInt(args[1]);
            if (!this.hunters.containsKey(player.getUniqueId())) {
              this.hunters.put(player.getUniqueId(), Integer.valueOf(hunterNumber));
              player.sendMessage(ChatColor.GREEN + "You have joined the 'hunter " + args[1] + "' team.");
              ItemStack compass = new ItemStack(Material.COMPASS);
              ItemMeta compassMeta = compass.getItemMeta();
              compassMeta.setDisplayName("Hunter " + this.hunters.get(player.getUniqueId()));
              compass.setItemMeta(compassMeta);
              if (player.getInventory().firstEmpty() == -1) {
                player.getWorld().dropItemNaturally(player.getLocation(), compass);
              } else {
                player.getInventory().addItem(new ItemStack[] { compass });
              } 
            } else {
              player.sendMessage(ChatColor.RED + "You are already a hunter!");
            } 
          } else {
            sendInvalid(sender);
          } 
        } else {
          sendInvalid(sender);
        } 
      } else if (args.length == 1) {
        if (args[0].equalsIgnoreCase("leave")) {
          if (this.hunters.containsKey(player.getUniqueId())) {
            this.hunters.remove(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "You are no longer a hunter.");
            player.getInventory().remove(Material.COMPASS);
          } else {
            player.sendMessage(ChatColor.RED + "You are not a hunter!");
          } 
        } else {
          sendInvalid(sender);
        } 
      } else if (args[0].equalsIgnoreCase("player")) {
        Player target = Bukkit.getPlayer(args[1]);
        if (args.length == 3) {
          if (args[2].equalsIgnoreCase("leave")) {
            if (this.hunters.containsKey(target.getUniqueId())) {
              this.hunters.remove(target.getUniqueId());
              player.sendMessage(ChatColor.GREEN + target.getName() + " is no longer a hunter.");
              target.sendMessage(ChatColor.GREEN + "You are no longer a hunter.");
              target.getInventory().remove(Material.COMPASS);
            } else {
              player.sendMessage(ChatColor.RED + "This player is not a hunter!");
            } 
          } else {
            sendInvalid(sender);
          } 
        } else if (args.length == 4 && 
          args[2].equalsIgnoreCase("join")) {
          if (isInt(args[3])) {
            int hunterNumber = Integer.parseInt(args[3]);
            if (!this.hunters.containsKey(player.getUniqueId())) {
              this.hunters.put(target.getUniqueId(), Integer.valueOf(hunterNumber));
              ItemStack compass = new ItemStack(Material.COMPASS);
              ItemMeta compassMeta = compass.getItemMeta();
              compassMeta.setDisplayName("Hunter " + this.hunters.get(target.getUniqueId()));
              compass.setItemMeta(compassMeta);
              if (target.getInventory().firstEmpty() == -1) {
                target.getWorld().dropItemNaturally(player.getLocation(), compass);
              } else {
                target.getInventory().addItem(new ItemStack[] { compass });
              } 
              player.sendMessage(ChatColor.GREEN + target.getName() + " have joined the 'hunter " + args[3] + "' team.");
              target.sendMessage(ChatColor.GREEN + "You have joined the 'hunter " + args[1] + "' team.");
            } else {
              player.sendMessage(ChatColor.RED + "This player is already a hunter!");
            } 
          } else {
            sendInvalid(sender);
          } 
        } 
      } else {
        sendInvalid(sender);
      } 
    } 
    return false;
  }
  
  private void sendInvalid(CommandSender sender) {
    sender.sendMessage(ChatColor.RED + "Invalid usage. Please use:");
    sender.sendMessage(ChatColor.RED + "/hunter join [number]");
    sender.sendMessage(ChatColor.RED + "/hunter leave");
  }
  
  @EventHandler
  public void onPlayerInteractEvent(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    if (this.hunters.containsKey(player.getUniqueId()) && event.hasItem() && event.getItem().getType() == Material.COMPASS && (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR)) {
      Player nearest = null;
      double distance = Double.MAX_VALUE;
      if (player.getWorld().getEnvironment().equals(World.Environment.NETHER) || player.getWorld().getEnvironment().equals(World.Environment.THE_END)) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
          if (onlinePlayer.equals(player) || !onlinePlayer.getWorld().equals(player.getWorld()) || onlinePlayer.getGameMode().equals(GameMode.SPECTATOR) || this.hunters.get(onlinePlayer.getUniqueId()) == this.hunters.get(player.getUniqueId()))
            continue; 
          double distanceSquared = onlinePlayer.getLocation().distanceSquared(player.getLocation());
          if (distanceSquared < distance) {
            distance = distanceSquared;
            nearest = onlinePlayer;
          } 
        } 
        if (nearest == null) {
          player.sendMessage(ChatColor.RED + "No players to track!");
          return;
        } 
        ItemStack compass = event.getItem();
        CompassMeta compassMeta = (CompassMeta)compass.getItemMeta();
        compassMeta.setLodestoneTracked(false);
        compassMeta.setLodestone(nearest.getLocation());
        compass.setItemMeta((ItemMeta)compassMeta);
        player.sendMessage(ChatColor.GREEN + "Compass is now pointing to " + nearest.getName() + ".");
      } else if (player.getWorld().getEnvironment().equals(World.Environment.NORMAL)) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
          if (onlinePlayer.equals(player) || !onlinePlayer.getWorld().equals(player.getWorld()) || onlinePlayer.getGameMode().equals(GameMode.SPECTATOR) || this.hunters.get(onlinePlayer.getUniqueId()) == this.hunters.get(player.getUniqueId()))
            continue; 
          double distanceSquared = onlinePlayer.getLocation().distanceSquared(player.getLocation());
          if (distanceSquared < distance) {
            distance = distanceSquared;
            nearest = onlinePlayer;
          } 
        } 
        if (nearest == null) {
          player.sendMessage(ChatColor.RED + "No players to track!");
          return;
        } 
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta compassMeta = compass.getItemMeta();
        compassMeta.setDisplayName("Hunter " + this.hunters.get(player.getUniqueId()));
        compass.setItemMeta(compassMeta);
        int slot = 0;
        if (event.getHand() == EquipmentSlot.HAND) {
          slot = player.getInventory().getHeldItemSlot();
        } else if (event.getHand() == EquipmentSlot.OFF_HAND) {
          slot = 40;
        } 
        event.getItem().setAmount(0);
        player.getInventory().setItem(slot, compass);
        player.setCompassTarget(nearest.getLocation());
        player.sendMessage(ChatColor.GREEN + "Compass is now pointing to " + nearest.getName() + ".");
      } 
    } 
    if (this.hunters.containsKey(player.getUniqueId()) && event.hasItem() && event.getItem().getType() == Material.COMPASS && (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR)) {
      Player nearest = null;
      double distance = Double.MAX_VALUE;
      if (player.getWorld().getEnvironment().equals(World.Environment.NETHER) || player.getWorld().getEnvironment().equals(World.Environment.THE_END)) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
          if (onlinePlayer.equals(player) || !onlinePlayer.getWorld().equals(player.getWorld()) || onlinePlayer.getGameMode().equals(GameMode.SPECTATOR) || this.hunters.get(onlinePlayer.getUniqueId()) == this.hunters.get(player.getUniqueId()))
            continue; 
          double distanceSquared = onlinePlayer.getLocation().distanceSquared(player.getLocation());
          if (distanceSquared < distance) {
            distance = distanceSquared;
            nearest = onlinePlayer;
          } 
        } 
        if (nearest == null) {
          player.sendMessage(ChatColor.RED + "No players to track!");
          return;
        } 
        ItemStack compass = event.getItem();
        CompassMeta compassMeta = (CompassMeta)compass.getItemMeta();
        compassMeta.setLodestoneTracked(false);
        compassMeta.setLodestone(nearest.getLocation());
        compassMeta.setDisplayName("Hunter " + this.hunters.get(player.getUniqueId()));
        compass.setItemMeta((ItemMeta)compassMeta);
        player.sendMessage(ChatColor.GREEN + "Compass is now pointing to " + nearest.getName() + ".");
      } else if (player.getWorld().getEnvironment().equals(World.Environment.NORMAL)) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
          if (onlinePlayer.equals(player) || !onlinePlayer.getWorld().equals(player.getWorld()) || onlinePlayer.getGameMode().equals(GameMode.SPECTATOR) || this.hunters.get(onlinePlayer.getUniqueId()) == this.hunters.get(player.getUniqueId()))
            continue; 
          double distanceSquared = onlinePlayer.getLocation().distanceSquared(player.getLocation());
          if (distanceSquared < distance) {
            distance = distanceSquared;
            nearest = onlinePlayer;
          } 
        } 
        if (nearest == null) {
          player.sendMessage(ChatColor.RED + "No players to track!");
          return;
        } 
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta compassMeta = compass.getItemMeta();
        compassMeta.setDisplayName("Hunter " + this.hunters.get(player.getUniqueId()));
        compass.setItemMeta(compassMeta);
        player.getInventory().addItem(new ItemStack[] { compass });
        int slot = 0;
        if (event.getHand() == EquipmentSlot.HAND) {
          slot = player.getInventory().getHeldItemSlot();
        } else if (event.getHand() == EquipmentSlot.OFF_HAND) {
          slot = 40;
        } 
        event.getItem().setAmount(0);
        player.getInventory().setItem(slot, compass);
        player.setCompassTarget(nearest.getLocation());
        player.sendMessage(ChatColor.GREEN + "Compass is now pointing to " + nearest.getName() + ".");
      } 
    } 
  }
  
  @EventHandler
  public void onPlayerDeathEvent(PlayerDeathEvent event) {
    if (this.hunters.containsKey(event.getEntity().getUniqueId())) {
      event.getDrops().removeIf(next -> (next.getType() == Material.COMPASS));
    } else if (this.hunters.containsKey(event.getEntity().getUniqueId())) {
      event.getDrops().removeIf(next -> (next.getType() == Material.COMPASS));
    } 
  }
  
  @EventHandler
  public void onPlayerDropItemEvent(PlayerDropItemEvent event) {
    if (this.hunters.containsKey(event.getPlayer().getUniqueId()) && event.getItemDrop().getItemStack().getType() == Material.COMPASS) {
      event.setCancelled(true);
    } else if (this.hunters.containsKey(event.getPlayer().getUniqueId()) && event.getItemDrop().getItemStack().getType() == Material.COMPASS) {
      event.setCancelled(true);
    } 
  }
  
  @EventHandler
  public void onPlayerRespawnEvent(PlayerRespawnEvent event) {
    Player player = event.getPlayer();
    if (this.hunters.containsKey(player.getUniqueId())) {
      ItemStack compass = new ItemStack(Material.COMPASS);
      ItemMeta compassMeta = compass.getItemMeta();
      compassMeta.setDisplayName("Hunter " + this.hunters.get(player.getUniqueId()));
      compass.setItemMeta(compassMeta);
      player.getInventory().addItem(new ItemStack[] { compass });
    } 
  }
  
  public void onDisable() {
    for (Player online : Bukkit.getOnlinePlayers()) {
      if (this.hunters.containsKey(online.getUniqueId()))
        online.getInventory().remove(Material.COMPASS); 
    } 
  }
  
  public static boolean isInt(String s) {
    try {
      Integer.parseInt(s);
    } catch (NumberFormatException ex) {
      return false;
    } 
    return true;
  }
  
  public ArrayList<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
    ArrayList<String> l = new ArrayList<>();
    if (args.length == 1) {
      l.clear();
      l.add("join");
      l.add("leave");
      l.add("player");
    } else if (args.length == 2) {
      if (args[0].equalsIgnoreCase("player")) {
        l.clear();
        for (Player p : Bukkit.getOnlinePlayers())
          l.add(p.getName()); 
      } 
    } else if (args.length == 3 && 
      args[0].equalsIgnoreCase("player")) {
      l.clear();
      l.add("join");
      l.add("leave");
    } 
    return l;
  }
}
