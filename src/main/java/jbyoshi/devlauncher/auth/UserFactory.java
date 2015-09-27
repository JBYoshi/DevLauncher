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
package jbyoshi.devlauncher.auth;

import java.net.*;
import java.util.*;

import org.apache.commons.lang3.mutable.*;

import com.google.common.base.*;
import com.mojang.authlib.*;
import com.mojang.authlib.exceptions.*;
import com.mojang.authlib.minecraft.*;
import com.mojang.authlib.yggdrasil.*;
import com.mojang.util.*;

public class UserFactory {
	private final AuthenticationService service;
	private final GameProfileRepository repo;
	final MinecraftSessionService sessions;

	public UserFactory(String clientToken) {
		this(ProxySelector.getDefault().select(URI.create("https://mojang.com")).get(0), clientToken);
	}

	public UserFactory(Proxy proxy, String clientToken) {
		this.service = new YggdrasilAuthenticationService(proxy, clientToken);
		this.repo = service.createProfileRepository();
		this.sessions = service.createMinecraftSessionService();
	}

	public User getUserByUUID(UUID uuid) {
		GameProfile partial = new GameProfile(uuid, null);
		GameProfile full = sessions.fillProfileProperties(partial, requireSecure());
		return new User(full, this);
	}

	public User getUserByUsername(String username) throws AuthenticationException {
		final MutableBoolean success = new MutableBoolean(false);
		final MutableObject<GameProfile> result = new MutableObject<GameProfile>();
		final MutableObject<Exception> error = new MutableObject<Exception>(
				new RuntimeException("An unknown error ocurred!"));
		repo.findProfilesByNames(new String[] { username }, Agent.MINECRAFT, new ProfileLookupCallback() {
			@Override
			public void onProfileLookupSucceeded(GameProfile profile) {
				success.setTrue();
				result.setValue(profile);
			}

			@Override
			public void onProfileLookupFailed(GameProfile profile, Exception exception) {
				success.setFalse();
				error.setValue(exception);
			}
		});
		if (success.booleanValue()) {
			// Already populated
			return new User(result.getValue(), this);
		}
		Throwables.propagateIfPossible(error.getValue(), AuthenticationException.class);
		throw Throwables.propagate(error.getValue());
	}

	public User hasJoinedServer(String username, String serverId) throws AuthenticationUnavailableException {
		// Populated here
		GameProfile prof = sessions.hasJoinedServer(new GameProfile(null, username), serverId);
		return new User(prof, this);
	}

	public static final String USE_INSECURE_TEXTURES_PROPERTY = "jbyoshi.mcauth.allowInsecureData";

	static boolean requireSecure() {
		return Boolean.getBoolean(USE_INSECURE_TEXTURES_PROPERTY);
	}

	public AuthenticatedUser logInWithPassword(String email, String password) throws AuthenticationException {
		UserAuthentication auth = service.createUserAuthentication(Agent.MINECRAFT);
		auth.setUsername(email);
		auth.setPassword(password);
		auth.logIn();
		return new AuthenticatedUser(auth, this);
	}

	public AuthenticatedUser logInWithToken(UUID uuid, String accessToken) throws AuthenticationException {
		return (AuthenticatedUser) logInWithToken(uuid, accessToken, null);
	}

	public User logInWithToken(UUID uuid, String accessToken, String lastKnownUsername) throws AuthenticationException {
		UserAuthentication auth = service.createUserAuthentication(Agent.MINECRAFT);
		Map<String, Object> storage = new HashMap<String, Object>();
		storage.put("accessToken", accessToken);
		storage.put("userid", uuid.toString());
		storage.put("uuid", UUIDTypeAdapter.fromUUID(uuid));
		storage.put("displayName", lastKnownUsername == null ? "Unknown" : lastKnownUsername);
		storage.put("username", "Unknown");
		auth.loadFromStorage(storage);
		try {
			auth.logIn();
		} catch (AuthenticationUnavailableException e) {
			if (lastKnownUsername == null) {
				throw e;
			}
			return new User(new GameProfile(uuid, lastKnownUsername), this);
		}
		return new AuthenticatedUser(auth, this);
	}
}
