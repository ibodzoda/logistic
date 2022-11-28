package tj.abad.duobtms.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.abad.duobtms.database.model.MoneyTransaction;
import tj.abad.duobtms.database.model.TransactionType;
import tj.abad.duobtms.database.model.application.Application;
import tj.abad.duobtms.database.model.application.ApplicationMoneyTransfer;
import tj.abad.duobtms.database.model.application.ApplicationStatus;
import tj.abad.duobtms.database.model.application.ApplicationType;
import tj.abad.duobtms.database.repository.ClientRepository;
import tj.abad.duobtms.database.repository.EmployeeRepository;
import tj.abad.duobtms.database.repository.MoneyTransactionRepository;
import tj.abad.duobtms.database.repository.application.ApplicationArticleRepository;
import tj.abad.duobtms.database.repository.application.ApplicationCargoIssueRepository;
import tj.abad.duobtms.database.repository.application.ApplicationClientBalanceRepository;
import tj.abad.duobtms.database.repository.application.ApplicationMoneyTransferRepository;
import tj.abad.duobtms.exception.BalanceNotEnoughException;
import tj.abad.duobtms.exception.RepeatAttemptException;
import tj.abad.duobtms.mapper.ApplicationMapper;
import tj.abad.duobtms.request.ApplicationTransactionRequest;
import tj.abad.duobtms.response.ApplicationClientBalanceResponse;
import tj.abad.duobtms.response.ApplicationMoneyTransferResponse;
import tj.abad.duobtms.response.application.ApplicationCargoIssueResponse;
import tj.abad.duobtms.response.application.ApplicationResponse;
import tj.abad.duobtms.service.MoneyTransactionService;
import tj.abad.duobtms.service.WarehouseService;
import tj.abad.duobtms.utils.SessionUtils;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class MoneyTransactionServiceImpl implements MoneyTransactionService {
    private final MoneyTransactionRepository repository;
    private final ApplicationClientBalanceRepository aCBRepository;
    private final ApplicationArticleRepository aARepository;
    private final ApplicationMoneyTransferRepository aMTRepository;
    private final ApplicationCargoIssueRepository aCIRepository;
    private final ClientRepository clientRepository;
    private final WarehouseService warehouseService;
    private final EmployeeRepository employeeRepository;
    private final ApplicationMapper applicationMapper;


    @Override
    public MoneyTransaction income(MoneyTransaction moneyTransaction) {
        return repository.save(moneyTransaction);
    }

    @Override
    public MoneyTransaction outcome(MoneyTransaction moneyTransaction) {
        return repository.save(moneyTransaction);
    }

    @Override
    public ApplicationClientBalanceResponse transactionClientBalance(Long applicationId, ApplicationTransactionRequest request) {
        var application = aCBRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Заявка " + applicationId + " не найден"));
        checkRepeatAttempt(application.getStatus(), applicationId);
        initApplication(application, request);

        var currentUser = SessionUtils.currentEmployee();
        var warehouse = warehouseService.findByEmployeeId(currentUser.getId());
        var client = clientRepository.findById(application.getClient().getId())
                .orElseThrow(() -> new EntityNotFoundException("Клиент " + application.getClient().getId() + " не найден"));
        var cbTransaction = initTransaction(application)
                .warehouse(warehouse)
                .createdBy(currentUser)
                .build();

        if (application.getType().equals(ApplicationType.INCOME)) {
            cbTransaction.setType(TransactionType.INCOME);
            warehouseService.income(warehouse, application.getActualAmount(), application.getConvertAmount());
            client.setBalance(BigDecimal.valueOf(client.getBalance()).add(application.getTotalAmount()).doubleValue());
        } else {
            cbTransaction.setType(TransactionType.OUTCOME);
            warehouseService.outcome(warehouse, application.getActualAmount(), application.getConvertAmount());
            if (application.getTotalAmount().compareTo(BigDecimal.valueOf(client.getBalance())) > 0) {
                throw new BalanceNotEnoughException("Ваш баланс не достаточно");
            }
            client.setBalance(BigDecimal.valueOf(client.getBalance()).subtract(application.getTotalAmount()).doubleValue());
        }
        application.setStatus(ApplicationStatus.PAID);
        clientRepository.save(client);
        repository.save(cbTransaction);
        return applicationMapper.toBalanceClientResponse(aCBRepository.save(application));
    }

    @Override
    public ApplicationResponse transactionByArticle(Long applicationId, ApplicationTransactionRequest request) {
        var application = aARepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Заявка " + applicationId + " не найден"));
        checkRepeatAttempt(application.getStatus(), applicationId);
        initApplication(application, request);

        var currentUser = SessionUtils.currentEmployee();
        var employeeWarehouse = warehouseService.findByEmployeeId(currentUser.getId());
        var aInTransaction = initTransaction(application)
                .warehouse(employeeWarehouse)
                .createdBy(currentUser)
                .build();
        application.setStatus(ApplicationStatus.PAID);
        if (application.getType().equals(ApplicationType.INCOME)) {
            warehouseService.income(employeeWarehouse, application.getActualAmount(), application.getConvertAmount());
            aInTransaction.setType(TransactionType.INCOME);
            repository.save(aInTransaction);
            return applicationMapper.toArticleInResponse(aARepository.save(application));
        } else if (application.getType().equals(ApplicationType.OUTCOME)) {
            warehouseService.outcome(employeeWarehouse, application.getActualAmount(), application.getConvertAmount());
            aInTransaction.setType(TransactionType.OUTCOME);
            repository.save(aInTransaction);
            return applicationMapper.toArticleOutResponse(aARepository.save(application));
        } else {
            throw new IllegalArgumentException("Choose correct application type");
        }
    }

    @Override
    public ApplicationMoneyTransferResponse transactionMoneyTransferOnRoad(Long applicationId, ApplicationTransactionRequest request) {
        var application = aMTRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Заявка " + applicationId + " не найден"));
        checkRepeatAttempt(application, applicationId);
        initApplication(application, request);
        application.setOnRoadMoneyUnit(application.getActualMoneyUnit());
        application.setAmountOnRoad(application.getTotalAmount());
        var currentUser = SessionUtils.currentEmployee();
        var employeeWarehouse = warehouseService.findByEmployeeId(currentUser.getId());
        application.setTransferBy(currentUser);
        application.setStatus(ApplicationStatus.ON_ROAD);

        var aMTTransaction = initTransaction(application)
                .warehouse(employeeWarehouse)
                .createdBy(currentUser)
                .type(TransactionType.OUTCOME)
                .build();

        warehouseService.outcome(employeeWarehouse, application.getActualAmount(), application.getConvertAmount());
        repository.save(aMTTransaction);
        return applicationMapper.toMoneyTransferResponse(aMTRepository.save(application));

    }

    @Override
    public ApplicationMoneyTransferResponse transactionMoneyTransferDelivered(Long applicationId) {
        var application = aMTRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Заявка " + applicationId + " не найден"));
        checkRepeatAttempt(application.getStatus(), applicationId);

        var toWarehouse = application.getToWarehouse();
        var currentUser = SessionUtils.currentEmployee();
        if (!employeeRepository.existsByWarehouseIdAndId(toWarehouse.getId(), currentUser.getId())) {
            throw new IllegalArgumentException("Current user isn't belong to this warehouse id=" + toWarehouse.getId());
        }

        if (application.getAmountOnRoad() != null && application.getOnRoadMoneyUnit() != null) {
            var incomeTransaction = MoneyTransaction.moneyTransactionBuilder()
                    .application(application)
                    .warehouse(toWarehouse)
                    .createdBy(currentUser)
                    .actualMoneyUnit(application.getOnRoadMoneyUnit())
                    .actualAmount(application.getAmountOnRoad())
                    .totalAmount(application.getTotalAmount())
                    .convertMoneyUnit(application.getConvertMoneyUnit())
                    .convertAmount(application.getConvertAmount())
                    .currency(application.getCurrency())
                    .type(TransactionType.INCOME)
                    .build();
            application.setStatus(ApplicationStatus.PAID);
            application.setDelivered(true);
            warehouseService.income(toWarehouse, application.getAmountOnRoad(), application.getConvertAmount());
            repository.save(incomeTransaction);
            return applicationMapper.toMoneyTransferResponse(aMTRepository.save(application));
        } else {
            throw new IllegalArgumentException("Choose correct application type");
        }
    }

    @Override
    public ApplicationCargoIssueResponse transactionCargoIssue(Long applicationId) {
        var application = aCIRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Заявка " + applicationId + " не найден"));

        checkRepeatAttempt(application.getStatus(), applicationId);
        var currentUser = SessionUtils.currentEmployee();
        var employeeWarehouse = warehouseService.findByEmployeeId(currentUser.getId());
        var client = application.getClient();
        var aCITransaction = initTransaction(application)
                .warehouse(employeeWarehouse)
                .createdBy(currentUser)
                .client(client)
                .build();
        application.setStatus(ApplicationStatus.PAID);

        if (application.getTotalAmount().compareTo(BigDecimal.valueOf(client.getBalance())) > 0 && !application.isCredit()) {
            throw new IllegalArgumentException("Ваш баланс не достаточно");
        } else if(application.getActualAmount().compareTo(BigDecimal.valueOf(client.getBalance())) > 0 && application.isCredit() && !SessionUtils.isAdmin(Objects.requireNonNull(SessionUtils.currentUser()))) {
            throw new IllegalArgumentException("Ваш баланс не достаточно");
        }

        client.setBalance(BigDecimal.valueOf(client.getBalance()).subtract(application.getTotalAmount()).doubleValue());

        repository.save(aCITransaction);
        clientRepository.save(client);
        return applicationMapper.toCargoDetailsIssueResponse(aCIRepository.save(application));
    }

    @Override
    public boolean checkUserBelongWarehouse(Long applicationId) {
        var application = aMTRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Заявка " + applicationId + " не найден"));
        var toWarehouse = application.getToWarehouse();
        var currentUser = SessionUtils.currentEmployee();
        return employeeRepository.existsByWarehouseIdAndId(toWarehouse.getId(), currentUser.getId());
    }

    private void checkRepeatAttempt(ApplicationStatus status, Long applicationId) {
        if (status.equals(ApplicationStatus.PAID)) {
            throw new RepeatAttemptException("Заявка " + applicationId + " уже обработано");
        }
    }

    private void checkRepeatAttempt(ApplicationMoneyTransfer application, Long applicationId) {
        if (application.getAmountOnRoad() != null && application.getOnRoadMoneyUnit() != null) {
            throw new RepeatAttemptException("Заявка " + applicationId + " уже обработано");
        }
    }

    private void initApplication(Application application, ApplicationTransactionRequest request) {
        application.setActualAmount(request.getActualAmount());
        application.setConvertAmount(request.getConvertAmount());
        application.setCurrency(request.getCurrency());
        application.setTotalAmount(request.getTotalAmount());
    }

    private MoneyTransaction.MoneyTransactionBuilder initTransaction(Application application) {
        return MoneyTransaction.moneyTransactionBuilder()
                .application(application)
                .actualMoneyUnit(application.getActualMoneyUnit())
                .actualAmount(application.getActualAmount())
                .totalAmount(application.getTotalAmount())
                .convertMoneyUnit(application.getConvertMoneyUnit())
                .convertAmount(application.getConvertAmount())
                .currency(application.getCurrency());
    }
}
