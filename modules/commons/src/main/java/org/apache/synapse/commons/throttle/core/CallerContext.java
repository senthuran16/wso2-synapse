/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
*
*/

package org.apache.synapse.commons.throttle.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.commons.throttle.core.internal.DistributedThrottleProcessor;
import org.apache.synapse.commons.throttle.core.internal.ThrottleServiceDataHolder;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Contains all runtime data for a particular remote caller.
 * provides the default rate based access controller algorithm implementation.
 * This is not thread-safe
 */

public abstract class CallerContext implements Serializable, Cloneable {
    private static final long serialVersionUID = 1652165180220263492L;
    private static Log log = LogFactory.getLog(CallerContext.class.getName());

    /* next access time - the end of prohibition */
    private long nextAccessTime = 0;  // this is set a value by the first request that exceeds throttle limit
    /* first access time - when caller came across the on first time */
    private long firstAccessTime = 0;
    /* The nextTimeWindow - beginning of next unit time period- end of current unit time period  */
    private long nextTimeWindow = 0;  // this is set a value by the first request that comes to GW
    /* The globalCount to keep track number of request */
    private AtomicLong globalCount = new AtomicLong(0);
    private long localQuota = 2; // TODO: Set this via the decided algorithm
    private String roleId;
    private long unitTime;
    // isThrottleParamSyncingModeSync - this is specific for each API EP. Should be updated via redis subscription
    private boolean isThrottleParamSyncingModeSync;
    private ThrottleProperties throttleProperties;


    /**
     * Count to keep track of local (specific to this node) number of requests
     */
    private AtomicLong localCount = new AtomicLong(0);
    private AtomicLong localHits = new AtomicLong(0);

    /**
     * Used for debugging purposes. *
     */
    private UUID uuid = UUID.randomUUID();

    /* The Id of caller */
    private String id;
    private long syncModeLastUpdatedTime;

    public CallerContext clone() throws CloneNotSupportedException {
        super.clone();
        CallerContext clone = new CallerContext(this.id) {
            @Override
            public int getType() {
                return CallerContext.this.getType();
            }
        };
        clone.nextAccessTime = this.nextAccessTime;
        clone.firstAccessTime = this.firstAccessTime;
        clone.nextTimeWindow = this.nextTimeWindow;
        clone.globalCount = new AtomicLong(this.globalCount.longValue());
        clone.localCount = new AtomicLong(this.localCount.longValue());

        clone.roleId = this.roleId;
        localCount.set(0);
        log.info(">>> Caller context clonined. " + clone.localCount);
        return clone;
    }

    public CallerContext(String ID) {
        log.info("Creating Caller Context object for :" + ID);
        if (ID == null || "".equals(ID)) {
            throw new InstantiationError("Couldn't create a CallContext for an empty " +
                                         "remote caller ID");
        }
        if (throttleProperties == null) {
            throttleProperties = ThrottleServiceDataHolder.getInstance().getThrottleProperties();
        }

        this.id = ID.trim();
    }

    public UUID getUuid() {
        return uuid;
    }

    /**
     * @return Returns Id of caller
     */
    public String getId() {
        return this.id;
    }

    /**
     * Init the access for a particular caller , caller will registered with context
     *
     * @param configuration   -The Configuration for this caller
     * @param throttleContext -The Throttle Context
     * @param currentTime     -The system current time in milliseconds
     */
    private void initAccess(CallerConfiguration configuration, ThrottleContext throttleContext, long currentTime) {
        this.unitTime = configuration.getUnitTime();
        this.firstAccessTime = currentTime;
        this.nextTimeWindow = this.firstAccessTime + this.unitTime;
        this.roleId = configuration.getID();
        //Also we need to pick counter value associated with time window.
        throttleContext.addCallerContext(this, this.id);

        if (!ThrottleServiceDataHolder.getInstance().getThrottleProperties().isThrottleSyncAsyncHybridModeEnabled()) {
            throttleContext.replicateTimeWindow(this.id);
        }
    }

