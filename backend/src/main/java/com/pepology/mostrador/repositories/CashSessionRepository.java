package com.pepology.mostrador.repositories;

import com.pepology.mostrador.models.entities.BranchEntity;
import com.pepology.mostrador.models.entities.CashSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a cash_session. Una planilla por sucursal y fecha.
 */
public interface CashSessionRepository extends JpaRepository<CashSessionEntity, Long> {

	/** La planilla de ese local en esa fecha, o vacío si todavía no se abrió. */
	Optional<CashSessionEntity> findByBranchAndBusinessDate(BranchEntity branch, LocalDate businessDate);

	/** Planillas de un local en un rango de fechas (inclusive), de la más reciente a la más vieja. */
	List<CashSessionEntity> findByBranchAndBusinessDateBetweenOrderByBusinessDateDesc(
			BranchEntity branch,
			LocalDate from,
			LocalDate to);

	/** La planilla anterior a esa fecha, para heredar el cierre como apertura. */
	Optional<CashSessionEntity> findFirstByBranchAndBusinessDateLessThanOrderByBusinessDateDesc(
			BranchEntity branch,
			LocalDate businessDate);

	/** Sesiones sin cerrar de un local (closing_amount vacío). */
	List<CashSessionEntity> findByBranchAndClosingAmountIsNullOrderByBusinessDateDesc(BranchEntity branch);
}
