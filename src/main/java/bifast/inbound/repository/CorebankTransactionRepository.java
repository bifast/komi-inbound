package bifast.inbound.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import bifast.inbound.model.CorebankTransaction;
						    
@Repository
public interface CorebankTransactionRepository extends JpaRepository<CorebankTransaction, Long> {

	
	public List<CorebankTransaction> findByTransactionTypeAndKomiTrnsId (String trnsType, String komiTrnsId);
	
}
