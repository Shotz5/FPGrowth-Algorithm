# FPGrowth Algorithm

## Execution
This is a Java implmentation of an FPGrowth Algorithm, which is designed to find commonalities between transactions in a database.
Execute by CDing into folder and typing `java fpgrowth ./[filename] [min_sup_percentage]`

## Data Input
Program is not designed to work with unformatted data. Any input files must be of format (\s meaning any white space character):
```
[# of transactions]\n
[transaction #]\s[# of items in transaction]\s[item 1]\s[item 2]\s...\s[item n]\n
.
.
.
```

## Misc Notes
This was a long time coming, and significantly more difficult than I imagined. Almost 400 lines of code to do the same thing that Apriori does, but entirely more efficiently. There may be some repetitive code, but it was necessary in order to think the problem through step-by-step in order to avoid any headaches.

In this problem, the most inefficient part of the problem with be the power set generator. It is a whopping O(2<sup>n</sup>). However it is necessary to find all the possible FPs in the conditional trees, and works surprisingly well for the case.

## Testing
1. java fpgrowth ./1k5L.txt 5 - 0 FPs in 16ms
2. java fpgrowth ./1k5L.txt 2 - 45 FPs in 21ms
3. java fpgrowth ./1k5L.txt 1 - 213 FPs in 34ms
4. java fpgrowth ./t25i10d10k.txt 20 - 0 FPs in 78ms
5. java fpgrowth ./t25i10d10k.txt 10 - 20 FPs in 130ms
6. java fpgrowth ./t25i10d10k.txt 5 - 142 FPs in 206ms
7. java fpgrowth ./retail.txt 50 - 1 FPs in 283ms
8. java fpgrowth ./retail.txt 25 - 3 FPs in 289ms
9. java fpgrowth ./retail.txt 10 - 10 FPs in 305ms
10. java fpgrowth ./retail.txt 5 - 16 FPs in 317ms
11. java fpgrowth ./retail.txt 2 - 46