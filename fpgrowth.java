import java.util.*;
import java.io.*;
import java.util.Map.*;

public class fpgrowth {
    public static int MIN_SUP;
    public static Node ROOT = new Node(-1);
    public static Map<Integer, Integer> countedTransItems = new LinkedHashMap<>();
    public static Map<Set<Integer>, Integer> FPs = new LinkedHashMap<>();

    public static void main(String[] args) throws FileNotFoundException {
        long startTime = System.currentTimeMillis();
        if (args.length != 2) {
            System.out.println("Incorrect args! Exiting");
            return;
        } else {
            inputFromFile(args[0], args[1], new HashMap<>());
        }
        trimMinSup();
        
        beginMining(args[0], args[1]);
        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("Found " + FPs.size() + " FPs and executed in " + totalTime + " milliseconds");
        outputToFile(FPs);
    }

    /**
     * Compiles the first sorted maps and begins the recursion
     * @param fileName External filename
     * @param minSupPercentage Min_Sup in terms of %
     * @throws FileNotFoundException
     */
    public static void beginMining(String fileName, String minSupPercentage) throws FileNotFoundException {
        Map<Integer, Integer> sortedItemMap = sortCountedItems(countedTransItems, false);
        Map<Integer, Integer> reverseSortedMap = sortCountedItems(countedTransItems, true);
        Map<Integer, Node> nodePointer = inputFromFile(fileName, minSupPercentage, sortedItemMap);
        mineFPTree(reverseSortedMap, nodePointer, new HashSet<>());
    }

    /**
     * Handles taking in the data from the external file, will be used twice for the initial count of the transactions, and building the first FPTree
     * @param fileName External filename
     * @param minSupPercentage Min_Sup in terms of %
     * @param sortedItemMap ItemMap ordered in terms of occurance from high to low. If on counting step, pass empty map for sortedItemMap
     * @return Returns the pointer to every item's first occuring node
     * @throws FileNotFoundException
     */
    public static Map<Integer, Node> inputFromFile(String fileName, String minSupPercentage, Map<Integer, Integer> sortedItemMap) throws FileNotFoundException {
        File dataSet = new File(fileName);
        Map<Integer, Node> nodePointer = new HashMap<>();

        // Stop invalid percentages
        int minSupPercent = Integer.parseInt(minSupPercentage);
        if (minSupPercent > 100 || minSupPercent < 0) {
            System.out.println("Cannot have more than 100% or less than 0% min_sup. Exiting.");
            System.exit(1);
        }

        // Set up scanner and make the MIN_SUP
        Scanner input = new Scanner(dataSet);

        int numTransactions = Integer.parseInt(input.nextLine());
        float minSupFloat = numTransactions * (minSupPercent / 100f);
        MIN_SUP = Math.round(minSupFloat);

        // Begin making the transactionDB
        Map<Integer, Set<Integer>> transactionDB = new HashMap<Integer, Set<Integer>>();
        int counter = 0;
        while(input.hasNextLine()) {
            // If there are 1000 items in the transactionDB
            if (counter % 1000 == 0) {
                // And we're on the counting stage, then add all the items to the countedItems global variable
                // Making sure to add the new counted amount to the already existing amount
                if (sortedItemMap.isEmpty()) {
                    Map<Integer, Integer> countedItems = countItems(transactionDB);

                    Set<Integer> key = countedItems.keySet();
                    Iterator<Integer> iterator = key.iterator();

                    while(iterator.hasNext()) {
                        int itemKey = iterator.next();
                        if (countedTransItems.containsKey(itemKey)) {
                            countedTransItems.put(itemKey, countedTransItems.get(itemKey) + countedItems.get(itemKey));
                        } else {
                            countedTransItems.put(itemKey, countedItems.get(itemKey));
                        }
                    }
                // And we're on the FPTree stage, sort the 1000 transaction items and put them in the FPTree
                } else {
                    Map<Integer, SortedSet<Integer>> sortedTDs = sortTransactions(transactionDB, sortedItemMap);
                    nodePointer = FPTree(sortedTDs, nodePointer);
                }
                // Empty the TDB
                transactionDB = new HashMap<>();
            }
            // Continue pulling in transactions
            String transaction = input.nextLine();
            String[] transactionSplit = transaction.split("\\s"); // Split on every whitespace character (tab or space)
            int transID = Integer.parseInt(transactionSplit[0]); // TransactionID will always be the first value
            // transactionSplit[1] not needed (size of transaction)
            Set<Integer> transItems = new HashSet<>();
            for (int i = 2; i < transactionSplit.length; i++) {
                transItems.add(Integer.parseInt(transactionSplit[i]));
            }
            transactionDB.put(transID, transItems);
            counter++;
        }
        input.close();

        // Empty the TDB one last time following the same steps as above
        if (sortedItemMap.isEmpty()) {
            Map<Integer, Integer> countedItems = countItems(transactionDB);

            Set<Integer> key = countedItems.keySet();
            Iterator<Integer> iterator = key.iterator();

            while(iterator.hasNext()) {
                int itemKey = iterator.next();
                if (countedTransItems.containsKey(itemKey)) {
                    countedTransItems.put(itemKey, countedTransItems.get(itemKey) + countedItems.get(itemKey));
                } else {
                    countedTransItems.put(itemKey, countedItems.get(itemKey));
                }
            }
        } else {
            Map<Integer, SortedSet<Integer>> sortedTDs = sortTransactions(transactionDB, sortedItemMap);
            nodePointer = FPTree(sortedTDs, nodePointer);
        }
        return nodePointer;
    }

