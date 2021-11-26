package com.volmit.adapt.content.adaptation.rift;

import com.volmit.adapt.api.adaptation.SimpleAdaptation;
import com.volmit.adapt.api.recipe.AdaptRecipe;
import com.volmit.adapt.content.item.BoundEnderPearl;
import com.volmit.adapt.util.C;
import com.volmit.adapt.util.Element;
import com.volmit.adapt.util.J;
import com.volmit.adapt.util.KList;
import lombok.NoArgsConstructor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;

public class RiftAccess extends SimpleAdaptation<RiftAccess.Config> {
    private final KList<InventoryView> activeViews = new KList<>();

    public RiftAccess() {
        super("rift-access");
        registerConfiguration(Config.class);
        setDescription("Pull from the void");
        setIcon(Material.NETHER_STAR);
        setBaseCost(0);
        setCostFactor(0);
        setMaxLevel(1);
        setInitialCost(15);
        setInterval(50);
        registerRecipe(AdaptRecipe.shapeless()
                .key("rift-access")
                .ingredient(Material.ENDER_PEARL)
                .ingredient(Material.DIAMOND)
                .ingredient(Material.CHEST)
                .result(BoundEnderPearl.io.withData(new BoundEnderPearl.Data(null)))
                .build());
    }



    @Override
    public void addStats(int level, Element v) {
        v.addLore(C.ITALIC + "Enderpearl + Chest + Diamond = Reliquary Portkey");
        v.addLore(C.ITALIC + "This item allows you to access containers remotely");
        v.addLore(C.ITALIC + "Once crafted look at item to see usage");
    }

    @EventHandler
    public void on(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        ItemMeta handMeta = hand.getItemMeta();
        Block block = e.getClickedBlock();


        if (!hasAdaptation(p) || !hand.hasItemMeta() || !handMeta.getLore().get(0).equals(C.UNDERLINE + "Portkey")) {
            return;
        }
        switch (e.getAction()) {
            case LEFT_CLICK_BLOCK, RIGHT_CLICK_BLOCK -> {
                if (isStorage(block.getBlockData())) { // Ensure its a container
                    if (p.isSneaking()) { // Binding (Sneak Container)
                        linkPearl(p, block);
                    } else if (!p.isSneaking()) {
                        openPearl(p);
                    }
                } else if (!isStorage(block.getBlockData())) {
                    if (p.isSneaking()) { //(Sneak NOT Container)
                        p.sendMessage(C.LIGHT_PURPLE + "That's not a container");
                    } else if (!p.isSneaking() && isBound(hand)) {
                        openPearl(p);
                    }

                }
                e.setCancelled(true);

            }
            case RIGHT_CLICK_AIR, LEFT_CLICK_AIR -> {
                if (isBound(hand)) {
                    openPearl(p);
                }
                e.setCancelled(true);

            }
        }

    }

    private void linkPearl(Player p, Block block) {
        ItemStack hand = p.getInventory().getItemInMainHand();

        if (hand.getAmount() == 1) {
            BoundEnderPearl.setData(hand, block);
        } else {
            hand.setAmount(hand.getAmount() - 1);
            ItemStack pearl = BoundEnderPearl.withData(block);
            p.getInventory().addItem(pearl).values().forEach(i -> p.getWorld().dropItemNaturally(p.getLocation(), i));
        }
    }


    private void openPearl(Player p) {
        Block b = BoundEnderPearl.getBlock(p.getInventory().getItemInMainHand());

        if (b != null && b.getState() instanceof InventoryHolder holder) {
            activeViews.add(p.openInventory(holder.getInventory()));
            p.playSound(p.getLocation(), Sound.PARTICLE_SOUL_ESCAPE, 100f, 0.10f);
            p.playSound(p.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 100f, 0.10f);
        }
    }

    private boolean isBound(ItemStack stack) {
        return BoundEnderPearl.getBlock(stack) != null;
    }

    @Override
    public void onTick() {
        J.s(() -> {
            for (int ii = activeViews.size() - 1; ii >= 0; ii--) {
                InventoryView i = activeViews.get(ii);

                if (i.getPlayer().getOpenInventory().equals(i)) {
                    if (i.getTopInventory().getLocation() == null || !isStorage(i.getTopInventory().getLocation().getBlock().getBlockData())) {
                        i.getPlayer().closeInventory();
                        i.getPlayer().removePotionEffect(PotionEffectType.BLINDNESS);
                        activeViews.remove(ii);
                    }
                } else {
                    i.getPlayer().removePotionEffect(PotionEffectType.BLINDNESS);
                    activeViews.remove(ii);
                }
            }
        });
    }

    @Override
    public boolean isEnabled() {
        return getConfig().enabled;
    }

    @NoArgsConstructor
    protected static class Config {
        boolean enabled = true;
    }
}