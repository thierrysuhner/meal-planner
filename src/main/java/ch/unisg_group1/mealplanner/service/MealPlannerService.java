package ch.unisg_group1.mealplanner.service;

import ch.unisg_group1.mealplanner.model.*;
import ch.unisg_group1.mealplanner.persistence.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MealPlannerService {
    private final RecipeRepository recipeRepo;
    private final MealRepository mealRepo;
    private final ShoppingListItemRepository shoppingListRepo;
    private final IngredientRepository ingredientRepo;
    private final MealShoppingListRepository mealShoppingListRepo;

    public MealPlannerService(RecipeRepository recipeRepo, MealRepository mealRepo, ShoppingListItemRepository shoppingListRepo,
                              IngredientRepository ingredientRepo, MealShoppingListRepository mealShoppingListRepo) {
        this.recipeRepo = recipeRepo;
        this.mealRepo = mealRepo;
        this.shoppingListRepo = shoppingListRepo;
        this.ingredientRepo = ingredientRepo;
        this.mealShoppingListRepo = mealShoppingListRepo;
    }

    // --- RECIPES ---
    public List<Recipe> getAllRecipes() { return recipeRepo.findAll(); }

    @Transactional
    public Recipe fetchRecipeWithIngredients(Long id) {
        Recipe r = recipeRepo.findById(id).orElseThrow();
        r.getIngredients().size(); // Trigger für das Laden der Liste
        return r;
    }

    public Recipe saveRecipe(Recipe recipe) { return recipeRepo.save(recipe); }

    @Transactional
    public Recipe updateRecipeDetails(Long recipeId, int portions, List<Ingredient> newIngredients) {
        // Load recipes from DB
        Recipe recipeToUpdate = fetchRecipeWithIngredients(recipeId);

        // Sum up calories for ingredients
        long totalCaloriesLong = newIngredients.stream()
                .mapToLong(Ingredient::getCalories) // long statt int
                .sum();

        // Check if calories are overflowing and then cast them to int
        if (totalCaloriesLong > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Calories summed together can't be bigger than integer max value");
        }
        int totalCalories = (int) totalCaloriesLong;

        // Set calories and portions if valid amount
        if (portions > 0) {
            recipeToUpdate.setCalories(totalCalories / portions);
            recipeToUpdate.setPortions(portions);
        } else {
            throw new IllegalArgumentException("Portions must be greater than 0");
        }

        // Update ingredients
        recipeToUpdate.getIngredients().clear();
        for (Ingredient ing : newIngredients) {
            recipeToUpdate.getIngredients().add(ing);
        }

        // Persist recipe
        return recipeRepo.save(recipeToUpdate);
    }

    @Transactional
    public void deleteRecipe(Long recipeId) {
        recipeRepo.deleteById(recipeId);
    }


    // --- MEALS ---
    public List<Meal> findMealsInRange(LocalDateTime start, LocalDateTime end) {
        return mealRepo.findByStartTimeBetween(start, end);
    }

    public Optional<Meal> findMealById(Long id) {
        return mealRepo.findById(id);
    }

    public Meal saveMeal(Meal meal) {
        return mealRepo.save(meal);
    }

    public int calculateCaloriesForDay(LocalDate localDate) {
        // Get meals of day
        List<Meal> meals = findMealsInRange(localDate.atStartOfDay(), localDate.atTime(23, 59, 59));

        // Calculate sum of recipe calories first as long
        long calorieSumLong = meals.stream()
                                .mapToLong(meal -> meal.getRecipes().stream()
                                        .mapToLong(Recipe::getCalories)
                                        .sum())
                                .sum();
        // Check if calories overflow int
        if (calorieSumLong > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Calories summed together can't be bigger than integer max value");
        }

        return (int) calorieSumLong;
    }

    @Transactional
    public void deleteMeal(Meal meal) {
        mealRepo.delete(meal);
    }

    @Transactional
    public void deleteMealById(long id) {
        mealRepo.deleteById(id);
    }


    // --- SHOPPING LISTS ---
    public List<ShoppingListItem> getAllShoppingListItems() {
        List<ShoppingListItem> allItems = shoppingListRepo.findAll();

        // Group items with name+unit, sum up amounts
        Map<String, ShoppingListItem> summary = allItems.stream()
                .collect(Collectors.toMap(
                        item -> item.getName().toLowerCase() + "-" + item.getUnit().toLowerCase(),
                        item -> {
                            // Create copy to not manipulate original objects in DB
                            ShoppingListItem copy = new ShoppingListItem();
                            copy.setName(item.getName());
                            copy.setUnit(item.getUnit());
                            copy.setTotalAmount(item.getTotalAmount());
                            return copy;
                        },
                        (existing, replacement) -> {
                            double newTotal = existing.getTotalAmount() + replacement.getTotalAmount();

                            // Check for double overflow
                            if (Double.isInfinite(newTotal)) {
                                throw new IllegalArgumentException("Total amount overflow for shopping list item: " + existing.getName());
                            }
                            existing.setTotalAmount(existing.getTotalAmount() + replacement.getTotalAmount());
                            return existing;
                        }
                ));

        return new ArrayList<>(summary.values());
    }

    public ShoppingListItem saveShoppingListItem(ShoppingListItem item) {
        return shoppingListRepo.save(item);
    }

    @Transactional
    public void generateShoppingListFromMeals(LocalDate start, LocalDate end) {
        mealShoppingListRepo.deleteAll();

        // Load all meals from given time frame
        List<Meal> meals = mealRepo.findByStartTimeBetween(start.atStartOfDay(), end.atTime(23, 59));

        // Do nothing if no meals found
        if (meals.isEmpty()) {
            return;
        }

        for (Meal meal : meals) {
            // Initialize empty map for each meal
            Map<String, ShoppingListItem> aggregatedItems = new HashMap<>();
            int mealPersons = meal.getPersons();
            // Check if person-amount is valid
            if (mealPersons < 1) { throw new IllegalArgumentException("Person-Amount for meal must be greater than 0."); }

            for (Recipe recipe : meal.getRecipes()) {
                double recipePortions = recipe.getPortions();
                for (Ingredient ing : recipe.getIngredients()) {
                    // Calculate amount adjusted for people eating meal
                    double adjustedAmount = (ing.getAmount() / recipePortions) * mealPersons;

                    // Round up when pieces are used
                    Set<String> pieceUnits = Set.of("pc", "pcs", "pieces", "piece");
                    if (pieceUnits.contains(ing.getUnit().toLowerCase())) {
                        adjustedAmount = Math.ceil(adjustedAmount);
                    }

                    String key = ing.getName().toLowerCase() + "-" + ing.getUnit();
                    // Update total amount or create new item
                    if (aggregatedItems.containsKey(key)) {
                        ShoppingListItem existing = aggregatedItems.get(key);
                        existing.setTotalAmount(existing.getTotalAmount() + adjustedAmount);
                    } else {
                        ShoppingListItem newItem = new ShoppingListItem();
                        newItem.setName(ing.getName());
                        newItem.setTotalAmount(adjustedAmount);
                        newItem.setUnit(ing.getUnit());
                        aggregatedItems.put(key, newItem);
                    }
                }
            }

            // Create new MealShoppingList and add items
            MealShoppingList mealList = new MealShoppingList();
            mealList.setItems(new ArrayList<>(aggregatedItems.values()));

            // Persist, ShoppingListItems automatically persisted because of CascadeType.ALL
            mealShoppingListRepo.save(mealList);
        }
    }

    @Transactional
    public void deleteShoppingListItem(ShoppingListItem item) { shoppingListRepo.delete(item); }

    @Transactional
    public void clearShoppingList() {
        mealShoppingListRepo.deleteAll();
        shoppingListRepo.deleteAll();
    }

    // --- INGREDIENTS ---
    public List<String> getAllAvailableIngredientNames() {
        return ingredientRepo.findAll().stream()
                .map(Ingredient::getName)
                .distinct()
                .sorted()
                .toList();
    }

    public List<Recipe> findRecipesMatchingIngredients(Set<String> myIngredients) {
        if (myIngredients.isEmpty()) return Collections.emptyList();

        // Collect all given ingredients to lower case
        Set<String> searchSet = myIngredients.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        // Get all recipes fresh from DB
        List<Recipe> allRecipes = recipeRepo.findAll();

        // Filter recipes, given ingredients should match at least one of recipe's ingredients
        return allRecipes.stream().filter(recipe ->
                recipe.getIngredients().stream()
                        .anyMatch(ing -> searchSet.contains(ing.getName().toLowerCase()))
        ).toList();
    }
}