    /**
     * Main recusive method, mines the FPTree given to it, and will recurse into itself until it is at the root node
     * @param reverseSortedItemMap Map of the items and their counts from lowest to highest
     * @param nodePointer Pointer for every item's first occuring node
     * @param base The conditional pattern base passed to the method by itself (empty set if beginning the recursion)
     */
    public static void mineFPTree(Map<Integer, Integer> reverseSortedItemMap, Map<Integer, Node> nodePointer, Set<Integer> base) {
        Set<Integer> sortedSet = reverseSortedItemMap.keySet();
        Iterator<Integer> itemIter = sortedSet.iterator();

        // Iterate through all the items in the reverseSortedMap
        while(itemIter.hasNext()) {
            int item = itemIter.next();
            Node firstNode = nodePointer.get(item);

            // Make the currently selected pattern base with the new item
            Set<Integer> currentPattern = new HashSet<>();
            currentPattern.addAll(base);
            currentPattern.add(item);

            // Counters for supports
            int condPatternSupport = firstNode.getCount();
            int totalSupport = 0;

            // Holds all the conditional pattern bases for the current pattern
            Map<Set<Integer>, Integer> condPatternBases = new HashMap<>();

            // Iterate through the node pointers
            while(firstNode != null) {
                Set<Integer> condPatternBase = new HashSet<>();

                // Set counters initial values
                totalSupport += firstNode.getCount();
                condPatternSupport = firstNode.getCount();

                Node currentNode = firstNode;
                // Iterate backwards to the root node, keeping track of the traversal
                while (currentNode.getPrevious().getId() != -1) {
                    currentNode = currentNode.getPrevious();
                    condPatternBase.add(currentNode.getId());
                }
                firstNode = firstNode.getNextPointer();
                // If the traversal isn't empty, this is a conditional pattern base
                if (!condPatternBase.isEmpty()) {
                    condPatternBases.put(condPatternBase, condPatternSupport);
                }
            }

            // If the totalSupport we summed up is >= min_sup, the currentPattern is an FP
            if (totalSupport >= MIN_SUP) {
                FPs.put(currentPattern, totalSupport);
            }

            // Build our frequency, sorted, and reverseSorted tables to build the next conditional tree
            Map<Integer, Integer> frequencyTable = countMinedItems(condPatternBases);
            Map<Integer, Integer> sortedFreqTable = sortCountedItems(frequencyTable, false);
            Map<Integer, Integer> reverseSortedFreqTable = sortCountedItems(frequencyTable, true);
            Map<SortedSet<Integer>, Integer> sortedCondPatternBases = sortForCondTree(condPatternBases, sortedFreqTable);

            // Build the next conditional tree
            Map<Integer, Node> conditionalTree = condFPTree(sortedCondPatternBases);

            // Recurse recurse (if we have a conditional tree)
            if (!conditionalTree.isEmpty()) {
                mineFPTree(reverseSortedFreqTable, conditionalTree, currentPattern);
            }
        }
    }

