package server.service;

import message.Response;
import model.Auction;
import server.network.AuctionServer;
import server.repository.AuctionRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuctionTimeManager implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(AuctionTimeManager.class.getName());
    private static final String FINISHED_STATUS = "FINISHED";
    private static final long DEFAULT_CHECK_INTERVAL_SECONDS = 5;

    private final AuctionRepository auctionRepository;
    private final ScheduledExecutorService scheduler;
    private final long checkIntervalSeconds;
    private final AtomicBoolean started = new AtomicBoolean(false);

    public AuctionTimeManager() {
        this(new AuctionRepository(), DEFAULT_CHECK_INTERVAL_SECONDS);
    }

    public AuctionTimeManager(AuctionRepository auctionRepository, long checkIntervalSeconds) {
        if (auctionRepository == null) {
            throw new IllegalArgumentException("auctionRepository must not be null");
        }
        if (checkIntervalSeconds <= 0) {
            throw new IllegalArgumentException("checkIntervalSeconds must be greater than 0");
        }

        this.auctionRepository = auctionRepository;
        this.checkIntervalSeconds = checkIntervalSeconds;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "auction-time-manager");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }

        scheduler.scheduleAtFixedRate(
                this::closeExpiredAuctionsSafely,
                0,
                checkIntervalSeconds,
                TimeUnit.SECONDS
        );
    }

    public void stop() {
        if (!started.compareAndSet(true, false)) {
            return;
        }

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        stop();
    }

    public boolean isExpired(Auction auction) {
        return auction == null
                || auction.getEnd_time() == null
                || !LocalDateTime.now().isBefore(auction.getEnd_time());
    }

    public long getRemainingSeconds(Auction auction) {
        if (auction == null || auction.getEnd_time() == null) {
            return 0;
        }

        long seconds = Duration.between(LocalDateTime.now(), auction.getEnd_time()).getSeconds();
        return Math.max(seconds, 0);
    }

    private void closeExpiredAuctionsSafely() {
        try {
            closeExpiredAuctions();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while checking expired auctions", e);
        }
    }

    private void closeExpiredAuctions() {
        List<Auction> activeAuctions = auctionRepository.getActiveAuctions();

        for (Auction auction : activeAuctions) {
            if (!isExpired(auction)) {
                continue;
            }

            boolean updated = auctionRepository.updateStatus(auction.getId(), FINISHED_STATUS);
            if (updated) {
                auction.setStatus(FINISHED_STATUS);
                notifyAuctionFinished(auction);
                LOGGER.log(Level.INFO, "Auction {0} finished automatically.", auction.getId());
            }
        }
    }

    private void notifyAuctionFinished(Auction auction) {
        Response response = new Response(
                "AUCTION_END",
                auction,
                "Auction has ended."
        );
        AuctionServer.broadcast(response);
    }
}
