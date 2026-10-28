package es.uvigo.dagss.recetas.entities;

import jakarta.persistence.*;

/**
 * unitsPerPackage is the number of doses in each box; together with the daily dose of a prescription
 * it determines the refill plan.
 */
@Entity
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String brandName;
    private String activeIngredient;
    private String manufacturer;
    private String category;

    private int unitsPerPackage;

    private boolean active;

    public Medication() {
        this.active = true;
    }

    public Medication(String brandName, String activeIngredient, String manufacturer, String category, int unitsPerPackage, boolean active) {
        this.brandName = brandName;
        this.activeIngredient = activeIngredient;
        this.manufacturer = manufacturer;
        this.category = category;
        this.unitsPerPackage = unitsPerPackage;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getActiveIngredient() {
        return activeIngredient;
    }

    public void setActiveIngredient(String activeIngredient) {
        this.activeIngredient = activeIngredient;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getUnitsPerPackage() {
        return unitsPerPackage;
    }

    public void setUnitsPerPackage(int unitsPerPackage) {
        this.unitsPerPackage = unitsPerPackage;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
