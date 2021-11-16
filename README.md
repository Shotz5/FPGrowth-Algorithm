# FPGrowth Algorithm

## Execution
This is a Java implmentation of an FPGrowth Algorithm, which is designed to find commonalities between transactions in a database.
Execute by CDing into folder and typing `java fpgrowth ./[filename] [min_sup_percentage]`

## Data Input
Program is not designed to work with unformatted data. Any input files must be of format (\s meaning any white space character, [item x] being any integer without duplicates per transaction):
```
[# of transactions]\n
[transaction #]\s[# of items in transaction]\s[item 1]\s[item 2]\s...\s[item n]\n
.
.
.
```

## Misc Notes
This was a long time coming, and significantly more difficult than I imagined. Almost 600 lines of code to do the same thing that Apriori does, but entirely less efficiently for our provided datasets.

There are many methods with very similar, but every so slightly different logic that had to be changed to fit the `mineFPTree()` method due to the method requiring the conditional pattern bases to be the *key.* In making the original set, the keys are the transaction numbers.

In this problem, the most inefficient part of the problem will be the recursion. It is a whopping O(2<sup>n</sup>) time complexity.

## Testing
Unit testing on a Ryzen 7 3800x reveals the following average execution speeds for given datasets:

1. java fpgrowth ./1k5L.txt 5 - 0 FPs in 86ms
2. java fpgrowth ./1k5L.txt 2 - 45 FPs in 82ms
3. java fpgrowth ./1k5L.txt 1 - 213 FPs in 161ms
4. java fpgrowth ./t25i10d10k.txt 20 - 0 FPs in 301ms
5. java fpgrowth ./t25i10d10k.txt 10 - 20 FPs in 441ms
6. java fpgrowth ./t25i10d10k.txt 5 - Not waiting the time to see if it executes...
7. java fpgrowth ./retail.txt 50 - 1 FPs in 765ms
8. java fpgrowth ./retail.txt 25 - 3 FPs in 795ms
9. java fpgrowth ./retail.txt 10 - 9 FPs in 791ms
10. java fpgrowth ./retail.txt 5 - 16 FPs in 802ms
11. java fpgrowth ./retail.txt 2 - 55 FPs in 887ms
12. java fpgrowth ./connect.txt 75 - Not waiting the time to see if it executes...