    /**
     * To verify access if the unit time has already not over
     *
     * @param configuration   -  The Configuration for this caller
     * @param throttleContext -The Throttle Context
     * @param currentTime     -The system current time
     * @return boolean        -The boolean value which say access will allow or not
     */
    private boolean canAccessIfUnitTimeNotOver(CallerConfiguration configuration,
                                               ThrottleContext throttleContext, long currentTime) {
        boolean canAccess = false;
        int maxRequest = configuration.getMaximumRequestPerUnitTime();
       // log.info("canAccessIfUnitTimeNotOver** : currentTime now:" + currentTime); // >>>
        if (maxRequest != 0) {
            if (ThrottleServiceDataHolder.getInstance().getThrottleProperties().isThrottleSyncAsyncHybridModeEnabled()) {
                if (isThrottleParamSyncingModeSync && this.localHits.get() >= localQuota) {
                    log.info("### Going to run throttle param syncing in sync mode");
                    syncThrottleWindowParams();
                    syncThrottleCounterParams();
                }
            }
            if ((this.globalCount.get() + this.localCount.get()) < maxRequest) {    //If the globalCount is less than max request
                log.info("### 1,2,6-canAccessIfUnitTimeNotOver** If the globalCount is less than max request : (this.globalCount.get() + this.localCount.get()) = " + (this.globalCount.get() + this.localCount.get())); // >>>
                if (log.isDebugEnabled()) {
                    log.debug("CallerContext Checking access if unit time is not over and less than max count>> Access "
                            + "allowed=" + maxRequest + " available="+ (maxRequest - (this.globalCount.get() + this.localCount.get()))
                            +" key=" + this.getId() + " currentGlobalCount=" + globalCount + " currentTime="
                            +  currentTime + " " + "nextTimeWindow=" + this.nextTimeWindow + " currentLocalCount=" + localCount + " Tier="
                            + configuration.getID() + " nextAccessTime=" + this.nextAccessTime);
                }
                canAccess = true;     // can continue access
                this.localCount.incrementAndGet();
                log.info("$$$ CC_UTNO1 localCount:" + this.localCount.get());

                if (ThrottleServiceDataHolder.getInstance().getThrottleProperties().isThrottleSyncAsyncHybridModeEnabled()) {
                    if (isThrottleParamSyncingModeSync) {
                        log.info("### Throttle counter syncing after allowing current request ");
                        syncThrottleCounterParams();
                    }
                }
                throttleContext.flushCallerContext(this, id);
                // can complete access

            } else { // if  count has come to be similar or exceeded max request count : set the nextAccessTime

                // if first exceeding request  (nextAccessTime = 0)
                if (this.nextAccessTime == 0) {
                   // log.info("### 8 canAccessIfUnitTimeNotOver** if caller has not already prohibit (nextAccessTime == 0)");
                    //and if there is no prohibit time  period in configuration
                    long prohibitTime = configuration.getProhibitTimePeriod();
                    log.info("C-canAccessIfUnitTimeNotOver** : prohibitTime:" + prohibitTime);
                    if (prohibitTime == 0) {
                        //prohibit access until unit time period is over
                        this.nextAccessTime = this.firstAccessTime + configuration.getUnitTime();
                     //   log.info("A-canAccessIfUnitTimeNotOver** : firstAccessTime:" + this.firstAccessTime);
                     //   log.info("B-canAccessIfUnitTimeNotOver** : nextAccessTime:" + this.nextAccessTime);
                    } else {
                        //if there is a prohibit time period in configuration ,then
                        //set it as prohibit period
                        this.nextAccessTime = currentTime + prohibitTime;
                     //   log.info("C-canAccessIfUnitTimeNotOver** : nextAccessTime:" + this.nextAccessTime);
                    }
                    if (log.isDebugEnabled()) {
                        String type = ThrottleConstants.IP_BASE == configuration.getType() ?
                                "IP address" : "domain";
                        log.debug("Maximum Number of requests are reached for caller with "
                                + type + " - " + this.id);
                    }
                    // Send the current state to others (clustered env)
                    throttleContext.flushCallerContext(this, id);
                } else { // second to onwards exceeding requests //  TODO: CC1: analyze this and change if needed
                   // log.info("canAccessIfUnitTimeNotOver** Else of (if caller has not already prohibit) : (nextAccessTime != 0)");
                    // else , if the caller has already prohibit and prohibit
                    // time period has already over
                    if (this.nextAccessTime <= currentTime) {
                        if (log.isDebugEnabled()) {
                            log.debug("CallerContext Checking access if unit time is not over before time window exceed >> "
                                    + "Access allowed=" + maxRequest + " available="
                                    +  (maxRequest - (this.globalCount.get() + this.localCount.get()))
                                    + " key=" + this.getId() + " currentGlobalCount=" + globalCount
                                    + " currentTime=" + currentTime + " " + "nextTimeWindow=" + this.nextTimeWindow
                                    + " currentLocalCount=" + localCount + " " + "Tier=" + configuration.getID()
                                    + " nextAccessTime=" + this.nextAccessTime);
                        }
                        // remove previous caller context
                        if (this.nextTimeWindow != 0) {
                            throttleContext.removeCallerContext(id);
                        }
                        // reset the states so that, this is the first access
                        this.nextAccessTime = 0;
                        canAccess = true;
                        setIsThrottleParamSyncingModeSync(false); // as this is the first access

                        this.globalCount.set(0);// can access the system   and this is same as first access
                        this.localCount.set(1);
                        this.firstAccessTime = currentTime;
                        this.nextTimeWindow = currentTime + configuration.getUnitTime();
                        log.info("$$$UTNO globalCount:" + this.globalCount + " , localCount:" + this.localCount +
                                ", firstAccessTime:" + this.firstAccessTime + " , nextTimeWindow:" + this.nextTimeWindow);
                        throttleContext.replicateTimeWindow(this.id); // 1-WindowReplicator
                        throttleContext.addAndFlushCallerContext(this, this.id); // 2-ThrottleCounterReplicator

                        if (log.isDebugEnabled()) {
                            log.debug("Caller=" + this.getId() + " has reset counters and added for replication when unit "
                                      + "time is not over");
                        }
                    } else {
                        if (log.isDebugEnabled()) {
                            String type = ThrottleConstants.IP_BASE == configuration.getType() ?
                                    "IP address" : "domain";
                            log.debug("Prohibit period is not yet over for caller with "
                                    + type + " - " + this.id);
                        }
                    }
                }
            }

        }
        log.info("$$$ Decission made");
        return canAccess;
    }

