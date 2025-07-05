package com.Calorizer.Bot.Repository;

import com.Calorizer.Bot.Model.UserPhysicalData;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for {@link UserPhysicalData} entities.
 * Extends {@link JpaRepository} to provide standard CRUD operations
 * and pagination/sorting capabilities for UserPhysicalData objects.
 * The primary key type for UserPhysicalData is Long.
 */
public interface UserPhysicalDataRepository extends JpaRepository<UserPhysicalData,Long> {
}
