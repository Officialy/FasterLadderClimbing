/**
 * This class was implemented by <JaSpr>. It is distributed as part
 * of the FasterLadderClimbing Mod.
 * https://github.com/JaSpr/FasterLadderClimbing
 *
 * FasterLadderClimbing is Open Source and distributed under the
 * CC-BY-NC-SA 3.0 License: https://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB
 *
 * This class was derived from works created by <Vazkii> which were distributed as
 * part of the Quark Mod. Get the Source Code in github:
 * https://github.com/Vazkii/Quark
 *
 * Quark is Open Source and distributed under the
 * CC-BY-NC-SA 3.0 License: https://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB
 */
package net.jaspr.base.module;

import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Module {

	public final String name = makeName();
	public final Map<String, Feature> features = new HashMap();
	public final List<Feature> enabledFeatures = new ArrayList();
	public boolean enabled;

	public void addFeatures() {
		// NO-OP
	}

	public void registerFeature(Feature feature) {
		registerFeature(feature, convertName(feature.getClass().getSimpleName()));
	}

	public void registerFeature(Feature feature, boolean enabledByDefault) {
		registerFeature(feature, convertName(feature.getClass().getSimpleName()), enabledByDefault);
	}

	public String convertName(String origName) {
		String withSpaces = origName.replaceAll("(?<=.)([A-Z])", " $1").toLowerCase();
		return Character.toUpperCase(withSpaces.charAt(0)) + withSpaces.substring(1);
	}

	public void registerFeature(Feature feature, String name) {
		registerFeature(feature, name, true);
	}

	public void registerFeature(Feature feature, String name, boolean enabledByDefault) {
		ModuleLoader.featureInstances.put(feature.getClass(), feature);
		features.put(name, feature);

		feature.enabledByDefault = enabledByDefault;
		feature.prevEnabled = false;

		feature.module = this;
		feature.configName = name;
		feature.configCategory = name;
	}

	public void setupConfig() {
		if(features.isEmpty())
			addFeatures();
 
		for (Feature feature : features.values()) {
			ConfigHelper.needsRestart = feature.requiresMinecraftRestartToEnable();
			feature.enabled = loadPropBool(feature.configName, feature.getFeatureDescription(), feature.enabledByDefault) && enabled;
 
			feature.setupConstantConfig();
 
			if(!feature.forceLoad) {
				String[] incompatibilities = feature.getIncompatibleMods();
				if(incompatibilities != null) {
					List<String> failures = new ArrayList<>();
 
					for(String s : incompatibilities)
						if(Loader.isModLoaded(s)) {
							feature.enabled = false;
							failures.add(s);
						}
 
					if(!failures.isEmpty())
						FMLLog.info("[" + feature.configName + "] is forcefully disabled as it's incompatible with the following loaded mods: " + failures);
				}
			}
 
			if(!feature.loadtimeDone) {
				feature.enabledAtLoadtime = feature.enabled;
				feature.loadtimeDone = true;
			}
 
			if(feature.enabled && !enabledFeatures.contains(feature))
				enabledFeatures.add(feature);
			else if(!feature.enabled && enabledFeatures.contains(feature))
				enabledFeatures.remove(feature);
 
			feature.setupConfig();
 
			if(!feature.enabled && feature.prevEnabled) {
				if(feature.hasSubscriptions())
					MinecraftForge.EVENT_BUS.unregister(feature);
				if(feature.hasTerrainSubscriptions())
					MinecraftForge.TERRAIN_GEN_BUS.unregister(feature);
				if(feature.hasOreGenSubscriptions())
					MinecraftForge.ORE_GEN_BUS.unregister(feature);
			} else if(feature.enabled && (feature.enabledAtLoadtime || !feature.requiresMinecraftRestartToEnable()) && !feature.prevEnabled) {
				if(feature.hasSubscriptions())
					MinecraftForge.EVENT_BUS.register(feature);
				if(feature.hasTerrainSubscriptions())
					MinecraftForge.TERRAIN_GEN_BUS.register(feature);
				if(feature.hasOreGenSubscriptions())
					MinecraftForge.ORE_GEN_BUS.register(feature);
			}
 
			feature.prevEnabled = feature.enabled;
		}
	}

	public void preInit(FMLPreInitializationEvent event) {
		for (Feature feature : enabledFeatures) {
			feature.preInit(event);
		}
	}
 
	public void init(FMLInitializationEvent event) {
		for (Feature feature : enabledFeatures) {
			feature.init(event);
		}
	}
 
	public void postInit(FMLPostInitializationEvent event) {
		for (Feature feature : enabledFeatures) {
			feature.postInit(event);
		}
	}
 
	@SideOnly(Side.CLIENT)
	public void preInitClient(FMLPreInitializationEvent event) {
		for (Feature feature : enabledFeatures) {
			feature.preInitClient(event);
		}
	}
 
	@SideOnly(Side.CLIENT)
	public void initClient(FMLInitializationEvent event) {
		for (Feature feature : enabledFeatures) {
			feature.initClient(event);
		}
	}
 
	@SideOnly(Side.CLIENT)
	public void postInitClient(FMLPostInitializationEvent event) {
		for (Feature feature : enabledFeatures) {
			feature.postInitClient(event);
		}
	}
 
	public void serverStarting(FMLServerStartingEvent event) {
		for (Feature feature : enabledFeatures) {
			feature.serverStarting(event);
		}
	}

	public boolean canBeDisabled() {
		return true;
	}

	public boolean isEnabledByDefault() {
		return true;
	}

	String makeName() {
		return getClass().getSimpleName().replaceAll("FasterLadderClimber", "").toLowerCase();
	}

	public String getModuleDescription() {
		return "";
	}

	public final void forEachFeature(java.util.function.Consumer<Feature> consumer) {
		for (Feature feature : features.values()) {
			consumer.accept(feature);
		}
	}
 
	public final void forEachEnabled(java.util.function.Consumer<Feature> consumer) {
		for (Feature feature : enabledFeatures) {
			consumer.accept(feature);
		}
	}

	public final int loadPropInt(String propName, String desc, int default_) {
		return ConfigHelper.loadPropInt(propName, name, desc, default_);
	}

	public final double loadPropDouble(String propName, String desc, double default_) {
		return ConfigHelper.loadPropDouble(propName, name, desc, default_);
	}

	public final boolean loadPropBool(String propName, String desc, boolean default_) {
		return ConfigHelper.loadPropBool(propName, name, desc, default_);
	}

	public final String loadPropString(String propName, String desc, String default_) {
		return ConfigHelper.loadPropString(propName, name, desc, default_);
	}

}