    /**
     * To verify access if unit time has already over
     *
     * @param configuration   -The Configuration for this caller
     * @param throttleContext -The Throttle that caller having pass
     * @param currentTime     -The system current time
     * @return boolean        -The boolean value which say access will allow or not
     */
    private boolean canAccessIfUnitTimeOver(CallerConfiguration configuration, ThrottleContext throttleContext, long currentTime) {

        //log.info("canAccessIfUnitTimeOver***");
        boolean canAccess = false;
        // if number of access for a unit time is less than MAX and
        // if the unit time period (session time) has just over
        int maxRequest = configuration.getMaximumRequestPerUnitTime();
        log.info("%%% : canAccessIfUnitTimeOver**  globalCount:" + this.globalCount + " , localCount:" + this.localCount +
                ", firstAccessTime:" + this.firstAccessTime + " , nextTimeWindow:" + this.nextTimeWindow + " localHits:"
                + this.localHits + " isThrottleParamSyncingModeSync:" + isThrottleParamSyncingModeSync);
        boolean isThrottleParamSyncingModeSync_local = false;

        /* if (isThrottleParamSyncingModeSync.updatedTime >= nextTimeWindow && isThrottleParamSyncingModeSync == true) {
             isThrottleParamSyncingModeSync_local = true;
        */
        if (maxRequest != 0) {
            // first req, after exceeding previous window if in previous window, the max limit was not exceeded
            if ((this.globalCount.get() + this.localCount.get()) < maxRequest) {
                log.info("%%%AAA");
                if (ThrottleServiceDataHolder.getInstance().getThrottleProperties().isThrottleSyncAsyncHybridModeEnabled()) { // remove this condition considering localHits
                    if (isThrottleParamSyncingModeSync_local) {
                        log.info("%%% Going to run throttle param syncing in sync mode");
                        syncThrottleWindowParams();
                        syncThrottleCounterParams();
                    }
                }
                if (this.nextTimeWindow != 0) {
                    log.info("%%%BBB");

                    // Removes and sends the current state to others  (clustered env)
                    //remove previous callercontext instance
                    throttleContext.removeCallerContext(id);
                    this.globalCount.set(0);// can access the system   and this is same as first access
                    this.localCount.set(1);
                    this.localHits.set(0);
                    this.firstAccessTime = currentTime;
                    this.nextTimeWindow = currentTime + configuration.getUnitTime();
                    if (!ThrottleServiceDataHolder.getInstance().getThrottleProperties().isThrottleSyncAsyncHybridModeEnabled()) {
                        throttleContext.replicateTimeWindow(this.id);
                    }
                    // registers caller and send the current state to others (clustered env)
                    throttleContext.addAndFlushCallerContext(this, id);
                    log.info("%%% : canAccessIfUnitTimeOver**  globalCount:" + this.globalCount + " , localCount:" + this.localCount +
                            ", firstAccessTime:" + this.firstAccessTime + " , nextTimeWindow:" + this.nextTimeWindow);

                }
                if (log.isDebugEnabled()) {
                    log.debug("CallerContext Checking access if unit time over next time window>> Access allowed="
                            +  maxRequest + " available=" + (maxRequest - (this.globalCount.get() + this.localCount.get()))
                            + " key=" + this.getId()+ " currentGlobalCount=" + globalCount + " currentTime=" + currentTime
                            + " nextTimeWindow=" + this.nextTimeWindow +" currentLocalCount=" + localCount + " Tier="
                            + configuration.getID() + " nextAccessTime="+ this.nextAccessTime);
                }
                canAccess = true; // this is bonus access
                //next time callers can access as a new one
            } else { // if in previous window, the max limit was exceeded
                log.info("CCC");

                // if caller in prohibit session  and prohibit period has just over
                if ((this.nextAccessTime == 0) || (this.nextAccessTime <= currentTime)) {
                    if (ThrottleServiceDataHolder.getInstance().getThrottleProperties().isThrottleSyncAsyncHybridModeEnabled()) { // remove this condition considering localHits
                        if (isThrottleParamSyncingModeSync_local) {
                            log.info("%%% Going to run throttle param syncing in sync mode");
                            syncThrottleWindowParams();
                            syncThrottleCounterParams();
                        }
                    }

                    if (log.isDebugEnabled()) {
                        log.debug("CallerContext Checking access if unit time over>> Access allowed=" + maxRequest
                                + " available=" + (maxRequest - (this.globalCount.get() + this.localCount.get())) + " key=" + this.getId()
                                + " currentGlobalCount=" + globalCount + " currentTime=" + currentTime + " nextTimeWindow=" + this.nextTimeWindow
                                + " currentLocalCount=" + localCount + " Tier=" + configuration.getID() + " nextAccessTime="
                                + this.nextAccessTime);
                    }
                    //remove previous callercontext instance
                    if (this.nextTimeWindow != 0) {
                        throttleContext.removeCallerContext(id);
                    }
                    // reset the states so that, this is the first access
                    this.nextAccessTime = 0;
                    canAccess = true;
                    log.info("### 9 - canAccessIfUnitTimeOver***");

                    //setIsThrottleParamSyncingModeSync(false); // as canAccess is set as 'true'
                    this.localHits.set(0);

                    this.globalCount.set(0);// can access the system   and this is same as first access
                    this.localCount.set(1);
                    this.firstAccessTime = currentTime;
                    this.nextTimeWindow = currentTime + configuration.getUnitTime();
                    // registers caller and send the current state to others (clustered env)
                    throttleContext.replicateTimeWindow(this.id);
                    throttleContext.addAndFlushCallerContext(this, id);
                    log.info("DDD : canAccessIfUnitTimeOver**  globalCount:" + this.globalCount + " , localCount:" + this.localCount +
                            ", firstAccessTime:" + this.firstAccessTime + " , nextTimeWindow:" + this.nextTimeWindow);
                    if (log.isDebugEnabled()) {
                        log.debug("Caller=" + this.getId() + " has reset counters and added for replication when unit "
                                  + "time is over");
                    }
                } else {
                    // if  caller in prohibit session  and prohibit period has not  over
                    if (log.isDebugEnabled()) {
                        String type = ThrottleConstants.IP_BASE == configuration.getType() ?
                                "IP address" : "domain";
                        log.debug("Even unit time has over , CallerContext in prohibit state :"
                                + type + " - " + this.id);
                    }
                }
            }

        }
        return canAccess;

    }

