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

import com.mojang.authlib.*;
import com.mojang.authlib.exceptions.*;

import java.util.Optional;

public final class AuthenticatedUser extends User {
	private final String token;
	private final String id;

	AuthenticatedUser(UserAuthentication login, UserFactory factory) {
		// If the profile is non-null, it will be complete.
		super(Optional.of(login).filter(UserAuthentication::isLoggedIn).orElseThrow(() -> new IllegalArgumentException(
				"auth.logIn() was not called!")).getSelectedProfile(), factory);
		this.token = login.getAuthenticatedToken();
		this.id = login.getUserID();
	}

	public String getAccessToken() {
		return token;
	}

	public void joinServer(String serverId) throws AuthenticationException {
		factory.sessions.joinServer(getProfile(), token, serverId);
	}

	public String getID() {
		return id;
	}
}
