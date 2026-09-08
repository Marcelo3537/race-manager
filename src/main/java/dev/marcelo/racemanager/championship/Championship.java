package dev.marcelo.racemanager.championship;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "championship")
public class Championship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private Integer season;

    @Column(length = 80)
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChampionshipStatus status;

    protected Championship() {
        // Requerido por JPA
    }

    public Championship(String name, Integer season, String country, ChampionshipStatus status) {
        this.name = name;
        this.season = season;
        this.country = country;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSeason() {
        return season;
    }

    public void setSeason(Integer season) {
        this.season = season;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public ChampionshipStatus getStatus() {
        return status;
    }

    public void setStatus(ChampionshipStatus status) {
        this.status = status;
    }
}