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

import com.google.gson.*;
import com.mojang.authlib.properties.*;

public class LegacyPropertyMapSerializer implements JsonSerializer<PropertyMap> {

	@Override
	public JsonElement serialize(PropertyMap src, Type typeOfSrc,
			JsonSerializationContext context) {
		JsonObject result = new JsonObject();
		for (String key : src.keySet()) {
			JsonArray values = new JsonArray();
			for (Property property : src.get(key)) {
				values.add(new JsonPrimitive(property.getValue()));
			}
			result.add(key, values);
		}
		return result;
	}
}
