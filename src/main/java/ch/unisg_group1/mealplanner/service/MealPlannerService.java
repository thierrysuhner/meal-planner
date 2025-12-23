package ch.unisg_group1.mealplanner.service;

import ch.unisg_group1.mealplanner.model.*;
import ch.unisg_group1.mealplanner.persistence.MealRepository;
import ch.unisg_group1.mealplanner.persistence.RecipeRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class MealPlannerService {
    private final RecipeRepository recipeRepo;
    private final MealRepository mealRepo;

    public MealPlannerService(RecipeRepository recipeRepo, MealRepository mealRepo) {
        this.recipeRepo = recipeRepo;
        this.mealRepo = mealRepo;
    }

    // Recipe CRUD
    public List<Recipe> getAllRecipes() {
        return recipeRepo.findAll();
    }

    public Recipe saveRecipe(Recipe recipe) {
        return recipeRepo.save(recipe);
    }

    public void deleteRecipe(Long recipeId) {
        recipeRepo.deleteById(recipeId);
    }

    // Calculate calories per day
    public int calculateCaloriesForDay(LocalDate date) {
        return mealRepo.findByDate(date).stream()
                .mapToInt(e -> e.getRecipe().getCalories() * e.getPersons()).sum();
    }

    // Generate shopping list for a meal
    public MealShoppingList generateMealShoppingList(Meal meal) {
        Map<String, ShoppingListItem> aggregated = new HashMap<>();

        for (Ingredient i : meal.getRecipe().getIngredients()) {
            double scaledAmount = i.getAmount() * meal.getPersons() / meal.getRecipe().getPortions();

            aggregated.computeIfAbsent(i.getName(), name -> {
                ShoppingListItem item = new ShoppingListItem();
                item.setName(name);
                item.setUnit(i.getUnit());
                item.setTotalAmount(0);
                return item;
            }).setTotalAmount(aggregated.get(i.getName()).getTotalAmount() + scaledAmount);
        }

        MealShoppingList list = new MealShoppingList();
        list.setItems(new ArrayList<>(aggregated.values()));
        return list;
    }

    // Suggest recipes based on available ingredients
    public List<Recipe> suggestRecipes(Set<String> availableIngredients) {
        return recipeRepo.findAll().stream().filter(r -> r.getIngredients().stream()
                .allMatch(i -> availableIngredients.contains(i.getName()))).toList();
    }

    @Transactional
    public Recipe fetchRecipeWithIngredients(Long id) {
        Recipe r = recipeRepo.findById(id).orElseThrow();
        r.getIngredients().size(); // Trigger für das Laden der Liste
        return r;
    }
}
