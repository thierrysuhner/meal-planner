package ch.unisg_group1.mealplanner.persistence;

import ch.unisg_group1.mealplanner.model.MealShoppingList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MealShoppingListRepository extends JpaRepository<MealShoppingList, Long> {
}