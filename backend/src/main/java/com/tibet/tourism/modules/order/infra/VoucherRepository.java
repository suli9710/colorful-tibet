package com.tibet.tourism.modules.order.infra;
import com.tibet.tourism.modules.order.domain.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {
}
