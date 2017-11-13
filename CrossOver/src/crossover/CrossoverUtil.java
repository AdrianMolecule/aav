package crossover;

import java.util.List;

public class CrossoverUtil {

    public static double standardDeviation(List<Integer> numbers) {
        if (numbers.isEmpty()) {
            return 0;
        }
        double mean = numbers.stream().mapToInt(i -> i).average().getAsDouble();
        double squareSum = 0;

        for (long x:numbers) {
            squareSum += StrictMath.pow(x - mean, 2);
        }

        return Math.sqrt((squareSum) / ((double)numbers.size() - 1));
    }


}
