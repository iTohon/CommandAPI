package dev.jorel.commandapi.nms;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkit;
import dev.jorel.commandapi.CommandAPIHandler;
import dev.jorel.commandapi.arguments.SuggestionProviders;
import dev.jorel.commandapi.commandsenders.AbstractCommandSender;
import dev.jorel.commandapi.commandsenders.BukkitCommandSender;
import dev.jorel.commandapi.commandsenders.BukkitNativeProxyCommandSender;
import dev.jorel.commandapi.exceptions.AngleArgumentException;
import dev.jorel.commandapi.exceptions.BiomeArgumentException;
import dev.jorel.commandapi.exceptions.TimeArgumentException;
import dev.jorel.commandapi.exceptions.UUIDArgumentException;
import dev.jorel.commandapi.preprocessor.Differs;
import dev.jorel.commandapi.preprocessor.NMSMeta;
import dev.jorel.commandapi.preprocessor.RequireField;
import dev.jorel.commandapi.wrappers.Rotation;
import dev.jorel.commandapi.wrappers.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.server.v1_13_R2.*;
import net.minecraft.server.v1_13_R2.CriterionConditionValue.c;
import net.minecraft.server.v1_13_R2.EnumDirection.EnumAxis;
import net.minecraft.server.v1_13_R2.IChatBaseComponent.ChatSerializer;
import org.bukkit.Particle;
import org.bukkit.*;
import org.bukkit.World;
import org.bukkit.Particle.DustOptions;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.*;
import org.bukkit.craftbukkit.v1_13_R2.*;
import org.bukkit.craftbukkit.v1_13_R2.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_13_R2.command.CraftBlockCommandSender;
import org.bukkit.craftbukkit.v1_13_R2.command.ProxiedNativeCommandSender;
import org.bukkit.craftbukkit.v1_13_R2.command.VanillaCommandWrapper;
import org.bukkit.craftbukkit.v1_13_R2.enchantments.CraftEnchantment;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftMinecartCommand;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_13_R2.help.CustomHelpTopic;
import org.bukkit.craftbukkit.v1_13_R2.help.SimpleHelpMap;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_13_R2.potion.CraftPotionEffectType;
import org.bukkit.craftbukkit.v1_13_R2.util.CraftChatMessage;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.help.HelpTopic;
import org.bukkit.inventory.Recipe;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

abstract class NMSWrapper_1_13_1 extends CommandAPIBukkit<CommandListenerWrapper> {}

/**
 * NMS implementation for Minecraft 1.13.1
 */
@NMSMeta(compatibleWith = "1.13.1")
@RequireField(in = CraftSound.class, name = "minecraftKey", ofType = String.class)
@RequireField(in = EntitySelector.class, name = "m", ofType = boolean.class)
@RequireField(in = LootTableRegistry.class, name = "e", ofType = Map.class)
@RequireField(in = SimpleHelpMap.class, name = "helpTopics", ofType = Map.class)
@RequireField(in = ParticleParamBlock.class, name = "c", ofType = IBlockData.class)
@RequireField(in = ParticleParamItem.class, name = "c", ofType = ItemStack.class)
@RequireField(in = ParticleParamRedstone.class, name = "f", ofType = float.class)
public class NMS_1_13_1 extends NMSWrapper_1_13_1 {

	protected static final MinecraftServer MINECRAFT_SERVER = ((CraftServer) Bukkit.getServer()).getServer();
	private static final VarHandle LootTableRegistry_e;
	private static final VarHandle SimpleHelpMap_helpTopics;
	private static final VarHandle ParticleParamBlock_c;
	private static final VarHandle ParticleParamItem_c;
	private static final VarHandle ParticleParamRedstone_f;

	// Compute all var handles all in one go so we don't do this during main server
	// runtime
	static {
		VarHandle ltr_e = null;
		VarHandle shm_ht = null;
		VarHandle ppb_c = null;
		VarHandle ppi_c = null;
		VarHandle ppr_g = null;
		try {
			ltr_e = MethodHandles.privateLookupIn(LootTableRegistry.class, MethodHandles.lookup())
					.findVarHandle(LootTableRegistry.class, "e", Map.class);
			shm_ht = MethodHandles.privateLookupIn(SimpleHelpMap.class, MethodHandles.lookup())
					.findVarHandle(SimpleHelpMap.class, "helpTopics", Map.class);
			ppb_c = MethodHandles.privateLookupIn(ParticleParamBlock.class, MethodHandles.lookup())
					.findVarHandle(ParticleParamBlock.class, "c", IBlockData.class);
			ppb_c = MethodHandles.privateLookupIn(ParticleParamItem.class, MethodHandles.lookup())
					.findVarHandle(ParticleParamItem.class, "c", ItemStack.class);
			ppr_g = MethodHandles.privateLookupIn(ParticleParamRedstone.class, MethodHandles.lookup())
					.findVarHandle(ParticleParamRedstone.class, "f", float.class);
		} catch (ReflectiveOperationException e) {
			e.printStackTrace();
		}
		LootTableRegistry_e = ltr_e;
		SimpleHelpMap_helpTopics = shm_ht;
		ParticleParamBlock_c = ppb_c;
		ParticleParamItem_c = ppi_c;
		ParticleParamRedstone_f = ppr_g;
	}

