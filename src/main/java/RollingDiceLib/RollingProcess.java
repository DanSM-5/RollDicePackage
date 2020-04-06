package RollingDiceLib;

import com.ea.async.Async;
import com.sun.jdi.IntegerValue;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.ea.async.Async.await;

public class RollingProcess {
    private int numberOfDices = 0;
    private int sequence = 0;
    private List<Counter> counterList;

    private int progress = 0;

    public RollingProcess(int numberOfDices, int sequence) {
        this.numberOfDices = numberOfDices;
        this.sequence = sequence;
    }

    static {
        Async.init();
    }

    public void rollDiceAsyncNonBlocking(BiConsumer<Integer,Integer> report, BiConsumer<Integer,Integer> whenDone, CancelationProcess cancelProcess){
        counterList = new ArrayList<>();
        Counter counter;
        for(int i = 0; i < numberOfDices; i++){
            if(cancelProcess.isCancellationRequested()){
                break;
            }

            counter = new Counter();
            counterList.add(counter);

            await (rollingTaskAsync(counter, i, report, whenDone, cancelProcess));
        }
    }

    public void rollDiceAsyncParallel(BiConsumer<Integer,Integer> report, BiConsumer<Integer,Integer> whenDone, CancelationProcess cancelProcess) {
        counterList = new ArrayList<>();
        List<Integer> results = await(CompletableFutureAsync.whenAll(
                IntStream.range(0, numberOfDices)
                .mapToObj(n -> {
                    Counter c = new Counter();
                    counterList.add(c);
                    return rollingTaskAsync(c, n, report, whenDone, cancelProcess);
                })
                .collect(Collectors.toList())));
    }

    private CompletableFuture<Integer> rollingTaskAsync(Counter counter, int diceNumber, BiConsumer<Integer, Integer> report, BiConsumer<Integer, Integer> whenDone, CancelationProcess cancelProcess) {
        return(CompletableFuture.supplyAsync(() -> {
            int result = 0;
            int nextReport = getStep();
            SecureRandom random = new SecureRandom();
            for (int i = 0; i < sequence; i++) {
                if (cancelProcess.isCancellationRequested()){
                    break;
                }

                result = random.nextInt(5) + 1;
                counter.value++;

                if (nextReport < i){
                    setProgress(counterList.stream().map(c -> c.value).collect(Collectors.summingInt(Integer::intValue)));
                    report.accept(progress, getPercentage());
                    nextReport += getStep();
                }
            }
            if(cancelProcess.isCancellationRequested()){
                counter.setResult(random.nextInt(5) + 1);
            }else {
                counter.setResult(new SecureRandom().nextInt(5) + 1);
            }

            setProgress(counterList.stream().map(c -> c.value).collect(Collectors.summingInt(Integer::intValue)));
            report.accept(progress, getPercentage());
            whenDone.accept(diceNumber, counter.getResult());

            return counter.getResult();
        }));
    }

    public void rollDiceSyncBlocking(BiConsumer<Integer,Integer> report, BiConsumer<Integer,Integer> whenDone, CancelationProcess cancelProcess){
        counterList = new ArrayList<>();
        Counter counter;
        for (int i = 0; i < numberOfDices; i++){
            if(cancelProcess.isCancellationRequested()){
                break;
            }
            counter = new Counter();
            counterList.add(counter);

            rollingTaskSync(counter, i, report, whenDone, cancelProcess);
        }
    }

    private int rollingTaskSync(Counter counter, int diceNumber, BiConsumer<Integer, Integer> report, BiConsumer<Integer, Integer> whenDone, CancelationProcess cancelProcess) {
        int result = 1;
        int nextReport = getStep();
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < sequence; i++){
            if (cancelProcess.isCancellationRequested()){
                break;
            }
            result = random.nextInt(5) + 1;
            counter.value++;

            if (nextReport < i){
//                List<Integer> b = counterList.stream().map(c -> c.value).collect(Collectors.toList());
//                int a = b.stream().collect(Collectors.summingInt(Integer::intValue));
                setProgress(counterList.stream().map(c -> c.value).collect(Collectors.summingInt(Integer::intValue)));
                report.accept(progress, getPercentage());
                nextReport += getStep();
                //setProgress(Arrays.stream(counterList.stream().mapToInt(c -> c.value).collect(Collectors.summingInt(Integer::intValue))).filter());
            }
        }
        counter.setResult(result);
        setProgress(counterList.stream().map(c -> c.value).collect(Collectors.summingInt(Integer::intValue)));
        whenDone.accept(diceNumber, counter.getResult());
        report.accept(progress, getPercentage());

        return counter.getResult();
    }

    public int getNumberOfDices() {
        return numberOfDices;
    }

    public void setNumberOfDices(int numberOfDices) {
        this.numberOfDices = numberOfDices;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public List<Counter> getCounterList() {
        return counterList;
    }

    public void setCounterList(List<Counter> counterList) {
        this.counterList = counterList;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getPercentage() {
        return (int) Math.floor(((double) progress / getTotal()) * 100);
    }

    public int getTotal() {
        return sequence * numberOfDices;
    }

    public int getStep() {
        return getTotal() / 100;
    }
}
