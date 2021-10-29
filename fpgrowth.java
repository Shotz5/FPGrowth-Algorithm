import java.util.*;
import java.io.*;
import java.util.Map.*;

public class fpgrowth {
    public static int NUMBER_OF_TRANSACTIONS = 0;
    public static void main(String[] args) throws FileNotFoundException {
    
        // Input source code taken from Assignment 1 due to the similar logic
        File dataSet;
        int min_sup_percentage;
        float min_sup_float;
        int min_sup;

        if (args.length != 2) {
            System.out.println("Incorrect args! Exiting");
            return;
        } else {
            dataSet = new File(args[0]);
            min_sup_percentage = Integer.parseInt(args[1]);
            if (min_sup_percentage > 100 || min_sup_percentage < 0) {
                System.out.println("Cannot have more than 100% or less than 0% min_sup. Exiting.");
            }
        }

        Scanner input = new Scanner(dataSet);
        NUMBER_OF_TRANSACTIONS = Integer.parseInt(input.nextLine());

        min_sup_float = NUMBER_OF_TRANSACTIONS * (min_sup_percentage / 100f);
        min_sup = Math.round(min_sup_float);

        Map<Integer, Set<Integer>> transactionDB = new HashMap<Integer, Set<Integer>>();
        while(input.hasNextLine()) {
            String transaction = input.nextLine();
            String[] transactionSplit = transaction.split("\\s"); // Split on every whitespace character (tab or space)
            int transID = Integer.parseInt(transactionSplit[0]); // TransactionID will always be the first value
            // transactionSplit[1] not needed (size of transaction)
            Set<Integer> transItems = new HashSet<>();
            for (int i = 2; i < transactionSplit.length; i++) {
                transItems.add(Integer.parseInt(transactionSplit[i]));
            }
            transactionDB.put(transID, transItems);
        }

        input.close();

        //long startTime = System.currentTimeMillis();
        /*Map<Set<Integer>, Integer> fps = */fpgrowth(transactionDB, min_sup);
        //long totalTime = System.currentTimeMillis() - startTime;
        //System.out.println("Found " + fps.size() + " FPs and executed in " + totalTime + " milliseconds");

        //outputToFile(fps);
    }

    public static /*Map<Set<Integer>, Integer>*/ void fpgrowth(Map<Integer, Set<Integer>> TD, int min_sup) {
        Map<Integer, Integer> itemset = new LinkedHashMap<Integer, Integer>();

        Set<Integer> keys = TD.keySet();
        Iterator<Integer> transiterator = keys.iterator();

        // Iterate through the transaction database
        // Count all the occurances of each item in the database and either add one to the current value or inset it into the table
        // Advantage of adding as we go along is that we don't have memory allocated for "0s" that will get pruned later
        // As well it's less to iterate through when we do begin pruning
        // Sourced from Assignment 1
        while (transiterator.hasNext()) {
            int hashKey = transiterator.next();
            Set<Integer> transaction = TD.get(hashKey);

            Iterator<Integer> itemIter = transaction.iterator();

            while(itemIter.hasNext()) {
                int itemID = itemIter.next();
                if (itemset.containsKey(itemID)) {
                    itemset.put(itemID, (itemset.get(itemID) + 1));
                } else {
                    itemset.put(itemID, 1);
                }
            }
        }

        // Sorts shit I guess
        List<Entry<Integer, Integer>> sortedList = new ArrayList<Entry<Integer, Integer>>(itemset.entrySet());

        Comparator<Entry<Integer, Integer>> comp = new Comparator<Entry<Integer,Integer>>() {
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

        Collections.sort(sortedList, comp);

        // Now I've gotta sort the items by occurance in the tdb
        Map<Integer, SortedSet<Integer>> newTD = new LinkedHashMap<Integer, SortedSet<Integer>>();

        keys = TD.keySet();
        transiterator = keys.iterator();

        while (transiterator.hasNext()) {
            int hashKey = transiterator.next();
            Set<Integer> transactionItems = TD.get(hashKey);

            Comparator<Integer> comp1 = new Comparator<Integer>() {
                public int compare(Integer a, Integer b) {
                    for (int i = 0; i < sortedList.size(); i++) {
                        if (sortedList.get(i).getKey() == b) {
                            return 1;
                        } else if (sortedList.get(i).getKey() == a) {
                            return -1;
                        }
                    }
                    return 0;
                }
            };

            SortedSet<Integer> orderedItems = new TreeSet<>(comp1);
            orderedItems.addAll(transactionItems);
            newTD.put(hashKey, orderedItems);
        }

        System.out.println(newTD);

        // TDB now sorted
        // Time to build the stupid node thing... this fucking sucks
        Node root = new Node(-1);

        keys = newTD.keySet();
        transiterator = keys.iterator();

        while (transiterator.hasNext()) {
            int hashKey = transiterator.next();
            SortedSet<Integer> transaction = newTD.get(hashKey);

            Iterator<Integer> itemIter = transaction.iterator();
            Node currentNode = root;
            ArrayList<Node> nextNodes= root.getNext();

            while(itemIter.hasNext()) {
                int itemID = itemIter.next();
                boolean makeNewNode = true;
                for (int i = 0; i < nextNodes.size(); i++) {
                    // Node already exists, just increment
                    if (nextNodes.get(i).getId() == itemID) {
                        makeNewNode = false;
                        nextNodes.get(i).incrementCount(1);
                        currentNode = nextNodes.get(i);
                        nextNodes = currentNode.getNext();
                    }
                }
                // Node doesn't exist, add it.
                if (makeNewNode) {
                    Node newNode = new Node(itemID);
                    currentNode.setNext(newNode);
                    currentNode = newNode;
                    nextNodes = currentNode.getNext();

                    // Have to add a pointer for the new node to the ArrayList so that we can backtrack later
                }
            }
        }
    }
}