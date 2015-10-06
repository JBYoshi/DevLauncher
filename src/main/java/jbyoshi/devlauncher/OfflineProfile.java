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

import java.util.*;
import java.util.prefs.*;

public final class OfflineProfile implements Profile {
	private static final Preferences PREFS;

	private final String name;

	static final Set<OfflineProfile> profiles;

	public OfflineProfile(String name) {
		this.name = name;
		profiles.add(this);
		save();
	}

	@Override
	public String toString() {
		return name + " (Fake)";
	}

	@Override
	public String getArgs() {
		return "--username " + name + " --accessToken NotValid --userProperties []";
	}

	public static boolean isValidUsername(String username) {
		if (username.length() < 3 || username.length() > 16) {
			return false;
		}
		for (char c : username.toCharArray()) {
			if (!allowed.contains(c)) {
				return false;
			}
		}
		return true;
	}

	private static final List<Character> allowed = Arrays.asList('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K',
			'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
			'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '_');

	@Override
	public void validate() {
	}

	@Override
	public void onRemoved() {
		profiles.remove(this);
		save();
	}

	private static void save() {
		StringBuilder sb = new StringBuilder();
		for (OfflineProfile prof : profiles) {
			sb.append(" ");
			sb.append(prof.name);
		}
		String profiles = sb.toString().trim();
		PREFS.put("offline", profiles);
		try {
			PREFS.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}

	static {
		PREFS = Preferences.userNodeForPackage(OfflineProfile.class);
		profiles = new LinkedHashSet<>();
		String names = PREFS.get("offline", "").trim();
		if (names.length() > 0) {
			for (String s : names.split(" ")) {
				if (!isValidUsername(s)) {
					continue;
				}
				profiles.add(new OfflineProfile(s));
			}
		}
	}

	@Override
	public String getAccessToken() {
		return "no-access-token";
	}

}
