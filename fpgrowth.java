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
        trimMinSup(countedTransItems);
        
        Map<Integer, Integer> sortedItemMap = sortCountedItems(countedTransItems, false);
        Map<Integer, Integer> reverseSortedMap = sortCountedItems(countedTransItems, true);
        Map<Integer, Node> nodePointer = inputFromFile(args[0], args[1], sortedItemMap);
        mineFPTree(reverseSortedMap, nodePointer, new HashSet<>());
        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("Found " + FPs.size() + " FPs and executed in " + totalTime + " milliseconds");
        outputToFile(FPs);
    }

    public static Map<Integer, Node> inputFromFile(String fileName, String minSupPercentage, Map<Integer, Integer> sortedItemMap) throws FileNotFoundException {
        File dataSet = new File(fileName);
        Map<Integer, Node> nodePointer = new HashMap<>();

        int minSupPercent = Integer.parseInt(minSupPercentage);
        if (minSupPercent > 100 || minSupPercent < 0) {
            System.out.println("Cannot have more than 100% or less than 0% min_sup. Exiting.");
            System.exit(1);
        }

        Scanner input = new Scanner(dataSet);
        int numTransactions = Integer.parseInt(input.nextLine());

        float minSupFloat = numTransactions * (minSupPercent / 100f);
        MIN_SUP = Math.round(minSupFloat);

        Map<Integer, Set<Integer>> transactionDB = new HashMap<Integer, Set<Integer>>();
        int counter = 0;
        while(input.hasNextLine()) {
            if (counter % 2 == 0) {
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

                transactionDB = new HashMap<>();
            }
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

    public static void mineFPTree(Map<Integer, Integer> sortedItemMap, Map<Integer, Node> nodePointer, Set<Integer> base) {
        Set<Integer> sortedSet = sortedItemMap.keySet();
        Iterator<Integer> itemIter = sortedSet.iterator();

        while(itemIter.hasNext()) {
            int item = itemIter.next();
            Node firstNode = nodePointer.get(item);
            Set<Integer> currentPattern = new HashSet<>();
            currentPattern.addAll(base);
            currentPattern.add(item);
            int condPatternSupport = firstNode.getCount();
            int totalSupport = 0;

            Map<Set<Integer>, Integer> condPatternBase = new HashMap<>();

            while(firstNode != null) {
                Set<Integer> traversal = new HashSet<>();
                totalSupport += firstNode.getCount();
                condPatternSupport = firstNode.getCount();
                Node currentNode = firstNode;
                while (currentNode.getPrevious().getId() != -1) {
                    currentNode = currentNode.getPrevious();
                    traversal.add(currentNode.getId());
                }
                firstNode = firstNode.getNextPointer();
                if (!traversal.isEmpty()) {
                    condPatternBase.put(traversal, condPatternSupport);
                }
            }

            if (totalSupport >= MIN_SUP) {
                FPs.put(currentPattern, totalSupport);
            }

            Map<Integer, Integer> frequencyTable = countMinedItems(condPatternBase);
            Map<Integer, Integer> sortedFreqTable = sortCountedItems(frequencyTable, false);
            Map<Integer, Integer> reverseSortedFreqTable = sortCountedItems(frequencyTable, true);
            Map<SortedSet<Integer>, Integer> sortedCondPatternBases = sortForCondTree(condPatternBase, sortedFreqTable);

            Map<Integer, Node> conditionalTree = condFPTree(sortedCondPatternBases);

            if (!conditionalTree.isEmpty()) {
                mineFPTree(reverseSortedFreqTable, conditionalTree, currentPattern);
            }
        }
    }

    public static Map<Integer, Integer> countItems(Map<Integer, Set<Integer>> TD) {
        Map<Integer, Integer> countedTransItems = new LinkedHashMap<>();
        Set<Integer> keys = TD.keySet();
        Iterator<Integer> transiterator = keys.iterator();

        // Iterate through the transaction database
        // Count all the occurances of each item in the database and either add one to the current value or inset it into the table
        // Advantage of adding as we go along is that we don't have memory allocated for "0s" that will get pruned later
        // As well it's less to iterate through when we do begin pruning
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

    public static void trimMinSup(Map<Integer, Integer> countedTransItems) {
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

    public static Map<Integer, Integer> sortCountedItems(Map<Integer, Integer> countedTransItems, boolean reverseSort) {
        List<Entry<Integer, Integer>> sortedList = new ArrayList<Entry<Integer, Integer>>(countedTransItems.entrySet());
        Map<Integer, Integer> sortedMap = new LinkedHashMap<Integer, Integer>();

        if (!reverseSort) {
            Comparator<Entry<Integer, Integer>> highToLowSorter = new Comparator<Entry<Integer,Integer>>() {
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
            Collections.sort(sortedList, highToLowSorter);
        } else {
            Comparator<Entry<Integer, Integer>> lowToHighSorter = new Comparator<Entry<Integer,Integer>>() {
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
            Collections.sort(sortedList, lowToHighSorter);
        }

        // Put all values back into a Map if they meet min_sup
        for (int i = 0; i < sortedList.size(); i++) {
            sortedMap.put(sortedList.get(i).getKey(), sortedList.get(i).getValue());
        }

        return sortedMap;
    }

    public static Map<Integer, SortedSet<Integer>> sortTransactions(Map<Integer, Set<Integer>> TD, Map<Integer, Integer> sortedOrder) {
        Map<Integer, SortedSet<Integer>> newTD = new LinkedHashMap<Integer, SortedSet<Integer>>();

        Set<Integer> keys = TD.keySet();
        Iterator<Integer> transiterator = keys.iterator();

        while (transiterator.hasNext()) {
            int hashKey = transiterator.next();
            Set<Integer> transactionItems = TD.get(hashKey);

            Comparator<Integer> comp1 = new Comparator<Integer>() {
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

            SortedSet<Integer> orderedItems = new TreeSet<>(comp1);
            Iterator<Integer> transactionIterator = transactionItems.iterator();

            while (transactionIterator.hasNext()) {
                int currentItem = transactionIterator.next();
                if (sortedOrder.containsKey(currentItem)) {
                    orderedItems.add(currentItem);
                }
            }
            newTD.put(hashKey, orderedItems);
        }

        return newTD;
    }

    public static Map<Integer, Node> FPTree(Map<Integer, SortedSet<Integer>> TD, Map<Integer, Node> nodePointer) {
        Set<Integer> keys = TD.keySet();
        Iterator<Integer> transiterator = keys.iterator();

        while (transiterator.hasNext()) {
            int hashKey = transiterator.next();
            SortedSet<Integer> transaction = TD.get(hashKey);

            Iterator<Integer> itemIter = transaction.iterator();

            Node currentNode = ROOT;
            ArrayList<Node> nextNodes = ROOT.getNext();

            while(itemIter.hasNext()) {
                int itemID = itemIter.next();
                boolean toContinue = true;
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


    /* THESE METHODS ARE SPECIAL TO THE MINING METHOD DUE TO THE MIRRORED FORMAT */
    
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

    public static Map<SortedSet<Integer>, Integer> sortForCondTree(Map<Set<Integer>, Integer> TD, Map<Integer, Integer> sortedOrder) {
        Map<SortedSet<Integer>, Integer> sortedPatternBase = new HashMap<>();

        Set<Set<Integer>> keySet = TD.keySet();
        Iterator<Set<Integer>> iterator = keySet.iterator();

        while(iterator.hasNext()) {
            Set<Integer> pattern = iterator.next();
            
            Comparator<Integer> comp1 = new Comparator<Integer>() {
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

            SortedSet<Integer> orderedItems = new TreeSet<>(comp1);
            Iterator<Integer> transactionIterator = pattern.iterator();

            while (transactionIterator.hasNext()) {
                int currentItem = transactionIterator.next();
                if (sortedOrder.containsKey(currentItem)) {
                    orderedItems.add(currentItem);
                }
            }
            sortedPatternBase.put(orderedItems, TD.get(pattern));
        }

        return sortedPatternBase;
    }

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