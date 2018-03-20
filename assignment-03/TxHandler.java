import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TxHandler {
    private UTXOPool pool;

    public TxHandler(UTXOPool utxoPool) {
        pool = new UTXOPool(utxoPool);
    }

    public UTXOPool getUTXOPool() {
        return pool;
    }

    public boolean isValidTx(Transaction tx) {
        double totalInput = 0, totalOutput = 0;

        // 4 - All of output values are non-negative.
        for (Transaction.Output output : tx.getOutputs()) {
            if (output.value < 0) {
                return false;
            }

            totalOutput += output.value;
        }

        Set<UTXO> used = new HashSet<>();

        for (int index = 0, total = tx.numInputs(); index < total; index++) {
            Transaction.Input input = tx.getInput(index);
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);

            // 1 - All outputs claimed by tx are in the current UTXO pool.
            if (!pool.contains(utxo)) {
                return false;
            }

            // 3 - No UTXO is claimed multiple times.
            if (used.contains(utxo)) {
                return false;
            }

            // Ignore unsigned inputs.
            if (input.signature == null) {
                return false;
            }

            Transaction.Output output = pool.getTxOutput(utxo);

            // 2 - All the signatures on each input are valid.
            if (!Crypto.verifySignature(output.address, tx.getRawDataToSign(index), input.signature)) {
                return false;
            }

            totalInput += output.value;
            used.add(utxo);
        }

        // 5 - The sum of input values is greater than or equal to the sum of its output.
        return totalInput >= totalOutput;
    }

    public Transaction[] handleTxs(Transaction[] possibleTxs)
    {
        ArrayList<Transaction> txs = new ArrayList<>();

        for (Transaction tx : Arrays.asList(possibleTxs)) {
            if (!isValidTx(tx)) {
                continue;
            }

            for (Transaction.Input input : tx.getInputs()) {
                pool.removeUTXO(new UTXO(input.prevTxHash, input.outputIndex));
            }

            for (int index = 0, total = tx.numOutputs(); index < total; index++) {
                pool.addUTXO(new UTXO(tx.getHash(), index), tx.getOutput(index));
            }

            txs.add(tx);
        }

        Transaction[] result = new Transaction[txs.size()];

        txs.toArray(result);

        return result;
    }
}