	@SuppressWarnings("deprecation")
	protected static NamespacedKey fromMinecraftKey(MinecraftKey key) {
		return new NamespacedKey(key.b(), key.getKey());
	}

	@Override
	public ArgumentType<?> _ArgumentAngle() {
		throw new AngleArgumentException();
	}

	@Override
	public ArgumentType<?> _ArgumentAxis() {
		return ArgumentRotationAxis.a();
	}

	@Override
	public ArgumentType<?> _ArgumentBlockPredicate() {
		return ArgumentBlockPredicate.a();
	}

	@Override
	public ArgumentType<?> _ArgumentBlockState() {
		return ArgumentTile.a();
	}

	@Override
	public ArgumentType<?> _ArgumentChat() {
		return ArgumentChat.a();
	}

	@Override
	public ArgumentType<?> _ArgumentChatComponent() {
		return ArgumentChatComponent.a();
	}

	@Override
	public ArgumentType<?> _ArgumentChatFormat() {
		return ArgumentChatFormat.a();
	}

	@Differs(from = "1.13", by = "Not throwing UnimplementedArgumentException")
	@Override
	public ArgumentType<?> _ArgumentDimension() {
		return ArgumentDimension.a();
	}

	@Differs(from = "1.13", by = "Not throwing EnvironmentArgumentException")
	@Override
	public ArgumentType<?> _ArgumentEnvironment() {
		return ArgumentDimension.a();
	}

	@Override
	public ArgumentType<?> _ArgumentEnchantment() {
		return ArgumentEnchantment.a();
	}

	@Override
	public ArgumentType<?> _ArgumentEntity(
			dev.jorel.commandapi.arguments.EntitySelector selector) {
		return switch (selector) {
			case MANY_ENTITIES -> ArgumentEntity.b();
			case MANY_PLAYERS -> ArgumentEntity.d();
			case ONE_ENTITY -> ArgumentEntity.a();
			case ONE_PLAYER -> ArgumentEntity.c();
		};
	}

	@Override
	public ArgumentType<?> _ArgumentEntitySummon() {
		return ArgumentEntitySummon.a();
	}

	@Override
	public ArgumentType<?> _ArgumentFloatRange() {
		return new ArgumentCriterionValue.a();
	}

	@Override
	public ArgumentType<?> _ArgumentIntRange() {
		return new ArgumentCriterionValue.b();
	}

	@Override
	public ArgumentType<?> _ArgumentItemPredicate() {
		return ArgumentItemPredicate.a();
	}

	@Override
	public ArgumentType<?> _ArgumentItemStack() {
		return ArgumentItemStack.a();
	}

	@Override
	public ArgumentType<?> _ArgumentMathOperation() {
		return ArgumentMathOperation.a();
	}

	@Override
	public ArgumentType<?> _ArgumentMinecraftKeyRegistered() {
		return ArgumentMinecraftKeyRegistered.a();
	}

	@Override
	public ArgumentType<?> _ArgumentMobEffect() {
		return ArgumentMobEffect.a();
	}

	@Override
	public ArgumentType<?> _ArgumentNBTCompound() {
		return ArgumentNBTTag.a();
	}

	@Override
	public ArgumentType<?> _ArgumentParticle() {
		return ArgumentParticle.a();
	}

	@Override
	public ArgumentType<?> _ArgumentPosition() {
		return ArgumentPosition.a();
	}

	@Differs(from = "1.13", by = "using ArgumentVec2I instead of ArgumentVec2")
	@Override
	public ArgumentType<?> _ArgumentPosition2D() {
		return ArgumentVec2I.a();
	}

	@Override
	public ArgumentType<?> _ArgumentProfile() {
		return ArgumentProfile.a();
	}

