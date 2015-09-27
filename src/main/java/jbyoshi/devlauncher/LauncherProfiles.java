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

import java.io.*;
import java.util.*;

import com.google.gson.*;
import com.mojang.util.*;

import jbyoshi.devlauncher.auth.*;

public final class LauncherProfiles {
	public static LauncherProfiles INSTANCE;

	static void modified() {
		try {
			FileWriter writer = new FileWriter(getFile());
			try {
				JsonObject obj = deepCopy(INSTANCE.everythingElse);
				obj.addProperty("clientToken", INSTANCE.clientToken);
				obj.add("authenticationDatabase", GSON.toJsonTree(INSTANCE.authenticationDatabase.profiles));
				GSON.toJson(obj, writer);
				System.out.println("Profiles saved");
			} finally {
				writer.flush();
				writer.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public static <T extends JsonElement> T deepCopy(T from) {
		if (from == null) {
			throw new IllegalArgumentException("from must not be null");
		}
		if (from instanceof JsonObject) {
			return (T) deepCopyObj((JsonObject) from);
		}
		if (from instanceof JsonArray) {
			return (T) deepCopyArr((JsonArray) from);
		}
		if (from instanceof JsonNull) {
			return (T) JsonNull.INSTANCE;
		}
		if (from instanceof JsonPrimitive) {
			// Nulls and primitives are immutable
			return from;
		}
		throw new AssertionError("Unknown element type " + from.getClass().getName());
	}

	private static JsonObject deepCopyObj(JsonObject from) {
		JsonObject result = new JsonObject();
		for (Map.Entry<String, JsonElement> entry : from.entrySet()) {
			result.add(entry.getKey(), deepCopy(entry.getValue()));
		}
		return result;
	}

	private static JsonArray deepCopyArr(JsonArray from) {
		JsonArray result = new JsonArray();
		for (JsonElement element : from) {
			result.add(deepCopy(element));
		}
		return result;
	}

	public static LauncherProfiles load() throws IOException {
		System.out.println("Loading Minecraft profiles");
		try {
			FileReader reader = new FileReader(getFile());
			try {
				LauncherProfiles profiles = new LauncherProfiles();
				JsonObject e = new JsonParser().parse(reader).getAsJsonObject();
				for (Map.Entry<String, JsonElement> entry : e.entrySet()) {
					if (entry.getKey().equals("clientToken")) {
						profiles.clientToken = entry.getValue().getAsString();
					} else if (entry.getKey().equals("authenticationDatabase")) {
						JsonObject o = entry.getValue().getAsJsonObject();
						for (Map.Entry<String, JsonElement> entry1 : o.entrySet()) {
							profiles.authenticationDatabase.profiles.put(UUIDTypeAdapter.fromString(entry1.getKey()),
									GSON.fromJson(entry1.getValue(), OnlineProfile.class));
						}
					} else {
						profiles.everythingElse.add(entry.getKey(), entry.getValue());
					}
				}
				INSTANCE = profiles;
				return INSTANCE;
			} finally {
				reader.close();
			}
		} finally {
			if (INSTANCE == null) {
				INSTANCE = new LauncherProfiles();
			}
			INSTANCE.markLoaded();
		}
	}

	private static File getFile() {
		File mcDataDir = DevLauncher.workingDirectory;
		if (!mcDataDir.exists()) {
			mcDataDir.mkdirs();
		}
		return new File(mcDataDir, "launcher_profiles.json");
	}

	private final JsonObject everythingElse = new JsonObject();
	private String clientToken = UUID.randomUUID().toString();
	private final AuthDatabase authenticationDatabase = new AuthDatabase();
	private transient UserFactory USERS;

	private void markLoaded() {
		USERS = new UserFactory(clientToken);
	}

	public UserFactory getUserFactory() {
		return USERS;
	}

	public String getClientToken() {
		return clientToken;
	}

	public void addProfile(OnlineProfile profile) {
		authenticationDatabase.profiles.put(profile.getUUID(), profile);
		modified();
	}

	public void removeProfile(OnlineProfile profile) {
		authenticationDatabase.profiles.remove(profile.getUUID());
		modified();
	}

	public Iterable<OnlineProfile> getProfiles() {
		return new Iterable<OnlineProfile>() {
			@Override
			public Iterator<OnlineProfile> iterator() {
				final Iterator<OnlineProfile> real = authenticationDatabase.profiles.values().iterator();
				return new Iterator<OnlineProfile>() {
					@Override
					public void remove() {
						real.remove();
						modified();
					}

					@Override
					public OnlineProfile next() {
						return real.next();
					}

					@Override
					public boolean hasNext() {
						return real.hasNext();
					}
				};
			}
		};
	}

	private static final Gson GSON = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();

	private static final class AuthDatabase {
		private final Map<UUID, OnlineProfile> profiles = new HashMap<UUID, OnlineProfile>();
	}
}
