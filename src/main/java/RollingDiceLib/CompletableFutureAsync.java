package RollingDiceLib;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CompletableFutureAsync {
    public static <T> CompletableFuture<List<T>> whenAll(List<CompletableFuture<T>> collection){
        return CompletableFuture.supplyAsync(() -> collection.stream()
                .map(cf -> cf.join())
                .collect(Collectors.toList()));
    }
}
