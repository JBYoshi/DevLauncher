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

final class Dependency {
	private final String name;
	private final Dependency parent;
	private Boolean present;

	private Dependency(Dependency parent, String name) {
		this.parent = parent;
		this.name = name;
	}

	private boolean isPresent(ClassLoader cl) {
		if (parent != null && !parent.isPresent(cl)) {
			return false;
		}
		if (present == null) {
			try {
				Class.forName(name, false, cl);
				present = true;
			} catch (ClassNotFoundException e) {
				present = false;
			}
		}
		return present;
	}

	static Iterable<String> load(ClassLoader cl, InputStream in) throws IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(in));
		List<Dependency> deps = new LinkedList<Dependency>();
		Deque<Dependency> stack = new LinkedList<Dependency>();
		String line;
		while ((line = r.readLine()) != null) {
			int indent = 0;
			for (char c : line.toCharArray()) {
				if (c == ' ') {
					indent++;
				} else {
					break;
				}
			}
			String name = line.substring(indent);
			if (name.length() == 0 || name.startsWith("#")) {
				continue;
			}
			while (stack.size() > indent) {
				stack.pop();
			}
			Dependency d = new Dependency(stack.peek(), name);
			deps.add(d);
			stack.push(d);
		}

		List<String> names = new ArrayList<String>();
		for (Dependency d : deps) {
			if (d.isPresent(cl)) {
				names.remove(d.name);
				names.add(d.name);
			}
		}
		return names;
	}
}
