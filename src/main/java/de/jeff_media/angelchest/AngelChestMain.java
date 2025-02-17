package de.jeff_media.angelchest;

import co.aikar.commands.*;
import com.allatori.annotations.DoNotRename;
import com.jeff_media.jefflib.JeffLib;
import com.jeff_media.jefflib.Tasks;
import com.jeff_media.jefflib.Ticks;
import com.jeff_media.jefflib.WorldUtils;
import com.jeff_media.jefflib.data.McVersion;
import com.jeff_media.jefflib.data.tuples.Pair;
import com.jeff_media.jefflib.exceptions.NMSNotSupportedException;
import de.jeff_media.angelchest.commands.*;
import de.jeff_media.angelchest.config.*;
import de.jeff_media.angelchest.data.AngelChest;
import de.jeff_media.angelchest.data.BlacklistEntry;
import de.jeff_media.angelchest.data.PendingConfirm;
import de.jeff_media.angelchest.enums.BlacklistResult;
import de.jeff_media.angelchest.enums.EconomyStatus;
import de.jeff_media.angelchest.enums.PremiumFeatures;
import de.jeff_media.angelchest.gui.GUIListener;
import de.jeff_media.angelchest.gui.GUIManager;
import de.jeff_media.angelchest.handlers.*;
import de.jeff_media.angelchest.hooks.*;
import de.jeff_media.angelchest.listeners.*;
import de.jeff_media.angelchest.nbt.NBTUtils;
import de.jeff_media.angelchest.utils.*;
import de.jeff_media.customblocks.CustomBlock;
import de.jeff_media.daddy.Daddy_Stepsister;
import de.jeff_media.updatechecker.UpdateChecker;
import de.jeff_media.updatechecker.UserAgentBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * AngelChest AngelChestMain class
 */
public final class AngelChestMain extends JavaPlugin implements AngelChestPlugin {

    private static boolean isPremium0() {
        String percent = "%";
        if(System.getProperty("hopefullyundefinedproperty_" + ThreadLocalRandom.current().nextInt()) != null) {
            percent = ".";
        }
        return !"%%__USER__%%".startsWith(percent);
    }


    public static final int BSTATS_ID = 3194;
    public static final String DISCORD_LINK = "https://discord.jeff-media.de";
    public static final String UPDATECHECKER_LINK_DONATE = "https://paypal.me/mfnalex";
    public static final UUID consoleSenderUUID = UUID.randomUUID();
    private static final String SPIGOT_RESOURCE_ID_FREE = "60383";
    public static final String UPDATECHECKER_LINK_DOWNLOAD_FREE = "https://www.spigotmc.org/resources/" + SPIGOT_RESOURCE_ID_FREE;
    private static final String SPIGOT_RESOURCE_ID_PLUS = "88214";
    public static final String UPDATECHECKER_LINK_DOWNLOAD_PLUS = "https://www.spigotmc.org/resources/" + SPIGOT_RESOURCE_ID_PLUS;
    public static final String UPDATECHECKER_LINK_CHANGELOG = "https://www.spigotmc.org/resources/" + SPIGOT_RESOURCE_ID_PLUS + "/updates";
    private static final String UPDATECHECKER_LINK_API = "https://api.jeff-media.de/angelchestplus/latest-version.txt";
    public static boolean SCHEDULE_TASKS = true;
    @DoNotRename
    public static boolean isPremiumVersion = isPremium0();

