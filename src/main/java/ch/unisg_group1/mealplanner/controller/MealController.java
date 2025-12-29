package ch.unisg_group1.mealplanner.controller;

import ch.unisg_group1.mealplanner.model.Meal;
import ch.unisg_group1.mealplanner.service.MealPlannerService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("api/meals")
public class MealController {
    private final MealPlannerService service;

    public MealController(MealPlannerService service) {
        this.service = service;
    }

    @GetMapping
    public List<Meal> getMealsInRange(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return service.findMealsInRange(
                start.atStartOfDay(), end.atTime(23, 59)
        );
    }

    @PostMapping
    public Meal saveMeal(@RequestBody Meal meal) {
        return service.saveMeal(meal);
    }

    @DeleteMapping("/{id}")
    public void deleteMeal(@PathVariable Long id) {
        service.deleteMealById(id);
    }

    @GetMapping("/calories")
    public int getCaloriesForDay(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.calculateCaloriesForDay(date);
    }
}