    /**
     *
     */
    public void syncThrottleCounterParams() {
        if (getNextTimeWindow() > System.currentTimeMillis()) {
            log.info("### 4 - Running throttleCounterParamSync. ");
            String id = getId();
            long localCounter = localCount.get();
            resetLocalCounter();
            Long distributedCounter = SharedParamManager.asyncGetAndAddDistributedCounter(id, localCounter);
            log.info("$$$ 4.1 sharedCounter increased from " + distributedCounter + " to:" + SharedParamManager.getDistributedCounter(id));

            //Update instance's global counter value with distributed counter
            long x = getGlobalCounter();
            setGlobalCounter(distributedCounter + localCounter);
            log.info("### 4.2 globalCounter increased from:" + x + " to : " + this.globalCount);
            log.info("### local counter reseted to 0");
        }
    }

    /**
     *
     */
    public void throttleCounterParamSync1() {
        if (getNextTimeWindow() > System.currentTimeMillis()) {
            log.info("### 4 - Running throttleCounterParamSync. ");
            String id = getId();
            long localCounter = localCount.get();
            resetLocalCounter();
            Long distributedCounter = SharedParamManager.asyncGetAndAddDistributedCounter(id, localCounter);
            log.info("$$$ 4.1 sharedCounter increased from " + distributedCounter + " to:" + SharedParamManager.getDistributedCounter(id));

            //Update instance's global counter value with distributed counter
            long x = getGlobalCounter();
            setGlobalCounter(distributedCounter + localCounter);
            log.info("### 4.2 globalCounter increased from:" + x + " to : " + this.globalCount);
            log.info("### local counter reseted to 0");
        }
    }

