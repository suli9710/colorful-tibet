package com.tibet.tourism.modules.order.infra;
import com.tibet.tourism.modules.order.domain.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
}
