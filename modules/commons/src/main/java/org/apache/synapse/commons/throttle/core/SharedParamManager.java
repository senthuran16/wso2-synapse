package org.apache.synapse.commons.throttle.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.commons.throttle.core.internal.ThrottleServiceDataHolder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SharedParamManager {

	private static Map<String, Long> counters = new ConcurrentHashMap<String, Long>();//Locally managed counters map for non clustered environment
	private static Map<String, Long> timestamps = new ConcurrentHashMap<String, Long>();//Locally managed time stamps map for non clustered environment
	private static Log log = LogFactory.getLog(SharedParamManager.class.getName());

	/**
	 * Return distributed shared counter for this caller context with given id. If it's not distributed will get from the
	 * local counter
	 *
	 * @param id of the shared counter
	 * @return shared hazelcast current shared counter
	 */
	public static long getDistributedCounter(String id) {
		if (log.isDebugEnabled()) {
			log.trace("GET TIMESTAMP WITH ID " + id);
		}
		id = ThrottleConstants.THROTTLE_SHARED_COUNTER_KEY + id;
		DistributedCounterManager distributedCounterManager =
				ThrottleServiceDataHolder.getInstance().getDistributedCounterManager();
		if (distributedCounterManager != null && distributedCounterManager.isEnable()) {
			return distributedCounterManager.getCounter(id);
		} else {
			Long counter = counters.get(id);
			if (counter != null) {
				return counter;
			} else {
				counters.put(id, 0L);
				return 0;
			}
		}
	}

	/**
	 * Set distribute counter of caller context of given id to the provided value. If it's not distributed do the same for
	 * local counter
	 *
	 * @param id    of the caller context
	 * @param value to set to the global counter
	 */
	public static void setDistributedCounter(String id, long value) {
		if (log.isDebugEnabled()) {
			log.trace("SETTING COUNTER WITH ID " + id);
		}
		id = ThrottleConstants.THROTTLE_SHARED_COUNTER_KEY + id;
		DistributedCounterManager distributedCounterManager =
				ThrottleServiceDataHolder.getInstance().getDistributedCounterManager();
		if (distributedCounterManager != null && distributedCounterManager.isEnable()) {
			distributedCounterManager.setCounter(id, value);
		} else {
			counters.put(id, value);
		}
	}

	/**
	 * Add given value to the distribute counter of caller context of given id. If it's not
	 * distributed return local counter
	 *
	 * @param id    of the caller context
	 * @param value to set to the global counter
	 */
	public static long addAndGetDistributedCounter(String id, long value) {

		DistributedCounterManager distributedCounterManager =
				ThrottleServiceDataHolder.getInstance().getDistributedCounterManager();
		id = ThrottleConstants.THROTTLE_SHARED_COUNTER_KEY + id;
		if (distributedCounterManager != null && distributedCounterManager.isEnable()) {
			return distributedCounterManager.addAndGetCounter(id, value);
		} else {
			long currentCount = counters.get(id);
			long updatedCount = currentCount + value;
			counters.put(id, updatedCount);
			return updatedCount;
		}
	}

	/**
	 * Asynchronously add given value to the distribute counter of caller context of given id. If it's not
	 * distributed return local counter. This will return global value before add the provided counter
	 *
	 * @param id    of the caller context
	 * @param value to set to the global counter
	 */
	public static long asyncGetAndAddDistributedCounter(String id, long value) {
		if (log.isDebugEnabled()) {
			log.trace("ASYNC CREATING AND SETTING COUNTER WITH ID " + id);
		}
		id = ThrottleConstants.THROTTLE_SHARED_COUNTER_KEY + id;
		DistributedCounterManager distributedCounterManager =
				ThrottleServiceDataHolder.getInstance().getDistributedCounterManager();
		if (distributedCounterManager != null && distributedCounterManager.isEnable()) {
			return distributedCounterManager.asyncGetAndAddCounter(id, value);
		} else {
			Long currentCount = counters.get(id);
			if (currentCount == null) {
				currentCount = 0L;
			}
			long updatedCount = currentCount + value;
			counters.put(id, updatedCount);
			return currentCount;
		}
	}

	/**
	 * Asynchronously add given value to the distribute counter of caller context of given id. If it's not
	 * distributed return local counter. This will return global value before add the provided counter
	 *
	 * @param id    of the caller context
	 * @param value to set to the global counter
	 */
	public static long asyncGetAndAlterDistributedCounter(String id, long value) {
		id = ThrottleConstants.THROTTLE_SHARED_COUNTER_KEY + id;
		DistributedCounterManager distributedCounterManager =
				ThrottleServiceDataHolder.getInstance().getDistributedCounterManager();
		if (distributedCounterManager != null && distributedCounterManager.isEnable()) {
			return distributedCounterManager.asyncGetAndAlterCounter(id, value);
		} else {
			Long currentCount = counters.get(id);
			if (currentCount == null) {
				currentCount = 0L;
			}
			long updatedCount = currentCount + value;
			counters.put(id, updatedCount);
			return currentCount;
		}
	}

	/**
	 * Destroy hazelcast global counter, if it's local then remove the map entry
	 *
	 * @param id of the caller context
	 */
	public static void removeCounter(String id) {
		if (log.isDebugEnabled()) {
			log.trace("REMOVING COUNTER WITH ID " + id);
		}
		id = ThrottleConstants.THROTTLE_SHARED_COUNTER_KEY + id;
		DistributedCounterManager distributedCounterManager =
				ThrottleServiceDataHolder.getInstance().getDistributedCounterManager();
		if (distributedCounterManager != null && distributedCounterManager.isEnable()) {
			distributedCounterManager.removeCounter(id);
		} else {
			counters.remove(id);
		}
	}

	/**
	 * Return hazelcast shared timestamp for this caller context with given id. If it's not distributed will get from the
	 * local counter
	 *
	 * @param id of the shared counter
	 * @return shared hazelcast current shared counter
	 */
	public static long getSharedTimestamp(String id) {
		if (log.isDebugEnabled()) {
			log.trace("GET TIMESTAMP WITH ID " + id);
		}
		String key = ThrottleConstants.THROTTLE_TIMESTAMP_KEY + id;

		DistributedCounterManager distributedCounterManager =
				ThrottleServiceDataHolder.getInstance().getDistributedCounterManager();
		if (distributedCounterManager != null && distributedCounterManager.isEnable()) {
			return distributedCounterManager.getTimestamp(key);
		} else {
			Long timestamp = timestamps.get(key);
			if (timestamp != null) {
				return timestamp;
			} else {
				timestamps.put(key, 0L);
				return 0;
			}
		}
	}


	/**
	 * Set distribute timestamp of caller context of given id to the provided value. If it's not distributed do the same for
	 * local counter
	 *
	 * @param id        of the caller context
	 * @param timestamp to set to the global counter
	 */
	public static void setSharedTimestamp(String id, long timestamp) {
		if (log.isDebugEnabled()) {
			log.trace("SETTING TIMESTAMP WITH ID" + id);
		}
		String key = ThrottleConstants.THROTTLE_TIMESTAMP_KEY + id;
		DistributedCounterManager distributedCounterManager =
				ThrottleServiceDataHolder.getInstance().getDistributedCounterManager();
		if (distributedCounterManager != null && distributedCounterManager.isEnable()) {
			distributedCounterManager.setTimestamp(key, timestamp);
		} else {
			timestamps.put(id, timestamp);
		}
	}

	/**
	 * Destroy hazelcast shared timggestamp counter, if it's local then remove the map entry
	 *
	 * @param id of the caller context
	 */
	public static void removeTimestamp(String id) {
		if (log.isDebugEnabled()) {
			log.trace("REMOVING TIMESTAMP WITH ID " + id);
		}
		String key = ThrottleConstants.THROTTLE_TIMESTAMP_KEY + id;
		DistributedCounterManager distributedCounterManager =
				ThrottleServiceDataHolder.getInstance().getDistributedCounterManager();
		if (distributedCounterManager != null && distributedCounterManager.isEnable()) {
			distributedCounterManager.removeTimestamp(key);
		} else {
			timestamps.remove(key);
		}
	}


	public static void setExpiryTime(String id, long expiryTimeStamp) {
		if (log.isDebugEnabled()) {
			log.trace("SETTING Expiry WITH ID " + id);
		}
		String sharedCounterKey = ThrottleConstants.THROTTLE_SHARED_COUNTER_KEY + id;
		String sharedTimeStampKey = ThrottleConstants.THROTTLE_TIMESTAMP_KEY + id;

		DistributedCounterManager distributedCounterManager =
				ThrottleServiceDataHolder.getInstance().getDistributedCounterManager();
		if (distributedCounterManager != null && distributedCounterManager.isEnable()) {
			log.trace("Setting expiry time for key:" + sharedCounterKey + " value: " + expiryTimeStamp);
			distributedCounterManager.setExpiry(sharedCounterKey, expiryTimeStamp);
			log.trace("Setting expiry time for key:" + sharedTimeStampKey + " value: " + expiryTimeStamp);
			distributedCounterManager.setExpiry(sharedTimeStampKey, expiryTimeStamp);

		}

	}

	//	public static long getExpiryTime(String id) {
//		if (log.isDebugEnabled()) {
//			log.trace("GETTING EXPIRY TIME WITH ID " + id);
//		}
//
//		DistributedCounterManager distributedCounterManager =
//				ThrottleServiceDataHolder.getInstance().getDistributedCounterManager();
//		if (distributedCounterManager != null && distributedCounterManager.isEnable()) {
//			return distributedCounterManager.getExpiry(id);
//		} else {
//			return 0;
//		}
//	}
	public static long getTtl(String key) {
		long ttl = 0;
		DistributedCounterManager distributedCounterManager =
				ThrottleServiceDataHolder.getInstance().getDistributedCounterManager();
		if (distributedCounterManager != null && distributedCounterManager.isEnable()) {
			ttl = distributedCounterManager.getTtl(key);
		}

		return ttl;
	}

	/**
	 *
	 * @param callerContextId
	 * @return true if lock acquired, false if lock is not acquired within the configured timeout period
	 */
	public static boolean lockSharedKeys(String callerContextId, String lockValue) {
		DistributedCounterManager distributedCounterManager =
				ThrottleServiceDataHolder.getInstance().getDistributedCounterManager();

		if (distributedCounterManager != null && distributedCounterManager.isEnable()) {

			long responseCode;
			long startTime = System.currentTimeMillis();
			do {
				responseCode = distributedCounterManager.setLock(callerContextId, lockValue);
				if (responseCode == 1) {
					// lock acquired
					long timeNow = System.currentTimeMillis();
					log.trace("current time:" + timeNow + "(" + CallerContext.getReadableTime(timeNow) + ")" +
							"Lock acquired for key: " + callerContextId + " within " +
					         (timeNow - startTime) + " ms" + " Thread name: " + Thread.currentThread().getName() + " Thread id: " + Thread.currentThread().getId());
					distributedCounterManager.setExpiry(callerContextId, timeNow +
					                                                 distributedCounterManager.getKeyLockRetrievalTimeout() * 2); // TODO: set the expiry in the same redis call with multi
					return true;
				} else if (responseCode == 0) {
					long timeNow = System.currentTimeMillis();
					long timeElapsed = timeNow - startTime;
					if (timeElapsed > distributedCounterManager.getKeyLockRetrievalTimeout()) {
						log.warn("current time:" + timeNow + "(" + CallerContext.getReadableTime(timeNow) + ")" +"Unable to acquire lock for key: " + callerContextId + " within the configured " +
						         "timeout period. Elapsed time: " + timeElapsed + " ms"  + " Thread name: " + Thread.currentThread().getName() + " Thread id: " + Thread.currentThread().getId());
						return false;
					}

					try {
						Thread.sleep(5); //TODO: make this configurable
						log.trace("current time:" + timeNow + "(" + CallerContext.getReadableTime(timeNow) + ")" + "Retrying to get lock for key: " + callerContextId + " Thread name: " + Thread.currentThread().getName() + " Thread id: " + Thread.currentThread().getId());
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			} while (responseCode == 0);
		}

		return true;
	}

	// no need to check the value before removal
	public static boolean releaseSharedKeys(String callerContextId) {
		DistributedCounterManager distributedCounterManager =
				ThrottleServiceDataHolder.getInstance().getDistributedCounterManager();

		if (distributedCounterManager != null && distributedCounterManager.isEnable()) {
			distributedCounterManager.removeLock(callerContextId);
			log.trace("current time:" + System.currentTimeMillis() + "(" + CallerContext.getReadableTime(System.currentTimeMillis()) + ")" + "Lock released for key: " + callerContextId + " Thread name: " + Thread.currentThread().getName() + " Thread id: " + Thread.currentThread().getId());
		}
		return false;
	}
}
