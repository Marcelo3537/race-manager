package dev.marcelo.racemanager.result;

import dev.marcelo.racemanager.entry.ChampionshipEntry;
import dev.marcelo.racemanager.race.Race;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "race_result")
public class RaceResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "race_id", nullable = false)
    private Race race;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entry_id", nullable = false)
    private ChampionshipEntry entry;

    @Column
    private Integer position;

    @Column(nullable = false)
    private Integer points;

    @Column(nullable = false)
    private Integer lapsCompleted;

    @Column(nullable = false)
    private Boolean fastestLap;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResultStatus status;

    protected RaceResult() {
        // Requerido por JPA
    }

    public RaceResult(Race race,
                      ChampionshipEntry entry,
                      Integer position,
                      Integer lapsCompleted,
                      Boolean fastestLap,
                      ResultStatus status) {
        this.race = race;
        this.entry = entry;
        this.position = position;
        this.points = 0;
        this.lapsCompleted = lapsCompleted;
        this.fastestLap = fastestLap;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Race getRace() {
        return race;
    }

    public ChampionshipEntry getEntry() {
        return entry;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public Integer getLapsCompleted() {
        return lapsCompleted;
    }

    public void setLapsCompleted(Integer lapsCompleted) {
        this.lapsCompleted = lapsCompleted;
    }

    public Boolean getFastestLap() {
        return fastestLap;
    }

    public void setFastestLap(Boolean fastestLap) {
        this.fastestLap = fastestLap;
    }

    public ResultStatus getStatus() {
        return status;
    }

    public void setStatus(ResultStatus status) {
        this.status = status;
    }
}