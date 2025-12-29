package ch.unisg_group1.mealplanner.persistence;

import ch.unisg_group1.mealplanner.model.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
}