    /**
     * Count items given in the transaction DB (must be Map<Int, Set<Int>>)
     * @param TD Transactions (Key = TransactionID, Value = Set of items)
     * @return Count of each item that occurs in the TD. (Key = Item, Value = Count)
     */
    public static Map<Integer, Integer> countItems(Map<Integer, Set<Integer>> TD) {
        Map<Integer, Integer> countedTransItems = new LinkedHashMap<>();
        Set<Integer> keys = TD.keySet();
        Iterator<Integer> transiterator = keys.iterator();

        // Iterate through the transaction database
        // Count all the occurances of each item in the database and either add one to the current value or inset it into the table
        // Modified from Assignment 1
        while (transiterator.hasNext()) {
            int hashKey = transiterator.next();
            Set<Integer> transaction = TD.get(hashKey);

            Iterator<Integer> itemIter = transaction.iterator();

            while(itemIter.hasNext()) {
                int itemID = itemIter.next();
                if (countedTransItems.containsKey(itemID)) {
                    countedTransItems.put(itemID, (countedTransItems.get(itemID) + 1));
                } else {
                    countedTransItems.put(itemID, 1);
                }
            }
        }
        return countedTransItems;
    }

    /**
     * Trims all values < MIN_SUP from the Map of counted items, countedTransItems is a global
     * @param countedTransItems Map of counted items (Key = TransactionID, Value = Set of items)
     */
    public static void trimMinSup() {
        Set<Integer> keys = countedTransItems.keySet();
        Iterator<Integer> itemsetIterator = keys.iterator();

        while (itemsetIterator.hasNext()) {
            int hashKey = itemsetIterator.next();
            if (countedTransItems.get(hashKey) < MIN_SUP) {
                itemsetIterator.remove();
            }
        }
        return;
    }

    /**
     * Sorts the Map of counted items in either forward or reverse order.
     * @param countedTransItems Map of counted items (Key = TransactionID, Value = Set of items)
     * @param reverseSort true: sort Low to High / false: sort High to Low
     * @return Sorted map in order specified
     */
    public static Map<Integer, Integer> sortCountedItems(Map<Integer, Integer> countedTransItems, boolean reverseSort) {
        List<Entry<Integer, Integer>> sortedList = new ArrayList<Entry<Integer, Integer>>(countedTransItems.entrySet());
        Map<Integer, Integer> sortedMap = new LinkedHashMap<Integer, Integer>();
        Comparator<Entry<Integer, Integer>> sorter;

        // High to Low
        if (!reverseSort) {
            // Comparator to sort the ArrayList from high to low
            sorter = new Comparator<Entry<Integer,Integer>>() {
                public int compare(Entry<Integer, Integer> a, Entry<Integer, Integer> b) {
                    if (a.getValue() < b.getValue()) {
                        return 1;
                    } else if (a.getValue() > b.getValue()) {
                        return -1;
                    } else {
                        if (a.getKey() > b.getKey()) {
                            return 1;
                        } else if (a.getKey() < b.getKey()) {
                            return -1;
                        }
                        return 0;
                    }
                }
            };
        // Low to High
        } else {
            // Comparator to sort the ArrayList from low to high
            sorter = new Comparator<Entry<Integer,Integer>>() {
                public int compare(Entry<Integer, Integer> a, Entry<Integer, Integer> b) {
                    if (a.getValue() > b.getValue()) {
                        return 1;
                    } else if (a.getValue() < b.getValue()) {
                        return -1;
                    } else {
                        if (a.getKey() < b.getKey()) {
                            return 1;
                        } else if (a.getKey() > b.getKey()) {
                            return -1;
                        }
                        return 0;
                    }
                }
            };
        }

        // Sort according to the Comparator
        Collections.sort(sortedList, sorter);

        // Put all values back into a Map if they meet min_sup
        for (int i = 0; i < sortedList.size(); i++) {
            sortedMap.put(sortedList.get(i).getKey(), sortedList.get(i).getValue());
        }

        return sortedMap;
    }

    /**
     * Sorts a transaction's items in order of the sorted map
     * @param TD Transactions to sort (Key = transactionID, Value = Set of Transactions)
     * @param sortedOrder Sorted Map to compare order to
     * @return (Key = transactionID, Value = SortedSet of items)
     */
    public static Map<Integer, SortedSet<Integer>> sortTransactions(Map<Integer, Set<Integer>> TD, Map<Integer, Integer> sortedOrder) {
        Map<Integer, SortedSet<Integer>> newTD = new LinkedHashMap<Integer, SortedSet<Integer>>();

        Set<Integer> keys = TD.keySet();
        Iterator<Integer> transiterator = keys.iterator();

        // Iterate through every transaction in TD
        while (transiterator.hasNext()) {
            int hashKey = transiterator.next();
            Set<Integer> transactionItems = TD.get(hashKey);

            // Comparator used to make sure items added to the TreeSet are added in order
            Comparator<Integer> comparator = new Comparator<Integer>() {
                public int compare(Integer a, Integer b) {
                    if (sortedOrder.get(a) < sortedOrder.get(b)) {
                        return 1;
                    } else if (sortedOrder.get(a) > sortedOrder.get(b)) {
                        return -1;
                    } else {
                        if (a > b) {
                            return 1;
                        } else if (a < b) {
                            return -1;
                        }
                        return 0;
                    }
                }
            };

            SortedSet<Integer> orderedItems = new TreeSet<>(comparator);
            Iterator<Integer> transactionIterator = transactionItems.iterator();

            // Add items to the ordered set
            while (transactionIterator.hasNext()) {
                int currentItem = transactionIterator.next();
                if (sortedOrder.containsKey(currentItem)) {
                    orderedItems.add(currentItem);
                }
            }
            // Put ordered set into new TD
            newTD.put(hashKey, orderedItems);
        }
        return newTD;
    }

