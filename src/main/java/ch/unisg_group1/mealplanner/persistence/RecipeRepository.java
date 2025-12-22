package ch.unisg_group1.mealplanner.persistence;

import ch.unisg_group1.mealplanner.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
}
