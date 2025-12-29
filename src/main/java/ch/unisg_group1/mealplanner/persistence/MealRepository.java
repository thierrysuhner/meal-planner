package ch.unisg_group1.mealplanner.persistence;

import ch.unisg_group1.mealplanner.model.Meal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface MealRepository extends JpaRepository<Meal, Long> {
    // Lädt nur die Meals, die im aktuell angezeigten Kalenderzeitraum liegen
    @Query("SELECT m FROM Meal m WHERE m.startTime >= :start AND m.startTime <= :end")
    List<Meal> findByStartTimeBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