    private static AngelChestMain instance;
    private static WorldGuardWrapper worldGuardWrapper;
    @Getter
    public final Object A_ENSURE_JEFFLIB_INIT = ((Supplier<Object>) () -> {
        try {
            JeffLib.init(AngelChestMain.this);
        } catch (Throwable ignored) {
        }
        return null;
    }).get();
    @Getter
    private final CurrencyFormatter currencyFormatter = new CurrencyFormatter(this);
    @Getter
    private final Map<String, Integer> worldMinBuildHeights = new HashMap<>();
    @Getter
    private final Map<String, Integer> worldMaxBuildHeights = new HashMap<>();
    public List<AngelChest> angelChests;
    public boolean debug = false;
    //public java.util.logging.Logger debugLogger;
    public boolean disableDeathEvent = false;
    /*public SkinManager getSkinManager() {
        return skinManager;
    }

    public NPCManager getNpcManager() {
        return npcManager;
    }*/
    public List<String> disabledMaterials;
    //private SkinManager skinManager;
    //private NPCManager npcManager;
    public List<String> disabledRegions;
    public List<String> disabledWorlds;
    public List<Material> dontSpawnOn;
    public Economy econ;
    public EconomyStatus economyStatus = EconomyStatus.UNKNOWN;
    public GenericHooks genericHooks;
    public GroupManager groupManager;
    public ProtectionUtils protectionUtils;
    public GUIListener guiListener;
    public GUIManager guiManager;
    public String[] invalidConfigFiles;
    public HashMap<UUID, Integer> invulnerableTasks;
    public Map<String, BlacklistEntry> itemBlacklist;
    public HashMap<UUID, Entity> killers;
    public HashMap<UUID, Block> lastPlayerPositions;
    public Logger logger;
    public Messages messages;
    public Metrics metrics;
    public MinepacksHook minepacksHook;
    public NBTUtils nbtUtils;
    public List<Material> onlySpawnIn;
    public HashMap<UUID, PendingConfirm> pendingConfirms;
    public boolean verbose = false;
    public Watchdog watchdog;
    public YamlConfiguration customDeathCauses;
    public IExecutableItemsHook executableItemsHook;
    //public MySqlManager mySqlManager;
    boolean emergencyMode = false;
    @Getter
    @Setter
    private ItemManager itemManager;
    @Getter
    private PvpTracker pvpTrackerDropHeads;
    //@Getter @Setter private IgnoredSlotsHandler ignoredSlotsHandler;
    @Getter
    @Setter
    private boolean itemsAdderLoaded = false;
    @SuppressWarnings({"FieldMayBeFinal", "CanBeFinal", "FieldCanBeLocal"})
    private String NONCE = "%%__NONCE__%%";
    @SuppressWarnings({"FieldMayBeFinal", "CanBeFinal", "FieldCanBeLocal"})
    private String RESOURCE = "%%__RESOURCE__%%";
    @SuppressWarnings({"FieldMayBeFinal", "CanBeFinal", "FieldCanBeLocal"})
    private String UID = "%%__USER__%%";

    {
        try {
            JeffLib.init(this);
        } catch (Throwable ignored) {

        }
    }

    public static AngelChestMain getInstance() {
        return instance;
    }

    public static WorldGuardWrapper getWorldGuardWrapper() {
        // We have to do this because softdepend doesn't assure that AngelChest enables after WorldGuard
        if (worldGuardWrapper == null) {
            worldGuardWrapper = WorldGuardWrapper.init();
        }
        return worldGuardWrapper;
    }

    public int getWorldMinHeight(World world) {
        return worldMinBuildHeights.getOrDefault(world.getName(), WorldUtils.getWorldMinHeight(world));
    }

    public int getWorldMaxHeight(World world) {
        int result = worldMaxBuildHeights.getOrDefault(world.getName(), world.getMaxHeight());
        //debug("MaxHeight for world " + world + " = " + result);
        return result;
    }

    public void debug(final String... text) {
        if (debug) {
            for (final String line : text) {
                getLogger().info("[DEBUG] " + line);
            }
        }
        /*for (String line : text) {
            debugLogger.info(line);
        }*/
    }

    // AngelChestPlugin interface
    @Override
    public Set<de.jeff_media.angelchest.AngelChest> getAllAngelChests() {
        final Set<de.jeff_media.angelchest.AngelChest> chests = new HashSet<>(angelChests);
        chests.stream().sorted(Comparator.comparingLong(de.jeff_media.angelchest.AngelChest::getCreated)).collect(Collectors.toList());
        return chests;
    }

    @Override
    public LinkedHashSet<de.jeff_media.angelchest.AngelChest> getAllAngelChestsFromPlayer(final OfflinePlayer player) {
        final LinkedHashSet<de.jeff_media.angelchest.AngelChest> set = new LinkedHashSet<>(AngelChestUtils.getAllAngelChestsFromPlayer(player));
        set.stream().sorted(Comparator.comparingLong(de.jeff_media.angelchest.AngelChest::getCreated)).collect(Collectors.toList());
        return set;
    }

    public ArrayList<UUID> getAllArmorStandUUIDs() {
        final ArrayList<UUID> armorStandUUIDs = new ArrayList<>();
        for (final AngelChest ac : angelChests) {
            if (ac == null || ac.hologram == null) continue;
            armorStandUUIDs.addAll(ac.hologram.armorStandUUIDs);
        }
        return armorStandUUIDs;
    }