    public void syncThrottleWindowParams() {
        log.info("### 5 - Running throttleWindowParamSync. ");

        // ThrottleWindowReplicator run() method
        String callerId = getId();
        long sharedTimestamp = SharedParamManager.getSharedTimestamp(getId());  // this will be set 0 if the redis key-value pair removed
        long sharedNextWindow = sharedTimestamp + getUnitTime();
        long localFirstAccessTime = getFirstAccessTime();
        // First if statement check whether local first access time is lower than the current
        // global counter if so it will adjust the local first access time to global time to
        // adjust the time window
        log.info("INITIAL ** sharedTimestamp :" + sharedTimestamp + " sharedNextWindow :" + sharedNextWindow + " localFirstAccessTime :" + localFirstAccessTime);

        log.info("$$$ localCounter:" + this.localCount + ", globalCounter:" + this.globalCount + ", localHits:" + this.localHits);
        if (localFirstAccessTime < sharedTimestamp) {  // TODO:  this condition needs review
            log.info("Hit if ***** A1");
            setFirstAccessTime(sharedTimestamp);
            setNextTimeWindow(sharedNextWindow);
            setGlobalCounter(SharedParamManager.getDistributedCounter(callerId));
            if (log.isDebugEnabled()) {
                log.debug("Setting time windows of caller context when window already set=" + callerId);
            }
            //If some request comes to a nodes after some node set the shared timestamp then this
            // check whether the first access time of local is in between the global time window
            // if so this will set local caller context time window to global
        } else if (localFirstAccessTime == sharedTimestamp) {
            setGlobalCounter(SharedParamManager.getDistributedCounter(callerId));
            log.info("### localFirstAccessTime == sharedTimestamp");
            log.info("### - globalCounter :" + getGlobalCounter());
        } else if (localFirstAccessTime > sharedTimestamp    // if another node had set the shared timestamp, earlier
                && localFirstAccessTime < sharedNextWindow) {
            log.info("Hit ELSE-IF**** A2");

            setFirstAccessTime(sharedTimestamp);
            setNextTimeWindow(sharedNextWindow);
            log.info("### - distributedCounter :" + SharedParamManager.getDistributedCounter(callerId));
            setGlobalCounter(SharedParamManager.getDistributedCounter(callerId));
            if (log.isDebugEnabled()) {
                log.debug("Setting time windows of caller context in intermediate interval=" +
                        callerId);
            }
            log.info("### - getGlobalCounter :" + getGlobalCounter());
            //If above two statements not meets, this is the place where node set new window if
            // global first access time is 0, then it will be the beginning of the throttle time time
            // window so present node will set shared timestamp and the distributed counter. Also if time
            // window expired this will be the node who set the next time window starting time
        } else {
            log.info("Hit Else**** A3");  // In the flow this is the first time that reaches throttleWindowParamSync method. And then at canAccessIfUnitTimeOver flow, the first call after the sharedTimestamp is removed from redis.
            SharedParamManager.setSharedTimestamp(callerId, localFirstAccessTime);
            SharedParamManager.setDistributedCounter(callerId, 0);
            SharedParamManager.setExpiryTime(callerId,
                    getUnitTime() + localFirstAccessTime);
            //Reset global counter here as throttle replicator task may have updated global counter
            //with dirty value
            //resetGlobalCounter();
            //callerContext.setLocalCounter(1)
            log.info("### - after setting GlobalCounter :" + getGlobalCounter());
            //setLocalCounter(1);//Local counter will be set to one as new time window starts
            if (log.isDebugEnabled()) {
                log.debug("Complete resetting time window of=" + callerId);
            }
        }
    }

