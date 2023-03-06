//**************************************************************************************************
// The MIT License (MIT)
//
// Copyright (c) Eclypses, Inc.
//
// All rights reserved.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, subLicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
//**************************************************************************************************

package multiClient;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.expiry.ExpiryPolicy;

import multiClient.Models.Constants;

public class CacheHelper {
	
	private static Cache<String, String> _mteClientState;
	
	public CacheHelper() {
		
		//----------------------------
		// Create cache expire policy
		//----------------------------
		java.time.Duration duration = java.time.Duration.ofMinutes(Constants.ExpireMinutes);
		ExpiryPolicy<Object, Object> expiryPolicy = ExpiryPolicyBuilder.timeToLiveExpiration(duration);
		
		//----------------------
		// Create cache manager
		//----------------------
		CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
		          .withCache("preConfigured",
		               CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, String.class,
		                                              ResourcePoolsBuilder.heap(100))
		               .withExpiry(expiryPolicy)
		               .build())
		          .build(true);

		//--------------------------------------------
		// Finally create mteClientState cache object
		//--------------------------------------------
		Cache<String, String> preConfigured
		          = cacheManager.getCache("preConfigured", String.class, String.class);

		_mteClientState = cacheManager.createCache("_mteClientCache",
		               CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, String.class,
		                                        ResourcePoolsBuilder.heap(100)).build());
	}
	
	/**
	 * Store the Cached Value
	 * @param key
	 * @param value
	 */
	public void Store(String key, String value) {
		try {
			//------------------------------
		    // Put the value into the cache
		    //------------------------------
			_mteClientState.put(key, value);	
		}catch (Exception ex) {
			throw ex;
		}
	}
	
	/**
	 * Get cached value
	 * @param key
	 * @return
	 */
	public String Get(String key) {
		try {
			//----------------------------------------------
	        // return the value based on the key from cache
	        //----------------------------------------------
			return  _mteClientState.get(key);							
		}catch (Exception ex) {
			throw ex;
		}
	}
}
