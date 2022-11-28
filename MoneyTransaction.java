package tj.abad.duobtms.database.model;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tj.abad.duobtms.database.model.application.Application;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@Entity
@Table(name = "money_transactions")
public class MoneyTransaction extends AbstractEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private Application application;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_account_id")
    private EmployeeAccount employeeAccount;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_credit_id")
    private ClientCredit clientCredit;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Employee createdBy;
    @Enumerated(EnumType.STRING)
    private TransactionType type;
    @Enumerated(EnumType.STRING)
    private MoneyUnit actualMoneyUnit;
    @Column(columnDefinition = "numeric(10,2)")
    private BigDecimal actualAmount;
    @Column(columnDefinition = "numeric(10,2)")
    private BigDecimal totalAmount;
    @Enumerated(EnumType.STRING)
    private MoneyUnit convertMoneyUnit;
    @Column(columnDefinition = "numeric(6,3)")
    private BigDecimal currency;
    @Column(columnDefinition = "numeric(10,2)")
    private BigDecimal convertAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exchange_transaction_id")
    private ExchangeTransaction exchangeTransaction;

    @Builder(builderMethodName = "moneyTransactionBuilder")

    public MoneyTransaction(Long id, boolean deleted, LocalDateTime createdDate, LocalDateTime updatedDate, Application application, EmployeeAccount employeeAccount,
                            ClientCredit clientCredit, Warehouse warehouse, Client client, Employee createdBy, TransactionType type, MoneyUnit actualMoneyUnit, BigDecimal actualAmount,
                            BigDecimal totalAmount, MoneyUnit convertMoneyUnit, BigDecimal currency, BigDecimal convertAmount, ExchangeTransaction exchangeTransaction) {
        super(id, deleted, createdDate, updatedDate);
        this.application = application;
        this.employeeAccount = employeeAccount;
        this.clientCredit = clientCredit;
        this.warehouse = warehouse;
        this.client = client;
        this.createdBy = createdBy;
        this.type = type;
        this.actualMoneyUnit = actualMoneyUnit;
        this.actualAmount = actualAmount;
        this.totalAmount = totalAmount;
        this.convertMoneyUnit = convertMoneyUnit;
        this.currency = currency;
        this.convertAmount = convertAmount;
        this.exchangeTransaction = exchangeTransaction;
    }
}