    /**
     * Clean up the callers - remove all callers that have expired their time window
     *
     * @param configuration   -The Configuration for this caller
     * @param throttleContext -The Throttle that caller having pass
     * @param currentTime     -The system current time
     */
    public void cleanUpCallers(CallerConfiguration configuration,
                               ThrottleContext throttleContext, long currentTime) {

      //  if (log.isDebugEnabled()) {
            log.info("Cleaning up the inactive caller's states ... ");
       // }
        if (configuration == null) {
           // if (log.isDebugEnabled()) {
                log.info("Couldn't find the configuration .");
           // }
            return;
        }
        // if number of access for a unit time is less than MAX and
        // if the unit time period (session time) has over

        int maxRequest = configuration.getMaximumRequestPerUnitTime();
        if (!(maxRequest == 0)) {
            if ((this.globalCount.get() + this.localCount.get()) <= (maxRequest - 1)) {
                if (this.nextTimeWindow != 0 && this.nextTimeWindow < (currentTime - this.unitTime)) {
                  //  if (log.isDebugEnabled()) {
                        log.info("Removing caller with id " + this.id);
                  //  }
                    //Removes the previous callercontext and Sends the current state to
                    //  others (clustered env)
                    throttleContext.removeAndDestroyShareParamsOfCaller(id);
                }
            } else {
                // if number of access for a unit time has just been greater than MAX
                // now same as a new session
                // OR
                //  if caller in prohibit session  and prohibit period has just over and only
                if ((this.nextAccessTime == 0) || this.nextAccessTime < (currentTime - this.unitTime)) {
                    if (this.nextTimeWindow != 0 && this.nextTimeWindow < (currentTime - this.unitTime)) {
                      //  if (log.isDebugEnabled()) {
                            log.debug("Removing caller with id " + this.id);
                      //  }
                        //Removes the previous callercontext and Sends
                        //  the current state to others (clustered env)
                        throttleContext.removeAndDestroyShareParamsOfCaller(id);
                    }
                }
            }
        }
    }

