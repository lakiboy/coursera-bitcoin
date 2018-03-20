import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.*;
import java.util.concurrent.ThreadLocalRandom;

public class Runner {

    public static void main(String[] args)
    {
        Security.addProvider(new BouncyCastleProvider());

        KeyPairGenerator keyGen = getKeyGen();

        // Scrooge
        KeyPair pair = keyGen.generateKeyPair();
        PublicKey scroogeAddress = pair.getPublic();
        PrivateKey scroogeSecret = pair.getPrivate();

        // Bob
        pair = keyGen.generateKeyPair();
        PublicKey bobAddress = pair.getPublic();
        PrivateKey bobSecret = pair.getPrivate();

        // Alice
        pair = keyGen.generateKeyPair();
        PublicKey aliceAddress = pair.getPublic();
        PrivateKey aliceSecret = pair.getPrivate();

        UTXOPool pool = new UTXOPool();

        //
        // Root
        //

        Transaction root = new Transaction();
        root.setHash(getHash());

        // Initial coins.
        root.addOutput(3.2, scroogeAddress);
        root.addOutput(1.4, bobAddress);
        root.addOutput(7.1, aliceAddress);

        pool.addUTXO(new UTXO(root.getHash(), 0), root.getOutput(0));
        pool.addUTXO(new UTXO(root.getHash(), 1), root.getOutput(1));
        pool.addUTXO(new UTXO(root.getHash(), 2), root.getOutput(2));

        //
        // One
        //

        Transaction txOne = new Transaction();
        txOne.setHash(getHash());

        // Bob -> Scrooge
        txOne.addInput(root.getHash(), 1);
        txOne.addOutput(1.4, scroogeAddress);

        // Alice -> Scrooge
        txOne.addInput(root.getHash(), 2);
        txOne.addOutput(5.1, scroogeAddress);

        // Alice -> Alice (remainder)
        txOne.addOutput(2.0, aliceAddress);

        txOne.addSignature(signInput(txOne, 0, bobSecret), 0);
        txOne.addSignature(signInput(txOne, 1, aliceSecret), 1);

        /*
        pool.addUTXO(new UTXO(txOne.getHash(), 0), txOne.getOutput(0));
        pool.addUTXO(new UTXO(txOne.getHash(), 1), txOne.getOutput(1));
        pool.addUTXO(new UTXO(txOne.getHash(), 2), txOne.getOutput(2));
        */

        TxHandler handler = new TxHandler(pool);

        System.out.println(handler.isValidTx(txOne));

        System.out.println(handler.handleTxs(new Transaction[]{ root, txOne }).length);
    }

    private static byte[] getHash()
    {
        byte[] hash = new byte[32];

        (ThreadLocalRandom.current()).nextBytes(hash);

        return hash;
    }

    private static KeyPairGenerator getKeyGen()
    {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "BC");
            generator.initialize(1024, SecureRandom.getInstance("SHA1PRNG", "SUN"));

            return generator;
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            e.printStackTrace();
        }

        throw new RuntimeException();
    }

    private static byte[] signInput(Transaction tx, int index, PrivateKey secret)
    {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(secret);
            signature.update(tx.getRawDataToSign(index));

            return signature.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }

        throw new RuntimeException();
    }
}
