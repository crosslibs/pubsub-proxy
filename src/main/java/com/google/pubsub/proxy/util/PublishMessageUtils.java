/* Copyright 2019 Google Inc. All rights reserved.
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
* limitations under the License. */

package com.google.pubsub.proxy.util;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;

public class PublishMessageUtils {
	
	/**
	 * Returns timestamp from string
	 * @param s
	 * @return
	 * @throws GenericAPIException
	 */
	public static Timestamp getTimeStamp(String s) throws Exception {
		try {
			return Timestamps.parse(s);
		} catch (Exception ex) {
			throw ex;
		}
	}

	/**
	 * Attributes captured as generic object returned 
	 * in the format understandable by PubSub
	 * @param attributes
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, String> getAllAttributes(Object attributes) throws Exception {
		try {
			return (LinkedHashMap<String, String>) attributes;
		} catch (Exception ex) {
			throw ex;
		}
	}
}
