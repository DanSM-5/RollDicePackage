# RollDicePackage
It is a package with a ported version of the RollingDiceLib for Java using the library ea-async.

## Awaiting multiple CompletableFuture objects

The class CompletableFuture allow us to write asynchronous code more easily. However, it is still not as simple as using async/await like in other lenguages.

I was curious about how to perform similar behaviours using CompletableFurture class when I found a very interesting library from EA. EA made an await functionality based on the Await keyword in .net (for more information visit https://github.com/electronicarts/ea-async).
This is great because it helps to reduce the complexity of asynchronous tasks, allowing to write asynchronous code that looks like normal synchronous code.

I starting using this approach with ea-async and it works very well until I got stuck in part, "How to await multiple CompletableFuture objects until all of them are done?"
To explain this let me made an anolog with C# where I originally wrote this function.

### Original method created in C#
```
public async Task RollDiceAsyncParallel(Action<int, int> report, Action<int,int> done, CancellationToken ct)
        {
            // List to save the reference of the tasks
            var tasks = new List<Task<int>>();
            CounterList = new List<Counter>();
            Counter counter;
            for (int i = 0; i < NumOfDices; i++)
            {
                counter = new Counter();
                CounterList.Add(counter);

                // call of independent rolling tasks
                // that will be executed in parallel
                tasks.Add(RollingTaskAsync(counter, i, report, done, ct));
                // Each RollingTaskAsync() returns Task<int>
            }
            // Wait for all the rolling tasks to finish
            int[] results = await Task.WhenAll(tasks); // Returns int[] with the results
```
After I call RollingTaskAync() I get a reference to a Task<int>, I group them in a collection named "tasks" and finally use await with the static method Task.WhenAll() that accepts a collection of tasks with the same return.

After numerous attempts, I finally was able to compute the same process in Java using only CompletableFuture objects with the help of this article https://medium.com/@kalpads/fantastic-completablefuture-allof-and-how-to-handle-errors-27e8a97144a0.

### First attempt of writting the method in Java
```
public void rollDiceAsyncParallel(BiConsumer<Integer,Integer> report, BiConsumer<Integer,Integer> whenDone, CancelationProcess cancelProcess) throws ExecutionException, InterruptedException {
        // List to store a reference of each CompletableFuture object
        List<CompletableFuture<Integer>> tasks = new ArrayList<>();
        counterList = new ArrayList<>();
        Counter counter;
        for(int i = 0; i < numberOfDices; i++){

            counter = new Counter();
            counterList.add(counter);

            tasks.add(rollingTaskAsync(counter, i, report, whenDone, cancelProcess));
            // Each rollingTaskAsync() returns CompletableFuture<Integer>
        }

        // Create a CompletableFuture<void> object that returns when all the CompletableFuture objects passed are complete
        CompletableFuture<Void> finalTask = CompletableFuture.allOf(tasks.toArray(new CompletableFuture[tasks.size()]));

        // Create a CompletableFuture<List<T>> that returns a list with the results of the CompletableFuture objects
        // using join() method
        CompletableFuture<List<Integer>> futures = finalTask.thenApply(future ->
            tasks.stream()
                 .map(completableFuture -> completableFuture.join())
                 .collect(Collectors.toList())
        );
        
        // Creation of CompletableFuture object that holds final collection of results
        CompletableFuture completableFuture = futures.thenApply(integers -> integers
                .stream()
                .map(Integer::intValue)
                .collect(Collectors.toList()));

        // Using get() to return the List<Integer> that contains the result of each CompletableFuture<Integer> object
        List<Integer> results = (List<Integer>) completableFuture.get(); // Cast is necessary because get() returns Object
}
```

Now both methods do exactly the same thing but the solution in Java is more complex and require more knowledge to understand what's happening.
Here is were the ea-async library comes into play. Using the await() method I was able to rewrite the same method in a simple way that resembles the C# version.

### Same method using await() from ea-async library 
```
public void rollDiceAsyncParallel(BiConsumer<Integer,Integer> report, BiConsumer<Integer,Integer> whenDone, CancelationProcess cancelProcess) throws ExecutionException, InterruptedException {
        // Same list to store the CompletableFuture objects
        List<CompletableFuture<Integer>> tasks = new ArrayList<>();
        counterList = new ArrayList<>();
        Counter counter;
        for(int i = 0; i < numberOfDices; i++){
            counter = new Counter();
            counterList.add(counter);

            // Saving a reference to each CompletableFuture<Integer> object as usual 
            tasks.add(rollingTaskAsync(counter, i, report, whenDone, cancelProcess));
        }

        // With the help of await() I can return the result of a CompletableFuture object
        // In this case, I use join() to create a CompletableFuture<List<Integer>> that 
        // will return the final List<Integer> with the results of the CompletableFuture objects
        List<Integer> results = await(CompletableFuture.supplyAsync(() -> tasks
                                    .stream()
                                    .map(cf -> cf.join())
                                    .collect(Collectors.toList())));
}
```

The method await() only accepts a Future or CompletableFuture object to return, so the way to await multiple CompletableFuture tasks is to map them into
a List<T>. However, List<T> is not an instance of CompletableFuture, so it can't be used with await directly. That's why I use supplyAsync() method
to process the maping to a List<T> asynchronously or in other words, to return a CompletableFuture\<List\<Integer\>\> that can be used with await().
