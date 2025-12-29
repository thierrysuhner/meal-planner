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

    public MealPlannerService(RecipeRepository recipeRepo, MealRepository mealRepo, ShoppingListItemRepository shoppingListRepo, IngredientRepository ingredientRepo, MealShoppingListRepository mealShoppingListRepo) {
        this.recipeRepo = recipeRepo;
        this.mealRepo = mealRepo;
        this.shoppingListRepo = shoppingListRepo;
        this.ingredientRepo = ingredientRepo;
        this.mealShoppingListRepo = mealShoppingListRepo;
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

    @Transactional
    public Recipe fetchRecipeWithIngredients(Long id) {
        Recipe r = recipeRepo.findById(id).orElseThrow();
        r.getIngredients().size(); // Trigger für das Laden der Liste
        return r;
    }

    public List<Meal> findMealsInRange(LocalDateTime start, LocalDateTime end) {
        return mealRepo.findByStartTimeBetween(start, end);
    }

    public int calculateCaloriesForDay(LocalDate localDate) {
        // 1. Alle Meals für den spezifischen Tag holen
        List<Meal> meals = findMealsInRange(localDate.atStartOfDay(), localDate.atTime(23, 59, 59));

        // 2. Summe berechnen: (Summe Kalorien der Rezepte) * Personen pro Meal
        return meals.stream()
                .mapToInt(meal -> meal.getRecipes().stream()
                        .mapToInt(Recipe::getCalories)
                        .sum())
                .sum();
    }

    public Meal saveMeal(Meal meal) {
        return mealRepo.save(meal);
    }

    public void deleteMeal(Meal meal) {
        mealRepo.delete(meal);
    }

    public void deleteMealById(long id) {
        mealRepo.deleteById(id);
    }

    public Optional<Meal> findById(Long id) {
        return mealRepo.findById(id);
    }

    @Transactional
    public void generateShoppingListFromMeals(LocalDate start, LocalDate end) {
        mealShoppingListRepo.deleteAll();

        // 1. Alle Meals im Zeitraum laden
        List<Meal> meals = mealRepo.findByStartTimeBetween(start.atStartOfDay(), end.atTime(23, 59));

        if (meals.isEmpty()) {
            return; // Nichts zu tun
        }

        // Map zum Aggregieren der Zutaten für DIESEN spezifischen Lauf
        Map<String, ShoppingListItem> aggregatedItems = new HashMap<>();

        for (Meal meal : meals) {
            int mealPersons = meal.getPersons();

            for (Recipe recipe : meal.getRecipes()) {
                double recipePortions = recipe.getPortions();
                for (Ingredient ing : recipe.getIngredients()) {
                    // Menge berechnen
                    double adjustedAmount = (ing.getAmount() / recipePortions) * mealPersons;

                    // Aufrunden bei Stückzahlen
                    if ("pcs".equalsIgnoreCase(ing.getUnit())) {
                        adjustedAmount = Math.ceil(adjustedAmount);
                    }

                    String key = ing.getName().toLowerCase() + "-" + ing.getUnit();

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
        }

        // 2. Neue MealShoppingList erstellen und mit den Items verknüpfen
        MealShoppingList mealList = new MealShoppingList();
        // Wir wandeln die Map-Werte in eine ArrayList um
        mealList.setItems(new ArrayList<>(aggregatedItems.values()));

        // 3. Speichern
        // Da CascadeType.ALL gesetzt ist, werden die ShoppingListItems automatisch mitgespeichert
        mealShoppingListRepo.save(mealList);
    }

    public List<ShoppingListItem> getAllShoppingListItems() {
        List<ShoppingListItem> allItems = shoppingListRepo.findAll();

        // Aggregation per Stream: Gruppieren nach Name+Einheit und Mengen summieren
        Map<String, ShoppingListItem> summary = allItems.stream()
                .collect(Collectors.toMap(
                        item -> item.getName().toLowerCase() + "-" + item.getUnit().toLowerCase(),
                        item -> {
                            // Kopie erstellen, um die Original-Objekte in der DB nicht zu verändern
                            ShoppingListItem copy = new ShoppingListItem();
                            copy.setName(item.getName());
                            copy.setUnit(item.getUnit());
                            copy.setTotalAmount(item.getTotalAmount());
                            return copy;
                        },
                        (existing, replacement) -> {
                            existing.setTotalAmount(existing.getTotalAmount() + replacement.getTotalAmount());
                            return existing;
                        }
                ));

        return new ArrayList<>(summary.values());
    }

    public void deleteShoppingListItem(ShoppingListItem item) {
        shoppingListRepo.delete(item);
    }

    public ShoppingListItem saveShoppingListItem(ShoppingListItem item) {
        return shoppingListRepo.save(item);
    }

    @Transactional
    public void clearShoppingList() {
        mealShoppingListRepo.deleteAll();

        // Safety
        shoppingListRepo.deleteAll();
    }

    public List<String> getAllAvailableIngredientNames() {
        return ingredientRepo.findAll().stream()
                .map(Ingredient::getName)
                .distinct()
                .sorted()
                .toList();
    }

    public List<Recipe> findRecipesMatchingIngredients(Set<String> myIngredients) {
        if (myIngredients.isEmpty()) return Collections.emptyList();

        // 1. Alle Suchbegriffe in Kleinschreibung umwandeln für den Vergleich
        Set<String> searchSet = myIngredients.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        List<Recipe> allRecipes = recipeRepo.findAll();

        // 2. Filtern
        return allRecipes.stream().filter(recipe ->
                recipe.getIngredients().stream()
                        .anyMatch(ing -> searchSet.contains(ing.getName().toLowerCase()))
        ).toList();
    }
}
