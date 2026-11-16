package com.uth.capstone.repository;

import com.uth.capstone.entity.ItemPost;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ItemPostRepository extends JpaRepository<ItemPost, Integer>, ItemPostRepositoryCustom {

    @EntityGraph(attributePaths = {"user", "category", "location", "images"})
    Optional<ItemPost> findWithDetailsByPostId(Integer postId);

    @Query("""
            SELECT DISTINCT p
            FROM ItemPost p
            LEFT JOIN FETCH p.user
            LEFT JOIN FETCH p.category
            LEFT JOIN FETCH p.location
            LEFT JOIN FETCH p.images
            WHERE p.postId IN :postIds
            """)
    List<ItemPost> findAllWithDetailsByPostIdIn(@Param("postIds") Collection<Integer> postIds);
}
