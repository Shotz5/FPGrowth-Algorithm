import java.util.*;
import java.io.*;
import java.util.Map.*;

public class fpgrowth {
    public static int MIN_SUP;
    public static Node ROOT = new Node(-1);
    public static Map<Integer, Integer> countedTransItems = new LinkedHashMap<>();
    public static void main(String[] args) throws FileNotFoundException {

        if (args.length != 2) {
            System.out.println("Incorrect args! Exiting");
            return;
        } else {
            inputFromFile(args[0], args[1], new LinkedHashMap<>());
        }
        
        Map<Integer, Integer> sortedItemMap = sortCountedItems(false);
        Map<Integer, Integer> reverseSortedMap = sortCountedItems(true);
        Map<Integer, ArrayList<Node>> nodePointer = inputFromFile(args[0], args[1], sortedItemMap);
        Map<Integer, Map<Set<Integer>, Integer>> patternBases = findTraversals(reverseSortedMap, nodePointer);
        Map<Integer, Map<Integer, Integer>> conditionalTrees = findConditionalTrees(patternBases);
        Map<Set<Integer>, Integer> FPs = parseConditionalTrees(conditionalTrees);

        System.out.println(FPs);
        //long startTime = System.currentTimeMillis();
        /*Map<Set<Integer>, Integer> fps = *///fpgrowth(transactionDB, min_sup);
        //long totalTime = System.currentTimeMillis() - startTime;
        //System.out.println("Found " + fps.size() + " FPs and executed in " + totalTime + " milliseconds");

        //outputToFile(fps);
    }

