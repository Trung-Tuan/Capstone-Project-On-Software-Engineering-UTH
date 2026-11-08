package com.uth.capstone.repository;

import com.uth.capstone.entity.ItemPost;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ItemPostRepository extends JpaRepository<ItemPost, Integer> {

    @EntityGraph(attributePaths = {"user", "category", "location", "images"})
    Optional<ItemPost> findWithDetailsByPostId(Integer postId);
}

