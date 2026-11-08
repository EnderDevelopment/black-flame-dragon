package com.enderdevelopment.plugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public final class DragonListener implements Listener {
    static DragonListener instance;
    private final JavaPlugin plugin;
    private final Set<UUID> dragons = new HashSet<>();
    private final Map<UUID, ArmorSet> savedArmor = new HashMap<>();
    private final Map<UUID, Long> fireCooldowns = new HashMap<>();

    DragonListener(JavaPlugin plugin) {
        this.plugin = plugin;
        instance = this;
        Bukkit.getScheduler().runTaskTimer(plugin, this::displayDragonEyes, 0L, 4L);
    }

    @EventHandler
    public void onClockUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.CLOCK) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (dragons.contains(player.getUniqueId())) {
            restore(player);
        } else {
            transform(player);
        }
    }

    private void transform(Player player) {
        UUID id = player.getUniqueId();
        savedArmor.put(id, new ArmorSet(player.getInventory()));
        dragons.add(id);
        PlayerInventory inventory = player.getInventory();
        inventory.setHelmet(leather(Material.LEATHER_HELMET, Color.BLACK));
        inventory.setChestplate(leather(Material.LEATHER_CHESTPLATE, Color.BLACK));
        inventory.setLeggings(leather(Material.LEATHER_LEGGINGS, Color.BLACK));
        inventory.setBoots(leather(Material.LEATHER_BOOTS, Color.BLACK));
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
        player.setAllowFlight(false);
        player.setFlying(false);
        player.sendMessage(Component.text("You became a black fire dragon! Shift to breathe fire; jump once to fly.", NamedTextColor.DARK_RED));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);
    }

    private ItemStack leather(Material material, Color color) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        if (meta != null) {
            meta.setColor(color);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void displayDragonEyes() {
        for (UUID id : new HashSet<>(dragons)) {
            Player player = Bukkit.getPlayer(id);
            if (player == null || !player.isOnline()) {
                continue;
            }
            Location head = player.getEyeLocation();
            Vector forward = head.getDirection().normalize();
            Vector side = new Vector(-forward.getZ(), 0.0, forward.getX()).normalize().multiply(0.22);
            Location leftEye = head.clone().add(forward.clone().multiply(0.18)).add(side);
            Location rightEye = head.clone().add(forward.clone().multiply(0.18)).subtract(side);
            Particle.DustOptions fireColor = new Particle.DustOptions(Color.RED, 1.2f);
            player.getWorld().spawnParticle(Particle.DUST, leftEye, 2, 0.02, 0.02, 0.02, 0.0, fireColor);
            player.getWorld().spawnParticle(Particle.DUST, rightEye, 2, 0.02, 0.02, 0.02, 0.0, fireColor);
            player.getWorld().spawnParticle(Particle.FLAME, leftEye, 1, 0.03, 0.03, 0.03, 0.01);
            player.getWorld().spawnParticle(Particle.FLAME, rightEye, 1, 0.03, 0.03, 0.03, 0.01);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!dragons.contains(player.getUniqueId()) || player.getAllowFlight()) {
            return;
        }
        if (event.getFrom().getY() < event.getTo().getY() && player.getVelocity().getY() > 0.25) {
            player.setAllowFlight(true);
            player.setFlying(true);
            player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 35, 0.5, 0.3, 0.5, 0.04);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.8f);
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking() || !dragons.contains(event.getPlayer().getUniqueId())) {
            return;
        }
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        long previous = fireCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - previous < 1200L) {
            return;
        }
        fireCooldowns.put(player.getUniqueId(), now);
        Vector direction = player.getEyeLocation().getDirection().normalize();
        SmallFireball fireball = player.getWorld().spawn(player.getEyeLocation().add(direction), SmallFireball.class);
        fireball.setDirection(direction);
        fireball.setShooter(player);
        fireball.setYield(1.5f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.7f);
    }

    @EventHandler
    public void onDragonDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && dragons.contains(player.getUniqueId())
                && event.getCause() == EntityDamageEvent.DamageCause.FIRE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (dragons.contains(event.getPlayer().getUniqueId())) {
            restore(event.getPlayer());
        }
    }

    private void restore(Player player) {
        UUID id = player.getUniqueId();
        ArmorSet armor = savedArmor.remove(id);
        if (armor != null) {
            armor.restore(player.getInventory());
        }
        dragons.remove(id);
        fireCooldowns.remove(id);
        player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        player.removePotionEffect(PotionEffectType.SPEED);
        if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            player.setFlying(false);
            player.setAllowFlight(false);
        }
        player.sendMessage(Component.text("You returned to your normal form.", NamedTextColor.GRAY));
    }

    void restoreAll() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (dragons.contains(player.getUniqueId())) {
                restore(player);
            }
        }
    }

    private record ArmorSet(ItemStack helmet, ItemStack chestplate, ItemStack leggings, ItemStack boots) {
        ArmorSet(PlayerInventory inventory) {
            this(inventory.getHelmet(), inventory.getChestplate(), inventory.getLeggings(), inventory.getBoots());
        }

        void restore(PlayerInventory inventory) {
            inventory.setHelmet(helmet);
            inventory.setChestplate(chestplate);
            inventory.setLeggings(leggings);
            inventory.setBoots(boots);
        }
    }
}