    public static Map<Integer, ArrayList<Node>> inputFromFile(String fileName, String minSupPercentage, Map<Integer, Integer> sortedOrder) throws FileNotFoundException {
        File dataSet = new File(fileName);
        Map<Integer, ArrayList<Node>> nodePointer = new LinkedHashMap<Integer, ArrayList<Node>>();

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
            if (counter % 1000 == 0) {
                if (sortedOrder.isEmpty()) {
                    countItems(transactionDB);
                } else {
                    Map<Integer, SortedSet<Integer>> sortedTDs = sortTransactions(transactionDB, sortedOrder);
                    nodePointer.putAll(FPTree(sortedTDs));
                }
                transactionDB = new HashMap<Integer, Set<Integer>>();
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

        if (sortedOrder.isEmpty()) {
            countItems(transactionDB);
            trimMinSup(MIN_SUP);
        } else {
            Map<Integer, SortedSet<Integer>> sortedTDs = sortTransactions(transactionDB, sortedOrder);
            nodePointer.putAll(FPTree(sortedTDs));
        }

        return nodePointer;
    }

    public static void countItems(Map<Integer, Set<Integer>> TD) {
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
    }

    public static void trimMinSup(int min_sup) {
        Set<Integer> keys = countedTransItems.keySet();
        Iterator<Integer> itemsetIterator = keys.iterator();

        while (itemsetIterator.hasNext()) {
            int hashKey = itemsetIterator.next();
            if (countedTransItems.get(hashKey) < min_sup) {
                itemsetIterator.remove();
            }
        }
        return;
    }

    public static Map<Integer, Integer> sortCountedItems(boolean reverseSort) {
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

    public static Map<Integer, ArrayList<Node>> FPTree(Map<Integer, SortedSet<Integer>> TD) {
        Map<Integer, ArrayList<Node>> nodePointer = new LinkedHashMap<Integer, ArrayList<Node>>();
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

                    if (nodePointer.get(itemID) != null) {
                        nodePointer.get(itemID).add(newNode);
                    } else {
                        ArrayList<Node> pointerList = new ArrayList<>();
                        pointerList.add(newNode);
                        nodePointer.put(itemID, pointerList);
                    }
                }
            }
        }
        return nodePointer;
    }

    public static Map<Integer, Map<Set<Integer>, Integer>> findTraversals(Map<Integer, Integer> reverseSortedMap, Map<Integer, ArrayList<Node>> nodePointer) {
        // Find all the traversals
        Map<Integer, Map<Set<Integer>, Integer>> patternBases = new LinkedHashMap<Integer, Map<Set<Integer>, Integer>>();

        Set<Integer> keys = reverseSortedMap.keySet();
        Iterator<Integer> transiterator = keys.iterator();

        while (transiterator.hasNext()) {
            int hashKey = transiterator.next();
            ArrayList<Node> idNodes = nodePointer.get(hashKey);
            int itemTraversed = -1;
            Map<Set<Integer>, Integer> itemTraversals = new LinkedHashMap<Set<Integer>, Integer>();

            // For all the nodes that it points to
            for (int i = 0; i < idNodes.size(); i++) {
                Node currentNode = idNodes.get(i);
                itemTraversed = currentNode.getId();
                int count = currentNode.getCount();
                Set<Integer> traversal = new HashSet<Integer>();
                // While I'm not at the root node
                while(currentNode.getPrevious().getId() != -1) {
                    traversal.add(currentNode.getPrevious().getId());
                    currentNode = currentNode.getPrevious();
                }
                if (traversal.size() != 0) {
                    itemTraversals.put(traversal, count);
                }
            }

            if (itemTraversed != -1) {
                patternBases.put(itemTraversed, itemTraversals);
            } else {
                System.out.println("Something went wrong... exiting...");
                System.exit(0);
            }
        }
        return patternBases;
    }

    public static Map<Integer, Map<Integer, Integer>> findConditionalTrees(Map<Integer, Map<Set<Integer>, Integer>> patternBases) {
        Map<Integer, Map<Integer, Integer>> conditionalTrees = new LinkedHashMap<>();

        // Find the conditional pattern bases
        Set<Integer> keys = patternBases.keySet();
        Iterator<Integer> transiterator = keys.iterator();
        

        while (transiterator.hasNext()) {
            int fpItem = transiterator.next(); // a
            Map<Set<Integer>, Integer> traversals = patternBases.get(fpItem); // {c:1} {bce:1}
            Set<Set<Integer>> newKeys = traversals.keySet(); // {[c], [bce]}
            Iterator<Set<Integer>> keyIter = newKeys.iterator();

            Map<Integer, Integer> itemCounts = new HashMap<Integer, Integer>();

            // Generate the conditional trees
            while (keyIter.hasNext()) {
                Set<Integer> traversal = keyIter.next();
                Iterator<Integer> travIter = traversal.iterator();
                while(travIter.hasNext()) {
                    int thisItem = travIter.next();
                    if (!itemCounts.containsKey(thisItem)) {
                        itemCounts.put(thisItem, traversals.get(traversal));
                    } else {
                        itemCounts.put(thisItem, (itemCounts.get(thisItem) + traversals.get(traversal)));
                    }
                }
            }

            Set<Integer> countKeys = itemCounts.keySet();
            Iterator<Integer> countIter = countKeys.iterator();

            // Remove the ones that don't meet min_sup
            while(countIter.hasNext()) {
                int itemCounter = countIter.next();
                if (itemCounts.get(itemCounter) < MIN_SUP) {
                    countIter.remove();
                }
            }

            conditionalTrees.put(fpItem, itemCounts);
        }
        return conditionalTrees;
    }

    public static Map<Set<Integer>, Integer> parseConditionalTrees(Map<Integer, Map<Integer, Integer>> conditionalTrees) {
        Map<Set<Integer>, Integer> FPs = new HashMap<>();
        // Parse the conditionalTrees to get the final FPs
        Set<Integer> treeKeys = conditionalTrees.keySet();
        Iterator<Integer> treeIter = treeKeys.iterator();

        while (treeIter.hasNext()) {
            int fpItem = treeIter.next();
            Map<Integer, Integer> currentTree = conditionalTrees.get(fpItem);

            Set<Integer> currentTreeKeys = currentTree.keySet();
            Set<Set<Integer>> powerSet = powerSet(currentTreeKeys);

            Iterator<Set<Integer>> powerSetIter = powerSet.iterator();
            while (powerSetIter.hasNext()) {
                Set<Integer> currentPossibleFP = powerSetIter.next();
                Iterator<Integer> FPValueIterator = currentPossibleFP.iterator();
                int lowestCommonValue = Integer.MAX_VALUE;
                while(FPValueIterator.hasNext()) {
                    int currentValue = FPValueIterator.next();
                    if (lowestCommonValue > currentTree.get(currentValue)) {
                        lowestCommonValue = currentTree.get(currentValue);
                    }
                }
                if (currentPossibleFP.isEmpty()) {
                    lowestCommonValue = countedTransItems.get(fpItem);
                }
                currentPossibleFP.add(fpItem);
                if (lowestCommonValue >= MIN_SUP) {
                    FPs.put(currentPossibleFP, lowestCommonValue);
                }
            }
        }
        return FPs;
    }

    public static Set<Set<Integer>> powerSet(Set<Integer> inSet) {
        Set<Set<Integer>> sets = new HashSet<>();
        if (inSet.isEmpty()) {
            sets.add(new HashSet<>());
            return sets;
        }
        List<Integer> arrayList = new ArrayList<>(inSet);
        int first = arrayList.get(0);
        Set<Integer> leftOver = new HashSet<>(arrayList.subList(1, arrayList.size()));
        for (Set<Integer> set : powerSet(leftOver)) {
            Set<Integer> newSet = new HashSet<>();
            newSet.add(first);
            newSet.addAll(set);
            sets.add(newSet);
            sets.add(set);
        }
        return sets;
    }
}