	@Override
	public ArgumentType<?> _ArgumentRotation() {
		return ArgumentRotation.a();
	}

	@Override
	public ArgumentType<?> _ArgumentScoreboardCriteria() {
		return ArgumentScoreboardCriteria.a();
	}

	@Override
	public ArgumentType<?> _ArgumentScoreboardObjective() {
		return ArgumentScoreboardObjective.a();
	}

	@Override
	public ArgumentType<?> _ArgumentScoreboardSlot() {
		return ArgumentScoreboardSlot.a();
	}

	@Override
	public ArgumentType<?> _ArgumentScoreboardTeam() {
		return ArgumentScoreboardTeam.a();
	}

	@Override
	public ArgumentType<?> _ArgumentScoreholder(boolean single) {
		return single ? ArgumentScoreholder.a() : ArgumentScoreholder.b();
	}

	@Override
	public ArgumentType<?> _ArgumentSyntheticBiome() {
		throw new BiomeArgumentException();
	}

	@Override
	public ArgumentType<?> _ArgumentTag() {
		return ArgumentTag.a();
	}

	@Override
	public ArgumentType<?> _ArgumentTime() {
		throw new TimeArgumentException();
	}

	@Override
	public ArgumentType<?> _ArgumentUUID() {
		throw new UUIDArgumentException();
	}

	@Override
	public ArgumentType<?> _ArgumentVec2() {
		return ArgumentVec2.a();
	}

	@Override
	public ArgumentType<?> _ArgumentVec3() {
		return ArgumentVec3.a();
	}

