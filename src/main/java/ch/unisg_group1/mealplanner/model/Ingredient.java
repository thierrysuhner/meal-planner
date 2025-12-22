package ch.unisg_group1.mealplanner.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor
public class Ingredient {
    @Id
    @GeneratedValue
    private long id;

    private String name;
    private double amount;
    private String unit;

    @ManyToOne
    private Recipe recipe;
}
