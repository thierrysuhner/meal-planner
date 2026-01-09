package ch.unisg_group1.mealplanner.controller;

import ch.unisg_group1.mealplanner.model.ShoppingListItem;
import ch.unisg_group1.mealplanner.service.MealPlannerService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("api/shoppinglists")
public class ShoppingListController {
    private final MealPlannerService service;

    public ShoppingListController(MealPlannerService service) {
        this.service = service;
    }

    // Trigger the generation for a period
    @PostMapping("/generate")
    public void generate(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        service.generateShoppingListFromMeals(start,end);
    }

    @GetMapping("/items")
    public List<ShoppingListItem> getAllItems() {
        return service.getAllShoppingListItems();
    }

    @PostMapping("/items")
    public ShoppingListItem addItem(@RequestBody ShoppingListItem item) {
        return service.saveShoppingListItem(item);
    }

    @DeleteMapping("/clear")
    public void clearList() {
        service.clearShoppingList();
    }

    @DeleteMapping("/items/{id}")
    public void deleteItem(@PathVariable Long id) { service.deleteShoppingListItemById(id); }

    @PutMapping("/items/{id}")
    public ShoppingListItem updateItem(@PathVariable Long id,
                                       @RequestBody ShoppingListItem updatedItem) {

        updatedItem.setId(id);
        return service.saveShoppingListItem(updatedItem);
    }
}
