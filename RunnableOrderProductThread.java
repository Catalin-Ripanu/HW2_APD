import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.BrokenBarrierException;

/* Clasa care modeleaza logica thread-urilor de pe nivelul 2 */

public class RunnableOrderProductThread implements Runnable {

    private FileChannel channelProducts;
    private MappedByteBuffer prodThreadBuffer;
    private HelperObject helperObj;
    private int nrThreads;
    private CountDownLatch latch;
    private Semaphore semProd;
    private int id;
    private long fileProdSize;

    public RunnableOrderProductThread(HelperObject helperObject, int id,
            FileChannel channelProducts, MappedByteBuffer prodThreadBuffer, int nrThreads,
            CountDownLatch latch, Semaphore semProd, long fileProdSize) {
        this.channelProducts = channelProducts;
        this.helperObj = helperObject;
        this.id = id;
        this.prodThreadBuffer = prodThreadBuffer;
        this.nrThreads = nrThreads;
        this.latch = latch;
        this.semProd = semProd;
        this.fileProdSize = fileProdSize;
    }

    /* Partea in care fiecare thread copil isi populeaza propriul buffer */

    @Override
    public void run() {

        /* Se ignora thread-ul parinte cu buffer-ul gol */

        if (prodThreadBuffer == null) {
            latch.countDown();
        } else {
            try {
                if (id != nrThreads - 1) {
                    synchronized (helperObj) {
                        if (helperObj.getPosition() == fileProdSize)
                            helperObj.setElemBuffer(null, id);
                        else {
                            helperObj.setElemBuffer(
                                    channelProducts.map(FileChannel.MapMode.READ_ONLY, helperObj.getPosition(),
                                            helperObj.getDimension()),
                                    id);

                            /* Se corecteaza erorile din buffer */

                            Tema2.setNewBuffer(channelProducts, helperObj, id);
                        }
                    }
                    semProd.release();

                } else if (id == nrThreads - 1) {

                    /* Ultimul thread porneste dupa procesarea celorlalte thread-uri */

                    semProd.acquire();
                    if (helperObj.getPosition() == fileProdSize)
                        helperObj.setElemBuffer(null, id);
                    else

                    {
                        helperObj.setElemBuffer(
                                channelProducts.map(FileChannel.MapMode.READ_ONLY, helperObj.getPosition(),
                                        fileProdSize - helperObj.getPosition()),
                                id);
                    }
                }
            } catch (IOException exception) {
                exception.printStackTrace();
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }

            try

            {
                /* Se asteapta toate thread-urile */

                Tema2.barrier.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }

            /* Se transforma buffer-ul parintelui in String (MappingByteBuffer -> String) */

            String orderProd = "";
            String prodThread = "";
            Charset charset = Charset.forName("UTF-8");
            String[] helperNewLine;
            String[] helperProd;
            ByteBuffer auxByte = (ByteBuffer) prodThreadBuffer.duplicate();
            orderProd = charset.decode(auxByte).toString();
            if (helperObj.getBuffer().get(id) != null) {

                /* Se transforma buffer-ul copilului curent in String */

                auxByte = (ByteBuffer) helperObj.getBuffer().get(id).duplicate();
                prodThread = charset.decode(auxByte).toString();

                helperNewLine = orderProd.split("\n");
                helperProd = prodThread.split("\n");
                String currOrder;
                for (int i = 0; i < helperNewLine.length; i++) {
                    String[] helperOrderProd = helperNewLine[i].split(",");

                    /* Se obtine comanda curenta */

                    currOrder = helperOrderProd[0];

                    /* Comenzile cu 0 produse sunt ignorate */

                    if (Integer.parseInt(helperOrderProd[1]) != 0) {
                        for (int j = 0; j < helperProd.length; j++) {

                            /*
                             * Se verifica daca comanda asociata produsului este valida (exista si in
                             * orders.txt)
                             */

                            String[] finalHelper = helperProd[j].split(",");
                            if (finalHelper[0].equals(currOrder)) {
                                try {

                                    /* In caz afirmativ, se marcheaza produsul ca fiind "shipped" */

                                    Tema2.writerProd.write(finalHelper[0] + "," + finalHelper[1] + ",shipped\n");
                                } catch (IOException exception) {
                                    exception.printStackTrace();
                                }
                            }

                        }

                        /*
                         * Se marchiaza comanda curenta ca fiind "shipped" doar dupa ce toate produsele
                         * aferente au fost marcate
                         */

                        try {
                            if (id == 0)
                                Tema2.writerOrders
                                        .write(currOrder + "," + Integer.parseInt(helperOrderProd[1]) + ",shipped\n");
                        } catch (IOException exception) {
                            exception.printStackTrace();
                        }
                    }
                }

                /* Se decrementeaza latch-ul pentru eliberarea parintelui */

                latch.countDown();
            } else
                latch.countDown();
        }
    }
}