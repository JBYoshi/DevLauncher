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

import java.util.*;

import org.apache.commons.lang3.*;

import com.mojang.authlib.*;
import com.mojang.authlib.minecraft.*;
import com.mojang.authlib.properties.*;

public class User {
	private final GameProfile profile;
	final UserFactory factory;

	User(GameProfile profile, UserFactory factory) {
		Validate.isTrue(profile.isComplete(), "The profile must be complete!");
		this.profile = profile;
		this.factory = factory;
	}

	public final GameProfile getProfile() {
		return profile;
	}

	public final UUID getUUID() {
		return profile.getId();
	}

	public final String getUsername() {
		return profile.getName();
	}

	public final PropertyMap getProperties() {
		return profile.getProperties();
	}

	public final boolean isLegacy() {
		return profile.isLegacy();
	}

	@Override
	public String toString() {
		return getUsername() + "(" + getUUID() + (isLegacy() ? "L)" : ")");
	}

	@Override
	public int hashCode() {
		return getUUID().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof User)) {
			return false;
		}
		User other = (User) obj;
		return getUUID().equals(other.getUUID());
	}

	public final MinecraftProfileTexture getTexture(
			MinecraftProfileTexture.Type type) {
		return getTextures().get(type);
	}

	public final Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures() {
		return factory.sessions.getTextures(profile,
				UserFactory.requireSecure());
	}

	public final MinecraftProfileTexture getSkin() {
		return getTexture(MinecraftProfileTexture.Type.SKIN);
	}

	public final MinecraftProfileTexture getCape() {
		return getTexture(MinecraftProfileTexture.Type.CAPE);
	}
}
