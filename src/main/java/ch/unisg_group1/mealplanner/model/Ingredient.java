package ch.unisg_group1.mealplanner.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor
public class Ingredient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String name;
    private double amount;
    private String unit;
    private int calories;

    @ManyToOne
    private Recipe recipe;
}
