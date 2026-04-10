package com.hospital.appointment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DistributedLockServiceTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    @InjectMocks
    private DistributedLockService distributedLockService;

    private String lockKey;

    @BeforeEach
    void setUp() {
        lockKey = "test:lock:key";
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
    }

    @Test
    void acquireLock_Success() throws InterruptedException {
        // Given
        when(rLock.tryLock(0, 30, TimeUnit.SECONDS)).thenReturn(true);

        // When
        boolean result = distributedLockService.acquireLock(lockKey);

        // Then
        assertTrue(result);
        verify(redissonClient).getLock(lockKey);
        verify(rLock).tryLock(0, 30, TimeUnit.SECONDS);
    }

    @Test
    void acquireLock_Failure() throws InterruptedException {
        // Given
        when(rLock.tryLock(0, 30, TimeUnit.SECONDS)).thenReturn(false);

        // When
        boolean result = distributedLockService.acquireLock(lockKey);

        // Then
        assertFalse(result);
        verify(redissonClient).getLock(lockKey);
        verify(rLock).tryLock(0, 30, TimeUnit.SECONDS);
    }

    @Test
    void acquireLock_InterruptedException() throws InterruptedException {
        // Given
        when(rLock.tryLock(0, 30, TimeUnit.SECONDS)).thenThrow(new InterruptedException("Test interrupt"));

        // When
        boolean result = distributedLockService.acquireLock(lockKey);

        // Then
        assertFalse(result);
        assertTrue(Thread.currentThread().isInterrupted());
        verify(redissonClient).getLock(lockKey);
        verify(rLock).tryLock(0, 30, TimeUnit.SECONDS);
    }

    @Test
    void releaseLock_Success() {
        // Given
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        // When
        boolean result = distributedLockService.releaseLock(lockKey);

        // Then
        assertTrue(result);
        verify(redissonClient).getLock(lockKey);
        verify(rLock).isHeldByCurrentThread();
        verify(rLock).unlock();
    }

    @Test
    void releaseLock_NotHeldByCurrentThread() {
        // Given
        when(rLock.isHeldByCurrentThread()).thenReturn(false);

        // When
        boolean result = distributedLockService.releaseLock(lockKey);

        // Then
        assertFalse(result);
        verify(redissonClient).getLock(lockKey);
        verify(rLock).isHeldByCurrentThread();
        verify(rLock, never()).unlock();
    }

    @Test
    void releaseLock_IllegalMonitorStateException() {
        // Given
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        doThrow(new IllegalMonitorStateException("Lock not held")).when(rLock).unlock();

        // When
        boolean result = distributedLockService.releaseLock(lockKey);

        // Then
        assertFalse(result);
        verify(redissonClient).getLock(lockKey);
        verify(rLock).isHeldByCurrentThread();
        verify(rLock).unlock();
    }

    @Test
    void executeWithLock_Success() throws InterruptedException {
        // Given
        Runnable task = mock(Runnable.class);
        when(rLock.tryLock(0, 30, TimeUnit.SECONDS)).thenReturn(true);

        // When
        boolean result = distributedLockService.executeWithLock(lockKey, task);

        // Then
        assertTrue(result);
        verify(redissonClient).getLock(lockKey);
        verify(rLock).tryLock(0, 30, TimeUnit.SECONDS);
        verify(task).run();
        verify(rLock).unlock();
    }

    @Test
    void executeWithLock_LockAcquisitionFails() throws InterruptedException {
        // Given
        Runnable task = mock(Runnable.class);
        when(rLock.tryLock(0, 30, TimeUnit.SECONDS)).thenReturn(false);

        // When
        boolean result = distributedLockService.executeWithLock(lockKey, task);

        // Then
        assertFalse(result);
        verify(redissonClient).getLock(lockKey);
        verify(rLock).tryLock(0, 30, TimeUnit.SECONDS);
        verify(task, never()).run();
        verify(rLock, never()).unlock();
    }

    @Test
    void isLockHeld_True() {
        // Given
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        // When
        boolean result = distributedLockService.isLockHeld(lockKey);

        // Then
        assertTrue(result);
        verify(redissonClient).getLock(lockKey);
        verify(rLock).isHeldByCurrentThread();
    }

    @Test
    void isLockHeld_False() {
        // Given
        when(rLock.isHeldByCurrentThread()).thenReturn(false);

        // When
        boolean result = distributedLockService.isLockHeld(lockKey);

        // Then
        assertFalse(result);
        verify(redissonClient).getLock(lockKey);
        verify(rLock).isHeldByCurrentThread();
    }

    @Test
    void generateBookingLockKey() {
        // Given
        Long doctorId = 1L;
        Long timeSlotId = 2L;

        // When
        String result = distributedLockService.generateBookingLockKey(doctorId, timeSlotId);

        // Then
        assertEquals("booking:lock:doctor:1:slot:2", result);
    }

    @Test
    void generateSlotManagementLockKey() {
        // Given
        Long doctorId = 1L;
        String date = "2024-01-15";

        // When
        String result = distributedLockService.generateSlotManagementLockKey(doctorId, date);

        // Then
        assertEquals("slot:management:doctor:1:date:2024-01-15", result);
    }
}
