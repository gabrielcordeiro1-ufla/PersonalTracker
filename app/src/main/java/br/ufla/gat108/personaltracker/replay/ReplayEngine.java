package br.ufla.gat108.personaltracker.replay;

import br.ufla.gat108.personaltracker.domain.Fix;
import br.ufla.gat108.personaltracker.fault.FaultInjector;
import br.ufla.gat108.personaltracker.fault.FaultSpec;
import br.ufla.gat108.personaltracker.reconciliation.KinematicReconciler;

import java.util.ArrayList;
import java.util.List;

/**
 * Ensaio reprodutível: pega um registro gravado, injeta falhas com semente fixa, corta em
 * janelas disjuntas e reconcilia cada uma com o detector de erro grosseiro, comparando o
 * que foi injetado com o que foi detectado. Sem relógio e sem Android: o resultado só depende
 * do registro, da especificação e da semente.
 */
public final class ReplayEngine {

    private final KinematicReconciler reconciler;

    public ReplayEngine() {
        this(new KinematicReconciler());
    }

    public ReplayEngine(KinematicReconciler reconciler) {
        this.reconciler = reconciler;
    }

    /** Resultado de uma janela. */
    public static final class WindowResult {
        public final int index;
        public final int startIndex;            // posição do primeiro fix da janela no registro degradado
        public final double residualBefore;
        public final double residualAfter;
        public final double normalizedChiSquare;
        public final List<Integer> removed;     // índices globais (no registro degradado)

        WindowResult(int index, int startIndex, double residualBefore, double residualAfter,
                     double normalizedChiSquare, List<Integer> removed) {
            this.index = index;
            this.startIndex = startIndex;
            this.residualBefore = residualBefore;
            this.residualAfter = residualAfter;
            this.normalizedChiSquare = normalizedChiSquare;
            this.removed = removed;
        }
    }

    /** Resultado do ensaio inteiro. */
    public static final class Report {
        public final long seed;
        public final int inputFixes;
        public final int degradedFixes;
        public final int dropped;
        public final int windowSize;
        public final List<WindowResult> windows;
        public final List<Integer> injected;    // saltos dentro das janelas avaliadas
        public final List<Integer> detected;    // injetados que foram removidos
        public final List<Integer> missed;      // injetados que NÃO foram removidos
        public final List<Integer> falseAlarms; // removidos que não eram injetados

        Report(long seed, int inputFixes, int degradedFixes, int dropped, int windowSize,
               List<WindowResult> windows, List<Integer> injected, List<Integer> detected,
               List<Integer> missed, List<Integer> falseAlarms) {
            this.seed = seed;
            this.inputFixes = inputFixes;
            this.degradedFixes = degradedFixes;
            this.dropped = dropped;
            this.windowSize = windowSize;
            this.windows = windows;
            this.injected = injected;
            this.detected = detected;
            this.missed = missed;
            this.falseAlarms = falseAlarms;
        }
    }

    public Report run(List<Fix> recorded, FaultSpec spec, long seed, int windowSize) {
        if (windowSize < 4) {
            throw new IllegalArgumentException("A janela precisa de pelo menos 4 fixes.");
        }
        FaultInjector.Result injection = new FaultInjector(seed).inject(recorded, spec);
        List<Fix> degraded = injection.getFixes();
        int windowCount = degraded.size() / windowSize;
        int evaluated = windowCount * windowSize;

        List<Integer> injected = new ArrayList<>();
        for (int idx : injection.getJumpIndices()) {
            if (idx < evaluated) {
                injected.add(idx);
            }
        }

        List<WindowResult> windows = new ArrayList<>();
        List<Integer> allRemoved = new ArrayList<>();
        for (int w = 0; w < windowCount; w++) {
            int start = w * windowSize;
            List<Fix> window = degraded.subList(start, start + windowSize);
            KinematicReconciler.Result r = reconciler.reconcile(new ArrayList<>(window), true);
            List<Integer> removedGlobal = new ArrayList<>();
            for (int local : r.getRemovedIndices()) {
                removedGlobal.add(start + local);
            }
            allRemoved.addAll(removedGlobal);
            windows.add(new WindowResult(w + 1, start, r.getResidualRmsBefore(), r.getResidualRmsAfter(),
                    r.getNormalizedChiSquare(), removedGlobal));
        }

        List<Integer> detected = new ArrayList<>();
        List<Integer> missed = new ArrayList<>();
        for (int idx : injected) {
            (allRemoved.contains(idx) ? detected : missed).add(idx);
        }
        List<Integer> falseAlarms = new ArrayList<>();
        for (int idx : allRemoved) {
            if (!injected.contains(idx)) {
                falseAlarms.add(idx);
            }
        }
        return new Report(seed, recorded.size(), degraded.size(), injection.getDropped(), windowSize,
                windows, injected, detected, missed, falseAlarms);
    }
}
