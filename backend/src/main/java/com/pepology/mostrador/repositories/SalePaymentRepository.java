package com.pepology.mostrador.repositories;

import com.pepology.mostrador.models.entities.CashSessionEntity;
import com.pepology.mostrador.models.entities.SaleEntity;
import com.pepology.mostrador.models.entities.SalePaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * Acceso a sale_payment. La caja suma el efectivo de acá, no de cash_movement.
 */
public interface SalePaymentRepository extends JpaRepository<SalePaymentEntity, Long> {

	List<SalePaymentEntity> findBySale(SaleEntity sale);

	/** Efectivo cobrado en ventas cerradas de esa planilla. */
	@Query("""
			select coalesce(sum(payment.amount), 0)
			from SalePaymentEntity payment
			where payment.sale.session = :session
				and payment.sale.status = com.pepology.mostrador.models.enums.SaleStatus.CERRADA
				and payment.method = com.pepology.mostrador.models.enums.SalePaymentMethod.EFECTIVO
			""")
	BigDecimal sumCashBySession(@Param("session") CashSessionEntity session);
}
