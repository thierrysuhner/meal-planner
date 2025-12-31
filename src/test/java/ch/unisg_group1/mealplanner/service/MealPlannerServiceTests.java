package ch.unisg_group1.mealplanner.service;

import ch.unisg_group1.mealplanner.model.*;
import ch.unisg_group1.mealplanner.persistence.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MealPlannerServiceTests {

    @Mock
    private RecipeRepository recipeRepo;
    @Mock
    private MealRepository mealRepo;
    @Mock
    private ShoppingListItemRepository shoppingListRepo;
    @Mock
    private IngredientRepository ingredientRepo;
    @Mock
    private MealShoppingListRepository mealShoppingListRepo;

    @InjectMocks
    private MealPlannerService service;

    // RECIPE TESTS

    @Test
    void fetchRecipeWithIngredients_ShouldLoadRecipeAndTriggerLazyList() {
        // Arrange
        Long id = 1L;
        Recipe mockRecipe = new Recipe();
        mockRecipe.setId(id);
        mockRecipe.setIngredients(new ArrayList<>());

        when(recipeRepo.findById(id)).thenReturn(Optional.of(mockRecipe));

        // Act
        Recipe result = service.fetchRecipeWithIngredients(id);

        // Assert
        assertThat(result.getId()).isEqualTo(id);
        verify(recipeRepo).findById(id);
    }

    @Test
    void updateRecipeDetails_ShouldCalculateCaloriesCorrectly() {
        // Arrange
        Long recipeId = 1L;
        int newPortions = 2;

        Recipe existingRecipe = new Recipe();
        existingRecipe.setId(recipeId);
        existingRecipe.setIngredients(new ArrayList<>());

        Ingredient ing1 = new Ingredient(); ing1.setCalories(200);
        Ingredient ing2 = new Ingredient(); ing2.setCalories(100);
        List<Ingredient> newIngredients = List.of(ing1, ing2); // Total 300 kcal

        when(recipeRepo.findById(recipeId)).thenReturn(Optional.of(existingRecipe));
        when(recipeRepo.save(any(Recipe.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Recipe updated = service.updateRecipeDetails(recipeId, newPortions, newIngredients);

        // Assert
        // 300 kcal Total / 2 Portionen = 150 kcal pro Portion
        assertThat(updated.getCalories()).isEqualTo(150);
    }

    @Test
    void updateRecipeDetails_ShouldUpdateIngredientRelation() {
        // Arrange
        Recipe existing = new Recipe();
        existing.setIngredients(new ArrayList<>());

        Ingredient newIng = new Ingredient();

        when(recipeRepo.findById(any())).thenReturn(Optional.of(existing));
        when(recipeRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        Recipe result = service.updateRecipeDetails(1L, 1, List.of(newIng));

        // Assert
        assertThat(result.getIngredients()).contains(newIng);
    }

    // MEAL / CALORIES TESTS

    @Test
    void calculateCaloriesForDay_ShouldSumUpAllMeals() {
        // Arrange
        Recipe r1 = new Recipe(); r1.setCalories(500);
        Recipe r2 = new Recipe(); r2.setCalories(300);

        Meal meal1 = new Meal(); meal1.setRecipes(List.of(r1));
        Meal meal2 = new Meal(); meal2.setRecipes(List.of(r2));

        when(mealRepo.findByStartTimeBetween(any(), any())).thenReturn(List.of(meal1, meal2));

        // Act
        int totalCals = service.calculateCaloriesForDay(LocalDate.now());

        // Assert
        assertThat(totalCals).isEqualTo(800);
    }

    @Test
    void calculateCaloriesForDay_ShouldReturnZeroIfNoMeals() {
        // Arrange
        when(mealRepo.findByStartTimeBetween(any(), any())).thenReturn(Collections.emptyList());

        // Act
        int totalCals = service.calculateCaloriesForDay(LocalDate.now());

        // Assert
        assertThat(totalCals).isZero();
    }

    // SHOPPING LIST TESTS

    @Test
    void getAllShoppingListItems_ShouldAggregateDuplicateItems() {
        // Arrange
        ShoppingListItem i1 = new ShoppingListItem(); i1.setName("Milk"); i1.setUnit("L"); i1.setTotalAmount(1.0);
        ShoppingListItem i2 = new ShoppingListItem(); i2.setName("Milk"); i2.setUnit("L"); i2.setTotalAmount(2.0);

        when(shoppingListRepo.findAll()).thenReturn(List.of(i1, i2));

        // Act
        List<ShoppingListItem> result = service.getAllShoppingListItems();

        // Assert
        assertThat(result.getFirst().getTotalAmount()).isEqualTo(3.0);
    }

    @Test
    void generateShoppingList_ShouldCalculateAmountBasedOnPersons() {
        // Arrange
        // Rezept: 400g Mehl für 4 Personen
        Ingredient flour = new Ingredient(); flour.setName("Flour"); flour.setUnit("g"); flour.setAmount(400);
        Recipe recipe = new Recipe(); recipe.setPortions(4); recipe.setIngredients(List.of(flour));

        // Meal: Wir kochen für 2 Personen
        Meal meal = new Meal(); meal.setPersons(2); meal.setRecipes(List.of(recipe));

        when(mealRepo.findByStartTimeBetween(any(), any())).thenReturn(List.of(meal));
        ArgumentCaptor<MealShoppingList> captor = ArgumentCaptor.forClass(MealShoppingList.class);

        // Act
        service.generateShoppingListFromMeals(LocalDate.now(), LocalDate.now());

        // Assert
        verify(mealShoppingListRepo).save(captor.capture());
        ShoppingListItem savedItem = captor.getValue().getItems().get(0);

        // (400g / 4) * 2 = 200g
        assertThat(savedItem.getTotalAmount()).isEqualTo(200.0);
    }

    @Test
    void generateShoppingList_ShouldRoundUpPieceUnits() {
        // Arrange
        Ingredient egg = new Ingredient(); egg.setName("Egg"); egg.setUnit("pcs"); egg.setAmount(1);
        Recipe recipe = new Recipe(); recipe.setPortions(2); recipe.setIngredients(List.of(egg));

        // Meal: Kochen für 3 Personen -> 1.5 Eier rechnerisch
        Meal meal = new Meal(); meal.setPersons(3); meal.setRecipes(List.of(recipe));

        when(mealRepo.findByStartTimeBetween(any(), any())).thenReturn(List.of(meal));
        ArgumentCaptor<MealShoppingList> captor = ArgumentCaptor.forClass(MealShoppingList.class);

        // Act
        service.generateShoppingListFromMeals(LocalDate.now(), LocalDate.now());

        // Assert
        verify(mealShoppingListRepo).save(captor.capture());

        ShoppingListItem item = captor.getValue().getItems().get(0);
        // Math.ceil(1.5) -> 2.0
        assertThat(item.getTotalAmount()).isEqualTo(2.0);
    }

    // SEARCH TESTS

    @Test
    void findRecipesMatchingIngredients_ShouldFindMatchingRecipe() {
        // Arrange
        Ingredient cheese = new Ingredient(); cheese.setName("Cheese");
        Recipe pizza = new Recipe(); pizza.setIngredients(List.of(cheese));

        when(recipeRepo.findAll()).thenReturn(List.of(pizza));

        // Act
        List<Recipe> result = service.findRecipesMatchingIngredients(Set.of("cheese"));

        // Assert
        assertThat(result).contains(pizza);
    }

    @Test
    void findRecipesMatchingIngredients_ShouldReturnEmptyIfNoMatch() {
        // Arrange
        Ingredient tomato = new Ingredient(); tomato.setName("Tomato");
        Recipe salad = new Recipe(); salad.setIngredients(List.of(tomato));

        when(recipeRepo.findAll()).thenReturn(List.of(salad));

        // Act
        List<Recipe> result = service.findRecipesMatchingIngredients(Set.of("Meat"));

        // Assert
        assertThat(result).isEmpty();
    }
}