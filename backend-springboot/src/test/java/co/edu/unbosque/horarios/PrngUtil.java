package co.edu.unbosque.horarios;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

public class PrngUtil {
    public static DoubleSupplier prng(int seed) {
        final int[] state = new int[]{seed};
        return () -> {
            int a = state[0];
            a = a + 0x6D2B79F5;
            state[0] = a;
            int t = (a ^ (a >>> 15)) * (1 | a);
            t = (t + ((t ^ (t >>> 7)) * (61 | t))) ^ t;
            return Integer.toUnsignedLong(t ^ (t >>> 14)) / 4294967296.0;
        };
    }

    public static <T> List<T> barajar(List<T> arr, DoubleSupplier rnd) {
        List<T> a = new ArrayList<>(arr);
        for (int i = a.size() - 1; i > 0; i--) {
            int j = (int) Math.floor(rnd.getAsDouble() * (i + 1));
            T tmp = a.get(i);
            a.set(i, a.get(j));
            a.set(j, tmp);
        }
        return a;
    }
}
