package com.mrlu.server.thread;

import lombok.Data;
import org.springframework.transaction.TransactionStatus;

/**
 * @author 简单de快乐
 * @create 2024-02-04 17:38
 */
@Data
public class TcInfo {

    private TransactionStatus transactionStatus;

    private MultiplyThreadTransactionManager.TransactionResource transactionResource;


}
