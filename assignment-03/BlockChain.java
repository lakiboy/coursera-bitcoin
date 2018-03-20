import java.util.*;

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;

    // Block height.
    private Block tail;
    private int maxHeight = 0;

    private TransactionPool transactions = new TransactionPool();

    private Map<ByteArrayWrapper, UTXOPool> utxoPools = new HashMap<>();
    private Map<ByteArrayWrapper, Integer> history = new HashMap<>();

    public BlockChain(Block genesisBlock) {
        recordBlock(genesisBlock, new UTXOPool(), 1);
    }

    public void addTransaction(Transaction tx) {
        transactions.addTransaction(tx);
    }

    public TransactionPool getTransactionPool() {
        return transactions;
    }

    public Block getMaxHeightBlock() {
        return tail;
    }

    public UTXOPool getMaxHeightUTXOPool() {
        return utxoPools.get(new ByteArrayWrapper(tail.getHash()));
    }

    public boolean addBlock(Block block) {

        // Ignore genesis block.
        if (block.getPrevBlockHash() == null) {
            return false;
        }

        ByteArrayWrapper prevBlockId = new ByteArrayWrapper(block.getPrevBlockHash());

        // Ignore block from the future.
        if (!history.containsKey(prevBlockId)) {
            return false;
        }

        // Incoming block height.
        final int height = history.get(prevBlockId) + 1;

        if (height <= maxHeight - CUT_OFF_AGE) {
            return false;
        }

        // Convert to array transaction.
        Transaction[] minedTxs = new Transaction[block.getTransactions().size()];
        block.getTransactions().toArray(minedTxs);

        TxHandler txHandler = new TxHandler(utxoPools.get(prevBlockId));
        Transaction[] resultTxs = txHandler.handleTxs(minedTxs);

        // All transactions in block must be valid.
        if (resultTxs.length != minedTxs.length) {
            return false;
        }

        // Remove block transactions from global pool.
        for (Transaction tx : minedTxs) {
            transactions.removeTransaction(tx.getHash());
        }

        recordBlock(block, txHandler.getUTXOPool(), height);

        return true;
    }

    private void recordBlock(Block block, UTXOPool utxoPool, int height) {
        ByteArrayWrapper blockId = new ByteArrayWrapper(block.getHash());

        history.put(blockId, height);
        utxoPools.put(blockId, utxoPool);

        utxoPool.addUTXO(new UTXO(block.getCoinbase().getHash(), 0), block.getCoinbase().getOutput(0));

        if (height > maxHeight) {
            maxHeight = height;
            tail = block;
        }
    }
}
