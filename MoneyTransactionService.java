package tj.abad.duobtms.service;

import tj.abad.duobtms.database.model.MoneyTransaction;
import tj.abad.duobtms.request.ApplicationTransactionRequest;
import tj.abad.duobtms.response.ApplicationClientBalanceResponse;
import tj.abad.duobtms.response.ApplicationMoneyTransferResponse;
import tj.abad.duobtms.response.application.ApplicationCargoIssueResponse;
import tj.abad.duobtms.response.application.ApplicationResponse;

public interface MoneyTransactionService {

    ApplicationClientBalanceResponse transactionClientBalance(Long applicationId, ApplicationTransactionRequest request);

    ApplicationResponse transactionByArticle(Long applicationId, ApplicationTransactionRequest requestL);

    ApplicationMoneyTransferResponse transactionMoneyTransferOnRoad(Long applicationId, ApplicationTransactionRequest request);

    ApplicationMoneyTransferResponse transactionMoneyTransferDelivered(Long applicationId);

    ApplicationCargoIssueResponse transactionCargoIssue(Long applicationId);

    boolean checkUserBelongWarehouse(Long applicationId);

    MoneyTransaction income(MoneyTransaction moneyTransaction);

    MoneyTransaction outcome(MoneyTransaction moneyTransaction);
}
