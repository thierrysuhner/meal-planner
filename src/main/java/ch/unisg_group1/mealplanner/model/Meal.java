package ch.unisg_group1.mealplanner.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter @NoArgsConstructor
public class Meal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String title;
    private int persons;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "meal_recipes",
            joinColumns = @JoinColumn(name = "meal_id"),
            inverseJoinColumns = @JoinColumn(name = "recipe_id")
    )
    private List<Recipe> recipes = new ArrayList<>(); // Initialisiere die Liste

    @OneToOne(cascade = CascadeType.ALL)
    private MealShoppingList mealShoppingList;
}
