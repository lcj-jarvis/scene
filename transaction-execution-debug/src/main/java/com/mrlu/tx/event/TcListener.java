package com.mrlu.tx.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author 简单de快乐
 * @create 2024-07-23 17:05
 */
@Slf4j
@Component
public class TcListener implements TransactionSynchronization {

    @Override
    public int getOrder() {
        return TransactionSynchronization.super.getOrder();
    }

    @Override
    public void suspend() {
        TransactionSynchronization.super.suspend();
    }

    @Override
    public void resume() {
        TransactionSynchronization.super.resume();
    }

    @Override
    public void flush() {
        TransactionSynchronization.super.flush();
    }

    @Override
    public void beforeCommit(boolean readOnly) {
        String transactionName = TransactionSynchronizationManager.getCurrentTransactionName();
        Integer currentTransactionIsolationLevel = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
        log.info("beforeCommit readOnly={},transactionName={},isolation={}", readOnly, transactionName, currentTransactionIsolationLevel);
    }

    @Override
    public void beforeCompletion() {
        String transactionName = TransactionSynchronizationManager.getCurrentTransactionName();
        Integer currentTransactionIsolationLevel = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
        log.info("beforeCompletion transactionName={},isolation={}", transactionName, currentTransactionIsolationLevel);
    }

    @Override
    public void afterCommit() {
        String transactionName = TransactionSynchronizationManager.getCurrentTransactionName();
        Integer currentTransactionIsolationLevel = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
        log.info("afterCommit transactionName={},isolation={}", transactionName, currentTransactionIsolationLevel);
    }

    /**
     *  TransactionSynchronization.STATUS_COMMITTED, TransactionSynchronization.STATUS_ROLLED_BACK, TransactionSynchronization.STATUS_UNKNOWN,
     */
    @Override
    public void afterCompletion(int status) {
        String transactionName = TransactionSynchronizationManager.getCurrentTransactionName();
        Integer currentTransactionIsolationLevel = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
        log.info("afterCompletion completion result={};transactionName={},isolation={}", status, transactionName, currentTransactionIsolationLevel);
    }

}
