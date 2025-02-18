package org.dimdev.dimdoors.world.pocket.type;

import com.mojang.serialization.Lifecycle;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.World;

import java.util.Map;
import java.util.function.Supplier;

public abstract class AbstractPocket<V extends AbstractPocket<?>> {
	public static final Registry<AbstractPocketType<? extends AbstractPocket<?>>> REGISTRY = FabricRegistryBuilder.from(new SimpleRegistry<AbstractPocketType<? extends AbstractPocket<?>>>(RegistryKey.ofRegistry(new Identifier("dimdoors", "abstract_pocket_type")), Lifecycle.stable())).buildAndRegister();

	protected Integer id;
	protected RegistryKey<World> world;

	public AbstractPocket(int id, RegistryKey<World> world) {
		this.id = id;
		this.world = world;
	}

	protected AbstractPocket() {
	}

	public int getId() {
		return id;
	}

	public static AbstractPocket<? extends AbstractPocket<?>> deserialize(CompoundTag tag) {
		Identifier id = Identifier.tryParse(tag.getString("type"));
		return REGISTRY.get(id).fromTag(tag);
	}

	public static AbstractPocketBuilder<?, ?> deserializeBuilder(CompoundTag tag) {
		Identifier id = Identifier.tryParse(tag.getString("type"));
		return REGISTRY.get(id).builder().fromTag(tag);
	}

	public static CompoundTag serialize(AbstractPocket<?> pocket) {
		return pocket.toTag(new CompoundTag());
	}

	public V fromTag(CompoundTag tag) {
		this.id = tag.getInt("id");
		this.world = RegistryKey.of(Registry.DIMENSION, new Identifier(tag.getString("world")));

		return (V) this;
	}

	public CompoundTag toTag(CompoundTag tag) {
		tag.putInt("id", id);
		tag.putString("world", world.getValue().toString());

		getType().toTag(tag);

		return tag;
	}

	public abstract AbstractPocketType<?> getType();

	public Map<String, Double> toVariableMap(Map<String, Double> variableMap) {
		variableMap.put("id", (double) this.id);
		return variableMap;
	}

	public abstract Pocket getReferencedPocket();

	public RegistryKey<World> getWorld() {
		return world;
	}

	public interface AbstractPocketType<T extends AbstractPocket<?>> {
		AbstractPocketType<IdReferencePocket> ID_REFERENCE = register(new Identifier("dimdoors", IdReferencePocket.KEY), IdReferencePocket::new, IdReferencePocket::builder);

		AbstractPocketType<Pocket> POCKET = register(new Identifier("dimdoors", Pocket.KEY), Pocket::new, Pocket::builder);
		AbstractPocketType<PrivatePocket> PRIVATE_POCKET = register(new Identifier("dimdoors", PrivatePocket.KEY), PrivatePocket::new, PrivatePocket::builderPrivatePocket);
		AbstractPocketType<LazyGenerationPocket> LAZY_GENERATION_POCKET = register(new Identifier("dimdoors", LazyGenerationPocket.KEY), LazyGenerationPocket::new, LazyGenerationPocket::builderLazyGenerationPocket);


		T fromTag(CompoundTag tag);

		CompoundTag toTag(CompoundTag tag);

		T instance();

		AbstractPocketBuilder<?, T> builder();

		static void register() {
		}

		static <U extends AbstractPocket<P>, P extends AbstractPocket<P>> AbstractPocketType<U> register(Identifier id, Supplier<U> supplier, Supplier<? extends AbstractPocketBuilder<?, U>> factorySupplier) {
			return Registry.register(REGISTRY, id, new AbstractPocketType<U>() {
				@Override
				public U fromTag(CompoundTag tag) {
					return (U) supplier.get().fromTag(tag);
				}

				@Override
				public CompoundTag toTag(CompoundTag tag) {
					tag.putString("type", id.toString());
					return tag;
				}

				@Override
				public U instance() {
					return supplier.get();
				}

				@Override
				public AbstractPocketBuilder<?, U> builder() {
					return factorySupplier.get();
				}
			});
		}
	}

	public static abstract class AbstractPocketBuilder<P extends AbstractPocketBuilder<P, T>, T extends AbstractPocket<?>> {
		protected final AbstractPocketType<T> type;

		private int id;
		private RegistryKey<World> world;

		protected AbstractPocketBuilder(AbstractPocketType<T> type) {
			this.type = type;
		}

		public Vec3i getExpectedSize() {
			return new Vec3i(1, 1, 1);
		}

		public T build() {
			T instance = type.instance();

			instance.id = id;
			instance.world = world;

			return instance;
		}

		public P id(int id) {
			this.id = id;
			return getSelf();
		}

		public P world(RegistryKey<World> world) {
			this.world = world;
			return getSelf();
		}

		public P getSelf() {
			return (P) this;
		}

		abstract public P fromTag(CompoundTag tag);

		abstract public CompoundTag toTag(CompoundTag tag);

		/*
		public P fromTag(CompoundTag tag) {
			id = tag.getInt("id");
			world = RegistryKey.of(Registry.DIMENSION, new Identifier(tag.getString("world")));

			return getSelf();
		}

		public CompoundTag toTag(CompoundTag tag) {
			tag.putInt("id", id);
			tag.putString("world", world.getValue().toString());

			return tag;
		}
		 */
	}
}
