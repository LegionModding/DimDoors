package org.dimdev.dimdoors.network.s2c;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.dimdev.dimdoors.network.ClientPacketHandler;
import org.dimdev.dimdoors.network.SimplePacket;
import org.dimdev.dimdoors.world.pocket.type.addon.AutoSyncedAddon;

import java.io.IOException;
import java.util.List;

public class SyncPocketAddonsS2CPacket implements SimplePacket<ClientPacketHandler> {
	public static final Identifier ID = new Identifier("dimdoors:sync_pocket_addons");

	private RegistryKey<World> world;
	private int gridSize;
	private int pocketId;
	private int pocketRange;
	private List<AutoSyncedAddon> addons;

	@Environment(EnvType.CLIENT)
	public SyncPocketAddonsS2CPacket() {
	}

	public SyncPocketAddonsS2CPacket(RegistryKey<World> world, int gridSize, int pocketId, int pocketRange, List<AutoSyncedAddon> addons) {
		this.world = world;
		this.gridSize = gridSize;
		this.pocketId = pocketId;
		this.pocketRange = pocketRange;
		this.addons = addons;
	}

	@Override
	public SimplePacket<ClientPacketHandler> read(PacketByteBuf buf) throws IOException {
		this.world = RegistryKey.of(Registry.DIMENSION, buf.readIdentifier());
		this.gridSize = buf.readInt();
		this.pocketId = buf.readInt();
		this.pocketRange = buf.readInt();
		this.addons = AutoSyncedAddon.readAutoSyncedAddonList(buf);
		return this;
	}

	@Override
	public PacketByteBuf write(PacketByteBuf buf) throws IOException {
		buf.writeIdentifier(world.getValue());
		buf.writeInt(gridSize);
		buf.writeInt(pocketId);
		buf.writeInt(pocketRange);
		AutoSyncedAddon.writeAutoSyncedAddonList(buf, addons);
		return buf;
	}

	@Override
	public void apply(ClientPacketHandler listener) {
		listener.onSyncPocketAddons(world, gridSize, pocketId, pocketRange, addons);
	}

	@Override
	public Identifier channelId() {
		return ID;
	}
}
