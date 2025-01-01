package com.mrlu.tx.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * @author 简单de快乐
 * @create 2024-07-15 18:20
 *
 * 有问题，先不管
 */
@Component
@Slf4j
public class TcEventListener {


    //@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    //public void listen() {
    //  log.info("after commit;");
    //}
    //
    //@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    //public void before() {
    //    log.info("before commit;");
    //}
    //
    //@TransactionalEventListener(phase = TransactionPhase.AFTER_COMPLETION)
    //public void complete() {
    //    log.info("after complete;");
    //}
    //
    //@TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    //public void afterRollBack() {
    //    log.info("after afterRollBack;");
    //}


}
