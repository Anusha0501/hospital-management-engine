package com.hospital.appointment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedLockService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DistributedLockService.class);
    private final RedissonClient redissonClient;

    @Value("${hospital.appointment.lock-timeout-seconds}")
    private long lockTimeoutSeconds;

    /**
     * Acquires a distributed lock for the given key.
     * 
     * @param lockKey The key to lock
     * @return true if lock acquired successfully, false otherwise
     */
    public boolean acquireLock(String lockKey) {
        return acquireLock(lockKey, lockTimeoutSeconds, TimeUnit.SECONDS);
    }

    /**
     * Acquires a distributed lock for the given key with custom timeout.
     * 
     * @param lockKey The key to lock
     * @param timeout Lock timeout
     * @param timeUnit Time unit for timeout
     * @return true if lock acquired successfully, false otherwise
     */
    public boolean acquireLock(String lockKey, long timeout, TimeUnit timeUnit) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            boolean acquired = lock.tryLock(0, timeout, timeUnit);
            
            if (acquired) {
                log.debug("Successfully acquired lock for key: {}", lockKey);
            } else {
                log.warn("Failed to acquire lock for key: {}", lockKey);
            }
            
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while acquiring lock for key: {}", lockKey, e);
            return false;
        } catch (Exception e) {
            log.error("Error acquiring lock for key: {}", lockKey, e);
            return false;
        }
    }

    /**
     * Releases a distributed lock for the given key.
     * 
     * @param lockKey The key to unlock
     * @return true if lock released successfully, false otherwise
     */
    public boolean releaseLock(String lockKey) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Successfully released lock for key: {}", lockKey);
                return true;
            } else {
                log.warn("Attempted to release lock not held by current thread for key: {}", lockKey);
                return false;
            }
        } catch (IllegalMonitorStateException e) {
            log.warn("Lock is not held by current thread for key: {}", lockKey);
            return false;
        } catch (Exception e) {
            log.error("Error releasing lock for key: {}", lockKey, e);
            return false;
        }
    }

    /**
     * Executes a task within a distributed lock.
     * 
     * @param lockKey The key to lock
     * @param task The task to execute
     * @return true if task executed successfully, false otherwise
     */
    public boolean executeWithLock(String lockKey, Runnable task) {
        return executeWithLock(lockKey, lockTimeoutSeconds, TimeUnit.SECONDS, task);
    }

    /**
     * Executes a task within a distributed lock with custom timeout.
     * 
     * @param lockKey The key to lock
     * @param timeout Lock timeout
     * @param timeUnit Time unit for timeout
     * @param task The task to execute
     * @return true if task executed successfully, false otherwise
     */
    public boolean executeWithLock(String lockKey, long timeout, TimeUnit timeUnit, Runnable task) {
        if (acquireLock(lockKey, timeout, timeUnit)) {
            try {
                task.run();
                return true;
            } finally {
                releaseLock(lockKey);
            }
        }
        return false;
    }

    /**
     * Checks if a lock is held by the current thread.
     * 
     * @param lockKey The key to check
     * @return true if lock is held, false otherwise
     */
    public boolean isLockHeld(String lockKey) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            return lock.isHeldByCurrentThread();
        } catch (Exception e) {
            log.error("Error checking lock status for key: {}", lockKey, e);
            return false;
        }
    }

    /**
     * Generates a lock key for time slot booking.
     * 
     * @param doctorId Doctor ID
     * @param timeSlotId Time slot ID
     * @return Lock key string
     */
    public String generateBookingLockKey(Long doctorId, Long timeSlotId) {
        return String.format("booking:lock:doctor:%d:slot:%d", doctorId, timeSlotId);
    }

    /**
     * Generates a lock key for slot management.
     * 
     * @param doctorId Doctor ID
     * @param date Date in YYYY-MM-DD format
     * @return Lock key string
     */
    public String generateSlotManagementLockKey(Long doctorId, String date) {
        return String.format("slot:management:doctor:%d:date:%s", doctorId, date);
    }
}
