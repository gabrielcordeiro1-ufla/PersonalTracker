package br.ufla.gat108.personaltracker.fault;

/**
 * Descreve a degradação a injetar num registro: ruído, perda e salto. Imutável.
 * Use FaultSpec.none() e os métodos with... para montar.
 */
public final class FaultSpec {

    private final double positionNoiseMeters;   // desvio-padrão extra, por eixo
    private final double speedNoiseMps;         // desvio-padrão extra da velocidade
    private final double lossProbability;       // chance de descartar cada fix, em [0, 1)
    private final int jumpCount;                // quantos saltos de posição
    private final double jumpMeters;            // tamanho de cada salto

    private FaultSpec(double positionNoiseMeters, double speedNoiseMps, double lossProbability,
                      int jumpCount, double jumpMeters) {
        if (!(positionNoiseMeters >= 0.0) || !(speedNoiseMps >= 0.0)
                || !(lossProbability >= 0.0 && lossProbability < 1.0)
                || jumpCount < 0 || !(jumpMeters >= 0.0)
                || Double.isInfinite(positionNoiseMeters) || Double.isInfinite(speedNoiseMps)
                || Double.isInfinite(jumpMeters)) {
            throw new IllegalArgumentException("Parâmetros de falha inválidos: ruído e salto não negativos "
                    + "e finitos, perda em [0, 1).");
        }
        this.positionNoiseMeters = positionNoiseMeters;
        this.speedNoiseMps = speedNoiseMps;
        this.lossProbability = lossProbability;
        this.jumpCount = jumpCount;
        this.jumpMeters = jumpMeters;
    }

    public static FaultSpec none() {
        return new FaultSpec(0.0, 0.0, 0.0, 0, 0.0);
    }

    public FaultSpec withPositionNoise(double sigmaMeters) {
        return new FaultSpec(sigmaMeters, speedNoiseMps, lossProbability, jumpCount, jumpMeters);
    }

    public FaultSpec withSpeedNoise(double sigmaMps) {
        return new FaultSpec(positionNoiseMeters, sigmaMps, lossProbability, jumpCount, jumpMeters);
    }

    public FaultSpec withLoss(double probability) {
        return new FaultSpec(positionNoiseMeters, speedNoiseMps, probability, jumpCount, jumpMeters);
    }

    public FaultSpec withJumps(int count, double meters) {
        return new FaultSpec(positionNoiseMeters, speedNoiseMps, lossProbability, count, meters);
    }

    public double getPositionNoiseMeters() { return positionNoiseMeters; }
    public double getSpeedNoiseMps() { return speedNoiseMps; }
    public double getLossProbability() { return lossProbability; }
    public int getJumpCount() { return jumpCount; }
    public double getJumpMeters() { return jumpMeters; }
}
