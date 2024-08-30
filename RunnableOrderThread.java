import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.nio.MappedByteBuffer;
import java.util.Vector;

/* Clasa care modeleaza logica thread-urilor de nivel 1 */

public class RunnableOrderThread implements Runnable {

    private FileChannel channelOrders;
    private FileChannel channelProducts;
    private HelperObject helperObj;
    private long fileSize;
    private int nrThreads;
    private int id;
    private long fileProdSize;
    private long dimensionProd;

    public RunnableOrderThread(FileChannel channelOrders, HelperObject helperObj, long fileSize, int nrThreads,
            int id, FileChannel channelProducts, long fileProdSize, long dimensionProd) {

        this.channelOrders = channelOrders;
        this.channelProducts = channelProducts;
        this.helperObj = helperObj;
        this.fileSize = fileSize;
        this.nrThreads = nrThreads;
        this.id = id;
        this.fileProdSize = fileProdSize;
        this.dimensionProd = dimensionProd;
    }

    @Override
    public void run() {

        /* Partea in care fiecare thread parinte isi populeaza propriul buffer */

        try {
            if (id != nrThreads - 1) {
                synchronized (helperObj) {
                    if (helperObj.getPosition() == fileSize)
                        helperObj.setElemBuffer(null, id);
                    else {
                        if (helperObj.getPosition() + (int) helperObj.getDimension() > fileSize)
                            helperObj.setElemBuffer(null, id);
                        else {
                            helperObj.setElemBuffer(
                                    channelOrders.map(FileChannel.MapMode.READ_ONLY, helperObj.getPosition(),
                                            helperObj.getDimension()),
                                    id);

                            /* Se corecteaza erorile din buffer */

                            Tema2.setNewBuffer(channelOrders, helperObj, id);
                        }
                    }
                }
                Tema2.sem.release();

            } else if (id == nrThreads - 1) {

                /* Ultimul thread porneste dupa procesarea celorlalte thread-uri */

                Tema2.sem.acquire();
                if (helperObj.getPosition() == fileSize)
                    helperObj.setElemBuffer(null, id);
                else

                {
                    helperObj.setElemBuffer(
                            channelOrders.map(FileChannel.MapMode.READ_ONLY, helperObj.getPosition(),
                                    fileSize - helperObj.getPosition()),
                            id);
                }
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        } catch (InterruptedException exception) {
            exception.printStackTrace();
        }

        /* Se asteapta toate thread-urile */

        try {
            Tema2.barrier.await();
            Tema2.barrier.reset();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }

        /*
         * Se foloseste un latch astfel incat nrThread-uri sa fie invocate de fiecare
         * thread parinte
         */
        CountDownLatch latch = new CountDownLatch(nrThreads);

        /*
         * Fiecare thread de pe nivel 2 va avea propriul buffer pentru stocarea
         * chunk-ului din fisierul de produse
         */

        Vector<MappedByteBuffer> vecBuffer = new Vector<>();
        vecBuffer.setSize(nrThreads);
        HelperObject helperThread = new HelperObject(vecBuffer, 0, 0);
        helperThread.setDimension(dimensionProd);
        helperThread.setPosition(0);

        Semaphore semProd = new Semaphore(-(nrThreads - 2));

        /* Se executa thread-urile de pe nivelul 2 */

        for (int i = 0; i < nrThreads; i++) {
            Tema2.tpe.execute(new RunnableOrderProductThread(helperThread, i, channelProducts,
                    helperObj.getBuffer().get(id), nrThreads, latch, semProd, fileProdSize));
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}