    public @Nullable AngelChest getAngelChest(final Block block) {
        return angelChests.stream().filter(ac -> ac.block.equals(block)).findFirst().orElse(null);
    }

    @Override
    public de.jeff_media.angelchest.AngelChest getAngelChestAtBlock(final Block block) {
        return getAngelChest(block);
    }

    public AngelChest getAngelChestByHologram(final ArmorStand armorStand) {
        for (final AngelChest angelChest : angelChests) {
            if (angelChest == null) continue;
            if (angelChest.hologram == null) continue;
            if (angelChest.hologram.armorStandUUIDs.contains(armorStand.getUniqueId())) {
                return angelChest;
            }
        }
        return null;
    }

    @SneakyThrows
    private CustomBlock getCustomBlock(String id, Material fallback) {
        return CustomBlock.fromStringOrDefault(id, fallback);
    }

    public CustomBlock getChestMaterial(final AngelChest chest) {
        if (!Daddy_Stepsister.allows(PremiumFeatures.GENERIC)) {
            return getCustomBlock(getConfig().getString(Config.MATERIAL), Material.CHEST);
        }
        if (Daddy_Stepsister.allows(PremiumFeatures.GRAVEYARDS)) {
            if (chest.getGraveyard() != null) {
                if (chest.getGraveyard().hasCustomMaterial()) {
                    return chest.getGraveyard().getCustomMaterial();
                }
            }
        }
        if (!getConfig().getBoolean(Config.USE_DIFFERENT_MATERIAL_WHEN_UNLOCKED)) {
            return getCustomBlock(getConfig().getString(Config.MATERIAL), Material.CHEST);
        }
        return chest.isProtected
                ? getCustomBlock(getConfig().getString(Config.MATERIAL), Material.CHEST)
                : getCustomBlock(getConfig().getString(Config.MATERIAL_UNLOCKED), Material.ENDER_CHEST);
    }

    public void initUpdateChecker() {
        UpdateChecker.init(this, UPDATECHECKER_LINK_API)
                .setDonationLink(UPDATECHECKER_LINK_DONATE)
                .setChangelogLink(UPDATECHECKER_LINK_CHANGELOG)
                .setPaidDownloadLink(UPDATECHECKER_LINK_DOWNLOAD_PLUS)
                .setFreeDownloadLink(UPDATECHECKER_LINK_DOWNLOAD_FREE)
                .setColoredConsoleOutput(true)
                .setUserAgent(UserAgentBuilder.getDefaultUserAgent().addUsingPaidVersion().addSpigotUserId().addKeyValue("Port", String.valueOf(Bukkit.getPort())))
                .setNamePaidVersion("Plus")
                .setNameFreeVersion("Free")
                .setNotifyRequesters(true)
                .setNotifyOpsOnJoin(true)
                .suppressUpToDateMessage(true)
                .setTimeout(10000);


        switch (getConfig().getString(Config.CHECK_FOR_UPDATES).toLowerCase()) {
            case "true":
                UpdateChecker.getInstance()
                        .checkEveryXHours(getConfig().getDouble(Config.CHECK_FOR_UPDATES_INTERVAL))
                        .checkNow();
                break;
            case "false":
                break;
            default:
                UpdateChecker.getInstance().checkNow();
        }
    }

    public boolean isAngelChest(final Block block) {
        return angelChests.stream().anyMatch(ac -> ac.block.equals(block));
    }

    public boolean isAngelChestHologram(final Entity e) {
        // Skip this because it is checked in the listener and this method is not needed elsewhere
        //if(!(e instanceof ArmorStand)) return false;

        return getAllArmorStandUUIDs().contains(e.getUniqueId());
    }

    private boolean isAngelChestPlus1XInstalled() {
        return Bukkit.getPluginManager().getPlugin("AngelChestPlus") != null;
    }

    public boolean isBrokenAngelChest(final Block block, final AngelChest chest) {
        if (isOutsideOfNormalWorld(block)) {
            return false;
        }
        Material shouldBe = getChestMaterial(chest).getMaterial();
        if (shouldBe == null) {
            return false;
        }
        Material actuallyIs = block.getLocation().getBlock().getType();
        //System.out.println("Should be " + shouldBe + " actually is " + actuallyIs);
        boolean result = block.getType() != getChestMaterial(chest).getMaterial();
        //System.out.println("Is broken: " + result);
        return result;
    }