    /**
     * Check whether that caller can access or not ,based on current state and pre-defined policy
     *
     * @param throttleContext -The Context for this caller - runtime state
     * @param configuration   -The Configuration for this caller - data from policy
     * @param currentTime     -The current system time
     * @return boolean        -The boolean value which say access will allow or not
     * @throws ThrottleException throws for invalid throttle configuration
     */
    public boolean canAccess(ThrottleContext throttleContext, CallerConfiguration configuration,
                             long currentTime) throws ThrottleException {
        boolean canAccess;
        if (configuration == null) {
            if (log.isDebugEnabled()) {
                log.debug("Couldn't find the configuration .");
            }
            return true;
        }
        if (configuration.getMaximumRequestPerUnitTime() < 0
                || configuration.getUnitTime() <= 0
                || configuration.getProhibitTimePeriod() < 0) {
            throw new ThrottleException("Invalid Throttle Configuration");
        }

        // if caller access first time in his new session
        if (this.firstAccessTime == 0) {
            initAccess(configuration, throttleContext, currentTime); // sets firstAccessTime, nextTimeWindow
        }
        // if unit time period (session time) is not over
        log.info("\n\n ### NEW REQUEST RECEIVED ! - currentTime: " + currentTime + " (" + getReadableTime(currentTime) + ") " + " Thread name: " + Thread.currentThread().getName() + " Thread id: " + Thread.currentThread().getId() );
        log.info("### Before evaluating:: localHits :" + localHits.get() + " ### localCount :" + localCount.get()
                + " ### globalCount :" + globalCount.get() + " MaxLimit:" + configuration.getMaximumRequestPerUnitTime()
                + " Thread name: " + Thread.currentThread().getName() + " Thread id: " + Thread.currentThread().getId());

        DistributedThrottleProcessor distributedThrottleProcessor =
                ThrottleServiceDataHolder.getInstance().getDistributedThrottleProcessor();
        if (distributedThrottleProcessor != null && distributedThrottleProcessor.isEnable()) {
            long startTime = System.currentTimeMillis();
            canAccess = distributedThrottleProcessor.canAccessBasedOnUnitTime(this, configuration, throttleContext, currentTime);
            long duration = System.currentTimeMillis() - startTime;
            log.info("*********** LATENCY FOR THROTTLE PROCESSING: " + duration + " ms" + " Thread name: " + Thread.currentThread().getName() + " Thread id: " + Thread.currentThread().getId());
        } else {
            log.info(">>>> ERROR: CALLED OLD METHOD");
            canAccess = canAccessBasedOnUnitTime(configuration, throttleContext, currentTime);
        }
//        if (this.nextTimeWindow > currentTime) {
//            canAccess = canAccessIfUnitTimeNotOver(configuration, throttleContext, currentTime);
//        } else {
//            canAccess = canAccessIfUnitTimeOver(configuration, throttleContext, currentTime);
//        }

//        if (canAccess) {
//            localHits.getAndIncrement();
//            log.info("### CCcA localHits:" + localHits.get());
//        }
//        if (throttleProperties.isThrottleSyncAsyncHybridModeEnabled() && this.localHits.get() == localQuota) {
//                log.info("### 3 - Local quota reached. SWITCHED TO SYNC MODE !!!. this.localHits : " + this.localHits.get());
//            this.isThrottleParamSyncingModeSync = true;
//        }

        return canAccess;

    }

