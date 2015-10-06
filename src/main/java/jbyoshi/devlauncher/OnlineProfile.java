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

import java.awt.*;
import java.util.*;

import javax.swing.*;

import com.google.gson.*;
import com.mojang.authlib.exceptions.*;
import com.mojang.authlib.properties.*;

import jbyoshi.devlauncher.auth.*;

public final class OnlineProfile implements Profile {
	@Deprecated
	public OnlineProfile() {
	}

	private OnlineProfile(String login, AuthenticatedUser user) {
		this.accessToken = user.getAccessToken();
		this.displayName = user.getUsername();
		this.user = user;
		this.userid = user.getID();
		this.username = login;
		this.uuid = user.getUUID().toString();
	}

	/**
	 * The email address, or username if legacy.
	 */
	private String username;
	private String accessToken;
	/**
	 * An ID for this user.
	 */
	@SuppressWarnings("unused")
	private String userid;
	private String uuid;
	/**
	 * The username.
	 */
	private String displayName;

	private transient User user;

	boolean logInSilent() {
		if (accessToken == null) {
			return false;
		}
		try {
			user = LauncherProfiles.INSTANCE.getUserFactory().logInWithToken(UUID.fromString(uuid), accessToken,
					username);
			if (user instanceof AuthenticatedUser) {
				accessToken = ((AuthenticatedUser) user).getAccessToken();
				displayName = user.getUsername();
				LauncherProfiles.modified();
			}
		} catch (InvalidCredentialsException e) {
			e.printStackTrace();
			return false;
		} catch (AuthenticationException e) {
			e.printStackTrace();
			// Yes I know I didn't put a return statement here. This method
			// returns true if the user should remain on the list of known
			// profiles, so if the servers are down, they shouldn't be removed
			// from the list!
		}
		return true;
	}

	public static OnlineProfile logIn(JFrame parent) throws AuthenticationException {
		JPanel panel = new JPanel(new BorderLayout(5, 5));

		JPanel label = new JPanel(new GridLayout(0, 1, 2, 2));
		label.add(new JLabel("Email/Username", SwingConstants.RIGHT));
		label.add(new JLabel("Password", SwingConstants.RIGHT));
		panel.add(label, BorderLayout.WEST);

		JPanel controls = new JPanel(new GridLayout(0, 1, 2, 2));
		JTextField username = new JTextField(20);
		controls.add(username);
		final JPasswordField password = new JPasswordField(20);
		controls.add(password);
		panel.add(controls, BorderLayout.CENTER);

		panel.setFocusTraversalPolicy(new DefaultFocusTraversalPolicy() {
			private static final long serialVersionUID = -2363554137676992216L;

			@Override
			public Component getDefaultComponent(Container aContainer) {
				return password;
			}
		});

		int result = JOptionPane.showConfirmDialog(parent, panel, "login", JOptionPane.OK_CANCEL_OPTION);
		if (result != JOptionPane.OK_OPTION) {
			return null;
		}

		return logIn(username.getText(), new String(password.getPassword()));
	}

	public static OnlineProfile logIn(String email, String password) throws AuthenticationException {
		AuthenticatedUser user = LauncherProfiles.INSTANCE.getUserFactory().logInWithPassword(email, password);
		OnlineProfile prof = new OnlineProfile(email, user);
		LauncherProfiles.INSTANCE.addProfile(prof);
		return prof;
	}

	public String getUsername() {
		return displayName;
	}

	@Override
	public String toString() {
		return displayName;
	}

	@Override
	public void validate() throws LaunchException {
		if (!logInSilent()) {
			LauncherProfiles.INSTANCE.removeProfile(this);
			throw new LaunchException("Automatic login failed, please log back in manually") {
				private static final long serialVersionUID = -7071068723608777636L;

				@Override
				public void handle(DevLauncher launcher) {
					launcher.showLoginDialog();
				}
			};
		}
	}

	@Override
	public String getArgs() {
		String userProperties = new GsonBuilder()
				.registerTypeAdapter(PropertyMap.class, new LegacyPropertyMapSerializer()).create()
				.toJson(user.getProperties());
		String args = String.format("--username %s --uuid %s", getUsername(), uuid);
		if (user != null) {
			args = String.format("%s --accessToken %s --userProperties %s --userType %s", args, accessToken,
					userProperties, user.isLegacy() ? "legacy" : "mojang");
		}
		return args;
	}

	public UUID getUUID() {
		return UUID.fromString(uuid);
	}

	@Override
	public void onRemoved() {
		LauncherProfiles.INSTANCE.removeProfile(this);
	}

	@Override
	public String getAccessToken() {
		return accessToken;
	}

}
