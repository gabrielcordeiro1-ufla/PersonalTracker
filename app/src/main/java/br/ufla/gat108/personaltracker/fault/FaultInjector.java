package br.ufla.gat108.personaltracker.fault;

import br.ufla.gat108.personaltracker.domain.Fix;
import br.ufla.gat108.personaltracker.domain.GeoMath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Injeção de falhas determinística: a mesma semente e a mesma especificação sobre o mesmo
 * registro dão sempre o mesmo resultado, em qualquer máquina.
 *
 * Ordem: (1) perda, (2) ruído, (3) saltos. Cada tipo de falha usa a sua própria sequência
 * pseudoaleatória derivada da semente, então ligar ou desligar um tipo não muda o que os
 * outros sorteiam. Os índices dos saltos valem para a lista JÁ DEGRADADA (depois da perda).
 */
public final class FaultInjector {

    /** Margem em fixes, nas duas pontas, onde não se põe salto. */
    static final int JUMP_MARGIN = 3;
    /** Distância mínima, em fixes, entre dois saltos. */
    static final int JUMP_SPACING = 5;

    private final long seed;

    public FaultInjector(long seed) {
        this.seed = seed;
    }

    public long getSeed() { return seed; }

    /** Resultado da injeção. As listas são cópias. */
    public static final class Result {
        private final List<Fix> fixes;
        private final List<Integer> jumpIndices;
        private final int dropped;

        Result(List<Fix> fixes, List<Integer> jumpIndices, int dropped) {
            this.fixes = fixes;
            this.jumpIndices = jumpIndices;
            this.dropped = dropped;
        }

        public List<Fix> getFixes() { return new ArrayList<>(fixes); }

        /** Índices (em getFixes) dos fixes que receberam salto, em ordem crescente. */
        public List<Integer> getJumpIndices() { return new ArrayList<>(jumpIndices); }

        public int getDropped() { return dropped; }
    }

    public Result inject(List<Fix> clean, FaultSpec spec) {
        if (clean == null || spec == null) {
            throw new IllegalArgumentException("Registro e especificação não podem ser nulos.");
        }
        Random lossRandom = new Random(seed * 31 + 1);
        Random noiseRandom = new Random(seed * 31 + 2);
        Random jumpRandom = new Random(seed * 31 + 3);

        // 1) perda: o primeiro e o último fix ficam, para a janela manter o mesmo intervalo de tempo
        List<Fix> kept = new ArrayList<>();
        int dropped = 0;
        for (int i = 0; i < clean.size(); i++) {
            boolean edge = i == 0 || i == clean.size() - 1;
            boolean drop = spec.getLossProbability() > 0.0
                    && lossRandom.nextDouble() < spec.getLossProbability() && !edge;
            if (drop) {
                dropped++;
            } else {
                kept.add(clean.get(i));
            }
        }

        // 2) ruído extra de posição e de velocidade
        List<Fix> degraded = new ArrayList<>(kept.size());
        for (Fix f : kept) {
            if (spec.getPositionNoiseMeters() > 0.0 || spec.getSpeedNoiseMps() > 0.0) {
                double dEast = noiseRandom.nextGaussian() * spec.getPositionNoiseMeters();
                double dNorth = noiseRandom.nextGaussian() * spec.getPositionNoiseMeters();
                double dSpeed = noiseRandom.nextGaussian() * spec.getSpeedNoiseMps();
                f = moved(f, dEast, dNorth, Math.max(0.0, f.getVelocity() + dSpeed));
            }
            degraded.add(f);
        }

        // 3) saltos de posição em fixes sorteados, com margem e espaçamento
        List<Integer> jumps = chooseJumpIndices(degraded.size(), spec.getJumpCount(), jumpRandom);
        for (int idx : jumps) {
            double angle = jumpRandom.nextDouble() * 2.0 * Math.PI;
            Fix f = degraded.get(idx);
            degraded.set(idx, moved(f, spec.getJumpMeters() * Math.sin(angle),
                    spec.getJumpMeters() * Math.cos(angle), f.getVelocity()));
        }
        return new Result(degraded, jumps, dropped);
    }

    private static List<Integer> chooseJumpIndices(int size, int count, Random random) {
        List<Integer> chosen = new ArrayList<>();
        if (count == 0) {
            return chosen;
        }
        int first = JUMP_MARGIN;
        int last = size - 1 - JUMP_MARGIN;
        if (last < first || (last - first) / JUMP_SPACING + 1 < count) {
            throw new IllegalArgumentException("Registro curto demais para " + count + " saltos.");
        }
        List<Integer> candidates = new ArrayList<>();
        for (int i = first; i <= last; i++) {
            candidates.add(i);
        }
        Collections.shuffle(candidates, random);
        for (int candidate : candidates) {
            boolean farEnough = true;
            for (int c : chosen) {
                if (Math.abs(c - candidate) < JUMP_SPACING) {
                    farEnough = false;
                    break;
                }
            }
            if (farEnough) {
                chosen.add(candidate);
                if (chosen.size() == count) {
                    break;
                }
            }
        }
        if (chosen.size() < count) {
            throw new IllegalArgumentException("Não coube o número pedido de saltos no registro.");
        }
        Collections.sort(chosen);
        return chosen;
    }

    private static Fix moved(Fix f, double dEast, double dNorth, double speed) {
        double lat = GeoMath.latitudeFromNorth(f.getLatitude(), dNorth);
        double lon = GeoMath.longitudeFromEast(f.getLatitude(), f.getLongitude(), dEast);
        return new Fix(f.getTimestamp(), lat, lon, (float) speed,
                f.getAccuracy(), f.getSpeedAccuracy(), f.getBearing());
    }
}
