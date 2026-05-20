package com.tibet.tourism.modules.community.infra;
import com.tibet.tourism.modules.community.domain.SharedRoute;
import com.tibet.tourism.modules.user.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SharedRouteRepository extends JpaRepository<SharedRoute, Long>, JpaSpecificationExecutor<SharedRoute> {
    
    // 获取用户分享的路线（解决 N+1：一次性加载 author）
    @EntityGraph(attributePaths = {"author"})
    List<SharedRoute> findByAuthorOrderByCreatedAtDesc(User author);

    @EntityGraph(attributePaths = {"author"})
    List<SharedRoute> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"author"})
    Page<SharedRoute> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<SharedRoute> findBySourceTypeAndSourceRouteId(SharedRoute.SourceType sourceType, Long sourceRouteId);

    long countBySourceType(SharedRoute.SourceType sourceType);
    
    // 简单的筛选查询（更复杂的筛选将使用Specification）
    Page<SharedRoute> findByDays(Integer days, Pageable pageable);
    Page<SharedRoute> findByBudget(String budget, Pageable pageable);
    Page<SharedRoute> findByPreference(String preference, Pageable pageable);

    // 根据作者删除路线
    void deleteByAuthor(User author);
}
