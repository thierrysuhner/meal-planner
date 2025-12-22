package ch.unisg_group1.mealplanner.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor
public class Meal {
    @Id
    @GeneratedValue
    private long id;

    private LocalDate date;
    private String mealType; // Lunch, Dinner, Breakfast, etc.
    private int persons;

    @ManyToOne
    private Recipe recipe;

    @OneToOne(cascade = CascadeType.ALL)
    private MealShoppingList mealShoppingList;
}