    /**
     * Generates an FPTree based on the list of transactions and sorted items passed to it.
     * Builds the Tree under the "root" global
     * To build the tree portions at a time, pass it the Node Pointer that it returns to continue building.
     * @param TD Transactions with sorted items
     * @param nodePointer For new trees, pass a new map, to build on the tree that already exists, pass the nodePointer to it recursively
     * @return Returns a pointer for the first occurance of each node's ID (Key = ItemID, Value = First Node with this ItemID)
     */
    public static Map<Integer, Node> FPTree(Map<Integer, SortedSet<Integer>> TD, Map<Integer, Node> nodePointer) {
        Set<Integer> keys = TD.keySet();
        Iterator<Integer> transiterator = keys.iterator();

        // Iterate through all transactions
        while (transiterator.hasNext()) {
            int hashKey = transiterator.next();
            SortedSet<Integer> transaction = TD.get(hashKey);

            Iterator<Integer> itemIter = transaction.iterator();

            Node currentNode = ROOT;
            ArrayList<Node> nextNodes = ROOT.getNext();

            // Iterate through all sorted items
            while(itemIter.hasNext()) {
                int itemID = itemIter.next();
                boolean toContinue = true;
                // Check to see if this item already exists in the nextnodes list
                // If it does, increment count by 1 and continue to next item
                for (int i = 0; i < nextNodes.size(); i++) {
                    if (nextNodes.get(i).getId() == itemID) {
                        nextNodes.get(i).incrementCount(1);
                        currentNode = nextNodes.get(i);
                        nextNodes = currentNode.getNext();
                        toContinue = false;
                    }
                }
                // If I made it here, there is no next node, one needs to be created
                if (toContinue) {
                    Node newNode = new Node(itemID);
                    currentNode.setNext(newNode);

                    currentNode = newNode;
                    nextNodes = currentNode.getNext();

                    // Check NodePointer to see if other Nodes with this ID exists
                    Node itemNode = nodePointer.get(itemID);
                    // If they do, iterate to the end to point another "arrow" at this new node
                    if (nodePointer.get(itemID) != null) {
                        while (itemNode.getNextPointer() != null) {
                            itemNode = itemNode.getNextPointer();
                        }
                        itemNode.setNextPointer(newNode);
                    // If they don't, make a new NodePointer for this ItemID
                    } else {
                        nodePointer.put(itemID, newNode);
                    }
                }
            }
        }
        return nodePointer;
    }

    /**
     * Outputs found FPs to external file
     * @param fps FPs to write
     * @return Success
     * @throws FileNotFoundException
     */
    private static boolean outputToFile(Map<Set<Integer>, Integer> fps) throws FileNotFoundException {
        PrintWriter fileOutput = new PrintWriter("MiningResult.txt");
        fileOutput.println("|FPs| = " + fps.size());

        Set<Set<Integer>> fpsKeys = fps.keySet();
        Iterator<Set<Integer>> fpsItem = fpsKeys.iterator();

        while(fpsItem.hasNext()) {
            Set<Integer> currentFps = fpsItem.next();
            fileOutput.println(currentFps + " : " + fps.get(currentFps));
        }

        fileOutput.close();

        return true;
    }

    /* THESE FOLLOWING METHODS ARE SPECIAL TO THE MINING METHOD DUE TO THE MIRRORED FORMAT */
    
