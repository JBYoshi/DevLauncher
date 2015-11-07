/*
 * Copyright (c) 2015 JBYoshi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jbyoshi.devlauncher;

import java.lang.reflect.*;

import javax.swing.*;

public final class GameLauncher {

	static String[] extraArgs;

	public static void launchClient(Profile prof) {
		callMain(prof, "--version MCDev " + prof.getArgs());
	}

	private static Class<?> getClientMainClass(ClassLoader cl) throws LaunchException {
		try {
			return loadClass(cl, "GradleStart");
		} catch (ClassNotFoundException e) {
		}
		try {
			return loadClass(cl, "net.minecraft.launchwrapper.Launch");
		} catch (ClassNotFoundException e) {
		}
		try {
			return loadClass(cl, "net.minecraft.client.main.Main");
		} catch (ClassNotFoundException e) {
		}
		throw new LaunchException(
				"Unable to find a supported main class. Make sure the Minecraft client is on your build path!");
	}

	public static void launchServer() {
		callMain(null, "");
	}

	private static Class<?> getServerMainClass(ClassLoader cl) throws LaunchException {
		try {
			return loadClass(cl, "GradleStartServer");
		} catch (ClassNotFoundException e) {
		}
		try {
			return loadClass(cl, "net.minecraft.server.MinecraftServer");
		} catch (ClassNotFoundException e) {
		}
		throw new LaunchException(
				"Unable to find a supported main class. Make sure the Minecraft server is on your build path!");
	}

	private static Class<?> loadClass(ClassLoader cl, String name) throws ClassNotFoundException {
		return Class.forName(name, false, cl);
	}

	private static void callMain(final Profile prof, String args) {
		if (SwingUtilities.isEventDispatchThread()) {
			final String args0 = args;
			new Thread(new Runnable() {
				@Override
				public void run() {
					callMain(prof, args0);
				}
			}).start();
			return;
		}
		ClassLoader cl = GameLauncher.class.getClassLoader();
		try {
			Class.forName("org.spongepowered.asm.launch.IMixinLaunchAgent");
			System.out.println("SpongePowered Mixin API 0.4.5+ found, adding tweaker");
			args += " --tweakClass org.spongepowered.asm.launch.MixinTweaker";
			System.setProperty("mixin.checks", "true");
		} catch (ClassNotFoundException e) {
			System.out.println("SpongePowered Mixin API 0.4.5+ not found, skipping");
		}
		String[] argsArray = args.trim().split(" ");
		String[] allArgs = new String[argsArray.length + extraArgs.length];
		System.arraycopy(argsArray, 0, allArgs, 0, argsArray.length);
		System.arraycopy(extraArgs, 0, allArgs, argsArray.length, extraArgs.length);
		// Sponge MixinBootstrap assumes the thread is called main.
		Thread.currentThread().setName("main");
		try {
			String clazz;
			if (prof == null) {
				clazz = getServerMainClass(cl).getName();
			} else {
				clazz = getClientMainClass(cl).getName();
			}
			Class.forName(clazz, true, cl).getMethod("main", String[].class).invoke(null, (Object) allArgs);
		} catch (InvocationTargetException e) {
			e.getCause().printStackTrace();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}
