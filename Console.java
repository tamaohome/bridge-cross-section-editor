import java.util.Arrays;
import java.util.stream.Collectors;

public class Console {
    /**
     * コンソールにログを出力する
     */
    public static void log(Object... args) {
        String message = Arrays.stream(args)
                .map(arg -> {
                    if (arg instanceof Double) {
                        return String.format("%.6f", (Double) arg);
                    } else {
                        return arg.toString();
                    }
                })
                .collect(Collectors.joining(" "));
        System.out.println(message);
    }
}
