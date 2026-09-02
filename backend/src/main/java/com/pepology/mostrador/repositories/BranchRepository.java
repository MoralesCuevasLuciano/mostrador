package com.pepology.mostrador.repositories;

import com.pepology.mostrador.models.entities.BranchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<BranchEntity, Long> {
}
