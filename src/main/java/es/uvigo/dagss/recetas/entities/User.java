package es.uvigo.dagss.recetas.entities;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.TableGenerator;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@DiscriminatorColumn(name = "USER_TYPE",
                     discriminatorType = DiscriminatorType.STRING,
                     length = 20)
public abstract class User implements Serializable {

    @Id
    @TableGenerator(name = "USER_GEN", table = "USER_GEN", pkColumnName = "GEN_NAME", valueColumnName = "GEN_VAL", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "USER_GEN")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "USER_TYPE", length = 20)
    protected UserType type;

    @Column(unique = true)
    private String username;
    private String password;

    private LocalDateTime registrationDate;

    private LocalDateTime lastAccess;

    private Boolean active = true;

    private String name; // for a pharmacy, this holds the business/establishment name

    private String email;

    public User() {
        this.registrationDate = LocalDateTime.now();
        this.lastAccess = LocalDateTime.now();
        this.active = true;
    }

    public User(UserType type) {
        this();
        this.type = type;
    }

    public User(UserType type, String username, String password, String name, String email) {
        this();
        this.type = type;
        this.username = username;
        this.password = password;
        this.name = name;
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }

    public LocalDateTime getLastAccess() {
        return lastAccess;
    }

    public void setLastAccess(LocalDateTime lastAccess) {
        this.lastAccess = lastAccess;
    }

    public UserType getType() {
        return type;
    }

    public void setType(UserType type) {
        this.type = type;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    @Override
    public int hashCode() {
        if (this.id != null)
            return Objects.hash(id);
        return Objects.hash(registrationDate, username);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        User other = (User) obj;
        if (this.id != null)
            return this.id.equals(other.getId());
        return Objects.equals(registrationDate, other.registrationDate)
                && Objects.equals(username, other.username);
    }

}
