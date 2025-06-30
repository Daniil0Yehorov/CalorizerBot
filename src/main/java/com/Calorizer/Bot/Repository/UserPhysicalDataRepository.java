package com.Calorizer.Bot.Repository;

import com.Calorizer.Bot.Model.UserPhysicalData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPhysicalDataRepository extends JpaRepository<UserPhysicalData,Long> {
}
