package dev.marcelo.racemanager.race;

import dev.marcelo.racemanager.championship.Championship;
import dev.marcelo.racemanager.circuit.Circuit;
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

import java.time.OffsetDateTime;

@Entity
@Table(name = "race")
public class Race {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "championship_id", nullable = false)
    private Championship championship;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "circuit_id", nullable = false)
    private Circuit circuit;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private Integer round;

    @Column(nullable = false)
    private OffsetDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RaceStatus status;

    protected Race() {
        // Requerido por JPA
    }

    public Race(Championship championship,
                Circuit circuit,
                String name,
                Integer round,
                OffsetDateTime scheduledAt,
                RaceStatus status) {
        this.championship = championship;
        this.circuit = circuit;
        this.name = name;
        this.round = round;
        this.scheduledAt = scheduledAt;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Championship getChampionship() {
        return championship;
    }

    public void setChampionship(Championship championship) {
        this.championship = championship;
    }

    public Circuit getCircuit() {
        return circuit;
    }

    public void setCircuit(Circuit circuit) {
        this.circuit = circuit;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getRound() {
        return round;
    }

    public void setRound(Integer round) {
        this.round = round;
    }

    public OffsetDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(OffsetDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public RaceStatus getStatus() {
        return status;
    }

    public void setStatus(RaceStatus status) {
        this.status = status;
    }
}