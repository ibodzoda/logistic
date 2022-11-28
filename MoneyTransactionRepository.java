package tj.abad.duobtms.database.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tj.abad.duobtms.database.model.MoneyTransaction;

public interface MoneyTransactionRepository extends JpaRepository<MoneyTransaction, Long> {
}