    public @Nullable Pair<String, Boolean> isItemBlacklisted(final ItemStack item, int slot) {
//        if (!Daddy_Stepsister.allows(PremiumFeatures.GENERIC)) { // Don't add feature here, gets called for every item on death
//            return null;
//        }
        Pair<String, Boolean> firstFound = null;
        for (final BlacklistEntry entry : itemBlacklist.values()) {
            final BlacklistResult result = entry.matches(item, slot);
            if (result == BlacklistResult.MATCH_IGNORE) {
                if (firstFound == null) {
                    firstFound = new Pair<>(result.getName(), false);
                }
            } else if (result == BlacklistResult.MATCH_DELETE) {
                return new Pair<>(result.getName(), true);
            }
        }
        return firstFound;
    }

    public boolean isOutsideOfNormalWorld(final Block block) {
        return block.getY() < getWorldMinHeight(block.getWorld()) || block.getY() >= getWorldMaxHeight(block.getWorld());
    }

    public void loadAllAngelChestsFromFile() {
        final File dir = new File(getDataFolder().getPath() + File.separator + "angelchests");
        final File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (final File child : directoryListing) {
                if (child.isDirectory()) continue;
                debug("Loading AngelChest " + child.getName());
                try {
                    final AngelChest ac = new AngelChest(child);
                    if (ac.success) {
                        angelChests.add(ac);
                    } else {
                        debug("Error while loading " + child.getName() + ", probably the world is not loaded yet. Will try again on next world load.");
                    }
                } catch (Throwable t) {
                    child.renameTo(new File(getDataFolder().getPath() + File.separator + "angelchests" + File.separator + "shadow", child.getName()));
                    if (debug) t.printStackTrace();
                }
            }
        }