    /**
     * Build our frequency table for the conditional pattern bases
     * Has mostly the same logic as countItems but the map is mirrored with the pattern base being the key
     * and the count increments by the support instead of by 1
     * @param traversals Conditional Pattern Bases and Support
     * @return Frequency Map
     */
    public static Map<Integer, Integer> countMinedItems(Map<Set<Integer>, Integer> traversals) {
        Set<Set<Integer>> keySet = traversals.keySet();
        Iterator<Set<Integer>> iterator = keySet.iterator();

        Map<Integer, Integer> itemCount = new HashMap<>();

        while(iterator.hasNext()) {
            Set<Integer> currentSet = iterator.next();
            Iterator<Integer> iterator2 = currentSet.iterator();
            int support = traversals.get(currentSet);

            while(iterator2.hasNext()) {
                int item = iterator2.next();
                if (itemCount.containsKey(item)) {
                    itemCount.put(item, (itemCount.get(item) + support));
                } else {
                    itemCount.put(item, support);
                }
            }
        }

        return itemCount;
    }

    /**
     * Sorts conditional pattern bases in the order of the frequency table
     * Must be its own method due to the mirrored map
     * Same logic as sortTransactions just mirrored
     * @param TD Conditional Pattern Bases (Key = Pattern Base, Value = Support)
     * @param sortedOrder H to L Sorted map of ItemIDs
     * @return Sorted conditional pattern bases
     */
    public static Map<SortedSet<Integer>, Integer> sortForCondTree(Map<Set<Integer>, Integer> condPatternBases, Map<Integer, Integer> sortedOrder) {
        Map<SortedSet<Integer>, Integer> sortedPatternBase = new HashMap<>();

        Set<Set<Integer>> keySet = condPatternBases.keySet();
        Iterator<Set<Integer>> iterator = keySet.iterator();

        while(iterator.hasNext()) {
            Set<Integer> pattern = iterator.next();
            
            Comparator<Integer> comparator = new Comparator<Integer>() {
                public int compare(Integer a, Integer b) {
                    if (sortedOrder.get(a) < sortedOrder.get(b)) {
                        return 1;
                    } else if (sortedOrder.get(a) > sortedOrder.get(b)) {
                        return -1;
                    } else {
                        if (a > b) {
                            return 1;
                        } else if (a < b) {
                            return -1;
                        }
                        return 0;
                    }
                }
            };

            SortedSet<Integer> orderedItems = new TreeSet<>(comparator);
            Iterator<Integer> transactionIterator = pattern.iterator();

            while (transactionIterator.hasNext()) {
                int currentItem = transactionIterator.next();
                if (sortedOrder.containsKey(currentItem)) {
                    orderedItems.add(currentItem);
                }
            }
            sortedPatternBase.put(orderedItems, condPatternBases.get(pattern));
        }

        return sortedPatternBase;
    }

    /**
     * Generates the conditional FPTree based on the sorted patternBases
     * Conditional FPTrees don't need to be generated iteratively, so the logic can differ from FPTree in spots
     * Uses a locally generated node because it doesn't need to be generated iteratively
     * @param patternBase Pattern base to generate the tree from
     * @return Returns the NodePointer for the conditional tree
     */
    public static Map<Integer, Node> condFPTree(Map<SortedSet<Integer>, Integer> patternBase) {
        Map<Integer, Node> nodePointer = new LinkedHashMap<Integer, Node>();
        Node root = new Node(-1);

        Set<SortedSet<Integer>> keys = patternBase.keySet();
        Iterator<SortedSet<Integer>> transiterator = keys.iterator();

        while (transiterator.hasNext()) {
            SortedSet<Integer> transaction = transiterator.next();

            Iterator<Integer> itemIter = transaction.iterator();

            Node currentNode = root;
            ArrayList<Node> nextNodes = root.getNext();

            while(itemIter.hasNext()) {
                int itemID = itemIter.next();
                boolean toContinue = true;
                for (int i = 0; i < nextNodes.size(); i++) {
                    if (nextNodes.get(i).getId() == itemID) {
                        nextNodes.get(i).incrementCount(patternBase.get(transaction));
                        currentNode = nextNodes.get(i);
                        nextNodes = currentNode.getNext();
                        toContinue = false;
                    }
                }
                // If I made it here, there is no next node, one needs to be created
                if (toContinue) {
                    Node newNode = new Node(itemID);
                    newNode.setCount(patternBase.get(transaction));
                    currentNode.setNext(newNode);

                    currentNode = newNode;
                    nextNodes = currentNode.getNext();

                    Node itemNode = nodePointer.get(itemID);
                    if (nodePointer.get(itemID) != null) {
                        while (itemNode.getNextPointer() != null) {
                            itemNode = itemNode.getNextPointer();
                        }
                        itemNode.setNextPointer(newNode);
                    } else {
                        nodePointer.put(itemID, newNode);
                    }
                }
            }
        }
        return nodePointer;
    }
}