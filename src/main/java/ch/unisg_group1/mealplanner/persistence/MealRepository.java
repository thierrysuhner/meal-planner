package ch.unisg_group1.mealplanner.persistence;

import ch.unisg_group1.mealplanner.model.Meal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.*;

import java.time.LocalDateTime;
import java.util.List;

public interface MealRepository extends JpaRepository<Meal, Long> {
    // Only loads meals, that are in range of the currently displayed calendar
    @Query("SELECT m FROM Meal m WHERE m.startTime >= :start AND m.startTime <= :end")
    List<Meal> findByStartTimeBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
