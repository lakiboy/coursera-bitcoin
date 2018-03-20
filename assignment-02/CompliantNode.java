import java.util.*;

public class CompliantNode implements Node {

    private int round = 0;
    private int totalRounds;
    private boolean[] followees;
    private Set<Transaction> transactions = new HashSet<>();
    private Map<Integer, Set<Transaction>> proposals = new HashMap<>();

    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
        totalRounds = numRounds;
    }

    public void setFollowees(boolean[] nodes) {
        followees = nodes;
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        transactions = pendingTransactions;
    }

    public Set<Transaction> sendToFollowers() {
        return transactions;
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
        followees = removeSilentFollowees(candidates);

        if (round > 0) {
            followees = removeInconsistentFollowees(candidates);
        }

        for (Candidate candidate : candidates) {
            if (!followees[candidate.sender]) {
                continue;
            }

            if (!transactions.contains(candidate.tx)) {
                transactions.add(candidate.tx);
            }
        }

        collectProposals(candidates, proposals);

        round++;
    }

    private boolean[] removeSilentFollowees(Set<Candidate> candidates)
    {
        boolean[] nodes = new boolean[followees.length];

        for (Candidate candidate : candidates) {
            if (followees[candidate.sender]) {
                nodes[candidate.sender] = true;
            }
        }

        return nodes;
    }

    private boolean[] removeInconsistentFollowees(Set<Candidate> candidates)
    {
        Map<Integer, Set<Transaction>> newProposals = new HashMap<>();

        collectProposals(candidates, newProposals);

        boolean[] nodes = new boolean[followees.length];

        for (Map.Entry<Integer, Set<Transaction>> entry : newProposals.entrySet()) {
            Set<Transaction> newTransactions = newProposals.get(entry.getKey());
            Set<Transaction> oldTransactions = proposals.get(entry.getKey());

            newTransactions.retainAll(oldTransactions);

            if (newTransactions.size() >= oldTransactions.size()) {
                nodes[entry.getKey()] = true;
            }
        }

        return nodes;
    }

    private void collectProposals(Set<Candidate> candidates, Map<Integer, Set<Transaction>> transactions)
    {
        for (Candidate candidate : candidates) {
            if (followees[candidate.sender]) {
                if (!transactions.containsKey(candidate.sender)) {
                    transactions.put(candidate.sender, new HashSet<>());
                }

                if (!transactions.get(candidate.sender).contains(candidate.tx)) {
                    transactions.get(candidate.sender).add(candidate.tx);
                }
            }
        }
    }
}