    // TODO: may remove or move this method to some util class
    public static String getReadableTime(long time) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
        Date date = new Date(time);
        String formattedTime = dateFormat.format(date);
        return formattedTime;
    }

    private boolean canAccessBasedOnUnitTime(CallerConfiguration configuration, ThrottleContext throttleContext, long currentTime) {
        if (this.nextTimeWindow > currentTime) {
            return canAccessIfUnitTimeNotOver(configuration, throttleContext, currentTime);
        } else {
            return canAccessIfUnitTimeOver(configuration, throttleContext, currentTime);
        }
    }


        /**
         * Returns the next time window
         *
         * @return long value of next time window
         */
    public long getNextTimeWindow() {
        return this.nextTimeWindow;
    }

    public void incrementGlobalCounter(int incrementBy) {
        globalCount.addAndGet(incrementBy);
    }

    public void incrementLocalCounter() {
        localCount.incrementAndGet();
    }

    public long getGlobalCounter() {
        return globalCount.get();
    }

    public void setGlobalCounter(long counter) {
        globalCount.set(counter);
    }

    public void setLocalCounter(long counter) {
        log.info(">>> changing local counter from:" + localCount.get() + " Thread name: " + Thread.currentThread().getName() + " Thread id: " + Thread.currentThread().getId());
        localCount.set(counter);
        log.info(">>> changing local counter to:" + localCount.get() + " Thread name: " + Thread.currentThread().getName() + " Thread id: " + Thread.currentThread().getId());
    }

    public long getLocalCounter() {
        return localCount.get();
    }

    public void setLocalHits(long counter) {
        localHits.set(counter);
    }

    public long getLocalHits() {
        return localHits.get();
    }

    public void incrementLocalHits() {
        localHits.incrementAndGet();
    }

    public void resetLocalCounter() {
        localCount.set(0);
    }

    public void resetGlobalCounter() {
        globalCount.set(0);
    }

    /**
     * Gets type of throttle that this caller belong  ex : ip/domain
     *
     * @return Returns the type of the throttle
     */
    public abstract int getType();

    public long getFirstAccessTime() {
        return firstAccessTime;
    }

    public void setFirstAccessTime(long firstAccessTime) {
        this.firstAccessTime = firstAccessTime;
    }

    public void setNextTimeWindow(long nextTimeWindow) {
        this.nextTimeWindow = nextTimeWindow;
    }

    public long getUnitTime() {
        return unitTime;
    }

    public void setUnitTime(long unitTime) {
        this.unitTime = unitTime;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public void setIsThrottleParamSyncingModeSync(boolean isThrottleParamSyncingModeSync) {
        this.isThrottleParamSyncingModeSync = isThrottleParamSyncingModeSync;
    }

    public boolean isThrottleParamSyncingModeSync() {
        return isThrottleParamSyncingModeSync;
    }

    public long getLocalQuota() {
        return localQuota;
    }

    public void setLocalQuota(long localQuota) {
        this.localQuota = localQuota;
    }

    public long getNextAccessTime() {
        return nextAccessTime;
    }

    public void setNextAccessTime(long nextAccessTime) {
        this.nextAccessTime = nextAccessTime;
    }


    public void setSyncModeLastUpdatedTime(long syncModeLastUpdatedTime) {
        this.syncModeLastUpdatedTime = syncModeLastUpdatedTime;
    }

    public long getSyncModeLastUpdatedTime() {
        return syncModeLastUpdatedTime;
    }
}
