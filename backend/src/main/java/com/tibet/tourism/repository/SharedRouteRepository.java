package com.tibet.tourism.repository;

import com.tibet.tourism.entity.SharedRoute;
import com.tibet.tourism.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SharedRouteRepository extends JpaRepository<SharedRoute, Long>, JpaSpecificationExecutor<SharedRoute> {
    
    // 获取用户分享的路线
    List<SharedRoute> findByAuthorOrderByCreatedAtDesc(User author);
    
    // 简单的筛选查询（更复杂的筛选将使用Specification）
    Page<SharedRoute> findByDays(Integer days, Pageable pageable);
    Page<SharedRoute> findByBudget(String budget, Pageable pageable);
    Page<SharedRoute> findByPreference(String preference, Pageable pageable);
}
