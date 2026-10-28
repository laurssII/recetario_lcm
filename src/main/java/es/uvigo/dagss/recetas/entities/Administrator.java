package es.uvigo.dagss.recetas.entities;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue(value = "ADMINISTRATOR")
public class Administrator extends User {

    public Administrator() {
        super(UserType.ADMINISTRATOR);
    }

}
