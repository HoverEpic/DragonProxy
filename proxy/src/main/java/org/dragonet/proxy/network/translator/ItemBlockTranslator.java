/*
 * GNU LESSER GENERAL PUBLIC LICENSE
 *                       Version 3, 29 June 2007
 *
 * Copyright (C) 2007 Free Software Foundation, Inc. <http://fsf.org/>
 * Everyone is permitted to copy and distribute verbatim copies
 * of this license document, but changing it is not allowed.
 *
 * You can view LICENCE file for details. 
 *
 * @author The Dragonet Team
 */
package org.dragonet.proxy.network.translator;

import java.util.HashMap;
import java.util.Map;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;

import org.dragonet.proxy.protocol.type.Slot;

public class ItemBlockTranslator {
	// vars
	public static final int UNSUPPORTED_BLOCK_ID = 165;
	public static final String DRAGONET_COMPOUND = "DragonetNBT";
	public static final Map<Integer, Integer> PC_TO_PE_OVERRIDE = new HashMap<>();
	public static final Map<Integer, Integer> PE_TO_PC_OVERRIDE = new HashMap<>();
	public static final Map<Integer, String> NAME_OVERRIDES = new HashMap<>();

	static {
		swap(125, 157); // Double Slab <-> Activator Rail
		onewayOverride(126, 158); // Slab <-> NULL
		onewayOverride(95, 20, "Stained Glass"); // Stained Glass = Glass
		onewayOverride(160, 102, "Stained Glass Pane"); // Stained Glass Pane = Glass Pane
		onewayOverride(119, 90); // End portal -> Nether portal
		onewayOverride(176, 63, "Banner"); // Sign =\_
		onewayOverride(177, 68, "Banner"); // Wall sign =/ We send banner as sign [Banner]
		onewayOverride(36, 248);
		onewayOverride(84, 248);
		onewayOverride(122, 248);
		onewayOverride(130, 248);
		onewayOverride(137, 248);
		onewayOverride(138, 248);
		onewayOverride(160, 248);
		onewayOverride(166, 248);
		onewayOverride(168, 248);
		onewayOverride(169, 248);
		onewayOverride(176, 248);
		onewayOverride(177, 248);
		onewayOverride(188, 248);
		onewayOverride(189, 248);
		onewayOverride(190, 248);
		onewayOverride(191, 248);
		onewayOverride(192, 248);
	}

	// constructor
	public ItemBlockTranslator() {

	}

	// public
	// Query handler
	public static int translateToPE(int pcItemBlockId) {
		if (!PC_TO_PE_OVERRIDE.containsKey(pcItemBlockId)) {
			return pcItemBlockId;
		}
		int ret = PC_TO_PE_OVERRIDE.get(pcItemBlockId);
		if (pcItemBlockId >= 255 && ret == UNSUPPORTED_BLOCK_ID) {
			ret = 0; // Unsupported item becomes air
		}
		return ret;
	}

	public static int translateToPC(int peItemBlockId) {
		if (!PE_TO_PC_OVERRIDE.containsKey(peItemBlockId)) {
			return peItemBlockId;
		}
		int ret = PE_TO_PC_OVERRIDE.get(peItemBlockId);
		return ret;
	}

	@SuppressWarnings("unchecked")
	public static org.dragonet.proxy.nbt.tag.CompoundTag translateNBT(int id, CompoundTag pcTag, org.dragonet.proxy.nbt.tag.CompoundTag target) {
		if (pcTag != null && pcTag.contains("display")) {
			CompoundTag pcDisplay = pcTag.get("display");
			org.dragonet.proxy.nbt.tag.CompoundTag peDisplay;
			if(target.contains("display")) {
				peDisplay = target.getCompound("display");
			} else {
				peDisplay = new org.dragonet.proxy.nbt.tag.CompoundTag();
				target.put("display", peDisplay);
			}
			if (pcDisplay.contains("Name")) {
				peDisplay.put("Name", new org.dragonet.proxy.nbt.tag.StringTag("Name",
						((StringTag)pcDisplay.get("Name")).getValue()));
			}
		} else {
			if (NAME_OVERRIDES.containsKey(id)) {
				org.dragonet.proxy.nbt.tag.CompoundTag peDisplay;
				if(target.contains("display")) {
					peDisplay = target.getCompound("display");
				} else {
					peDisplay = new org.dragonet.proxy.nbt.tag.CompoundTag();
					target.put("display", peDisplay);
				}
				target.put("display", peDisplay);
				peDisplay.put("Name", new org.dragonet.proxy.nbt.tag.StringTag("Name", NAME_OVERRIDES.get(id)));
			}
		}
		return target;
	}

	public static Slot translateSlotToPE(ItemStack item) {
		if (item == null || item.getId() == 0)
			return null;
		Slot inv = new Slot();
		inv.id = translateToPE(item.getId());
		inv.damage = item.getData();
		inv.count = (item.getAmount() & 0xff);
		org.dragonet.proxy.nbt.tag.CompoundTag tag = new org.dragonet.proxy.nbt.tag.CompoundTag();
		tag.putShort("id", item.getId());
		tag.putShort("amount", item.getAmount());
		tag.putShort("data", item.getData());
		org.dragonet.proxy.nbt.tag.CompoundTag rootTag = new org.dragonet.proxy.nbt.tag.CompoundTag();
		rootTag.put(DRAGONET_COMPOUND, tag);
		inv.tag = rootTag;
		translateNBT(item.getId(), item.getNBT(), inv.tag);
		return inv;
	}

	public static ItemStack translateToPC(Slot slot) {
		ItemStack item;
		org.dragonet.proxy.nbt.tag.CompoundTag tag = slot.tag;
		if (tag != null && tag.contains(DRAGONET_COMPOUND)) {
			item = new ItemStack(tag.getCompound(DRAGONET_COMPOUND).getShort("id"),
					tag.getCompound(DRAGONET_COMPOUND).getShort("amount"),
					tag.getCompound(DRAGONET_COMPOUND).getShort("data"));
		} else {
			item = new ItemStack(translateToPC(slot.id), slot.count, slot.damage);
		}

		return item;
	}

	// private
	private static void swap(int pcId, int peId) {
		PC_TO_PE_OVERRIDE.put(pcId, peId);
		PE_TO_PC_OVERRIDE.put(peId, pcId);
	}

	private static void onewayOverride(int fromPc, int toPe, String nameOverride) {
		onewayOverride(fromPc, toPe);
		if (nameOverride != null) {
			NAME_OVERRIDES.put(fromPc, nameOverride);
		}
	}

	private static void onewayOverride(int fromPc, int toPe) {
		PC_TO_PE_OVERRIDE.put(fromPc, toPe);
	}

	public static CompoundTag newTileTag(String id, int x, int y, int z) {
		CompoundTag t = new CompoundTag(null);
		t.put(new StringTag("id", id));
		t.put(new IntTag("x", x));
		t.put(new IntTag("y", y));
		t.put(new IntTag("z", z));
		return t;
	}
}