        final File shadowDir = new File(getDataFolder().getPath() + File.separator + "angelchests" + File.separator + "shadow");
        final File[] shadowDirectoryListing = shadowDir.listFiles();
        if (shadowDirectoryListing != null) {
            for (final File child : shadowDirectoryListing) {
                if (child.isDirectory()) continue;
                debug("Loading shadowed AngelChest " + child.getName());
                try {
                    final AngelChest ac = new AngelChest(child);
                    if (ac.success) {
                        angelChests.add(ac);
                    }
                } catch (Throwable ignored) {

                }
            }
        }

    }

    private void migrateFromAngelChestPlus1X() {
        final File ACFolder = new File(getDataFolder().getAbsolutePath());
        final File ACPlusFolder = new File(ACFolder.getParentFile(), "AngelChestPlus");

        // ACPlus folder exists
        if (ACPlusFolder.isDirectory()) {
            getLogger().warning("You are upgrading from AngelChestPlus 1.XX to " + getDescription().getVersion());
            getLogger().warning("Trying to rename your AngelChestPlus folder to AngelChest...");

            // Shit: AC folder also exists
            if (ACFolder.isDirectory()) {
                getLogger().warning("Strange - you already have a directory called AngelChest. Trying to rename that one...");

                ACFolder.renameTo(new File(getDataFolder().getAbsolutePath(), "AngelChest-backup"));

                if (ACFolder.isDirectory()) {
                    getLogger().warning("It gets stranger: Couldn't rename that directory.");
                    getLogger().severe("Well, we couldn't remove your old AngelChest folder, which means we cannot apply your old config. Please manually rename the AngelChestPlus folder to AngelChest.");
                }
            }

            // AC folder does not (longer) exist
            if (!ACFolder.isDirectory()) {
                ACPlusFolder.renameTo(ACFolder);
            }

            if (!ACFolder.isDirectory() || ACPlusFolder.isDirectory()) {
                getLogger().severe("Could not rename your AngelChestPlus folder to AngelChest. Please do so manually.");
            } else {
                getLogger().warning(ChatColor.GREEN + "Successfully upgraded from AngelChestPlus 1.XX to " + getDescription().getVersion());
            }

        }
    }

    public void onDisable() {
        if (emergencyMode) return;

        saveAllAngelChestsToFile(true);
        ChunkManager.reset();

//        Bukkit.getScheduler().getActiveWorkers().stream().filter(worker -> worker.getOwner() == this).forEach(worker -> {
//            worker.getThread().interrupt();
//        });
    }

    private void showTasks() {
        getServer().getScheduler().getActiveWorkers().stream().filter(worker -> worker.getOwner() == this).forEach(worker -> {
            System.out.println("Worker " + worker);
        });
        getServer().getScheduler().getPendingTasks().stream().filter(task -> task.getOwner() == this).forEach(task -> {
            System.out.println("Task " + task);
        });
    }

    @Override
    public void onEnable() {

        if (!getDataFolder().exists()) {
            if (!getDataFolder().mkdirs()) {
                throw new RuntimeException("Could not create data folder");
            }
        }

        pvpTrackerDropHeads = new PvpTracker(this, () -> getConfig().getDouble("only-drop-heads-in-pvp-cooldown"));

        //skinManager = new SkinManager();
        //npcManager = new NPCManager();

        /*Daddy start*/
        isPremiumVersion = false; // DO NOT REMOVE THIS LINE
        Daddy_Stepsister.init(this); // TODO TODO TODO
        if (Daddy_Stepsister.allows(PremiumFeatures.GENERIC)) {
            Daddy_Stepsister.createVerificationFile();
            isPremiumVersion = true;
        } else {
            isPremiumVersion = false;
        }
        /*Daddy end*/

        if (Bukkit.getPluginManager().getPlugin("ExecutableItems") != null) {
            try {
                executableItemsHook = new ExecutableItems2Hook();
            } catch (Throwable t) {
                try {
                    executableItemsHook = new ExecutableItemsHook();
                } catch (Throwable t2) {
                    executableItemsHook = new IExecutableItemsHook();
                    AngelChestMain.getInstance().getLogger().warning("Warning: Could not hook into ExecutableItems although it's installed.");
                }
            }
        } else {
            executableItemsHook = new IExecutableItemsHook();
        }

        //ConfigurationSerialization.registerClass(CustomBlock.class, "CustomBlock");

        migrateFromAngelChestPlus1X();
        ChestFileUpdater.updateChestFilesToNewDeathCause();

        if (!McVersion.current().isAtLeast(1, 14, 1)) {
            EmergencyMode.severe(EmergencyMode.UNSUPPORTED_MC_VERSION_1_13);
            emergencyMode = true;
            return;
        }

        if (Bukkit.getPluginManager().getPlugin("ItemsAdder") != null) {
            itemsAdderLoaded = false;
            Bukkit.getPluginManager().registerEvents(new ItemsAdderInitListener(), this);
        } else {
            itemsAdderLoaded = true;
        }

        //ConfigurationSerialization.registerClass(DeathCause.class);

        if (isAngelChestPlus1XInstalled()) {
            emergencyMode = true;
            EmergencyMode.severe(EmergencyMode.FREE_VERSION_INSTALLED);
            return;
        }

        watchdog = new Watchdog(this);
        try {
            metrics = new Metrics(this, BSTATS_ID);
        } catch (Throwable t) {
            getLogger().warning("Could not initialize bStats.");
        }
        ConfigUtils.reloadCompleteConfig(false);


        angelChests = new CopyOnWriteArrayList<>();
        lastPlayerPositions = new HashMap<>();
        invulnerableTasks = new HashMap<>();
        killers = new HashMap<>();
        logger = new Logger();

        ItemsAdderHook.runOnceItemsAdderLoaded(this::scheduleRepeatingTasks);

        debug("Registering commands");
        registerCommands();
        //BukkitCommandManager acfCommandManager = new BukkitCommandManager(this);
        //acfCommandManager.registerCommand(new CommandGraveyard());
        debug("Setting command executors...");
        final CommandFetchOrTeleport commandFetchOrTeleport = new CommandFetchOrTeleport();
        final GenericTabCompleter genericTabCompleter = new GenericTabCompleter();
        Objects.requireNonNull(this.getCommand("acunlock")).setExecutor(new CommandUnlock());
        Objects.requireNonNull(this.getCommand("acunlock")).setTabCompleter(genericTabCompleter);
        Objects.requireNonNull(this.getCommand("aclist")).setExecutor(new CommandList());
        Objects.requireNonNull(this.getCommand("acfetch")).setExecutor(commandFetchOrTeleport);
        Objects.requireNonNull(this.getCommand("acfetch")).setTabCompleter(genericTabCompleter);
        Objects.requireNonNull(this.getCommand("actp")).setExecutor(commandFetchOrTeleport);
        Objects.requireNonNull(this.getCommand("actp")).setTabCompleter(genericTabCompleter);
        Objects.requireNonNull(this.getCommand("acreload")).setExecutor(new CommandReload());
        Objects.requireNonNull(this.getCommand("acgui")).setExecutor(new CommandGUI());
        //Objects.requireNonNull(this.getCommand("actoggle")).setExecutor(new CommandToggle());
        //Objects.requireNonNull(this.getCommand("acadmin")).setExecutor(new CommandAdmin());
        Objects.requireNonNull(this.getCommand("acgraveyard")).setExecutor(new CommandGraveyard());

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderAPIHook(this).register();
        }

        final CommandDebug commandDebug = new CommandDebug();
        Objects.requireNonNull(this.getCommand("acdebug")).setExecutor(commandDebug);
        Objects.requireNonNull(this.getCommand("acdebug")).setTabCompleter(commandDebug);
        Objects.requireNonNull(this.getCommand("acversion")).setExecutor(new CommandVersion());


        debug("Registering listeners");
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new HologramListener(), this);
        getServer().getPluginManager().registerEvents(new ChestProtectionListener(), this);
        getServer().getPluginManager().registerEvents(new PistonListener(), this);
        getServer().getPluginManager().registerEvents(new EmergencyListener(), this);
        getServer().getPluginManager().registerEvents(new InvulnerabilityListener(), this);
        getServer().getPluginManager().registerEvents(new EnderCrystalListener(), this);
        getServer().getPluginManager().registerEvents(new CraftingListener(), this);
        Tasks.nextTick(() -> {
            if (Bukkit.getPluginManager().isPluginEnabled("ChestSort")) {
                getServer().getPluginManager().registerEvents(new ChestSortListener(), this);
            }
        });
        //getServer().getPluginManager().registerEvents(new NPCListener(), this);
        guiListener = new GUIListener();
        getServer().getPluginManager().registerEvents(guiListener, this);
        if (Daddy_Stepsister.allows(PremiumFeatures.GRAVEYARDS)) {
            getServer().getPluginManager().registerEvents(new GraveyardListener(), this);
        }


        if (debug) getLogger().info("Disabled Worlds: " + disabledWorlds.size());
        if (debug) getLogger().info("Disabled WorldGuard regions: " + disabledRegions.size());

        setEconomyStatus();

        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
            final char color = Daddy_Stepsister.allows(PremiumFeatures.DONT_SHOW_NAG_MESSAGE) ? 'a' : '6';
            if (color == '6') // Do not mock paid users
                for (final String line : Daddy_Stepsister.allows(PremiumFeatures.DONT_SHOW_NAG_MESSAGE) ? Messages.usingPlusVersion : Messages.usingFreeVersion) {
                    getLogger().info(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', "&" + color + line)));
                }
        }, 60L);

        ItemsAdderHook.runOnceItemsAdderLoaded(() -> {
            debug("Loading AngelChests from disk");
            loadAllAngelChestsFromFile();
        });

        PaperCommandManager commandManager = new PaperCommandManager(this);
        commandManager.enableUnstableAPI("help");
        CommandReplacements replacements = commandManager.getCommandReplacements();
        replacements.addReplacement("actoggle", getCommandReplacements("actoggle"));
        commandManager.registerCommand(new ACFactoggle());
        commandManager.getCommandCompletions().registerCompletion("items", new CommandCompletions.CommandCompletionHandler<BukkitCommandCompletionContext>() {
            @Override
            public Collection<String> getCompletions(BukkitCommandCompletionContext context) throws InvalidCommandArgument {
                return itemManager.getItems().keySet();
            }
        });
        commandManager.getCommandCompletions().registerCompletion("onlinePlayerNames", context -> Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
        commandManager.getCommandCompletions().registerAsyncCompletion("offlinePlayerNamesWithChests", context -> angelChests.stream().map(AngelChest::getPlayer).distinct().map(OfflinePlayer::getName).collect(Collectors.toList()));
        commandManager.registerCommand(new ACFacadmin());
        commandManager.getCommandCompletions().registerAsyncCompletion("chestsBySecondArg", new CommandCompletions.AsyncCommandCompletionHandler<BukkitCommandCompletionContext>() {
            @Override
            public Collection<String> getCompletions(BukkitCommandCompletionContext context) throws InvalidCommandArgument {
                String owner = context.getContextValueByName(String.class, "owner");
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(owner);
                int size = getAllAngelChestsFromPlayer(offlinePlayer).size();
                if (size == 0) return null;
                return IntStream.rangeClosed(1, size).mapToObj(String::valueOf).collect(Collectors.toList());
            }
        });


        Bukkit.getOnlinePlayers().forEach(itemManager::autodiscover);

        // Fix NoClassDefFoundError in onDisable
        ChunkManager.getLoadedChunks();

        String plusTag = isPremiumVersion ? " Plus" : "";
        String versionAddendum = isPremiumVersion ? "/" + SpigotIdGetter.getSpigotId() : "";
        getLogger().info("Successfully enabled AngelChest" + plusTag + " v" + getDescription().getVersion() + versionAddendum + " (Premium: " + isPremiumVersion + ")");

    }

    private String getCommandReplacements(String command) {
        String alias = command;
        if (getConfig().isSet("command-aliases-" + command) && getConfig().isList("command-aliases-" + command)) {
            String aliases = getConfig().getStringList("command-aliases-" + command)
                    .stream().map(s -> s.split("\\[")[0]).collect(Collectors.joining("|"));
            return alias + "|" + aliases;
        }
        return alias;
    }

    @Override
    public void onLoad() {
        try {
            JeffLib.enableNMS();
        } catch (NMSNotSupportedException ex) {

        }
        instance = this;
        WorldGuardWrapper.tryToRegisterFlags();
    }

    private void registerCommands() {
        final String[][] commands = new String[][]{{"acgui", Permissions.USE}, {"aclist", Permissions.USE}, {"acfetch", Permissions.FETCH}, {"actp", Permissions.TP}, {"acunlock", Permissions.PROTECT}, {"acreload", Permissions.RELOAD}, {"acdebug", Permissions.DEBUG}, {"acversion", Permissions.VERSION}, {"actoggle", Permissions.TOGGLE}};
        for (final String[] commandAndPermission : commands) {
            final ArrayList<String> command = new ArrayList<>();
            try {
                command.add(commandAndPermission[0]);
                final List<String> aliases = getConfig().getStringList("command-aliases-" + commandAndPermission[0]);
                command.addAll(aliases);
                CommandManager.registerCommand(commandAndPermission[1], command.toArray(new String[0]));
            } catch (Throwable t) {
                getLogger().severe("Could not register command: " + command.stream().collect(Collectors.joining(", ")) + " - are those all valid command names?");
            }
        }
        //Chicken.wing(this, SCHEDULE_TASKS);
    }

    public void saveAllAngelChestsToFile(final boolean removeChests) {
        // Destroy all Angel Chests, including hologram AND CONTENTS!
        //for(Entry<Block,AngelChest> entry : angelChests.entrySet()) {
        //	Utils.destroyAngelChest(entry.getKey(), entry.getValue(), this);
        //}
        for (final AngelChest entry : angelChests) {
            entry.saveToFile(removeChests);

            // The following line isn't needed anymore but it doesn't hurt either
            if (removeChests) {
                entry.hologram.destroy();
            }
        }
        if (removeChests) {
            angelChests.clear();
        }
    }

    private void scheduleRepeatingTasks() {

        // Track player positions
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this::trackPlayerPositions, Ticks.fromSeconds(1), Ticks.fromSeconds(1));

        // Fix broken AngelChests
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> fixBrokenAngelChests(true), 0L, Ticks.fromSeconds(2));

        // Holograms, Durations
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this::updateAngelChests, 0, Ticks.fromSeconds(1));

        // Remove dead holograms
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this::removeDeadHolograms, Ticks.fromMinutes(1), Ticks.fromMinutes(1));

        // Clear Cooldown entries
        if (SCHEDULE_TASKS) Tasks.repeatAsync(this::clearCooldowns, Ticks.fromMinutes(10), Ticks.fromMinutes(10));
    }

    private void clearCooldowns() {
        pvpTrackerDropHeads.getTracker().clearOldEntries();
    }

    private void removeDeadHolograms() {
        for (final World world : Bukkit.getWorlds()) {
            HologramFixer.removeDeadHolograms(world);
        }
    }

    private void updateAngelChests() {

        boolean allowSuspendOffline = Daddy_Stepsister.allows(Config.SUSPEND_COUNTDOWN_OFFLINE_PLAYERS);

        for (AngelChest it : angelChests) {

            boolean suspendThis = allowSuspendOffline && it.suspendWhenOffline && it.owner != null && Bukkit.getPlayer(it.owner) == null;

            final AngelChest ac = it;
            if (ac == null) continue;
            if (!suspendThis) {
                ac.secondsLeft--;
            }
            if (ac.secondsLeft < 0 && !ac.infinite) {
                DeathMapManager.removeDeathMap(ac);
                if (getServer().getPlayer(ac.owner) != null) {
                    Messages.send(getServer().getPlayer(ac.owner), messages.MSG_ANGELCHEST_DISAPPEARED);
                }
                ac.destroy(true, true);
                angelChests.remove(it);
                logger.logRemoval(logger.getLogFile(ac.logfile));
                continue;
            }
            if (Daddy_Stepsister.allows(PremiumFeatures.GENERIC) && ac.isProtected && ac.unlockIn > -1) { // Don't add feature here, gets called every second
                if (!suspendThis) {
                    ac.unlockIn--;
                }
                if (ac.unlockIn == -1) {
                    ac.unlock();
                    ac.scheduleBlockChange();
                    if (getServer().getPlayer(ac.owner) != null) {
                        Messages.send(getServer().getPlayer(ac.owner), messages.MSG_UNLOCKED_AUTOMATICALLY);
                    }
                }
            }
            if (ac.hologram != null) {
                ac.hologram.update(ac);
            }
        }
    }

    private void fixBrokenAngelChests(boolean fix) {
        if (fix) {
            //System.out.println("===================================== FIX START =====================================");
        }
        ArrayList<AngelChest> toRemove = new ArrayList<>();
        ArrayList<AngelChest> toAdd = new ArrayList<>();
        // The following might only be needed for chests destroyed by end crystals spawning during the init phase of the ender dragon
        for (final AngelChest entry : angelChests) {

            World world = entry.getWorld();
            if (world == null) continue;
            if (!world.isChunkLoaded(entry.block.getX() >> 4, entry.getBlock().getZ() >> 4)) {
                //verbose("Chunk at " + entry.getKey().getLocation() + " is not loaded, skipping repeating task regarding angelChests.entrySet()");
                // CONTINUE IF CHUNK IS NOT LOADED
                continue;
            }
            final Block block = entry.block;

            //System.out.println(entry.uniqueId + " @ " + entry.block);
            if (isBrokenAngelChest(block, entry)) {
                if (fix) {
                    //System.out.println("Fixing broken AngelChest at " + block.getLocation() + " with AngelChest ID " + entry.uniqueId);
                    toAdd.add(new AngelChest(Objects.requireNonNull(getAngelChest(block)).saveToFile(true)));
                    toRemove.add(entry);
                }
            }
            angelChests.removeAll(toRemove);
            angelChests.addAll(toAdd);
            angelChests.sort(Comparator.comparing(AngelChest::getCreated));
        }
        if (fix) {
            //System.out.println("===================================== FIX END =====================================");
        }
    }

    private void trackPlayerPositions() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (((Entity) player).isOnGround()) {
                if (!getConfig().getBoolean(Config.LAVA_DETECTION) || (player.getEyeLocation().getBlock().getType() != Material.LAVA && player.getEyeLocation().getBlock().getRelative(BlockFace.DOWN).getType() != Material.LAVA)) {
                    lastPlayerPositions.put(player.getUniqueId(), player.getLocation().getBlock());
                }
            }
        }
    }

    private void setEconomyStatus() {
        final Plugin v = getServer().getPluginManager().getPlugin("Vault");

        if (v == null) {
            getLogger().info("Vault not installed, disabling economy functions.");
            economyStatus = EconomyStatus.INACTIVE;
            return;
        }

        final RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().info("No EconomyServiceProvider found, disabling economy functions.");
            economyStatus = EconomyStatus.INACTIVE;
            return;
        }

        if (rsp.getProvider() == null) {
            getLogger().info("No EconomyProvider found, disabling economy functions.");
            economyStatus = EconomyStatus.INACTIVE;
            return;
        }

        econ = rsp.getProvider();
        economyStatus = EconomyStatus.ACTIVE;
        getLogger().info("Successfully hooked into Vault and the EconomyProvider, enabling economy functions.");
    }

    public void verbose(final String t) {
        if (verbose) getLogger().info("[DEBUG] [VERBOSE] " + t);
    }


}