	@Override
	public void addToHelpMap(Map<String, HelpTopic> helpTopicsToAdd) {
		Map<String, HelpTopic> helpTopics = (Map<String, HelpTopic>) SimpleHelpMap_helpTopics
				.get(Bukkit.getServer().getHelpMap());
		for (Map.Entry<String, HelpTopic> entry : helpTopicsToAdd.entrySet()) {
			helpTopics.put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public String[] compatibleVersions() {
		return new String[] { "1.13.1" };
	}

	@Override
	public String convert(org.bukkit.inventory.ItemStack is) {
		return is.getType().getKey().toString() + CraftItemStack.asNMSCopy(is).getOrCreateTag().toString();
	}

	@Override
	public String convert(ParticleData<?> particle) {
		return CraftParticle.toNMS(particle.particle(), particle.data()).a();
	}

	@Override
	public String convert(PotionEffectType potion) {
		return potion.getName().toLowerCase(Locale.ENGLISH);
	}

	@Override
	public String convert(Sound sound) {
		return CraftSound.getSound(sound);
	}

	// Converts NMS function to SimpleFunctionWrapper
	private SimpleFunctionWrapper convertFunction(CustomFunction customFunction) {
		ToIntFunction<CommandListenerWrapper> appliedObj = clw -> MINECRAFT_SERVER.getFunctionData().a(customFunction,
				clw);

		Object[] cArr = customFunction.b();
		String[] result = new String[cArr.length];
		for (int i = 0, size = cArr.length; i < size; i++) {
			result[i] = cArr[i].toString();
		}
		return new SimpleFunctionWrapper(fromMinecraftKey(customFunction.a()), appliedObj, result);
	}

	@Override
	public void createDispatcherFile(File file,
			com.mojang.brigadier.CommandDispatcher<CommandListenerWrapper> dispatcher) {
		MINECRAFT_SERVER.vanillaCommandDispatcher.a(file);
	}

	@Override
	public HelpTopic generateHelpTopic(String commandName, String shortDescription, String fullDescription,
			String permission) {
		return new CustomHelpTopic(commandName, shortDescription, fullDescription, permission);
	}

	@Override
	public org.bukkit.advancement.Advancement getAdvancement(CommandContext<CommandListenerWrapper> cmdCtx, String key)
			throws CommandSyntaxException {
		return ArgumentMinecraftKeyRegistered.a(cmdCtx, key).bukkit;
	}

	@Override
	public Component getAdventureChat(CommandContext<CommandListenerWrapper> cmdCtx, String key)
			throws CommandSyntaxException {
		String jsonString = ChatSerializer.a(ArgumentChat.a(cmdCtx, key));
		return GsonComponentSerializer.gson().deserialize(jsonString);
	}

	@Override
	public Component getAdventureChatComponent(CommandContext<CommandListenerWrapper> cmdCtx, String key) {
		String jsonString = ChatSerializer.a(ArgumentChatComponent.a(cmdCtx, key));
		return GsonComponentSerializer.gson().deserialize(jsonString);
	}

	@Override
	public float getAngle(CommandContext<CommandListenerWrapper> cmdCtx, String key) {
		throw new AngleArgumentException();
	}

	@Override
	public EnumSet<Axis> getAxis(CommandContext<CommandListenerWrapper> cmdCtx, String key) {
		EnumSet<Axis> set = EnumSet.noneOf(Axis.class);
		EnumSet<EnumAxis> parsedEnumSet = ArgumentRotationAxis.a(cmdCtx, key);
		for (EnumAxis element : parsedEnumSet) {
			set.add(switch (element) {
				case X -> Axis.X;
				case Y -> Axis.Y;
				case Z -> Axis.Z;
			});
		}
		return set;
	}

	@Override
	public Biome getBiome(CommandContext<CommandListenerWrapper> cmdCtx, String key) {
		throw new BiomeArgumentException();
	}

	@Override
	public Predicate<Block> getBlockPredicate(CommandContext<CommandListenerWrapper> cmdCtx, String key)
			throws CommandSyntaxException {
		Predicate<ShapeDetectorBlock> predicate = ArgumentBlockPredicate.a(cmdCtx, key);
		return (Block block) -> {
			return predicate.test(new ShapeDetectorBlock(cmdCtx.getSource().getWorld(),
					new BlockPosition(block.getX(), block.getY(), block.getZ()), true));
		};
	}

	@Override
	public BlockData getBlockState(CommandContext<CommandListenerWrapper> cmdCtx, String key) {
		return CraftBlockData.fromData(ArgumentTile.a(cmdCtx, key).a());
	}

	@Override
	public com.mojang.brigadier.CommandDispatcher<CommandListenerWrapper> getBrigadierDispatcher() {
		return MINECRAFT_SERVER.vanillaCommandDispatcher.a();
	}

	@Override
	public BaseComponent[] getChat(CommandContext<CommandListenerWrapper> cmdCtx, String key)
			throws CommandSyntaxException {
		return ComponentSerializer.parse(ChatSerializer.a(ArgumentChat.a(cmdCtx, key)));
	}

	@Override
	public ChatColor getChatColor(CommandContext<CommandListenerWrapper> cmdCtx, String str) {
		return CraftChatMessage.getColor(ArgumentChatFormat.a(cmdCtx, str));
	}

	@Override
	public BaseComponent[] getChatComponent(CommandContext<CommandListenerWrapper> cmdCtx, String str) {
		return ComponentSerializer.parse(ChatSerializer.a(ArgumentChatComponent.a(cmdCtx, str)));
	}

	@Override
	public CommandListenerWrapper getBrigadierSourceFromCommandSender(AbstractCommandSender<? extends CommandSender> senderWrapper) {
		CommandSender sender = senderWrapper.getSource();
		if (sender instanceof CraftPlayer player) {
			return player.getHandle().getCommandListener();
		} else if (sender instanceof CraftBlockCommandSender blockCommandSender) {
			return blockCommandSender.getWrapper();
		} else if (sender instanceof CraftMinecartCommand minecartCommandSender) {
			return minecartCommandSender.getHandle().getCommandBlock().getWrapper();
		} else if (sender instanceof RemoteConsoleCommandSender) {
			return ((DedicatedServer) MINECRAFT_SERVER).remoteControlCommandListener.f();
		} else if (sender instanceof ConsoleCommandSender) {
			return MINECRAFT_SERVER.getServerCommandListener();
		} else if (sender instanceof ProxiedNativeCommandSender proxiedCommandSender) {
			return proxiedCommandSender.getHandle();
		} else {
			throw new IllegalArgumentException("Cannot make " + sender + " a vanilla command listener");
		}
	}

	@Override
	public BukkitCommandSender<? extends CommandSender> getCommandSenderFromCommandSource(CommandListenerWrapper clw) {
		try {
			return wrapCommandSender(clw.getBukkitSender());
		} catch (UnsupportedOperationException ignored) {
			return null;
		}
	}

	@Differs(from = "1.13", by = "Implements getDimension for DimensionArgument")
	@Override
	public World getDimension(CommandContext<CommandListenerWrapper> cmdCtx, String key) {
		return MINECRAFT_SERVER.getWorldServer(ArgumentDimension.a(cmdCtx, key)).getWorld();
	}

	@Differs(from = "1.13", by = "Implements getEnvironment for EnvironmentArgument")
	@Override
	public Environment getEnvironment(CommandContext<CommandListenerWrapper> cmdCtx, String key) {
		DimensionManager manager = ArgumentDimension.a(cmdCtx, key);
		return switch (manager.getDimensionID()) {
			case 0 -> Environment.NORMAL;
			case -1 -> Environment.NETHER;
			case 1 -> Environment.THE_END;
			default -> null;
		};
	}

	@Override
	public Enchantment getEnchantment(CommandContext<CommandListenerWrapper> cmdCtx, String str) {
		return new CraftEnchantment(ArgumentEnchantment.a(cmdCtx, str));
	}

	@Override
	public Object getEntitySelector(CommandContext<CommandListenerWrapper> cmdCtx, String str,
			dev.jorel.commandapi.arguments.EntitySelector selector)
			throws CommandSyntaxException {
		EntitySelector argument = cmdCtx.getArgument(str, EntitySelector.class);
		try {
			CommandAPIHandler.getField(EntitySelector.class, "m").set(argument, false);
		} catch (IllegalArgumentException | IllegalAccessException e1) {
			e1.printStackTrace();
		}

		return switch (selector) {
			case MANY_ENTITIES:
				// ArgumentEntity.c -> EntitySelector.b
				try {
					List<org.bukkit.entity.Entity> result = new ArrayList<>();
					for (Entity entity : argument.b(cmdCtx.getSource())) {
						result.add(entity.getBukkitEntity());
					}
					yield result;
				} catch (CommandSyntaxException e) {
					yield new ArrayList<org.bukkit.entity.Entity>();
				}
			case MANY_PLAYERS:
				// ArgumentEntity.d -> EntitySelector.d
				try {
					List<Player> result = new ArrayList<>();
					for (EntityPlayer player : argument.d(cmdCtx.getSource())) {
						result.add(player.getBukkitEntity());
					}
					yield result;
				} catch (CommandSyntaxException e) {
					yield new ArrayList<Player>();
				}
			case ONE_ENTITY:
				// ArgumentEntity.a -> EntitySelector.a
				yield argument.a(cmdCtx.getSource()).getBukkitEntity();
			case ONE_PLAYER:
				// ArgumentEntity.e -> EntitySelector.c
				yield argument.c(cmdCtx.getSource()).getBukkitEntity();
		};
	}

	@Differs(from = "1.13", by = "uses IRegistry.ENTITY_TYPE instead of EntityTypes")
	@Override
	public EntityType getEntityType(CommandContext<CommandListenerWrapper> cmdCtx, String str)
			throws CommandSyntaxException {
		return IRegistry.ENTITY_TYPE.get(ArgumentEntitySummon.a(cmdCtx, str))
				.a(((CraftWorld) getWorldForCSS(cmdCtx.getSource())).getHandle()).getBukkitEntity().getType();
	}

	@Override
	public FloatRange getFloatRange(CommandContext<CommandListenerWrapper> cmdCtx, String key) {
		CriterionConditionValue.c range = (c) cmdCtx.getArgument(key, CriterionConditionValue.c.class);
		float low = range.a() == null ? -Float.MAX_VALUE : range.a();
		float high = range.b() == null ? Float.MAX_VALUE : range.b();
		return new FloatRange(low, high);
	}

	@Override
	public FunctionWrapper[] getFunction(CommandContext<CommandListenerWrapper> cmdCtx, String str)
			throws CommandSyntaxException {
		List<FunctionWrapper> result = new ArrayList<>();
		CommandListenerWrapper commandListenerWrapper = cmdCtx.getSource().a().b(2);

		for (CustomFunction customFunction : ArgumentTag.a(cmdCtx, str)) {
			result.add(FunctionWrapper.fromSimpleFunctionWrapper(convertFunction(customFunction),
					commandListenerWrapper, e -> {
						return cmdCtx.getSource().a(((CraftEntity) e).getHandle());
					}));
		}

		return result.toArray(new FunctionWrapper[0]);
	}

	@Override
	public SimpleFunctionWrapper getFunction(NamespacedKey key) {
		return convertFunction(
				MINECRAFT_SERVER.getFunctionData().a(new MinecraftKey(key.getNamespace(), key.getKey())));
	}

	@Override
	public Set<NamespacedKey> getFunctions() {
		Set<NamespacedKey> functions = new HashSet<>();
		for (MinecraftKey key : MINECRAFT_SERVER.getFunctionData().c().keySet()) {
			functions.add(fromMinecraftKey(key));
		}
		return functions;
	}

	@Override
	public IntegerRange getIntRange(CommandContext<CommandListenerWrapper> cmdCtx, String key) {
		CriterionConditionValue.d range = ArgumentCriterionValue.b.a(cmdCtx, key);
		int low = range.a() == null ? Integer.MIN_VALUE : range.a();
		int high = range.b() == null ? Integer.MAX_VALUE : range.b();
		return new IntegerRange(low, high);
	}

	@Override
	public org.bukkit.inventory.ItemStack getItemStack(CommandContext<CommandListenerWrapper> cmdCtx, String str)
			throws CommandSyntaxException {
		return CraftItemStack.asBukkitCopy(ArgumentItemStack.a(cmdCtx, str).a(1, false));
	}

	@Override
	public Predicate<org.bukkit.inventory.ItemStack> getItemStackPredicate(
			CommandContext<CommandListenerWrapper> cmdCtx, String key) throws CommandSyntaxException {
		Predicate<ItemStack> predicate = ArgumentItemPredicate.a(cmdCtx, key);
		return item -> predicate.test(CraftItemStack.asNMSCopy(item));
	}

	@Differs(from = "1.13", by = "uses ArgumentVec2I instead of ArgumentVec2")
	@Override
	public Location2D getLocation2DBlock(CommandContext<CommandListenerWrapper> cmdCtx, String key)
			throws CommandSyntaxException {
		ArgumentVec2I.a blockPos = ArgumentVec2I.a(cmdCtx, key);
		return new Location2D(getWorldForCSS(cmdCtx.getSource()), blockPos.a, blockPos.b);
	}

	@Override
	public Location2D getLocation2DPrecise(CommandContext<CommandListenerWrapper> cmdCtx, String key)
			throws CommandSyntaxException {
		Vec2F vecPos = ArgumentVec2.a(cmdCtx, key);
		return new Location2D(getWorldForCSS(cmdCtx.getSource()), vecPos.i, vecPos.j);
	}

	@Override
	public Location getLocationBlock(CommandContext<CommandListenerWrapper> cmdCtx, String str)
			throws CommandSyntaxException {
		BlockPosition blockPos = ArgumentPosition.a(cmdCtx, str);
		return new Location(getWorldForCSS(cmdCtx.getSource()), blockPos.getX(), blockPos.getY(), blockPos.getZ());
	}

	@Override
	public Location getLocationPrecise(CommandContext<CommandListenerWrapper> cmdCtx, String str)
			throws CommandSyntaxException {
		Vec3D vecPos = ArgumentVec3.a(cmdCtx, str);
		return new Location(getWorldForCSS(cmdCtx.getSource()), vecPos.x, vecPos.y, vecPos.z);
	}

	@Differs(from = "1.13", by = "method name change: aP().a() -> getLootTableRegistry().getLootTable()")
	@Override
	public org.bukkit.loot.LootTable getLootTable(CommandContext<CommandListenerWrapper> cmdCtx, String str) {
		MinecraftKey minecraftKey = ArgumentMinecraftKeyRegistered.c(cmdCtx, str);
		return new CraftLootTable(fromMinecraftKey(minecraftKey),
				MINECRAFT_SERVER.getLootTableRegistry().getLootTable(minecraftKey));
	}

	@Override
	public MathOperation getMathOperation(CommandContext<CommandListenerWrapper> cmdCtx, String key)
			throws CommandSyntaxException {
		// We run this to ensure the argument exists/parses properly
		ArgumentMathOperation.a(cmdCtx, key);
		return MathOperation.fromString(CommandAPIHandler.getRawArgumentInput(cmdCtx, key));
	}

	@SuppressWarnings("deprecation")
	@Override
	public NamespacedKey getMinecraftKey(CommandContext<CommandListenerWrapper> cmdCtx, String key) {
		MinecraftKey resourceLocation = ArgumentMinecraftKeyRegistered.c(cmdCtx, key);
		return new NamespacedKey(resourceLocation.b(), resourceLocation.getKey());
	}

	@Override
	public <NBTContainer> Object getNBTCompound(CommandContext<CommandListenerWrapper> cmdCtx, String key,
			Function<Object, NBTContainer> nbtContainerConstructor) {
		return nbtContainerConstructor.apply(ArgumentNBTTag.a(cmdCtx, key));
	}

	@Override
	public String getObjective(CommandContext<CommandListenerWrapper> cmdCtx, String key)
			throws IllegalArgumentException, CommandSyntaxException {
		return ArgumentScoreboardObjective.a(cmdCtx, key).getName();
	}

	@Override
	public String getObjectiveCriteria(CommandContext<CommandListenerWrapper> cmdCtx, String key) {
		return ArgumentScoreboardCriteria.a(cmdCtx, key).getName();
	}

	@Override
	public OfflinePlayer getOfflinePlayer(CommandContext<CommandListenerWrapper> cmdCtx, String str)
			throws CommandSyntaxException {
		OfflinePlayer target = Bukkit
				.getOfflinePlayer((ArgumentProfile.a(cmdCtx, str).iterator().next()).getId());
		if (target == null) {
			throw ArgumentProfile.a.create();
		} else {
			return target;
		}
	}

	@Override
	public ParticleData<?> getParticle(CommandContext<CommandListenerWrapper> cmdCtx, String str) {
		final ParticleParam particleOptions = ArgumentParticle.a(cmdCtx, str);

		final Particle particle = CraftParticle.toBukkit(particleOptions);
		if (particleOptions instanceof ParticleParamBlock options) {
			IBlockData blockData = (IBlockData) ParticleParamBlock_c.get(options);
			return new ParticleData<BlockData>(particle, CraftBlockData.fromData(blockData));
		}
		if (particleOptions instanceof ParticleParamRedstone options) {
			String optionsStr = options.a(); // Of the format "particle_type float float float"
			String[] optionsArr = optionsStr.split(" ");
			final float red = Float.parseFloat(optionsArr[1]);
			final float green = Float.parseFloat(optionsArr[2]);
			final float blue = Float.parseFloat(optionsArr[3]);

			final Color color = Color.fromRGB((int) (red * 255.0F), (int) (green * 255.0F), (int) (blue * 255.0F));
			return new ParticleData<DustOptions>(particle,
					new DustOptions(color, (float) ParticleParamRedstone_f.get(options)));
		}
		if (particleOptions instanceof ParticleParamItem options) {
			return new ParticleData<org.bukkit.inventory.ItemStack>(particle,
					CraftItemStack.asBukkitCopy((ItemStack) ParticleParamItem_c.get(options)));
		}
		CommandAPI.getLogger().warning("Invalid particle data type for " + particle.getDataType().toString());
		return new ParticleData<Void>(particle, null);
	}

	@Override
	public Player getPlayer(CommandContext<CommandListenerWrapper> cmdCtx, String str) throws CommandSyntaxException {
		Player target = Bukkit.getPlayer((ArgumentProfile.a(cmdCtx, str).iterator().next()).getId());
		if (target == null) {
			throw ArgumentProfile.a.create();
		} else {
			return target;
		}
	}

	@Override
	public PotionEffectType getPotionEffect(CommandContext<CommandListenerWrapper> cmdCtx, String str)
			throws CommandSyntaxException {
		return new CraftPotionEffectType(ArgumentMobEffect.a(cmdCtx, str));
	}

	@Override
	public Recipe getRecipe(CommandContext<CommandListenerWrapper> cmdCtx, String key) throws CommandSyntaxException {
		return ArgumentMinecraftKeyRegistered.b(cmdCtx, key).toBukkitRecipe();
	}

	@Override
	public Rotation getRotation(CommandContext<CommandListenerWrapper> cmdCtx, String key) {
		Vec2F vec = ArgumentRotation.a(cmdCtx, key).b(cmdCtx.getSource());
		return new Rotation(vec.i, vec.j);
	}

	@Override
	public ScoreboardSlot getScoreboardSlot(CommandContext<CommandListenerWrapper> cmdCtx, String key) {
		return new ScoreboardSlot(ArgumentScoreboardSlot.a(cmdCtx, key));
	}

	@Override
	public Collection<String> getScoreHolderMultiple(CommandContext<CommandListenerWrapper> cmdCtx, String key)
			throws CommandSyntaxException {
		return ArgumentScoreholder.b(cmdCtx, key);
	}

	@Override
	public String getScoreHolderSingle(CommandContext<CommandListenerWrapper> cmdCtx, String key)
			throws CommandSyntaxException {
		return ArgumentScoreholder.a(cmdCtx, key);
	}

	@Override
	public BukkitCommandSender<? extends CommandSender> getSenderForCommand(CommandContext<CommandListenerWrapper> cmdCtx, boolean isNative) {
		CommandListenerWrapper clw = cmdCtx.getSource();

		CommandSender sender = clw.getBukkitSender();
		Vec3D pos = clw.getPosition();
		Vec2F rot = clw.i();
		World world = getWorldForCSS(clw);
		Location location = new Location(world, pos.x, pos.y, pos.z, rot.j, rot.i);

		Entity proxyEntity = clw.f();
		CommandSender proxy = proxyEntity == null ? null : proxyEntity.getBukkitEntity();
		if (isNative || (proxy != null && !sender.equals(proxy))) {
			return new BukkitNativeProxyCommandSender(new NativeProxyCommandSender(sender, proxy, location, world));
		} else {
			return wrapCommandSender(sender);
		}
	}

	@Override
	public SimpleCommandMap getSimpleCommandMap() {
		return ((CraftServer) Bukkit.getServer()).getCommandMap();
	}

	@Override
	public Sound getSound(CommandContext<CommandListenerWrapper> cmdCtx, String key) {
		MinecraftKey minecraftKey = ArgumentMinecraftKeyRegistered.c(cmdCtx, key);
		for (CraftSound sound : CraftSound.values()) {
			try {
				if (CommandAPIHandler.getField(CraftSound.class, "minecraftKey").get(sound)
						.equals(minecraftKey.getKey())) {
					return Sound.valueOf(sound.name());
				}
			} catch (IllegalArgumentException | IllegalAccessException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}

	@Differs(from = "1.13", by = "use of getLootTableRegistry() instead of .aP(). No use of ::iterator for advancements")
	@Override
	public SuggestionProvider<CommandListenerWrapper> getSuggestionProvider(SuggestionProviders provider) {
		return switch (provider) {
			case FUNCTION -> (context, builder) -> {
				CustomFunctionData functionData = MINECRAFT_SERVER.getFunctionData();
				ICompletionProvider.a(functionData.g().a(), builder, "#");
				return ICompletionProvider.a(functionData.c().keySet(), builder);
			};
			case RECIPES -> CompletionProviders.b;
			case SOUNDS -> CompletionProviders.c;
			case ADVANCEMENTS -> (cmdCtx, builder) -> {
				return ICompletionProvider
						.a(MINECRAFT_SERVER.getAdvancementData().b().stream().map(Advancement::getName), builder);
			};
			case LOOT_TABLES -> (cmdCtx, builder) -> {
				Map<MinecraftKey, LootTable> map = (Map<MinecraftKey, LootTable>) LootTableRegistry_e
						.get(MINECRAFT_SERVER.getLootTableRegistry());
				return ICompletionProvider.a(map.keySet(), builder);
			};
			case ENTITIES -> CompletionProviders.d;
			default -> (context, builder) -> Suggestions.empty();
		};
	}

	@Override
	public SimpleFunctionWrapper[] getTag(NamespacedKey key) {
		List<CustomFunction> customFunctions = new ArrayList<>(
				MINECRAFT_SERVER.getFunctionData().g().b(new MinecraftKey(key.getNamespace(), key.getKey())).a());
		SimpleFunctionWrapper[] result = new SimpleFunctionWrapper[customFunctions.size()];
		for (int i = 0, size = customFunctions.size(); i < size; i++) {
			result[i] = convertFunction(customFunctions.get(i));
		}
		return result;
	}

	@Override
	public Set<NamespacedKey> getTags() {
		Set<NamespacedKey> functions = new HashSet<>();
		for (MinecraftKey key : MINECRAFT_SERVER.getFunctionData().g().a()) {
			functions.add(fromMinecraftKey(key));
		}
		return functions;
	}

	@Override
	public String getTeam(CommandContext<CommandListenerWrapper> cmdCtx, String key) throws CommandSyntaxException {
		return ArgumentScoreboardTeam.a(cmdCtx, key).getName();
	}

	@Override
	public int getTime(CommandContext<CommandListenerWrapper> cmdCtx, String key) {
		throw new TimeArgumentException();
	}

	@Override
	public UUID getUUID(CommandContext<CommandListenerWrapper> cmdCtx, String key) {
		throw new UUIDArgumentException();
	}

	@Override
	public World getWorldForCSS(CommandListenerWrapper clw) {
		return (clw.getWorld() == null) ? null : clw.getWorld().getWorld();
	}

	@Override
	public boolean isVanillaCommandWrapper(Command command) {
		return command instanceof VanillaCommandWrapper;
	}

	@Override
	public void reloadDataPacks() {
		// Datapacks don't need reloading in this version
	}

	@Override
	public void resendPackets(Player player) {
		MINECRAFT_SERVER.getCommandDispatcher().a(((CraftPlayer) player).getHandle());
	}

	@Override
	public Message generateMessageFromJson(String json) {
		return ChatSerializer.a(json);
	}

}
